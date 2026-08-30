const express = require("express");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { notificationPrefsSchema } = require("../domain/validators");

const router = express.Router();
router.use(requireAuth);

const FIELD_MAP = {
  masterEnabled: "master_enabled", threshold50: "threshold_50", threshold75: "threshold_75",
  threshold90: "threshold_90", threshold100: "threshold_100", thresholdExceeded: "threshold_exceeded",
};

router.get("/preferences", (req, res) => {
  const row = db.prepare("SELECT * FROM notification_preferences WHERE user_id = ?").get(req.userId);
  res.json({ preferences: row || null });
});

router.patch("/preferences", validateBody(notificationPrefsSchema), (req, res) => {
  const v = req.validated;
  const existing = db.prepare("SELECT * FROM notification_preferences WHERE user_id = ?").get(req.userId);
  if (!existing) {
    db.prepare("INSERT INTO notification_preferences (user_id) VALUES (?)").run(req.userId);
  }
  const sets = [];
  const params = [];
  Object.entries(v).forEach(([key, val]) => {
    if (val === undefined) return;
    sets.push(`${FIELD_MAP[key]} = ?`);
    params.push(val ? 1 : 0);
  });
  if (sets.length) {
    params.push(req.userId);
    db.prepare(`UPDATE notification_preferences SET ${sets.join(", ")}, updated_at = datetime('now') WHERE user_id = ?`).run(...params);
  }
  const row = db.prepare("SELECT * FROM notification_preferences WHERE user_id = ?").get(req.userId);
  res.json({ preferences: row });
});

module.exports = router;
