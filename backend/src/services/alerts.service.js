const crypto = require("crypto");
const db = require("../db");
const { utilizationPct, thresholdFor } = require("../domain/financeEngine");

const MESSAGES = {
  50: (name, pct) => `You have used ${pct.toFixed(0)}% of your ${name} budget.`,
  75: (name) => `You have used 75% of your ${name} budget.`,
  90: (name) => `You have almost reached your ${name} budget.`,
  100: (name) => `You have reached your ${name} budget limit.`,
  EXCEEDED: (name, _pct, over) => `You exceeded your ${name} budget by ₹${over.toLocaleString("en-IN")}.`,
};

// Evaluates budget thresholds for the category touched by a transaction and
// creates at most one alert per (budget, category, threshold, period) —
// enforced at the DB layer via the alerts table's UNIQUE constraint, so this
// is race-safe even under concurrent writes, not just logic-level dedup.
function evaluateThresholds(userId, categoryId, transactionDate) {
  if (!categoryId) return [];
  const [year, month] = transactionDate.split("-").map(Number);

  const budget = db.prepare("SELECT * FROM budgets WHERE user_id = ? AND period_year = ? AND period_month = ?").get(userId, year, month);
  if (!budget) return [];

  const alloc = db.prepare("SELECT * FROM budget_allocations WHERE budget_id = ? AND category_id = ?").get(budget.id, categoryId);
  if (!alloc) return [];

  const cat = db.prepare("SELECT * FROM categories WHERE id = ?").get(categoryId);
  const monthStr = `${year}-${String(month).padStart(2, "0")}`;
  const spentRow = db.prepare(
    `SELECT SUM(amount) as spent FROM transactions
     WHERE user_id = ? AND category_id = ? AND status = 'ACTIVE' AND transaction_type = 'EXPENSE' AND transaction_date LIKE ?`
  ).get(userId, categoryId, `${monthStr}%`);
  const spent = spentRow.spent || 0;
  const pct = utilizationPct(spent, alloc.allocated_amount);
  const threshold = thresholdFor(pct);
  if (!threshold) return [];

  const prefs = db.prepare("SELECT * FROM notification_preferences WHERE user_id = ?").get(userId);
  if (prefs && !prefs.master_enabled) return [];
  const prefKey = threshold === "EXCEEDED" ? "threshold_exceeded" : `threshold_${threshold}`;
  if (prefs && prefs[prefKey] === 0) return [];

  const message = threshold === "EXCEEDED"
    ? MESSAGES.EXCEEDED(cat.name, pct, spent - alloc.allocated_amount)
    : MESSAGES[threshold](cat.name, pct);

  try {
    db.prepare(
      `INSERT INTO alerts (id, user_id, alert_type, threshold, message, related_budget_id, category_id, period_month, period_year)
       VALUES (?, ?, 'BUDGET_THRESHOLD', ?, ?, ?, ?, ?, ?)`
    ).run(crypto.randomUUID(), userId, threshold, message, budget.id, categoryId, month, year);

    db.prepare(
      `INSERT OR IGNORE INTO budget_notification_history (id, user_id, budget_id, budget_month, threshold) VALUES (?, ?, ?, ?, ?)`
    ).run(crypto.randomUUID(), userId, budget.id, monthStr, threshold);

    return [{ threshold, message }];
  } catch (err) {
    // UNIQUE constraint violation = this threshold already fired for this period. Not an error.
    if (String(err.message).includes("UNIQUE")) return [];
    throw err;
  }
}

module.exports = { evaluateThresholds };
