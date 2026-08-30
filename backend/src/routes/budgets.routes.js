const express = require("express");
const crypto = require("crypto");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { budgetSchema } = require("../domain/validators");
const { utilizationPct, thresholdFor } = require("../domain/financeEngine");

const router = express.Router();
router.use(requireAuth);

function loadBudgetWithAllocations(budgetId) {
  const budget = db.prepare("SELECT * FROM budgets WHERE id = ?").get(budgetId);
  if (!budget) return null;
  const allocations = db.prepare(
    `SELECT ba.*, c.name as category_name, c.icon as category_icon
     FROM budget_allocations ba JOIN categories c ON c.id = ba.category_id
     WHERE ba.budget_id = ?`
  ).all(budgetId);
  return { ...budget, allocations };
}

// GET /api/budgets?year=2026&month=8
router.get("/", (req, res) => {
  const { year, month } = req.query;
  let row;
  if (year && month) {
    row = db.prepare("SELECT * FROM budgets WHERE user_id = ? AND period_year = ? AND period_month = ?").get(req.userId, Number(year), Number(month));
  } else {
    row = db.prepare("SELECT * FROM budgets WHERE user_id = ? ORDER BY period_year DESC, period_month DESC LIMIT 1").get(req.userId);
  }
  if (!row) return res.json({ budget: null });
  res.json({ budget: loadBudgetWithAllocations(row.id) });
});

// POST /api/budgets
router.post("/", validateBody(budgetSchema), (req, res) => {
  const v = req.validated;
  const totalBudget = v.allocations.reduce((s, a) => s + a.allocatedAmount, 0);

  if (totalBudget > v.incomeAmount && !v.confirmExceedsIncome) {
    return res.status(409).json({
      error: "Allocated budget exceeds income. Resubmit with confirmExceedsIncome=true to proceed.",
      totalBudget, income: v.incomeAmount,
    });
  }

  const existing = db.prepare("SELECT id FROM budgets WHERE user_id = ? AND period_year = ? AND period_month = ?").get(req.userId, v.periodYear, v.periodMonth);
  if (existing) return res.status(409).json({ error: "A budget for this month already exists. Use PATCH to edit it." });

  const id = crypto.randomUUID();
  const tx = db.transaction(() => {
    db.prepare(
      "INSERT INTO budgets (id, user_id, period_month, period_year, income_amount, total_budget) VALUES (?, ?, ?, ?, ?, ?)"
    ).run(id, req.userId, v.periodMonth, v.periodYear, v.incomeAmount, totalBudget);
    const insertAlloc = db.prepare(
      "INSERT INTO budget_allocations (id, budget_id, category_id, allocated_amount) VALUES (?, ?, ?, ?)"
    );
    v.allocations.forEach((a) => insertAlloc.run(crypto.randomUUID(), id, a.categoryId, a.allocatedAmount));
  });
  tx();

  res.status(201).json({ budget: loadBudgetWithAllocations(id) });
});

