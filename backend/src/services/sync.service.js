const crypto = require("crypto");
const db = require("../db");

// Enqueue a background sync job. The caller's request has already succeeded
// against the primary database by the time this runs — a Sheets outage must
// never fail the user's actual transaction/budget/income save.
function enqueueSync(userId, entityType, entityId, operation) {
  const integration = db.prepare(
    "SELECT * FROM google_integrations WHERE user_id = ? AND provider = 'google_sheets'"
  ).get(userId);
  if (!integration || !integration.sync_enabled) return; // no-op if user hasn't opted in

  const idempotencyKey = `${userId}:${entityType}:${entityId}:${operation}:${Date.now()}`;
  db.prepare(
    `INSERT INTO sync_jobs (id, user_id, entity_type, entity_id, operation, idempotency_key, status)
     VALUES (?, ?, ?, ?, ?, ?, 'PENDING')`
  ).run(crypto.randomUUID(), userId, entityType, entityId, operation, idempotencyKey);
}

module.exports = { enqueueSync };
