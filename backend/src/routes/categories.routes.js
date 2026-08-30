const express = require("express");
const crypto = require("crypto");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { categorySchema } = require("../domain/validators");

const router = express.Router();
router.use(requireAuth);

router.get("/", (req, res) => {
  const rows = db.prepare("SELECT * FROM categories WHERE user_id = ? AND is_active = 1 ORDER BY is_system DESC, name ASC").all(req.userId);
  res.json({ categories: rows });
});

router.post("/", validateBody(categorySchema), (req, res) => {
  const v = req.validated;
  const dup = db.prepare("SELECT id FROM categories WHERE user_id = ? AND name = ? AND category_type = ?").get(req.userId, v.name, v.categoryType);
  if (dup) return res.status(409).json({ error: "A category with this name already exists." });

  const id = crypto.randomUUID();
  db.prepare(
    "INSERT INTO categories (id, user_id, name, category_type, budget_group, icon, is_system) VALUES (?, ?, ?, ?, ?, ?, 0)"
  ).run(id, req.userId, v.name, v.categoryType, v.budgetGroup || "OTHER", v.icon || null);

  const row = db.prepare("SELECT * FROM categories WHERE id = ?").get(id);
  res.status(201).json({ category: row });
});

router.delete("/:id", (req, res) => {
  const cat = db.prepare("SELECT * FROM categories WHERE id = ? AND user_id = ?").get(req.params.id, req.userId);
  if (!cat) return res.status(404).json({ error: "Category not found." });
  if (cat.is_system) return res.status(400).json({ error: "System categories can't be deleted." });
  db.prepare("UPDATE categories SET is_active = 0 WHERE id = ?").run(req.params.id);
  res.json({ success: true });
});

module.exports = router;
