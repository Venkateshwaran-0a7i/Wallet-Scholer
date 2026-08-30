const Database = require("better-sqlite3");
const path = require("path");
const fs = require("fs");
require("dotenv").config();

const dbFile = process.env.DATABASE_FILE || "./data/wallet_scholer.db";
const dir = path.dirname(dbFile);
if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

const db = new Database(dbFile);
db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");

module.exports = db;
