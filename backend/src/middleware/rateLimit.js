const rateLimit = require("express-rate-limit");

// Auth routes: strict, matches security requirement (5 attempts / 15 minutes)
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Too many attempts. Please try again in 15 minutes." },
  handler: (req, res, _next, options) => {
    res.status(429).json(options.message);
  },
});

// Registration / credential-recovery style routes: strict but distinct from login
const strictLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Too many requests. Please try again later." },
});

// General authenticated API traffic: higher, usage-based limit so normal use isn't blocked
const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Rate limit exceeded. Please slow down." },
});

module.exports = { authLimiter, strictLimiter, generalLimiter };
