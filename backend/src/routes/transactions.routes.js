const express = require("express");
const crypto = require("crypto");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { transactionSchema, transactionUpdateSchema } = require("../domain/validators");
const { enqueueSync } = require("../services/sync.service");
const { evaluateThresholds } = require("../services/alerts.service");

const router = express.Router();
router.use(requireAuth);

// GET /api/transactions?month=YYYY-MM&status=ACTIVE
router.get("/", (req, res) => {
  const { month, status } = req.query;
  let sql = "SELECT * FROM transactions WHERE user_id = ?";
  const params = [req.userId];
  if (month && /^\d{4}-\d{2}$/.test(month)) {
    sql += " AND transaction_date LIKE ?";
    params.push(`${month}%`);
  }
  if (status && ["ACTIVE", "VOIDED"].includes(status)) {
    sql += " AND status = ?";
    params.push(status);
  }
  sql += " ORDER BY transaction_date DESC, transaction_time DESC";
  const rows = db.prepare(sql).all(...params);
  res.json({ transactions: rows });
});

// POST /api/transactions
router.post("/", validateBody(transactionSchema), (req, res) => {
  const v = req.validated;
  const id = crypto.randomUUID();
  db.prepare(
    `INSERT INTO transactions (id, user_id, transaction_type, amount, category_id, description, transaction_date, transaction_time, status)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')`
  ).run(id, req.userId, v.transactionType, v.amount, v.categoryId || null, v.description || "", v.transactionDate, v.transactionTime || null);

  enqueueSync(req.userId, "transaction", id, "create");
  const newAlerts = v.transactionType === "EXPENSE" ? evaluateThresholds(req.userId, v.categoryId, v.transactionDate) : [];
  const row = db.prepare("SELECT * FROM transactions WHERE id = ?").get(id);
  res.status(201).json({ transaction: row, alerts: newAlerts });
});

// PATCH /api/transactions/:id  (edit fields)
router.patch("/:id", validateBody(transactionUpdateSchema), (req, res) => {
  const existing = db.prepare("SELECT * FROM transactions WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!existing) return res.status(404).json({ error: "Transaction not found." });

  const v = req.validated;
  const updated = {
    transaction_type: v.transactionType ?? existing.transaction_type,
    amount: v.amount ?? existing.amount,
    category_id: v.categoryId !== undefined ? v.categoryId : existing.category_id,
    description: v.description !== undefined ? v.description : existing.description,
    transaction_date: v.transactionDate ?? existing.transaction_date,
    transaction_time: v.transactionTime ?? existing.transaction_time,
  };

  db.prepare(
    `UPDATE transactions SET transaction_type=?, amount=?, category_id=?, description=?, transaction_date=?, transaction_time=?, updated_at=datetime('now')
     WHERE id = ? AND user_id = ?`
  ).run(updated.transaction_type, updated.amount, updated.category_id, updated.description, updated.transaction_date, updated.transaction_time, req.params.id, req.userId);

  enqueueSync(req.userId, "transaction", req.params.id, "update");
  const row = db.prepare("SELECT * FROM transactions WHERE id = ?").get(req.params.id);
  res.json({ transaction: row });
});

// PATCH /api/transactions/:id/void  — soft delete only, never a hard delete
router.patch("/:id/void", (req, res) => {
  const existing = db.prepare("SELECT * FROM transactions WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!existing) return res.status(404).json({ error: "Transaction not found." });
  if (existing.status === "VOIDED") return res.json({ transaction: existing });

  db.prepare(
    "UPDATE transactions SET status='VOIDED', voided_at=datetime('now'), updated_at=datetime('now') WHERE id = ? AND user_id = ?"
  ).run(req.params.id, req.userId);

  enqueueSync(req.userId, "transaction", req.params.id, "delete");
  const row = db.prepare("SELECT * FROM transactions WHERE id = ?").get(req.params.id);
  res.json({ transaction: row });
});

module.exports = router;
