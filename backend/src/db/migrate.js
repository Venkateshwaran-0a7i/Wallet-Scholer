const fs = require("fs");
const path = require("path");
const db = require("./index");

const migrationsDir = path.join(__dirname, "..", "..", "migrations");
const files = fs.readdirSync(migrationsDir).filter((f) => f.endsWith(".sql")).sort();

db.exec(`CREATE TABLE IF NOT EXISTS _migrations (name TEXT PRIMARY KEY, applied_at TEXT NOT NULL DEFAULT (datetime('now')))`);
const applied = new Set(db.prepare("SELECT name FROM _migrations").all().map((r) => r.name));

for (const file of files) {
  if (applied.has(file)) continue;
  const sql = fs.readFileSync(path.join(migrationsDir, file), "utf8");
  console.log(`Applying migration: ${file}`);
  db.exec(sql);
  db.prepare("INSERT INTO _migrations (name) VALUES (?)").run(file);
}
console.log("Migrations up to date.");
