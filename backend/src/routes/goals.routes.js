const express = require("express");
const crypto = require("crypto");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { goalSchema } = require("../domain/validators");

const router = express.Router();
router.use(requireAuth);

// GET /api/goals
router.get("/", (req, res) => {
  const rows = db.prepare("SELECT * FROM goals WHERE user_id = ? ORDER BY created_at DESC").all(req.userId);
  res.json({ goals: rows });
});

// POST /api/goals
router.post("/", validateBody(goalSchema), (req, res) => {
  const v = req.validated;
  const id = crypto.randomUUID();
  db.prepare(
    `INSERT INTO goals (id, user_id, name, target_amount, current_amount, target_date, icon, status)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  ).run(id, req.userId, v.name, v.targetAmount, v.currentAmount, v.targetDate || null, v.icon || null, v.status);

  const row = db.prepare("SELECT * FROM goals WHERE id = ?").get(id);
  res.status(201).json({ goal: row });
});

// PATCH /api/goals/:id
router.patch("/:id", validateBody(goalSchema.partial()), (req, res) => {
  const existing = db.prepare("SELECT * FROM goals WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!existing) return res.status(404).json({ error: "Goal not found." });

  const v = req.validated;
  const updated = {
    name: v.name ?? existing.name,
    target_amount: v.targetAmount ?? existing.target_amount,
    current_amount: v.currentAmount ?? existing.current_amount,
    target_date: v.targetDate !== undefined ? v.targetDate : existing.target_date,
    icon: v.icon !== undefined ? v.icon : existing.icon,
    status: v.status ?? existing.status,
  };

  db.prepare(
    `UPDATE goals SET name=?, target_amount=?, current_amount=?, target_date=?, icon=?, status=?, updated_at=datetime('now')
     WHERE id = ? AND user_id = ?`
  ).run(updated.name, updated.target_amount, updated.current_amount, updated.target_date, updated.icon, updated.status, req.params.id, req.userId);

  const row = db.prepare("SELECT * FROM goals WHERE id = ?").get(req.params.id);
  res.json({ goal: row });
});

// DELETE /api/goals/:id
router.delete("/:id", (req, res) => {
  const resInfo = db.prepare("DELETE FROM goals WHERE id = ? AND user_id = ?").run(req.params.id, req.userId);
  if (resInfo.changes === 0) return res.status(404).json({ error: "Goal not found." });
  res.status(204).end();
});

module.exports = router;
