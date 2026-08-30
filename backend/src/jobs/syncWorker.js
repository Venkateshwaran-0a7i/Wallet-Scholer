require("dotenv").config();
const { runPendingJobs } = require("../services/sheetsSync.service");

const POLL_INTERVAL_MS = 15_000;

async function tick() {
  try {
    const results = await runPendingJobs();
    if (results.length) console.log(`[sync-worker] processed ${results.length} job(s):`, results);
  } catch (err) {
    console.error("[sync-worker] error:", err.message);
  } finally {
    setTimeout(tick, POLL_INTERVAL_MS);
  }
}

console.log("[sync-worker] started, polling every", POLL_INTERVAL_MS / 1000, "seconds");
tick();
