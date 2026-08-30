# Wallet Scholer — Backend

Node.js + Express + SQLite (better-sqlite3). Verified working: installed,
migrated, and exercised through every core endpoint before delivery.

## Setup

```bash
npm install
cp .env.example .env
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"    # -> JWT_SECRET
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))" # -> TOKEN_ENCRYPTION_KEY
# put both values into .env
npm run migrate
npm run dev        # http://localhost:4000, auto-restarts on file changes
```

Run the background sync worker in a second terminal if you're testing Google
Sheets backup:

```bash
npm run worker
```

## Environment variables

See `.env.example`. Never commit a real `.env` — it's already in `.gitignore`.

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Signs session tokens. Required — server refuses to start without it. |
| `TOKEN_ENCRYPTION_KEY` | AES-256-GCM key (base64, 32 bytes) used to encrypt Google refresh tokens at rest. Required. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | From Google Cloud Console. Needed for Google Sign-In verification and Sheets OAuth. |
| `GOOGLE_SHEETS_REDIRECT_URI` | Must exactly match a redirect URI configured in Google Cloud Console. |
| `DATABASE_FILE` | Path to the SQLite file (auto-created). |

## Google Cloud setup (for Sign-In + Sheets backup)

1. Create a project at console.cloud.google.com.
2. Enable the **Google Sheets API**.
3. Configure the OAuth consent screen (External, or Internal for a Workspace org).
4. Create an OAuth 2.0 Client ID:
   - For the **mobile Sign-In flow**, create an OAuth client of the appropriate
     platform type for your Expo build (Android/iOS) — the mobile app obtains
     an ID token natively and sends it to `POST /api/auth/google`, so this
     backend never needs the mobile client's secret.
   - For **Sheets backup** (server-side authorization-code flow), create a
     **Web application** client, and set its redirect URI to match
     `GOOGLE_SHEETS_REDIRECT_URI` exactly.
5. Put the Web client's ID/secret into `.env` as `GOOGLE_CLIENT_ID` /
   `GOOGLE_CLIENT_SECRET`.

## API Reference

All authenticated routes require `Authorization: Bearer <token>`.

### Auth
```
POST /api/auth/register        { displayName, email, password }
POST /api/auth/login           { email, password }              — rate-limited: 5 / 15 min
POST /api/auth/google          { idToken }                       — rate-limited: 5 / 15 min
POST /api/auth/logout
GET  /api/auth/me
```

### Transactions
```
GET   /api/transactions?month=YYYY-MM&status=ACTIVE|VOIDED
POST  /api/transactions        { transactionType, amount, categoryId, description, transactionDate }
PATCH /api/transactions/:id
PATCH /api/transactions/:id/void   — soft delete only, never a hard delete
```

### Categories
```
GET    /api/categories
POST   /api/categories         { name, categoryType, budgetGroup, icon }
DELETE /api/categories/:id     — system categories can't be deleted
```

### Budgets
```
GET    /api/budgets?year=2026&month=8
POST   /api/budgets            { periodYear, periodMonth, incomeAmount, allocations[], confirmExceedsIncome }
PATCH  /api/budgets/:id
DELETE /api/budgets/:id
POST   /api/budgets/copy-last-month   { periodYear, periodMonth }
GET    /api/budgets/:id/utilization   — authoritative server-computed per-category utilization
```
If `allocations` sum exceeds `incomeAmount` and `confirmExceedsIncome` is not
`true`, the API responds `409` with the computed totals so the client can show
a warning and resubmit with explicit confirmation — it never silently rejects
or silently saves.

### Notifications
```
GET   /api/notifications/preferences
PATCH /api/notifications/preferences   { masterEnabled, threshold75, threshold90, threshold100, thresholdExceeded }
```

### Google Sheets integration
```
GET  /api/integrations/google-sheets/connect     — returns a consent URL
GET  /api/integrations/google-sheets/callback    — OAuth redirect target
GET  /api/integrations/google-sheets/status
POST /api/integrations/google-sheets/sync
POST /api/integrations/google-sheets/disconnect
```

## Security (implemented, not just documented)

- **Rate limiting**: `express-rate-limit`, 5 attempts / 15 min on `/login` and
  `/auth/google`; verified live (4× `401`, then `429` on the 5th+ request).
- **Secrets**: nothing hardcoded; server refuses to boot without
  `JWT_SECRET`/`TOKEN_ENCRYPTION_KEY` set; `.env` is git-ignored; `.env.example`
  has placeholders only.
- **Refresh token encryption**: AES-256-GCM before the token ever touches the
  database (`src/services/crypto.service.js`).
- **Input validation**: every write route validates with Zod
  (`src/domain/validators.js`) — oversized/malformed/negative/non-finite
  values are rejected with `400` before reaching a handler; a 256kb body-size
  ceiling rejects oversized payloads outright.
- **AuthZ**: every query is scoped to `req.userId` from the verified JWT — a
  user can never read or modify another user's rows.
- **No stack traces leaked**: centralized error handler logs internally only.
- **Alert dedup enforced at the DB layer** (`UNIQUE` constraint on
  `alerts`), not just application logic, so it's race-safe.

## Tests run against this build

All of the following were executed against a live instance before delivery
(see the project README for the summary table):
register → duplicate-email rejection → login → 5-attempt rate limit →
category listing → valid/invalid transaction creation (negative amount,
malformed date, oversized description, all correctly `400`) → unauthenticated
request correctly `401` → budget creation blocked by exceeds-income check →
budget creation succeeding with explicit confirmation → threshold alert firing
at 75%, correctly not duplicating on a second crossing of the same threshold,
then firing `EXCEEDED` past 100% → utilization endpoint → copy-last-month
(previous month verified unchanged) → void transaction (confirmed excluded
from the active-only query).

There's no automated test suite file yet (`npm test` is wired to
`node --test test/` but the `test/` directory is a placeholder) — the checks
above were run manually against the live server. Adding the equivalent as
real `node:test` files is the natural next step.
