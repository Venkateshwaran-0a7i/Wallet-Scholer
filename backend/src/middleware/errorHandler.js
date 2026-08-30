function errorHandler(err, req, res, _next) {
  // Never log secrets or full request bodies (may contain financial data).
  console.error(`[error] ${req.method} ${req.path}:`, err.message);

  if (res.headersSent) return;

  const status = err.status || 500;
  const isProd = process.env.NODE_ENV === "production";

  res.status(status).json({
    error: isProd ? "Something went wrong. Please try again." : err.message,
    // Stack traces and internal details are never sent to the client, in any environment.
  });
}

module.exports = errorHandler;
