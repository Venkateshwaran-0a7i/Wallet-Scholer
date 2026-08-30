const express = require("express");
const bcrypt = require("bcryptjs");
const { OAuth2Client } = require("google-auth-library");
const db = require("../db");
const { signToken, requireAuth } = require("../middleware/auth");
const { validateBody } = require("../middleware/validate");
const { registerSchema, loginSchema, googleAuthSchema } = require("../domain/validators");
const { authLimiter, strictLimiter } = require("../middleware/rateLimit");

const router = express.Router();
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

function uid() {
  return require("crypto").randomUUID();
}

function recordLoginAttempt(identifier, succeeded) {
  db.prepare("INSERT INTO login_attempts (identifier, succeeded) VALUES (?, ?)").run(identifier, succeeded ? 1 : 0);
}

function seedDefaultCategories(userId) {
  const expense = [
    ["Rent", "NEEDS"], ["Food", "NEEDS"], ["Transport", "NEEDS"], ["Shopping", "WANTS"],
    ["Entertainment", "WANTS"], ["Savings", "SAVINGS"], ["Emergency", "SAVINGS"],
    ["Medicine", "NEEDS"], ["EMI", "NEEDS"], ["Investment", "SAVINGS"], ["Other", "OTHER"],
  ];
  const income = ["Salary", "Freelance", "Bonus", "Other"];
  const insert = db.prepare(
    "INSERT INTO categories (id, user_id, name, category_type, budget_group, is_system) VALUES (?, ?, ?, ?, ?, 1)"
  );
  const tx = db.transaction(() => {
    expense.forEach(([name, group]) => insert.run(uid(), userId, name, "EXPENSE", group));
    income.forEach((name) => insert.run(uid(), userId, name, "INCOME", null));
  });
  tx();
}

// POST /api/auth/register
router.post("/register", strictLimiter, validateBody(registerSchema), (req, res) => {
  const { displayName, email, password } = req.validated;
  const existing = db.prepare("SELECT id FROM users WHERE email = ?").get(email.toLowerCase());
  if (existing) return res.status(409).json({ error: "An account with this email already exists." });

  const passwordHash = bcrypt.hashSync(password, 12);
  const id = uid();
  db.prepare(
    "INSERT INTO users (id, display_name, email, password_hash, auth_provider) VALUES (?, ?, ?, ?, 'LOCAL')"
  ).run(id, displayName, email.toLowerCase(), passwordHash);
  db.prepare("INSERT INTO notification_preferences (user_id) VALUES (?)").run(id);
  seedDefaultCategories(id);

  const token = signToken(id);
  res.status(201).json({ token, user: { id, displayName, email: email.toLowerCase() } });
});

// POST /api/auth/login
router.post("/login", authLimiter, validateBody(loginSchema), (req, res) => {
  const { email, password } = req.validated;
  const identifier = email.toLowerCase();
  const user = db.prepare("SELECT * FROM users WHERE email = ?").get(identifier);

  const fail = () => {
    recordLoginAttempt(identifier, false);
    return res.status(401).json({ error: "Invalid email or password." }); // never reveal which field was wrong
  };

  if (!user || !user.password_hash) return fail();
  const ok = bcrypt.compareSync(password, user.password_hash);
  if (!ok) return fail();

  recordLoginAttempt(identifier, true);
  const token = signToken(user.id);
  res.json({ token, user: { id: user.id, displayName: user.display_name, email: user.email } });
});

// POST /api/auth/google
// Mobile flow: the app obtains a Google ID token via native Google Sign-In,
// then sends it here for verification. We never see or need the client secret
// for this flow, and never trust a client-asserted user id.
router.post("/google", authLimiter, validateBody(googleAuthSchema), async (req, res, next) => {
  try {
    const { idToken } = req.validated;
    const ticket = await googleClient.verifyIdToken({
      idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    if (!payload?.sub) return res.status(401).json({ error: "Invalid Google token." });

    let user = db.prepare("SELECT * FROM users WHERE google_sub = ?").get(payload.sub);
    if (!user) {
      // Do not use email as the permanent identity key — link by Google's stable `sub`.
      const id = uid();
      db.prepare(
        "INSERT INTO users (id, display_name, email, google_sub, auth_provider) VALUES (?, ?, ?, ?, 'GOOGLE')"
      ).run(id, payload.name || "Wallet Scholer User", payload.email || null, payload.sub);
      db.prepare("INSERT INTO notification_preferences (user_id) VALUES (?)").run(id);
      seedDefaultCategories(id);
      user = db.prepare("SELECT * FROM users WHERE id = ?").get(id);
    }

    const token = signToken(user.id);
    res.json({ token, user: { id: user.id, displayName: user.display_name, email: user.email } });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/logout — stateless JWT, client just discards the token.
// Included for API-contract completeness / future session-based auth.
router.post("/logout", requireAuth, (req, res) => {
  res.json({ success: true });
});

router.get("/me", requireAuth, (req, res) => {
  const user = db.prepare("SELECT id, display_name, email, currency FROM users WHERE id = ?").get(req.userId);
  if (!user) return res.status(404).json({ error: "User not found." });
  res.json({ user });
});

module.exports = router;
