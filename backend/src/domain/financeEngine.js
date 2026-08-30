// Pure, framework-free financial calculations.
// Every function here is unit-testable in isolation (see /test).

function isFiniteNum(n) {
  return typeof n === "number" && Number.isFinite(n);
}

function clamp(n, lo, hi) {
  return Math.min(hi, Math.max(lo, n));
}

function computeBalance(transactions) {
  return transactions
    .filter((t) => t.status === "ACTIVE")
    .reduce((sum, t) => sum + (t.transaction_type === "INCOME" ? Number(t.amount) : -Number(t.amount)), 0);
}

function utilizationPct(spent, allocated) {
  if (!allocated || allocated <= 0) return spent > 0 ? 100 : 0;
  return clamp((spent / allocated) * 100, 0, 999);
}

function remainingBudget(allocated, spent) {
  return allocated - spent;
}

function thresholdFor(pct) {
  if (pct > 100) return "EXCEEDED";
  if (pct >= 100) return "100";
  if (pct >= 90) return "90";
  if (pct >= 75) return "75";
  if (pct >= 50) return "50";
  return null;
}

function simpleInterest(P, R, T) {
  if (!isFiniteNum(P) || !isFiniteNum(R) || !isFiniteNum(T) || P <= 0 || T <= 0) return null;
  const SI = (P * R * T) / 100;
  return { interest: SI, total: P + SI };
}

function compoundInterest(P, R, T, n) {
  if (!isFiniteNum(P) || !isFiniteNum(R) || !isFiniteNum(T) || !isFiniteNum(n) || P <= 0 || T <= 0 || n <= 0) return null;
  const r = R / 100;
  const A = P * Math.pow(1 + r / n, n * T);
  return { total: A, interest: A - P };
}

function sipFutureValue(monthly, annualReturnPct, years) {
  if (!isFiniteNum(monthly) || monthly <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const invested = monthly * n;
  if (!annualReturnPct) return { futureValue: invested, invested, gain: 0 };
  const i = annualReturnPct / 12 / 100;
  const fv = monthly * ((Math.pow(1 + i, n) - 1) / i) * (1 + i);
  return { futureValue: fv, invested, gain: fv - invested };
}

function requiredMonthlySavings(target, annualReturnPct, years) {
  if (!isFiniteNum(target) || target <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  if (!annualReturnPct) return { monthly: target / n, totalDeposited: target, gain: 0 };
  const i = annualReturnPct / 12 / 100;
  const factor = ((Math.pow(1 + i, n) - 1) / i) * (1 + i);
  const monthly = target / factor;
  return { monthly, totalDeposited: monthly * n, gain: target - monthly * n };
}

function emiCalc(P, annualRatePct, years) {
  if (!isFiniteNum(P) || P <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const r = (annualRatePct || 0) / 12 / 100;
  const emi = r === 0 ? P / n : (P * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
  const totalPayment = emi * n;
  return { emi, totalPayment, totalInterest: totalPayment - P, n };
}

function loanAffordability(desiredEmi, annualRatePct, years) {
  if (!isFiniteNum(desiredEmi) || desiredEmi <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const r = (annualRatePct || 0) / 12 / 100;
  const principal = r === 0 ? desiredEmi * n : (desiredEmi * (Math.pow(1 + r, n) - 1)) / (r * Math.pow(1 + r, n));
  return { principal, totalPayment: desiredEmi * n, totalInterest: desiredEmi * n - principal };
}

function percentageOf(x, y) {
  if (!isFiniteNum(x) || !isFiniteNum(y)) return null;
  return (x / 100) * y;
}

function whatPercent(x, y) {
  if (!isFiniteNum(x) || !isFiniteNum(y) || y === 0) return null;
  return (x / y) * 100;
}

function percentChange(x, pct, mode) {
  if (!isFiniteNum(x) || !isFiniteNum(pct)) return null;
  return mode === "increase" ? x * (1 + pct / 100) : x * (1 - pct / 100);
}

function safeToSpend({ balance, essentialRemaining, savingsCommitment, emiCommitment }) {
  const reserved = Math.max(0, essentialRemaining || 0) + Math.max(0, savingsCommitment || 0) + Math.max(0, emiCommitment || 0);
  return Math.max(0, balance - reserved);
}

module.exports = {
  computeBalance, utilizationPct, remainingBudget, thresholdFor,
  simpleInterest, compoundInterest, sipFutureValue, requiredMonthlySavings,
  emiCalc, loanAffordability, percentageOf, whatPercent, percentChange, safeToSpend,
};
