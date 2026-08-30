require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");

const authRoutes = require("./routes/auth.routes");
const transactionsRoutes = require("./routes/transactions.routes");
const categoriesRoutes = require("./routes/categories.routes");
const budgetsRoutes = require("./routes/budgets.routes");
const goalsRoutes = require("./routes/goals.routes");
const notificationsRoutes = require("./routes/notifications.routes");
const integrationsRoutes = require("./routes/integrations.routes");
const { generalLimiter } = require("./middleware/rateLimit");
const { rejectOversizedJson } = require("./middleware/validate");
const errorHandler = require("./middleware/errorHandler");

// Fail fast if required secrets are missing — never fall back to an insecure default.
["JWT_SECRET", "TOKEN_ENCRYPTION_KEY"].forEach((key) => {
  if (!process.env[key]) {
    console.error(`Missing required environment variable: ${key}. See .env.example.`);
    process.exit(1);
  }
});

const app = express();

app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN || "*" }));
app.use(express.json({ limit: "256kb" })); // oversized payloads are rejected before they reach any route
app.use(rejectOversizedJson);
app.use(generalLimiter);

app.get("/health", (req, res) => res.json({ status: "ok" }));

app.use("/api/auth", authRoutes);
app.use("/api/transactions", transactionsRoutes);
app.use("/api/categories", categoriesRoutes);
app.use("/api/budgets", budgetsRoutes);
app.use("/api/goals", goalsRoutes);
app.use("/api/notifications", notificationsRoutes);
app.use("/api/integrations/google-sheets", integrationsRoutes);

app.use((req, res) => res.status(404).json({ error: "Not found." }));
app.use(errorHandler);

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`Wallet Scholer API listening on port ${PORT} (${process.env.NODE_ENV || "development"})`);
});

module.exports = app;
