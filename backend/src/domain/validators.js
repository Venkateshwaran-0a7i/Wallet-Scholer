const { z } = require("zod");

const MAX_AMOUNT = 100_000_000_000; // hard ceiling to block overflow/garbage input
const MAX_DESCRIPTION_LEN = 200;
const MAX_NAME_LEN = 50;

const amountSchema = z.number().finite().positive().max(MAX_AMOUNT);

const dateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine((v) => {
  const d = new Date(v + "T00:00:00Z");
  return !isNaN(d.getTime()) && v === d.toISOString().slice(0, 10);
}, { message: "Invalid calendar date" });

const registerSchema = z.object({
  displayName: z.string().trim().min(1).max(MAX_NAME_LEN),
  email: z.string().trim().email().max(254),
  password: z.string().min(8).max(128),
});

const loginSchema = z.object({
  email: z.string().trim().email().max(254),
  password: z.string().min(1).max(128),
});

const googleAuthSchema = z.object({
  idToken: z.string().min(10).max(4096),
});

const transactionSchema = z.object({
  transactionType: z.enum(["INCOME", "EXPENSE"]),
  amount: amountSchema,
  categoryId: z.string().max(100).nullable().optional(),
  description: z.string().max(MAX_DESCRIPTION_LEN).optional().default(""),
  transactionDate: dateSchema,
  transactionTime: z.string().regex(/^\d{2}:\d{2}$/).optional(),
});

const transactionUpdateSchema = transactionSchema.partial().extend({
  status: z.enum(["ACTIVE", "VOIDED"]).optional(),
});

const categorySchema = z.object({
  name: z.string().trim().min(1).max(MAX_NAME_LEN),
  categoryType: z.enum(["INCOME", "EXPENSE"]),
  budgetGroup: z.enum(["NEEDS", "WANTS", "SAVINGS", "OTHER"]).optional(),
  icon: z.string().max(50).optional(),
});

const budgetSchema = z.object({
  periodMonth: z.number().int().min(1).max(12),
  periodYear: z.number().int().min(2000).max(2100),
  incomeAmount: z.number().finite().min(0).max(MAX_AMOUNT),
  allocations: z.array(z.object({
    categoryId: z.string().max(100),
    allocatedAmount: z.number().finite().min(0).max(MAX_AMOUNT),
  })).max(200),
  confirmExceedsIncome: z.boolean().optional().default(false),
});

const notificationPrefsSchema = z.object({
  masterEnabled: z.boolean().optional(),
  threshold50: z.boolean().optional(),
  threshold75: z.boolean().optional(),
  threshold90: z.boolean().optional(),
  threshold100: z.boolean().optional(),
  thresholdExceeded: z.boolean().optional(),
});

const goalSchema = z.object({
  name: z.string().trim().min(1).max(MAX_NAME_LEN),
  targetAmount: amountSchema,
  currentAmount: z.number().finite().min(0).max(MAX_AMOUNT).optional().default(0),
  targetDate: dateSchema.nullable().optional(),
  icon: z.string().max(50).optional(),
  status: z.enum(["ACTIVE", "ACHIEVED", "CANCELLED"]).optional().default("ACTIVE"),
});

function sanitizeString(input, maxLen) {
  if (typeof input !== "string") return "";
  // strip control characters, trim, hard cap length
  return input.replace(/[\u0000-\u001F\u007F]/g, "").trim().slice(0, maxLen);
}

module.exports = {
  MAX_AMOUNT, MAX_DESCRIPTION_LEN, MAX_NAME_LEN,
  registerSchema, loginSchema, googleAuthSchema,
  transactionSchema, transactionUpdateSchema, categorySchema,
  budgetSchema, notificationPrefsSchema, goalSchema, sanitizeString,
};
