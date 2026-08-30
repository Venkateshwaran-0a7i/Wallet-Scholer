const express = require("express");
const crypto = require("crypto");
const { google } = require("googleapis");
const db = require("../db");
const { requireAuth } = require("../middleware/auth");
const { encrypt } = require("../services/crypto.service");
const { runSyncForUser } = require("../services/sheetsSync.service");

const router = express.Router();

const SCOPES = ["https://www.googleapis.com/auth/spreadsheets"];

function oauthClient() {
  return new google.auth.OAuth2(
    process.env.GOOGLE_CLIENT_ID,
    process.env.GOOGLE_CLIENT_SECRET,
    process.env.GOOGLE_SHEETS_REDIRECT_URI
  );
}

// In-memory state store for CSRF protection on the OAuth redirect (swap for
// a persistent store — e.g. Redis — behind multiple server instances).
const pendingStates = new Map();

// GET /api/integrations/google-sheets/connect
// Requires the mobile app's own JWT (query param since this opens a browser),
// separate consent from Sign-In (incremental authorization).
router.get("/connect", requireAuth, (req, res) => {
  const state = crypto.randomUUID();
  pendingStates.set(state, { userId: req.userId, createdAt: Date.now() });

  const url = oauthClient().generateAuthUrl({
    access_type: "offline", // needed for a refresh token (background sync)
    prompt: "consent",
    scope: SCOPES,
    state,
  });
  res.json({ url });
});

// GET /api/integrations/google-sheets/callback
router.get("/callback", async (req, res, next) => {
  try {
    const { code, state } = req.query;
    const pending = pendingStates.get(state);
    if (!pending) return res.status(400).send("Invalid or expired authorization state.");
    pendingStates.delete(state);
    if (Date.now() - pending.createdAt > 10 * 60 * 1000) return res.status(400).send("Authorization expired, please try again.");

    const client = oauthClient();
    const { tokens } = await client.getToken(code);
    if (!tokens.refresh_token) {
      return res.status(400).send("Google did not return a refresh token. Revoke prior access and try again.");
    }
    client.setCredentials(tokens);

    // Create the user's personal spreadsheet
    const sheets = google.sheets({ version: "v4", auth: client });
    const created = await sheets.spreadsheets.create({
      requestBody: {
        properties: { title: "Wallet Scholer - Financial Backup" },
        sheets: ["Dashboard", "Transactions", "Budgets", "Income", "Monthly Summary"].map((title) => ({ properties: { title } })),
      },
    });
    const spreadsheetId = created.data.spreadsheetId;
    const spreadsheetUrl = created.data.spreadsheetUrl;

    await sheets.spreadsheets.values.update({
      spreadsheetId,
      range: "Transactions!A1:H1",
      valueInputOption: "RAW",
      requestBody: { values: [["Transaction ID", "Date", "Type", "Category", "Amount", "Description", "Created At", "Updated At"]] },
    });

    const encryptedRefresh = encrypt(tokens.refresh_token);
    const existing = db.prepare("SELECT id FROM google_integrations WHERE user_id = ? AND provider = 'google_sheets'").get(pending.userId);
    if (existing) {
      db.prepare(
        `UPDATE google_integrations SET refresh_token_encrypted=?, token_expiry=?, spreadsheet_id=?, spreadsheet_url=?, sync_enabled=1, sync_status='CONNECTED', updated_at=datetime('now') WHERE id=?`
      ).run(encryptedRefresh, String(tokens.expiry_date || ""), spreadsheetId, spreadsheetUrl, existing.id);
    } else {
      db.prepare(
        `INSERT INTO google_integrations (id, user_id, provider, refresh_token_encrypted, token_expiry, spreadsheet_id, spreadsheet_url, sync_enabled, sync_status)
         VALUES (?, ?, 'google_sheets', ?, ?, ?, ?, 1, 'CONNECTED')`
      ).run(crypto.randomUUID(), pending.userId, encryptedRefresh, String(tokens.expiry_date || ""), spreadsheetId, spreadsheetUrl);
    }

    // A deep link back into the mobile app; adjust scheme to match app.json.
    res.redirect(`walletscholer://backup-connected?spreadsheetUrl=${encodeURIComponent(spreadsheetUrl)}`);
  } catch (err) {
    next(err);
  }
});

router.get("/status", requireAuth, (req, res) => {
  const row = db.prepare(
    "SELECT google_email, spreadsheet_url, sync_enabled, sync_status, last_synced_at FROM google_integrations WHERE user_id = ? AND provider = 'google_sheets'"
  ).get(req.userId);
  res.json({ integration: row || { sync_enabled: 0, sync_status: "DISCONNECTED" } });
});

router.post("/sync", requireAuth, async (req, res, next) => {
  try {
    const result = await runSyncForUser(req.userId);
    res.json(result);
  } catch (err) {
    next(err);
  }
});

router.post("/disconnect", requireAuth, (req, res) => {
  db.prepare(
    "UPDATE google_integrations SET sync_enabled = 0, sync_status = 'DISCONNECTED', updated_at = datetime('now') WHERE user_id = ? AND provider = 'google_sheets'"
  ).run(req.userId);
  // Note: disconnecting revokes future syncing only — it does not delete the
  // user's existing spreadsheet or any in-app data.
  res.json({ success: true });
});

module.exports = router;