// PATCH /api/budgets/:id
router.patch("/:id", validateBody(budgetSchema.partial()), (req, res) => {
  const existing = db.prepare("SELECT * FROM budgets WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!existing) return res.status(404).json({ error: "Budget not found." });

  const v = req.validated;
  const income = v.incomeAmount ?? existing.income_amount;

  if (v.allocations) {
    const totalBudget = v.allocations.reduce((s, a) => s + a.allocatedAmount, 0);
    if (totalBudget > income && !v.confirmExceedsIncome) {
      return res.status(409).json({ error: "Allocated budget exceeds income. Resubmit with confirmExceedsIncome=true to proceed.", totalBudget, income });
    }
    const tx = db.transaction(() => {
      db.prepare("DELETE FROM budget_allocations WHERE budget_id = ?").run(req.params.id);
      const insertAlloc = db.prepare("INSERT INTO budget_allocations (id, budget_id, category_id, allocated_amount) VALUES (?, ?, ?, ?)");
      v.allocations.forEach((a) => insertAlloc.run(crypto.randomUUID(), req.params.id, a.categoryId, a.allocatedAmount));
      db.prepare("UPDATE budgets SET income_amount=?, total_budget=?, updated_at=datetime('now') WHERE id=?").run(income, totalBudget, req.params.id);
    });
    tx();
  } else if (v.incomeAmount !== undefined) {
    db.prepare("UPDATE budgets SET income_amount=?, updated_at=datetime('now') WHERE id=?").run(income, req.params.id);
  }

  res.json({ budget: loadBudgetWithAllocations(req.params.id) });
});

// DELETE /api/budgets/:id
router.delete("/:id", (req, res) => {
  const existing = db.prepare("SELECT * FROM budgets WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!existing) return res.status(404).json({ error: "Budget not found." });
  const tx = db.transaction(() => {
    db.prepare("DELETE FROM budget_allocations WHERE budget_id = ?").run(req.params.id);
    db.prepare("DELETE FROM budgets WHERE id = ?").run(req.params.id);
  });
  tx();
  res.json({ success: true });
});

// POST /api/budgets/copy-last-month  { periodYear, periodMonth }
router.post("/copy-last-month", (req, res) => {
  const { periodYear, periodMonth } = req.body || {};
  if (!Number.isInteger(periodYear) || !Number.isInteger(periodMonth) || periodMonth < 1 || periodMonth > 12) {
    return res.status(400).json({ error: "periodYear and periodMonth (1-12) are required." });
  }
  const already = db.prepare("SELECT id FROM budgets WHERE user_id = ? AND period_year = ? AND period_month = ?").get(req.userId, periodYear, periodMonth);
  if (already) return res.status(409).json({ error: "A budget already exists for this month." });

  const previous = db.prepare(
    `SELECT * FROM budgets WHERE user_id = ? AND (period_year < ? OR (period_year = ? AND period_month < ?))
     ORDER BY period_year DESC, period_month DESC LIMIT 1`
  ).get(req.userId, periodYear, periodYear, periodMonth);

  if (!previous) return res.status(404).json({ error: "No previous budget found to copy." });

  const prevAllocations = db.prepare("SELECT * FROM budget_allocations WHERE budget_id = ?").all(previous.id);

  // Independent IDs, new month — the previous month's records are never touched.
  const id = crypto.randomUUID();
  const tx = db.transaction(() => {
    db.prepare(
      "INSERT INTO budgets (id, user_id, period_month, period_year, income_amount, total_budget) VALUES (?, ?, ?, ?, ?, ?)"
    ).run(id, req.userId, periodMonth, periodYear, previous.income_amount, previous.total_budget);
    const insertAlloc = db.prepare("INSERT INTO budget_allocations (id, budget_id, category_id, allocated_amount) VALUES (?, ?, ?, ?)");
    prevAllocations.forEach((a) => insertAlloc.run(crypto.randomUUID(), id, a.category_id, a.allocated_amount));
  });
  tx();

  res.status(201).json({ budget: loadBudgetWithAllocations(id), copiedFrom: { year: previous.period_year, month: previous.period_month } });
});

// GET /api/budgets/:id/utilization — authoritative server-computed utilization per category
router.get("/:id/utilization", (req, res) => {
  const budget = loadBudgetWithAllocations(req.params.id);
  if (!budget || budget.user_id !== req.userId) return res.status(404).json({ error: "Budget not found." });

  const monthStr = `${budget.period_year}-${String(budget.period_month).padStart(2, "0")}`;
  const spentRows = db.prepare(
    `SELECT category_id, SUM(amount) as spent FROM transactions
     WHERE user_id = ? AND status = 'ACTIVE' AND transaction_type = 'EXPENSE' AND transaction_date LIKE ?
     GROUP BY category_id`
  ).all(req.userId, `${monthStr}%`);
  const spentMap = Object.fromEntries(spentRows.map((r) => [r.category_id, r.spent]));

  const result = budget.allocations.map((a) => {
    const spent = spentMap[a.category_id] || 0;
    const pct = utilizationPct(spent, a.allocated_amount);
    return {
      categoryId: a.category_id, categoryName: a.category_name,
      allocated: a.allocated_amount, spent, utilizationPct: pct, threshold: thresholdFor(pct),
    };
  });

  res.json({ utilization: result });
});

module.exports = router;
