-- Wallet Scholer initial schema

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  email TEXT UNIQUE,
  password_hash TEXT,
  currency TEXT NOT NULL DEFAULT 'INR',
  google_sub TEXT UNIQUE,
  auth_provider TEXT NOT NULL DEFAULT 'LOCAL', -- LOCAL | GOOGLE
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS categories (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  name TEXT NOT NULL,
  category_type TEXT NOT NULL, -- INCOME | EXPENSE
  budget_group TEXT,           -- NEEDS | WANTS | SAVINGS | OTHER
  icon TEXT,
  is_system INTEGER NOT NULL DEFAULT 0,
  is_active INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(user_id, name, category_type)
);

CREATE TABLE IF NOT EXISTS transactions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  transaction_type TEXT NOT NULL, -- INCOME | EXPENSE
  amount NUMERIC NOT NULL,
  category_id TEXT REFERENCES categories(id),
  description TEXT,
  transaction_date TEXT NOT NULL,
  transaction_time TEXT,
  status TEXT NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | VOIDED
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  voided_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_tx_user_date ON transactions(user_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_tx_user_status ON transactions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_tx_user_category ON transactions(user_id, category_id);

CREATE TABLE IF NOT EXISTS budgets (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  period_month INTEGER NOT NULL,
  period_year INTEGER NOT NULL,
  income_amount NUMERIC NOT NULL,
  total_budget NUMERIC NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(user_id, period_year, period_month)
);

CREATE TABLE IF NOT EXISTS budget_allocations (
  id TEXT PRIMARY KEY,
  budget_id TEXT NOT NULL REFERENCES budgets(id),
  category_id TEXT NOT NULL REFERENCES categories(id),
  allocated_amount NUMERIC NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(budget_id, category_id)
);
CREATE INDEX IF NOT EXISTS idx_alloc_budget ON budget_allocations(budget_id);

CREATE TABLE IF NOT EXISTS alerts (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  alert_type TEXT NOT NULL,
  threshold TEXT,
  message TEXT NOT NULL,
  related_budget_id TEXT,
  category_id TEXT,
  period_month INTEGER,
  period_year INTEGER,
  is_read INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(related_budget_id, category_id, period_year, period_month, threshold)
);
CREATE INDEX IF NOT EXISTS idx_alerts_budget_period ON alerts(related_budget_id, period_year, period_month);

CREATE TABLE IF NOT EXISTS notification_preferences (
  user_id TEXT PRIMARY KEY REFERENCES users(id),
  master_enabled INTEGER NOT NULL DEFAULT 1,
  threshold_50 INTEGER NOT NULL DEFAULT 1,
  threshold_75 INTEGER NOT NULL DEFAULT 1,
  threshold_90 INTEGER NOT NULL DEFAULT 1,
  threshold_100 INTEGER NOT NULL DEFAULT 1,
  threshold_exceeded INTEGER NOT NULL DEFAULT 1,
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS google_integrations (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  provider TEXT NOT NULL DEFAULT 'google_sheets',
  google_email TEXT,
  refresh_token_encrypted TEXT,
  token_expiry TEXT,
  spreadsheet_id TEXT,
  spreadsheet_url TEXT,
  sync_enabled INTEGER NOT NULL DEFAULT 0,
  sync_status TEXT NOT NULL DEFAULT 'DISCONNECTED', -- DISCONNECTED | CONNECTING | CONNECTED | FAILED
  last_synced_at TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(user_id, provider)
);

CREATE TABLE IF NOT EXISTS sync_jobs (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  entity_type TEXT NOT NULL, -- transaction | budget | income
  entity_id TEXT NOT NULL,
  operation TEXT NOT NULL,   -- create | update | delete
  idempotency_key TEXT NOT NULL UNIQUE,
  status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING | RUNNING | DONE | FAILED
  retry_count INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  completed_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_sync_jobs_status ON sync_jobs(status);

CREATE TABLE IF NOT EXISTS budget_notification_history (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  budget_id TEXT NOT NULL,
  budget_month TEXT NOT NULL, -- 'YYYY-MM'
  threshold TEXT NOT NULL,
  sent_at TEXT NOT NULL DEFAULT (datetime('now')),
  UNIQUE(budget_id, budget_month, threshold)
);

CREATE TABLE IF NOT EXISTS login_attempts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  identifier TEXT NOT NULL, -- email or IP
  succeeded INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_login_attempts_identifier ON login_attempts(identifier, created_at);
