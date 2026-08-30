// Wraps a Zod schema as Express middleware. Rejects malformed/oversized
// payloads with a 400 before the request reaches any route handler.
function validateBody(schema) {
  return (req, res, next) => {
    const result = schema.safeParse(req.body);
    if (!result.success) {
      return res.status(400).json({
        error: "Invalid request body.",
        details: result.error.issues.map((i) => ({ path: i.path.join("."), message: i.message })),
      });
    }
    req.validated = result.data;
    next();
  };
}

// Reject bodies above a byte-size ceiling before they're even parsed as JSON
// (paired with express.json({ limit }) at the app level as the first line of defense).
function rejectOversizedJson(err, req, res, next) {
  if (err && err.type === "entity.too.large") {
    return res.status(413).json({ error: "Request payload too large." });
  }
  next(err);
}

module.exports = { validateBody, rejectOversizedJson };
