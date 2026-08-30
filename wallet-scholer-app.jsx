import React, { useState, useEffect, useMemo, useRef } from "react";
import {
  Home, Wallet as WalletIcon, Calculator as CalcIcon, PieChart, Menu,
  Plus, X, ArrowUpRight, ArrowDownRight, Check, ChevronRight, ChevronDown,
  Utensils, Car, ShoppingBag, Film, PiggyBank, AlertTriangle, Pill,
  CreditCard, TrendingUp, MoreHorizontal, Briefcase, Laptop, Gift,
  Building2, Sun, Moon, Bell, Cloud, CloudOff, RefreshCw, LogIn,
  Target, Trash2, Pencil, ChevronLeft, Info, ShieldCheck, User,
} from "lucide-react";

/* ============================== THEME ============================== */

const FONT_LINKS = [
  "https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&display=swap",
  "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap",
];

function useInjectFonts() {
  useEffect(() => {
    FONT_LINKS.forEach((href) => {
      if (!document.querySelector(`link[href="${href}"]`)) {
        const l = document.createElement("link");
        l.rel = "stylesheet";
        l.href = href;
        document.head.appendChild(l);
      }
    });
  }, []);
}

const DISPLAY_FONT = "'Space Grotesk', system-ui, sans-serif";
const BODY_FONT = "'Inter', system-ui, sans-serif";

function useTheme(isDark) {
  return useMemo(() => {
    if (isDark) {
      return {
        appBg: "#0F0F12",
        bg: "#0F0F12",
        surface: "#19191E",
        surfaceRaised: "#212127",
        border: "#2A2A31",
        borderSoft: "#232328",
        text: "#F4F3F1",
        subtext: "#9B9AA3",
        faint: "#6B6A73",
        accent: "#F2A93B",
        accentSoft: "rgba(242,169,59,0.14)",
        accentText: "#1A1206",
        success: "#3ECF8E",
        successSoft: "rgba(62,207,142,0.14)",
        danger: "#FB6F6F",
        dangerSoft: "rgba(251,111,111,0.14)",
        warn: "#F5C451",
        warnSoft: "rgba(245,196,81,0.14)",
        navBg: "#16161B",
        shadow: "0 12px 30px rgba(0,0,0,0.45)",
      };
    }
    return {
      appBg: "#F3F1EC",
      bg: "#F3F1EC",
      surface: "#FFFFFF",
      surfaceRaised: "#FFFFFF",
      border: "#E7E3D9",
      borderSoft: "#EFEBE1",
      text: "#211D14",
      subtext: "#6F6A5C",
      faint: "#9B9584",
      accent: "#B5720E",
      accentSoft: "rgba(181,114,14,0.10)",
      accentText: "#FFFFFF",
      success: "#1F9D63",
      successSoft: "rgba(31,157,99,0.10)",
      danger: "#D14343",
      dangerSoft: "rgba(209,67,67,0.10)",
      warn: "#B98A15",
      warnSoft: "rgba(185,138,21,0.12)",
      navBg: "#FFFFFF",
      shadow: "0 12px 24px rgba(90,70,30,0.10)",
    };
  }, [isDark]);
}

/* ============================== HELPERS ============================== */

const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36);
const todayISO = () => new Date().toISOString().slice(0, 10);
const monthKey = (d) => (d ? d.slice(0, 7) : todayISO().slice(0, 7));
const fmtMoney = (n) => {
  const v = Number(n) || 0;
  const sign = v < 0 ? "-" : "";
  return sign + "₹" + Math.abs(v).toLocaleString("en-IN", { maximumFractionDigits: 0 });
};
const fmtMoneyPrecise = (n) => {
  const v = Number(n) || 0;
  return "₹" + v.toLocaleString("en-IN", { maximumFractionDigits: 2 });
};
const clamp = (n, lo, hi) => Math.min(hi, Math.max(lo, n));
const isFiniteNum = (n) => typeof n === "number" && Number.isFinite(n);

const EXPENSE_CATEGORIES = [
  { id: "rent", name: "Rent", group: "NEEDS", icon: Building2 },
  { id: "food", name: "Food", group: "NEEDS", icon: Utensils },
  { id: "transport", name: "Transport", group: "NEEDS", icon: Car },
  { id: "shopping", name: "Shopping", group: "WANTS", icon: ShoppingBag },
  { id: "entertainment", name: "Entertainment", group: "WANTS", icon: Film },
  { id: "savings", name: "Savings", group: "SAVINGS", icon: PiggyBank },
  { id: "emergency", name: "Emergency", group: "SAVINGS", icon: AlertTriangle },
  { id: "medicine", name: "Medicine", group: "NEEDS", icon: Pill },
  { id: "emi", name: "EMI", group: "NEEDS", icon: CreditCard },
  { id: "investment", name: "Investment", group: "SAVINGS", icon: TrendingUp },
  { id: "other", name: "Other", group: "OTHER", icon: MoreHorizontal },
];

const INCOME_CATEGORIES = [
  { id: "salary", name: "Salary", icon: Briefcase },
  { id: "freelance", name: "Freelance", icon: Laptop },
  { id: "bonus", name: "Bonus", icon: Gift },
  { id: "other_income", name: "Other", icon: MoreHorizontal },
];

const ICON_MAP = {};
[...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES].forEach((c) => (ICON_MAP[c.id] = c.icon));

function seedTransactions() {
  const now = new Date();
  const d = (offset) => {
    const x = new Date(now);
    x.setDate(x.getDate() - offset);
    return x.toISOString().slice(0, 10);
  };
  return [
    { id: uid(), type: "INCOME", categoryId: "salary", amount: 45000, date: d(6), time: "09:00", description: "Monthly salary", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "rent", amount: 12000, date: d(5), time: "10:00", description: "Rent", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "food", amount: 650, date: d(4), time: "13:20", description: "Groceries", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "transport", amount: 320, date: d(3), time: "08:15", description: "Cab", status: "ACTIVE" },
    { id: uid(), type: "INCOME", categoryId: "freelance", amount: 6000, date: d(3), time: "18:00", description: "Design gig", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "shopping", amount: 1800, date: d(2), time: "17:00", description: "Shoes", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "food", amount: 480, date: d(1), time: "20:30", description: "Dinner out", status: "ACTIVE" },
    { id: uid(), type: "EXPENSE", categoryId: "entertainment", amount: 599, date: d(0), time: "21:00", description: "Streaming", status: "ACTIVE" },
  ];
}

function seedBudget() {
  return {
    month: monthKey(todayISO()),
    income: 51000,
    allocations: {
      rent: 12000, food: 6000, transport: 2500, shopping: 3000,
      entertainment: 1500, savings: 8000, emergency: 2000,
      medicine: 1000, emi: 0, investment: 5000, other: 1000,
    },
    customCategories: [],
  };
}

function seedGoals() {
  return [
    { id: uid(), name: "New Laptop", target: 100000, current: 40000, targetDate: "2026-12-31" },
    { id: uid(), name: "Emergency Fund", target: 60000, current: 22000, targetDate: "2026-10-01" },
  ];
}

/* ============================== FINANCIAL ENGINES ============================== */

function computeBalance(transactions) {
  return transactions
    .filter((t) => t.status === "ACTIVE")
    .reduce((sum, t) => sum + (t.type === "INCOME" ? t.amount : -t.amount), 0);
}

function utilizationPct(spent, allocated) {
  if (!allocated || allocated <= 0) return spent > 0 ? 100 : 0;
  return clamp((spent / allocated) * 100, 0, 999);
}

function thresholdFor(pct) {
  if (pct >= 100.0001) return pct > 100 ? "EXCEEDED" : "100";
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
  if (!isFiniteNum(P) || !isFiniteNum(R) || !isFiniteNum(T) || P <= 0 || T <= 0 || n <= 0) return null;
  const r = R / 100;
  const A = P * Math.pow(1 + r / n, n * T);
  return { total: A, interest: A - P };
}

function sipFutureValue(monthly, annualReturnPct, years) {
  if (!isFiniteNum(monthly) || monthly <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const invested = monthly * n;
  if (!annualReturnPct || annualReturnPct === 0) {
    return { futureValue: invested, invested, gain: 0 };
  }
  const i = (annualReturnPct / 12) / 100;
  const fv = monthly * ((Math.pow(1 + i, n) - 1) / i) * (1 + i);
  return { futureValue: fv, invested, gain: fv - invested };
}

function requiredMonthlySavings(target, annualReturnPct, years) {
  if (!isFiniteNum(target) || target <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  if (!annualReturnPct || annualReturnPct === 0) {
    return { monthly: target / n, totalDeposited: target, gain: 0 };
  }
  const i = (annualReturnPct / 12) / 100;
  const factor = ((Math.pow(1 + i, n) - 1) / i) * (1 + i);
  const monthly = target / factor;
  return { monthly, totalDeposited: monthly * n, gain: target - monthly * n };
}

function emiCalc(P, annualRatePct, years) {
  if (!isFiniteNum(P) || P <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const r = (annualRatePct || 0) / 12 / 100;
  let emi;
  if (r === 0) emi = P / n;
  else emi = (P * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
  const totalPayment = emi * n;
  return { emi, totalPayment, totalInterest: totalPayment - P, n };
}

function loanAffordability(desiredEmi, annualRatePct, years) {
  if (!isFiniteNum(desiredEmi) || desiredEmi <= 0 || !isFiniteNum(years) || years <= 0) return null;
  const n = Math.round(years * 12);
  const r = (annualRatePct || 0) / 12 / 100;
  let principal;
  if (r === 0) principal = desiredEmi * n;
  else principal = (desiredEmi * (Math.pow(1 + r, n) - 1)) / (r * Math.pow(1 + r, n));
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

/* ============================== SMALL UI PRIMITIVES ============================== */

function Card({ t, children, style, className = "", onClick }) {
  return (
    <div
      onClick={onClick}
      className={className}
      style={{
        background: t.surface,
        border: `1px solid ${t.border}`,
        borderRadius: 18,
        ...style,
      }}
    >
      {children}
    </div>
  );
}

function Field({ t, label, children }) {
  return (
    <label className="block" style={{ marginBottom: 14 }}>
      <div style={{ color: t.subtext, fontSize: 12, fontWeight: 600, marginBottom: 6, letterSpacing: 0.3, textTransform: "uppercase" }}>
        {label}
      </div>
      {children}
    </label>
  );
}

function inputStyle(t, hasError) {
  return {
    width: "100%",
    background: t.bg,
    border: `1.5px solid ${hasError ? t.danger : t.border}`,
    borderRadius: 12,
    padding: "11px 13px",
    color: t.text,
    fontSize: 15,
    outline: "none",
    fontFamily: BODY_FONT,
    boxSizing: "border-box",
  };
}

function PrimaryButton({ t, children, onClick, disabled, style }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        width: "100%",
        padding: "13px 16px",
        borderRadius: 14,
        border: "none",
        background: disabled ? t.border : t.accent,
        color: disabled ? t.faint : t.accentText,
        fontWeight: 700,
        fontSize: 15,
        cursor: disabled ? "not-allowed" : "pointer",
        fontFamily: BODY_FONT,
        transition: "transform 0.08s ease",
        ...style,
      }}
      onMouseDown={(e) => { if (!disabled) e.currentTarget.style.transform = "scale(0.98)"; }}
      onMouseUp={(e) => { e.currentTarget.style.transform = "scale(1)"; }}
    >
      {children}
    </button>
  );
}

function GhostButton({ t, children, onClick, style }) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: "13px 16px",
        borderRadius: 14,
        border: `1.5px solid ${t.border}`,
        background: "transparent",
        color: t.text,
        fontWeight: 600,
        fontSize: 15,
        cursor: "pointer",
        fontFamily: BODY_FONT,
        ...style,
      }}
    >
      {children}
    </button>
  );
}

function Pill({ t, color, bg, children, icon: Icon }) {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 5,
        padding: "4px 10px",
        borderRadius: 999,
        background: bg,
        color,
        fontSize: 12,
        fontWeight: 700,
      }}
    >
      {Icon && <Icon size={12} strokeWidth={2.5} />}
      {children}
    </span>
  );
}

function ProgressRing({ t, pct, size = 76, stroke = 8, color }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const clamped = clamp(pct, 0, 100);
  const dash = (clamped / 100) * c;
  const ringColor = color || (pct >= 100 ? t.danger : pct >= 90 ? t.warn : t.accent);
  return (
    <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
      <circle cx={size / 2} cy={size / 2} r={r} stroke={t.borderSoft} strokeWidth={stroke} fill="none" />
      <circle
        cx={size / 2} cy={size / 2} r={r} stroke={ringColor} strokeWidth={stroke} fill="none"
        strokeDasharray={`${dash} ${c}`} strokeLinecap="round"
        style={{ transition: "stroke-dasharray 0.4s ease" }}
      />
    </svg>
  );
}

function BottomSheet({ t, title, onClose, children }) {
  return (
    <div
      style={{
        position: "absolute", inset: 0, background: "rgba(0,0,0,0.55)",
        display: "flex", alignItems: "flex-end", zIndex: 50,
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: t.surface, width: "100%", maxHeight: "88%",
          borderTopLeftRadius: 24, borderTopRightRadius: 24,
          padding: "18px 18px 22px", overflowY: "auto",
          borderTop: `1px solid ${t.border}`,
        }}
      >
        <div style={{ width: 40, height: 4, background: t.border, borderRadius: 4, margin: "0 auto 14px" }} />
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
          <h3 style={{ margin: 0, fontFamily: DISPLAY_FONT, fontSize: 19, color: t.text, fontWeight: 600 }}>{title}</h3>
          <button onClick={onClose} style={{ background: t.borderSoft, border: "none", borderRadius: 999, width: 30, height: 30, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
            <X size={16} color={t.subtext} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function SectionLabel({ t, children, right }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", margin: "22px 0 10px" }}>
      <div style={{ fontSize: 13, fontWeight: 700, color: t.subtext, textTransform: "uppercase", letterSpacing: 0.5 }}>{children}</div>
      {right}
    </div>
  );
}

function EmptyState({ t, icon: Icon, title, sub }) {
  return (
    <div style={{ textAlign: "center", padding: "36px 20px", color: t.subtext }}>
      <Icon size={30} style={{ opacity: 0.5, marginBottom: 10 }} />
      <div style={{ color: t.text, fontWeight: 600, marginBottom: 4 }}>{title}</div>
      <div style={{ fontSize: 13 }}>{sub}</div>
    </div>
  );
}

/* ============================== TRANSACTION ROW ============================== */

function TxRow({ t, tx, onClick }) {
  const cat = ICON_MAP[tx.categoryId] || MoreHorizontal;
  const Icon = cat;
  const isIncome = tx.type === "INCOME";
  const voided = tx.status === "VOIDED";
  const allCats = [...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES];
  const catName = (allCats.find((c) => c.id === tx.categoryId) || {}).name || "Other";
  return (
    <div
      onClick={() => onClick(tx)}
      style={{
        display: "flex", alignItems: "center", gap: 12, padding: "11px 4px",
        cursor: "pointer", opacity: voided ? 0.45 : 1,
      }}
    >
      <div
        style={{
          width: 40, height: 40, borderRadius: 12, display: "flex", alignItems: "center", justifyContent: "center",
          background: isIncome ? t.successSoft : t.borderSoft, flexShrink: 0,
        }}
      >
        <Icon size={18} color={isIncome ? t.success : t.subtext} />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ color: t.text, fontWeight: 600, fontSize: 14.5, textDecoration: voided ? "line-through" : "none", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
          {tx.description || catName}
        </div>
        <div style={{ color: t.faint, fontSize: 12, marginTop: 1 }}>
          {catName} · {tx.date}{voided ? " · Voided" : ""}
        </div>
      </div>
      <div style={{ fontFamily: DISPLAY_FONT, fontWeight: 600, fontSize: 15, color: voided ? t.faint : isIncome ? t.success : t.text, flexShrink: 0 }}>
        {isIncome ? "+" : "-"}{fmtMoney(tx.amount)}
      </div>
    </div>
  );
}

/* ============================== ADD TRANSACTION SHEET ============================== */

function AddTransactionSheet({ t, onClose, onSave, editing }) {
  const [type, setType] = useState(editing?.type || "EXPENSE");
  const [amount, setAmount] = useState(editing ? String(editing.amount) : "");
  const [categoryId, setCategoryId] = useState(editing?.categoryId || "food");
  const [date, setDate] = useState(editing?.date || todayISO());
  const [description, setDescription] = useState(editing?.description || "");
  const [error, setError] = useState("");

  const cats = type === "INCOME" ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

  useEffect(() => {
    if (!editing) setCategoryId(type === "INCOME" ? "salary" : "food");
  }, [type]);

  function submit() {
    const amt = parseFloat(amount);
    if (!amount || isNaN(amt) || !isFinite(amt) || amt <= 0) {
      setError("Enter an amount greater than 0.");
      return;
    }
    if (amt > 100000000000) {
      setError("That amount is too large.");
      return;
    }
    if (!date) {
      setError("Select a valid date.");
      return;
    }
    if (description.length > 200) {
      setError("Description is too long (max 200 characters).");
      return;
    }
    onSave({
      id: editing?.id || uid(),
      type, amount: amt, categoryId, date,
      time: editing?.time || new Date().toTimeString().slice(0, 5),
      description: description.trim(),
      status: editing?.status || "ACTIVE",
    });
  }

  return (
    <BottomSheet t={t} title={editing ? "Edit Transaction" : "Add Transaction"} onClose={onClose}>
      <div style={{ display: "flex", gap: 8, marginBottom: 18 }}>
        {["EXPENSE", "INCOME"].map((tp) => (
          <button
            key={tp}
            onClick={() => setType(tp)}
            style={{
              flex: 1, padding: "11px 0", borderRadius: 12, border: `1.5px solid ${type === tp ? t.accent : t.border}`,
              background: type === tp ? t.accentSoft : "transparent",
              color: type === tp ? t.accent : t.subtext, fontWeight: 700, cursor: "pointer", fontFamily: BODY_FONT,
            }}
          >
            {tp === "EXPENSE" ? "Expense" : "Income"}
          </button>
        ))}
      </div>

      <Field t={t} label="Amount (₹)">
        <input
          style={inputStyle(t, false)}
          inputMode="decimal"
          placeholder="0.00"
          value={amount}
          onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ""))}
        />
      </Field>

      <Field t={t} label="Category">
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {cats.map((c) => {
            const Icon = c.icon;
            const active = categoryId === c.id;
            return (
              <button
                key={c.id}
                onClick={() => setCategoryId(c.id)}
                style={{
                  display: "flex", alignItems: "center", gap: 6, padding: "8px 12px", borderRadius: 999,
                  border: `1.5px solid ${active ? t.accent : t.border}`,
                  background: active ? t.accentSoft : "transparent",
                  color: active ? t.accent : t.subtext, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: BODY_FONT,
                }}
              >
                <Icon size={14} /> {c.name}
              </button>
            );
          })}
        </div>
      </Field>

      <Field t={t} label="Date">
        <input type="date" style={inputStyle(t, false)} value={date} onChange={(e) => setDate(e.target.value)} />
      </Field>

      <Field t={t} label="Description (optional)">
        <input
          style={inputStyle(t, false)}
          placeholder="e.g. Groceries"
          value={description}
          maxLength={200}
          onChange={(e) => setDescription(e.target.value)}
        />
      </Field>

      {error && (
        <div style={{ display: "flex", gap: 8, alignItems: "flex-start", color: t.danger, fontSize: 13, marginBottom: 12, background: t.dangerSoft, padding: "9px 11px", borderRadius: 10 }}>
          <Info size={15} style={{ flexShrink: 0, marginTop: 1 }} /> {error}
        </div>
      )}

      <PrimaryButton t={t} onClick={submit}>{editing ? "Save Changes" : "Save Transaction"}</PrimaryButton>
      {editing && editing.status === "ACTIVE" && (
        <GhostButton
          t={t}
          style={{ width: "100%", marginTop: 10 }}
          onClick={() => onSave({ ...editing, status: "VOIDED" })}
        >
          Void Transaction
        </GhostButton>
      )}
    </BottomSheet>
  );
}

/* ============================== HOME / DASHBOARD ============================== */

function HomeScreen({ t, transactions, budget, goals, onOpenAdd, onTxClick, isDark, setIsDark }) {
  const active = transactions.filter((tx) => tx.status === "ACTIVE");
  const balance = computeBalance(transactions);
  const mKey = monthKey(todayISO());
  const monthTx = active.filter((tx) => monthKey(tx.date) === mKey);
  const monthIncome = monthTx.filter((t2) => t2.type === "INCOME").reduce((s, t2) => s + t2.amount, 0);
  const monthExpense = monthTx.filter((t2) => t2.type === "EXPENSE").reduce((s, t2) => s + t2.amount, 0);
  const totalAllocated = Object.values(budget.allocations).reduce((a, b) => a + (b || 0), 0);
  const utilization = utilizationPct(monthExpense, totalAllocated);
  const remainingBudget = totalAllocated - monthExpense;

  const essentialCats = EXPENSE_CATEGORIES.filter((c) => c.group === "NEEDS").map((c) => c.id);
  const essentialRemaining = essentialCats.reduce((sum, id) => {
    const alloc = budget.allocations[id] || 0;
    const spent = monthTx.filter((tx) => tx.type === "EXPENSE" && tx.categoryId === id).reduce((s, tx) => s + tx.amount, 0);
    return sum + Math.max(0, alloc - spent);
  }, 0);
  const savingsCommitment = Math.max(0, budget.allocations.savings || 0);
  const safeToSpend = Math.max(0, balance - (essentialRemaining + savingsCommitment));

  const plannedSavingsPct = 20;
  const actualSavingsPct = monthIncome > 0 ? ((monthIncome - monthExpense) / monthIncome) * 100 : 0;

  // alerts
  const alerts = [];
  EXPENSE_CATEGORIES.forEach((c) => {
    const alloc = budget.allocations[c.id];
    if (!alloc) return;
    const spent = monthTx.filter((tx) => tx.type === "EXPENSE" && tx.categoryId === c.id).reduce((s, tx) => s + tx.amount, 0);
    const pct = utilizationPct(spent, alloc);
    const th = thresholdFor(pct);
    if (th) {
      alerts.push({ id: c.id, name: c.name, pct, th, spent, alloc });
    }
  });
  alerts.sort((a, b) => b.pct - a.pct);

  const topCategory = (() => {
    const byCat = {};
    monthTx.filter((t2) => t2.type === "EXPENSE").forEach((t2) => (byCat[t2.categoryId] = (byCat[t2.categoryId] || 0) + t2.amount));
    const entries = Object.entries(byCat).sort((a, b) => b[1] - a[1]);
    return entries[0];
  })();

  const insights = [];
  if (topCategory) {
    const cat = EXPENSE_CATEGORIES.find((c) => c.id === topCategory[0]);
    insights.push(`Your biggest spend this month is ${cat?.name || "Other"} at ${fmtMoney(topCategory[1])}.`);
  }
  insights.push(
    actualSavingsPct >= plannedSavingsPct
      ? `You're saving ${actualSavingsPct.toFixed(0)}% of income — at or above your 20% target.`
      : `Your actual savings (${actualSavingsPct.toFixed(0)}%) are below your 20% target this month.`
  );
  if (alerts.length > 0) {
    insights.push(`You have used ${alerts[0].pct.toFixed(0)}% of your ${alerts[0].name} budget.`);
  }

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 18 }}>
        <div>
          <div style={{ color: t.subtext, fontSize: 13 }}>Good day 👋</div>
          <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontSize: 19, fontWeight: 600 }}>Wallet Scholer</div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button onClick={() => setIsDark(!isDark)} style={{ width: 38, height: 38, borderRadius: 12, border: `1px solid ${t.border}`, background: t.surface, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
            {isDark ? <Sun size={16} color={t.text} /> : <Moon size={16} color={t.text} />}
          </button>
        </div>
      </div>

      {/* Wallet card - signature element */}
      <div
        style={{
          borderRadius: 22, padding: "22px 22px 24px", position: "relative", overflow: "hidden",
          background: isDark
            ? "linear-gradient(135deg, #1F1B12 0%, #17151A 55%, #141318 100%)"
            : "linear-gradient(135deg, #FCEFD6 0%, #F6EEDD 60%, #F3F1EC 100%)",
          border: `1px solid ${isDark ? "#2E2718" : "#EADFBF"}`,
          boxShadow: t.shadow,
        }}
      >
        <div style={{ position: "absolute", top: -40, right: -40, width: 160, height: 160, borderRadius: "50%", background: t.accentSoft, filter: "blur(6px)" }} />
        <div style={{ position: "relative" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <div style={{ color: t.subtext, fontSize: 12.5, fontWeight: 700, letterSpacing: 0.6, textTransform: "uppercase" }}>Current Balance</div>
            <div style={{ width: 30, height: 20, borderRadius: 4, background: t.accent, opacity: 0.85 }} />
          </div>
          <div style={{ fontFamily: DISPLAY_FONT, fontSize: 38, fontWeight: 700, color: t.text, marginTop: 6, letterSpacing: -0.5 }}>
            {fmtMoney(balance)}
          </div>
          <div style={{ display: "flex", gap: 18, marginTop: 16 }}>
            <div>
              <div style={{ color: t.faint, fontSize: 11.5, fontWeight: 600, textTransform: "uppercase" }}>Safe to spend</div>
              <div style={{ color: t.success, fontFamily: DISPLAY_FONT, fontSize: 17, fontWeight: 600 }}>{fmtMoney(safeToSpend)}</div>
            </div>
            <div>
              <div style={{ color: t.faint, fontSize: 11.5, fontWeight: 600, textTransform: "uppercase" }}>Remaining budget</div>
              <div style={{ color: remainingBudget < 0 ? t.danger : t.text, fontFamily: DISPLAY_FONT, fontSize: 17, fontWeight: 600 }}>{fmtMoney(remainingBudget)}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick stats */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginTop: 14 }}>
        <Card t={t} style={{ padding: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6, color: t.subtext, fontSize: 12, fontWeight: 600 }}>
            <ArrowDownRight size={14} color={t.danger} /> This month spent
          </div>
          <div style={{ fontFamily: DISPLAY_FONT, fontSize: 20, fontWeight: 700, color: t.text, marginTop: 4 }}>{fmtMoney(monthExpense)}</div>
        </Card>
        <Card t={t} style={{ padding: 14 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 6, color: t.subtext, fontSize: 12, fontWeight: 600 }}>
            <ArrowUpRight size={14} color={t.success} /> This month income
          </div>
          <div style={{ fontFamily: DISPLAY_FONT, fontSize: 20, fontWeight: 700, color: t.text, marginTop: 4 }}>{fmtMoney(monthIncome)}</div>
        </Card>
      </div>

      {/* Budget utilization */}
      <Card t={t} style={{ padding: 16, marginTop: 12, display: "flex", alignItems: "center", gap: 16 }}>
        <ProgressRing t={t} pct={utilization} />
        <div style={{ flex: 1 }}>
          <div style={{ color: t.text, fontWeight: 700, fontSize: 15 }}>Budget utilization</div>
          <div style={{ color: t.subtext, fontSize: 12.5, marginTop: 2 }}>{fmtMoney(monthExpense)} of {fmtMoney(totalAllocated)} used</div>
          <div style={{ marginTop: 8 }}>
            <Pill
              t={t}
              color={utilization >= 100 ? t.danger : utilization >= 90 ? t.warn : t.success}
              bg={utilization >= 100 ? t.dangerSoft : utilization >= 90 ? t.warnSoft : t.successSoft}
            >
              {utilization.toFixed(0)}% used
            </Pill>
          </div>
        </div>
      </Card>

      {/* Alerts */}
      {alerts.length > 0 && (
        <>
          <SectionLabel t={t}>Alerts</SectionLabel>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {alerts.slice(0, 3).map((a) => {
              const exceeded = a.th === "EXCEEDED";
              const color = exceeded ? t.danger : a.th === "100" ? t.danger : a.th === "90" ? t.warn : t.accent;
              const bg = exceeded || a.th === "100" ? t.dangerSoft : a.th === "90" ? t.warnSoft : t.accentSoft;
              const msg = exceeded
                ? `You exceeded your ${a.name} budget by ${fmtMoney(a.spent - a.alloc)}.`
                : a.th === "100"
                ? `You have reached your ${a.name} budget limit.`
                : a.th === "90"
                ? `You have almost reached your ${a.name} budget.`
                : `You have used ${a.pct.toFixed(0)}% of your ${a.name} budget.`;
              return (
                <Card key={a.id} t={t} style={{ padding: "12px 14px", display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ width: 34, height: 34, borderRadius: 10, background: bg, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    <Bell size={15} color={color} />
                  </div>
                  <div style={{ color: t.text, fontSize: 13.5, fontWeight: 500 }}>{msg}</div>
                </Card>
              );
            })}
          </div>
        </>
      )}

      <SectionLabel t={t}>Insights</SectionLabel>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {insights.map((ins, i) => (
          <Card key={i} t={t} style={{ padding: "12px 14px", display: "flex", gap: 10, alignItems: "flex-start" }}>
            <TrendingUp size={15} color={t.accent} style={{ marginTop: 2, flexShrink: 0 }} />
            <div style={{ color: t.text, fontSize: 13.5 }}>{ins}</div>
          </Card>
        ))}
      </div>

      {goals.length > 0 && (
        <>
          <SectionLabel t={t}>Savings Goals</SectionLabel>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {goals.slice(0, 2).map((g) => {
              const pct = clamp((g.current / g.target) * 100, 0, 100);
              return (
                <Card key={g.id} t={t} style={{ padding: 14, display: "flex", alignItems: "center", gap: 14 }}>
                  <ProgressRing t={t} pct={pct} size={54} stroke={6} color={t.success} />
                  <div style={{ flex: 1 }}>
                    <div style={{ color: t.text, fontWeight: 700, fontSize: 14 }}>{g.name}</div>
                    <div style={{ color: t.subtext, fontSize: 12.5 }}>{fmtMoney(g.current)} of {fmtMoney(g.target)} · {pct.toFixed(0)}%</div>
                  </div>
                </Card>
              );
            })}
          </div>
        </>
      )}

      <div style={{ height: 90 }} />
    </div>
  );
}

/* ============================== WALLET SCREEN ============================== */

function WalletScreen({ t, transactions, onTxClick }) {
  const [filter, setFilter] = useState("ALL");
  const sorted = [...transactions].sort((a, b) => (a.date + a.time < b.date + b.time ? 1 : -1));
  const filtered = sorted.filter((tx) => {
    if (filter === "ALL") return true;
    if (filter === "INCOME") return tx.type === "INCOME";
    if (filter === "EXPENSE") return tx.type === "EXPENSE";
    if (filter === "VOIDED") return tx.status === "VOIDED";
    return true;
  });

  const groups = {};
  filtered.forEach((tx) => {
    groups[tx.date] = groups[tx.date] || [];
    groups[tx.date].push(tx);
  });
  const dateKeys = Object.keys(groups).sort((a, b) => (a < b ? 1 : -1));

  return (
    <div>
      <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontSize: 22, fontWeight: 600, marginBottom: 14 }}>Wallet</div>
      <div style={{ display: "flex", gap: 8, marginBottom: 14, overflowX: "auto" }}>
        {["ALL", "INCOME", "EXPENSE", "VOIDED"].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            style={{
              padding: "7px 14px", borderRadius: 999, whiteSpace: "nowrap",
              border: `1.5px solid ${filter === f ? t.accent : t.border}`,
              background: filter === f ? t.accentSoft : "transparent",
              color: filter === f ? t.accent : t.subtext, fontWeight: 700, fontSize: 12.5, cursor: "pointer", fontFamily: BODY_FONT,
            }}
          >
            {f === "ALL" ? "All" : f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {dateKeys.length === 0 && <EmptyState t={t} icon={WalletIcon} title="No transactions yet" sub="Tap + to add your first income or expense." />}

      {dateKeys.map((dk) => (
        <div key={dk} style={{ marginBottom: 6 }}>
          <div style={{ color: t.faint, fontSize: 12, fontWeight: 700, margin: "14px 0 2px", textTransform: "uppercase", letterSpacing: 0.4 }}>
            {dk === todayISO() ? "Today" : dk}
          </div>
          <Card t={t} style={{ padding: "2px 12px" }}>
            {groups[dk].map((tx, i) => (
              <div key={tx.id} style={{ borderTop: i > 0 ? `1px solid ${t.borderSoft}` : "none" }}>
                <TxRow t={t} tx={tx} onClick={onTxClick} />
              </div>
            ))}
          </Card>
        </div>
      ))}
      <div style={{ height: 90 }} />
    </div>
  );
}

/* ============================== CALCULATOR HUB ============================== */

const CALC_MODULES = [
  { id: "emi", label: "EMI" },
  { id: "loan", label: "Loan" },
  { id: "si", label: "Simple Interest" },
  { id: "ci", label: "Compound Interest" },
  { id: "savings", label: "Savings" },
  { id: "sip", label: "SIP" },
  { id: "pct", label: "Percentage" },
];

function CalcInput({ t, label, value, onChange, suffix, placeholder }) {
  return (
    <Field t={t} label={label}>
      <div style={{ position: "relative" }}>
        <input
          style={inputStyle(t, false)}
          inputMode="decimal"
          placeholder={placeholder || "0"}
          value={value}
          onChange={(e) => onChange(e.target.value.replace(/[^0-9.]/g, ""))}
        />
        {suffix && <span style={{ position: "absolute", right: 13, top: "50%", transform: "translateY(-50%)", color: t.faint, fontSize: 13 }}>{suffix}</span>}
      </div>
    </Field>
  );
}

function ResultPanel({ t, rows, disclaimer }) {
  return (
    <Card t={t} style={{ padding: 16, marginTop: 4, background: t.accentSoft, border: `1px solid ${t.accent}33` }}>
      {rows.map((r, i) => (
        <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "5px 0" }}>
          <div style={{ color: t.subtext, fontSize: 13 }}>{r.label}</div>
          <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontWeight: 700, fontSize: r.big ? 20 : 15 }}>{r.value}</div>
        </div>
      ))}
      {disclaimer && (
        <div style={{ display: "flex", gap: 6, marginTop: 8, color: t.subtext, fontSize: 11.5, alignItems: "flex-start" }}>
          <Info size={13} style={{ flexShrink: 0, marginTop: 1 }} /> {disclaimer}
        </div>
      )}
    </Card>
  );
}

function CalculatorScreen({ t }) {
  const [tab, setTab] = useState("emi");
  return (
    <div>
      <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontSize: 22, fontWeight: 600, marginBottom: 14 }}>Financial Calculator</div>
      <div style={{ display: "flex", gap: 6, overflowX: "auto", marginBottom: 18, paddingBottom: 2 }}>
        {CALC_MODULES.map((m) => (
          <button
            key={m.id}
            onClick={() => setTab(m.id)}
            style={{
              padding: "8px 14px", borderRadius: 999, whiteSpace: "nowrap",
              border: `1.5px solid ${tab === m.id ? t.accent : t.border}`,
              background: tab === m.id ? t.accentSoft : "transparent",
              color: tab === m.id ? t.accent : t.subtext, fontWeight: 700, fontSize: 12.5, cursor: "pointer", fontFamily: BODY_FONT,
            }}
          >
            {m.label}
          </button>
        ))}
      </div>
      {tab === "emi" && <EmiCalc t={t} />}
      {tab === "loan" && <LoanCalc t={t} />}
      {tab === "si" && <SiCalc t={t} />}
      {tab === "ci" && <CiCalc t={t} />}
      {tab === "savings" && <SavingsCalc t={t} />}
      {tab === "sip" && <SipCalc t={t} />}
      {tab === "pct" && <PctCalc t={t} />}
      <div style={{ height: 90 }} />
    </div>
  );
}

function ResetRow({ t, onReset }) {
  return (
    <button
      onClick={onReset}
      style={{ background: "none", border: "none", color: t.subtext, fontSize: 12.5, fontWeight: 700, cursor: "pointer", padding: "10px 0 0", display: "block" }}
    >
      Reset
    </button>
  );
}

function EmiCalc({ t }) {
  const [p, setP] = useState("500000");
  const [r, setR] = useState("9.5");
  const [y, setY] = useState("5");
  const res = emiCalc(parseFloat(p), parseFloat(r), parseFloat(y));
  return (
    <div>
      <CalcInput t={t} label="Loan Amount" value={p} onChange={setP} suffix="₹" />
      <CalcInput t={t} label="Annual Interest Rate" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Tenure" value={y} onChange={setY} suffix="years" />
      {res ? (
        <ResultPanel t={t} rows={[
          { label: "Monthly EMI", value: fmtMoneyPrecise(res.emi), big: true },
          { label: "Total Interest", value: fmtMoney(res.totalInterest) },
          { label: "Total Payment", value: fmtMoney(res.totalPayment) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter valid loan amount and tenure", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setP("500000"); setR("9.5"); setY("5"); }} />
    </div>
  );
}

function LoanCalc({ t }) {
  const [emi, setEmi] = useState("15000");
  const [r, setR] = useState("9.5");
  const [y, setY] = useState("5");
  const res = loanAffordability(parseFloat(emi), parseFloat(r), parseFloat(y));
  return (
    <div>
      <div style={{ color: t.subtext, fontSize: 12.5, marginBottom: 12 }}>Find out how much you can borrow for a given monthly payment.</div>
      <CalcInput t={t} label="Desired Monthly Payment" value={emi} onChange={setEmi} suffix="₹" />
      <CalcInput t={t} label="Annual Interest Rate" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Tenure" value={y} onChange={setY} suffix="years" />
      {res ? (
        <ResultPanel t={t} rows={[
          { label: "Max Loan Amount", value: fmtMoney(res.principal), big: true },
          { label: "Total Interest", value: fmtMoney(res.totalInterest) },
          { label: "Total Payment", value: fmtMoney(res.totalPayment) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter a valid monthly payment and tenure", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setEmi("15000"); setR("9.5"); setY("5"); }} />
    </div>
  );
}

function SiCalc({ t }) {
  const [p, setP] = useState("100000");
  const [r, setR] = useState("6");
  const [y, setY] = useState("2");
  const res = simpleInterest(parseFloat(p), parseFloat(r), parseFloat(y));
  return (
    <div>
      <CalcInput t={t} label="Principal" value={p} onChange={setP} suffix="₹" />
      <CalcInput t={t} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Time" value={y} onChange={setY} suffix="years" />
      {res ? (
        <ResultPanel t={t} rows={[
          { label: "Interest Earned", value: fmtMoney(res.interest), big: true },
          { label: "Final Amount", value: fmtMoney(res.total) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter a valid principal and time", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setP("100000"); setR("6"); setY("2"); }} />
    </div>
  );
}

function CiCalc({ t }) {
  const [p, setP] = useState("100000");
  const [r, setR] = useState("7");
  const [y, setY] = useState("3");
  const [n, setN] = useState("12");
  const res = compoundInterest(parseFloat(p), parseFloat(r), parseFloat(y), parseFloat(n));
  return (
    <div>
      <CalcInput t={t} label="Principal" value={p} onChange={setP} suffix="₹" />
      <CalcInput t={t} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Time" value={y} onChange={setY} suffix="years" />
      <Field t={t} label="Compounding Frequency">
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          {[["1", "Annually"], ["2", "Semi-annual"], ["4", "Quarterly"], ["12", "Monthly"], ["365", "Daily"]].map(([val, lbl]) => (
            <button key={val} onClick={() => setN(val)} style={{
              padding: "7px 12px", borderRadius: 999, border: `1.5px solid ${n === val ? t.accent : t.border}`,
              background: n === val ? t.accentSoft : "transparent", color: n === val ? t.accent : t.subtext,
              fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: BODY_FONT,
            }}>{lbl}</button>
          ))}
        </div>
      </Field>
      {res ? (
        <ResultPanel t={t} rows={[
          { label: "Interest Earned", value: fmtMoney(res.interest), big: true },
          { label: "Final Amount", value: fmtMoney(res.total) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter a valid principal and time", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setP("100000"); setR("7"); setY("3"); setN("12"); }} />
    </div>
  );
}

function SavingsCalc({ t }) {
  const [target, setTarget] = useState("100000");
  const [r, setR] = useState("6");
  const [y, setY] = useState("2");
  const res = requiredMonthlySavings(parseFloat(target), parseFloat(r), parseFloat(y));
  return (
    <div>
      <div style={{ color: t.subtext, fontSize: 12.5, marginBottom: 12 }}>Find the monthly amount you need to save to hit a goal.</div>
      <CalcInput t={t} label="Target Amount" value={target} onChange={setTarget} suffix="₹" />
      <CalcInput t={t} label="Expected Annual Return" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Duration" value={y} onChange={setY} suffix="years" />
      {res ? (
        <ResultPanel t={t} disclaimer="Projected returns are estimates and not guaranteed." rows={[
          { label: "Required Monthly Saving", value: fmtMoneyPrecise(res.monthly), big: true },
          { label: "Total Deposited", value: fmtMoney(res.totalDeposited) },
          { label: "Estimated Growth", value: fmtMoney(res.gain) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter a valid target and duration", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setTarget("100000"); setR("6"); setY("2"); }} />
    </div>
  );
}

function SipCalc({ t }) {
  const [m, setM] = useState("5000");
  const [r, setR] = useState("12");
  const [y, setY] = useState("10");
  const res = sipFutureValue(parseFloat(m), parseFloat(r), parseFloat(y));
  return (
    <div>
      <CalcInput t={t} label="Monthly Investment" value={m} onChange={setM} suffix="₹" />
      <CalcInput t={t} label="Expected Annual Return" value={r} onChange={setR} suffix="%" />
      <CalcInput t={t} label="Duration" value={y} onChange={setY} suffix="years" />
      {res ? (
        <ResultPanel t={t} disclaimer="Projected returns are estimates and not guaranteed." rows={[
          { label: "Estimated Future Value", value: fmtMoney(res.futureValue), big: true },
          { label: "Total Invested", value: fmtMoney(res.invested) },
          { label: "Estimated Gain", value: fmtMoney(res.gain) },
        ]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter a valid monthly amount and duration", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setM("5000"); setR("12"); setY("10"); }} />
    </div>
  );
}

function PctCalc({ t }) {
  const [mode, setMode] = useState("of");
  const [x, setX] = useState("15");
  const [y, setY] = useState("2000");
  let result = null, label = "";
  if (mode === "of") { result = percentageOf(parseFloat(x), parseFloat(y)); label = `${x || 0}% of ${y || 0}`; }
  if (mode === "what") { result = whatPercent(parseFloat(x), parseFloat(y)); label = `${x || 0} is what % of ${y || 0}`; }
  if (mode === "increase") { result = percentChange(parseFloat(y), parseFloat(x), "increase"); label = `${y || 0} increased by ${x || 0}%`; }
  if (mode === "decrease") { result = percentChange(parseFloat(y), parseFloat(x), "decrease"); label = `${y || 0} decreased by ${x || 0}%`; }

  return (
    <div>
      <Field t={t} label="Mode">
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          {[["of", "X% of Y"], ["what", "X is what % of Y"], ["increase", "Increase Y by X%"], ["decrease", "Decrease Y by X%"]].map(([val, lbl]) => (
            <button key={val} onClick={() => setMode(val)} style={{
              padding: "7px 12px", borderRadius: 999, border: `1.5px solid ${mode === val ? t.accent : t.border}`,
              background: mode === val ? t.accentSoft : "transparent", color: mode === val ? t.accent : t.subtext,
              fontSize: 12, fontWeight: 700, cursor: "pointer", fontFamily: BODY_FONT,
            }}>{lbl}</button>
          ))}
        </div>
      </Field>
      <CalcInput t={t} label="X" value={x} onChange={setX} suffix={mode === "of" || mode === "what" ? "" : "%"} />
      <CalcInput t={t} label="Y" value={y} onChange={setY} />
      {isFiniteNum(result) ? (
        <ResultPanel t={t} rows={[{ label, value: mode === "what" ? `${result.toFixed(2)}%` : fmtMoneyPrecise(result), big: true }]} />
      ) : <ResultPanel t={t} rows={[{ label: "Enter valid numbers", value: "—" }]} />}
      <ResetRow t={t} onReset={() => { setX("15"); setY("2000"); }} />
    </div>
  );
}

/* ============================== BUDGET SCREEN ============================== */

function BudgetScreen({ t, budget, setBudget, transactions }) {
  const [editing, setEditing] = useState(false);
  const [showAddCat, setShowAddCat] = useState(false);
  const mKey = monthKey(todayISO());
  const monthTx = transactions.filter((tx) => tx.status === "ACTIVE" && tx.type === "EXPENSE" && monthKey(tx.date) === mKey);

  const allCats = [...EXPENSE_CATEGORIES, ...budget.customCategories];
  const totalAllocated = Object.values(budget.allocations).reduce((a, b) => a + (b || 0), 0);
  const totalSpent = monthTx.reduce((s, tx) => s + tx.amount, 0);
  const exceedsIncome = totalAllocated > budget.income;

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 14 }}>
        <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontSize: 22, fontWeight: 600 }}>Budget</div>
        <button onClick={() => setEditing((e) => !e)} style={{ background: "none", border: "none", color: t.accent, fontWeight: 700, fontSize: 13.5, cursor: "pointer", display: "flex", alignItems: "center", gap: 4 }}>
          <Pencil size={14} /> {editing ? "Done" : "Edit"}
        </button>
      </div>

      <Card t={t} style={{ padding: 16 }}>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, color: t.subtext, marginBottom: 6 }}>
          <span>Allocated</span><span>{fmtMoney(totalAllocated)} / {fmtMoney(budget.income)} income</span>
        </div>
        <div style={{ height: 8, borderRadius: 8, background: t.borderSoft, overflow: "hidden" }}>
          <div style={{ height: "100%", width: `${clamp((totalAllocated / (budget.income || 1)) * 100, 0, 100)}%`, background: exceedsIncome ? t.danger : t.accent, borderRadius: 8, transition: "width 0.3s" }} />
        </div>
        {exceedsIncome && (
          <div style={{ display: "flex", gap: 6, alignItems: "flex-start", marginTop: 10, color: t.danger, fontSize: 12.5 }}>
            <AlertTriangle size={14} style={{ flexShrink: 0, marginTop: 1 }} />
            Your allocated budget ({fmtMoney(totalAllocated)}) exceeds your income ({fmtMoney(budget.income)}).
          </div>
        )}
        <div style={{ marginTop: 10 }}>
          <span style={{ color: t.subtext, fontSize: 12.5 }}>Spent so far: </span>
          <span style={{ color: t.text, fontWeight: 700, fontSize: 12.5 }}>{fmtMoney(totalSpent)}</span>
        </div>
      </Card>

      <SectionLabel t={t} right={editing && (
        <button onClick={() => setShowAddCat(true)} style={{ display: "flex", alignItems: "center", gap: 4, background: "none", border: "none", color: t.accent, fontWeight: 700, fontSize: 12.5, cursor: "pointer" }}>
          <Plus size={14} /> Add category
        </button>
      )}>
        Categories
      </SectionLabel>

      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {allCats.map((c) => {
          const alloc = budget.allocations[c.id] || 0;
          const spent = monthTx.filter((tx) => tx.categoryId === c.id).reduce((s, tx) => s + tx.amount, 0);
          const pct = utilizationPct(spent, alloc);
          const Icon = c.icon || Target;
          return (
            <Card key={c.id} t={t} style={{ padding: "12px 14px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <div style={{ width: 34, height: 34, borderRadius: 10, background: t.borderSoft, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                  <Icon size={16} color={t.subtext} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ color: t.text, fontWeight: 600, fontSize: 14 }}>{c.name}</div>
                  <div style={{ color: t.faint, fontSize: 11.5 }}>{fmtMoney(spent)} of {fmtMoney(alloc)}</div>
                </div>
                {editing ? (
                  <input
                    style={{ ...inputStyle(t, false), width: 90, padding: "7px 9px", fontSize: 13 }}
                    inputMode="decimal"
                    value={String(alloc)}
                    onChange={(e) => {
                      const v = parseFloat(e.target.value.replace(/[^0-9.]/g, "")) || 0;
                      setBudget({ ...budget, allocations: { ...budget.allocations, [c.id]: v } });
                    }}
                  />
                ) : (
                  <Pill t={t} color={pct >= 100 ? t.danger : pct >= 90 ? t.warn : t.success} bg={pct >= 100 ? t.dangerSoft : pct >= 90 ? t.warnSoft : t.successSoft}>
                    {pct.toFixed(0)}%
                  </Pill>
                )}
              </div>
              {!editing && (
                <div style={{ height: 6, borderRadius: 6, background: t.borderSoft, marginTop: 10, overflow: "hidden" }}>
                  <div style={{ height: "100%", width: `${clamp(pct, 0, 100)}%`, background: pct >= 100 ? t.danger : pct >= 90 ? t.warn : t.accent, borderRadius: 6 }} />
                </div>
              )}
            </Card>
          );
        })}
      </div>

      {showAddCat && (
        <AddCategorySheet t={t} onClose={() => setShowAddCat(false)} onAdd={(cat, amount) => {
          setBudget({
            ...budget,
            customCategories: [...budget.customCategories, cat],
            allocations: { ...budget.allocations, [cat.id]: amount },
          });
          setShowAddCat(false);
        }} />
      )}
      <div style={{ height: 90 }} />
    </div>
  );
}

const ICON_CHOICES = [
  { key: "target", icon: Target }, { key: "gift", icon: Gift }, { key: "book", icon: Laptop },
  { key: "heart", icon: Pill }, { key: "shield", icon: ShieldCheck }, { key: "more", icon: MoreHorizontal },
];

function AddCategorySheet({ t, onClose, onAdd }) {
  const [name, setName] = useState("");
  const [amount, setAmount] = useState("");
  const [iconKey, setIconKey] = useState("target");
  const [error, setError] = useState("");

  function submit() {
    if (!name.trim() || name.trim().length > 50) { setError("Enter a category name (max 50 characters)."); return; }
    const amt = parseFloat(amount);
    if (!amount || isNaN(amt) || amt <= 0) { setError("Enter a budget amount greater than 0."); return; }
    const IconComp = ICON_CHOICES.find((i) => i.key === iconKey).icon;
    onAdd({ id: "custom_" + uid(), name: name.trim(), group: "OTHER", icon: IconComp }, amt);
  }

  return (
    <BottomSheet t={t} title="Add Custom Category" onClose={onClose}>
      <Field t={t} label="Category Name">
        <input style={inputStyle(t, false)} value={name} maxLength={50} onChange={(e) => setName(e.target.value)} placeholder="e.g. Gym" />
      </Field>
      <Field t={t} label="Budget Amount (₹)">
        <input style={inputStyle(t, false)} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ""))} placeholder="0" />
      </Field>
      <Field t={t} label="Icon (optional)">
        <div style={{ display: "flex", gap: 8 }}>
          {ICON_CHOICES.map(({ key, icon: Ic }) => (
            <button key={key} onClick={() => setIconKey(key)} style={{
              width: 40, height: 40, borderRadius: 10, border: `1.5px solid ${iconKey === key ? t.accent : t.border}`,
              background: iconKey === key ? t.accentSoft : "transparent", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer",
            }}>
              <Ic size={16} color={iconKey === key ? t.accent : t.subtext} />
            </button>
          ))}
        </div>
      </Field>
      {error && <div style={{ color: t.danger, fontSize: 13, marginBottom: 12 }}>{error}</div>}
      <PrimaryButton t={t} onClick={submit}>Add Category</PrimaryButton>
    </BottomSheet>
  );
}

/* ============================== MORE SCREEN ============================== */

function MoreScreen({ t, goals, setGoals, budget, setBudget, settings, setSettings, isDark, setIsDark }) {
  const [showGoal, setShowGoal] = useState(false);
  const [syncState, setSyncState] = useState("disconnected"); // disconnected | connecting | connected
  const [signedIn, setSignedIn] = useState(false);

  function addGoal(g) {
    setGoals([...goals, g]);
    setShowGoal(false);
  }

  return (
    <div>
      <div style={{ color: t.text, fontFamily: DISPLAY_FONT, fontSize: 22, fontWeight: 600, marginBottom: 16 }}>More</div>

      {/* Profile / Google Sign-in */}
      <Card t={t} style={{ padding: 16, marginBottom: 16 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{ width: 44, height: 44, borderRadius: "50%", background: t.accentSoft, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <User size={20} color={t.accent} />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ color: t.text, fontWeight: 700, fontSize: 14.5 }}>{signedIn ? "Alex Kumar" : "Local Profile"}</div>
            <div style={{ color: t.subtext, fontSize: 12 }}>{signedIn ? "alex.kumar@gmail.com" : "Not signed in with Google"}</div>
          </div>
        </div>
        {!signedIn ? (
          <PrimaryButton t={t} style={{ marginTop: 14 }} onClick={() => setSignedIn(true)}>
            <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}><LogIn size={16} /> Continue with Google</span>
          </PrimaryButton>
        ) : (
          <GhostButton t={t} style={{ marginTop: 14, width: "100%" }} onClick={() => { setSignedIn(false); setSyncState("disconnected"); }}>Sign out</GhostButton>
        )}
      </Card>

      {/* Savings Goals */}
      <SectionLabel t={t} right={
        <button onClick={() => setShowGoal(true)} style={{ display: "flex", alignItems: "center", gap: 4, background: "none", border: "none", color: t.accent, fontWeight: 700, fontSize: 12.5, cursor: "pointer" }}>
          <Plus size={14} /> New goal
        </button>
      }>Savings Goals</SectionLabel>
      <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 4 }}>
        {goals.length === 0 && <EmptyState t={t} icon={Target} title="No goals yet" sub="Create a savings goal to track progress." />}
        {goals.map((g) => {
          const pct = clamp((g.current / g.target) * 100, 0, 100);
          return (
            <Card key={g.id} t={t} style={{ padding: 14, display: "flex", alignItems: "center", gap: 14 }}>
              <ProgressRing t={t} pct={pct} size={50} stroke={6} color={t.success} />
              <div style={{ flex: 1 }}>
                <div style={{ color: t.text, fontWeight: 700, fontSize: 14 }}>{g.name}</div>
                <div style={{ color: t.subtext, fontSize: 12 }}>{fmtMoney(g.current)} / {fmtMoney(g.target)} · by {g.targetDate}</div>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Salary Cycle */}
      <SectionLabel t={t}>Salary Cycle</SectionLabel>
      <Card t={t} style={{ padding: 16, marginBottom: 4 }}>
        <Field t={t} label="Monthly Income (₹)">
          <input style={inputStyle(t, false)} inputMode="decimal" value={settings.salaryAmount}
            onChange={(e) => setSettings({ ...settings, salaryAmount: e.target.value.replace(/[^0-9.]/g, "") })} />
        </Field>
        <Field t={t} label="Salary Date (day of month)">
          <input type="number" min={1} max={31} style={inputStyle(t, false)} value={settings.salaryDate}
            onChange={(e) => setSettings({ ...settings, salaryDate: e.target.value })} />
        </Field>
        <div style={{ color: t.faint, fontSize: 12 }}>We'll remind you to set your budget on this date. We won't assume your salary has arrived automatically.</div>
      </Card>

      {/* Notification preferences */}
      <SectionLabel t={t}>Notifications</SectionLabel>
      <Card t={t} style={{ padding: 4, marginBottom: 4 }}>
        <ToggleRow t={t} label="Budget alerts" checked={settings.notifMaster} onChange={(v) => setSettings({ ...settings, notifMaster: v })} />
        {["75", "90", "100", "Over budget"].map((th) => (
          <ToggleRow key={th} t={t} label={`${th}${th === "Over budget" ? "" : "% used"}`} sub
            checked={settings.notifThresholds[th] !== false}
            disabled={!settings.notifMaster}
            onChange={(v) => setSettings({ ...settings, notifThresholds: { ...settings.notifThresholds, [th]: v } })}
          />
        ))}
      </Card>

      {/* Backup */}
      <SectionLabel t={t}>Backup &amp; Sync</SectionLabel>
      <Card t={t} style={{ padding: 16, marginBottom: 4 }}>
        {!signedIn && <div style={{ color: t.subtext, fontSize: 13 }}>Sign in with Google to enable Sheets backup.</div>}
        {signedIn && syncState === "disconnected" && (
          <>
            <div style={{ color: t.subtext, fontSize: 13, marginBottom: 12 }}>
              Back up your Transactions, Budgets, Income, and Monthly Summary to a personal Google Sheet. Only these structured records are sent — nothing else.
            </div>
            <PrimaryButton t={t} onClick={() => { setSyncState("connecting"); setTimeout(() => setSyncState("connected"), 1200); }}>
              <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}><Cloud size={16} /> Enable Google Sheets Backup</span>
            </PrimaryButton>
          </>
        )}
        {signedIn && syncState === "connecting" && (
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: t.subtext, fontSize: 13.5 }}>
            <RefreshCw size={16} className="spin" color={t.accent} /> Creating your personal spreadsheet…
          </div>
        )}
        {signedIn && syncState === "connected" && (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
              <Pill t={t} color={t.success} bg={t.successSoft} icon={Cloud}>Up to date</Pill>
              <span style={{ color: t.faint, fontSize: 11.5 }}>Last synced: just now</span>
            </div>
            <div style={{ color: t.text, fontSize: 13.5, fontWeight: 600, marginBottom: 2 }}>Wallet Scholer - Financial Backup</div>
            <div style={{ color: t.subtext, fontSize: 12, marginBottom: 12 }}>alex.kumar@gmail.com</div>
            <div style={{ display: "flex", gap: 8 }}>
              <GhostButton t={t} style={{ flex: 1 }} onClick={() => {}}>Sync now</GhostButton>
              <GhostButton t={t} style={{ flex: 1, color: t.danger, borderColor: t.danger + "55" }} onClick={() => setSyncState("disconnected")}>
                <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}><CloudOff size={14} /> Disconnect</span>
              </GhostButton>
            </div>
          </>
        )}
      </Card>

      {/* Appearance */}
      <SectionLabel t={t}>Appearance</SectionLabel>
      <Card t={t} style={{ padding: 4, marginBottom: 4 }}>
        <ToggleRow t={t} label="Dark theme" checked={isDark} onChange={setIsDark} />
      </Card>

      <div style={{ height: 90 }} />

      {showGoal && (
        <AddGoalSheet t={t} onClose={() => setShowGoal(false)} onAdd={addGoal} />
      )}
    </div>
  );
}

function ToggleRow({ t, label, checked, onChange, sub, disabled }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: sub ? "9px 12px 9px 24px" : "12px 12px" }}>
      <div style={{ color: disabled ? t.faint : t.text, fontSize: sub ? 13 : 14, fontWeight: sub ? 500 : 600 }}>{label}</div>
      <button
        onClick={() => !disabled && onChange(!checked)}
        style={{
          width: 42, height: 24, borderRadius: 999, border: "none", cursor: disabled ? "not-allowed" : "pointer",
          background: checked && !disabled ? t.accent : t.borderSoft, position: "relative", transition: "background 0.15s", flexShrink: 0,
        }}
      >
        <div style={{ width: 18, height: 18, borderRadius: "50%", background: "#fff", position: "absolute", top: 3, left: checked ? 21 : 3, transition: "left 0.15s" }} />
      </button>
    </div>
  );
}

function AddGoalSheet({ t, onClose, onAdd }) {
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [current, setCurrent] = useState("");
  const [date, setDate] = useState("");
  const [error, setError] = useState("");

  function submit() {
    if (!name.trim() || name.trim().length > 50) { setError("Enter a goal name (max 50 characters)."); return; }
    const t1 = parseFloat(target), c1 = parseFloat(current || "0");
    if (!target || isNaN(t1) || t1 <= 0) { setError("Enter a target amount greater than 0."); return; }
    if (isNaN(c1) || c1 < 0) { setError("Current saved amount can't be negative."); return; }
    onAdd({ id: uid(), name: name.trim(), target: t1, current: c1, targetDate: date || "—" });
  }

  return (
    <BottomSheet t={t} title="New Savings Goal" onClose={onClose}>
      <Field t={t} label="Goal Name"><input style={inputStyle(t, false)} value={name} maxLength={50} onChange={(e) => setName(e.target.value)} placeholder="e.g. New Laptop" /></Field>
      <Field t={t} label="Target Amount (₹)"><input style={inputStyle(t, false)} inputMode="decimal" value={target} onChange={(e) => setTarget(e.target.value.replace(/[^0-9.]/g, ""))} /></Field>
      <Field t={t} label="Current Saved (₹)"><input style={inputStyle(t, false)} inputMode="decimal" value={current} onChange={(e) => setCurrent(e.target.value.replace(/[^0-9.]/g, ""))} placeholder="0" /></Field>
      <Field t={t} label="Target Date"><input type="date" style={inputStyle(t, false)} value={date} onChange={(e) => setDate(e.target.value)} /></Field>
      {error && <div style={{ color: t.danger, fontSize: 13, marginBottom: 12 }}>{error}</div>}
      <PrimaryButton t={t} onClick={submit}>Create Goal</PrimaryButton>
    </BottomSheet>
  );
}

/* ============================== NAV ============================== */

function BottomNav({ t, screen, setScreen, onAdd }) {
  const items = [
    { id: "home", icon: Home, label: "Home" },
    { id: "wallet", icon: WalletIcon, label: "Wallet" },
    { id: "calculator", icon: CalcIcon, label: "Calculator", center: true },
    { id: "budget", icon: PieChart, label: "Budget" },
    { id: "more", icon: Menu, label: "More" },
  ];
  return (
    <div style={{
      position: "absolute", bottom: 0, left: 0, right: 0, background: t.navBg,
      borderTop: `1px solid ${t.border}`, display: "flex", alignItems: "center", justifyContent: "space-around",
      padding: "10px 6px calc(10px + env(safe-area-inset-bottom))", zIndex: 20,
    }}>
      {items.map((it) => {
        const active = screen === it.id;
        const Icon = it.icon;
        if (it.center) {
          return (
            <button key={it.id} onClick={() => setScreen(it.id)} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 3 }}>
              <div style={{
                width: 46, height: 46, borderRadius: "50%", background: active ? t.accent : t.accentSoft,
                display: "flex", alignItems: "center", justifyContent: "center", marginTop: -22, boxShadow: t.shadow,
                border: `3px solid ${t.navBg}`,
              }}>
                <Icon size={20} color={active ? t.accentText : t.accent} />
              </div>
              <span style={{ fontSize: 10.5, fontWeight: 700, color: active ? t.accent : t.faint }}>{it.label}</span>
            </button>
          );
        }
        return (
          <button key={it.id} onClick={() => setScreen(it.id)} style={{ background: "none", border: "none", cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 4, padding: "4px 10px" }}>
            <Icon size={20} color={active ? t.accent : t.faint} strokeWidth={active ? 2.4 : 2} />
            <span style={{ fontSize: 10.5, fontWeight: 700, color: active ? t.accent : t.faint }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

/* ============================== APP ROOT ============================== */

const STORAGE_KEY = "wallet-scholer-state-v1";

export default function App() {
  useInjectFonts();
  const [loaded, setLoaded] = useState(false);
  const [isDark, setIsDark] = useState(true);
  const [screen, setScreen] = useState("home");
  const [transactions, setTransactions] = useState(seedTransactions);
  const [budget, setBudget] = useState(seedBudget);
  const [goals, setGoals] = useState(seedGoals);
  const [settings, setSettings] = useState({
    salaryAmount: "51000", salaryDate: "1", notifMaster: true,
    notifThresholds: { "75": true, "90": true, "100": true, "Over budget": true },
  });
  const [showAdd, setShowAdd] = useState(false);
  const [editingTx, setEditingTx] = useState(null);

  const t = useTheme(isDark);

  // load persisted state once
  useEffect(() => {
    (async () => {
      try {
        const res = await window.storage?.get(STORAGE_KEY);
        if (res?.value) {
          const data = JSON.parse(res.value);
          if (data.transactions) setTransactions(data.transactions);
          if (data.budget) setBudget(data.budget);
          if (data.goals) setGoals(data.goals);
          if (data.settings) setSettings(data.settings);
          if (typeof data.isDark === "boolean") setIsDark(data.isDark);
        }
      } catch (e) {
        // no saved state yet — keep seed data
      } finally {
        setLoaded(true);
      }
    })();
  }, []);

  // persist on change
  useEffect(() => {
    if (!loaded) return;
    const payload = JSON.stringify({ transactions, budget, goals, settings, isDark });
    window.storage?.set(STORAGE_KEY, payload).catch(() => {});
  }, [transactions, budget, goals, settings, isDark, loaded]);

  function saveTx(tx) {
    setTransactions((prev) => {
      const exists = prev.some((p) => p.id === tx.id);
      return exists ? prev.map((p) => (p.id === tx.id ? tx : p)) : [tx, ...prev];
    });
    setShowAdd(false);
    setEditingTx(null);
  }

  function openEdit(tx) {
    setEditingTx(tx);
    setShowAdd(true);
  }

  const showFab = screen === "home" || screen === "wallet";

  return (
    <div style={{ width: "100%", minHeight: "100vh", display: "flex", justifyContent: "center", background: isDark ? "#08080A" : "#EAE6DC", fontFamily: BODY_FONT }}>
      <style>{`
        @keyframes spin { from { transform: rotate(0deg);} to { transform: rotate(360deg);} }
        .spin { animation: spin 1s linear infinite; }
        input::placeholder { color: ${t.faint}; }
        * { box-sizing: border-box; }
      `}</style>
      <div style={{
        width: "100%", maxWidth: 430, minHeight: "100vh", background: t.bg, position: "relative",
        boxShadow: "0 0 60px rgba(0,0,0,0.35)", overflow: "hidden",
      }}>
        <div style={{ padding: "22px 18px 0", position: "relative", minHeight: "100vh" }}>
          {screen === "home" && (
            <HomeScreen t={t} transactions={transactions} budget={budget} goals={goals} onOpenAdd={() => setShowAdd(true)} onTxClick={openEdit} isDark={isDark} setIsDark={setIsDark} />
          )}
          {screen === "wallet" && <WalletScreen t={t} transactions={transactions} onTxClick={openEdit} />}
          {screen === "calculator" && <CalculatorScreen t={t} />}
          {screen === "budget" && <BudgetScreen t={t} budget={budget} setBudget={setBudget} transactions={transactions} />}
          {screen === "more" && (
            <MoreScreen t={t} goals={goals} setGoals={setGoals} budget={budget} setBudget={setBudget} settings={settings} setSettings={setSettings} isDark={isDark} setIsDark={setIsDark} />
          )}

          {showFab && (
            <button
              onClick={() => { setEditingTx(null); setShowAdd(true); }}
              style={{
                position: "absolute", right: 18, bottom: 96, width: 54, height: 54, borderRadius: "50%",
                background: t.accent, border: "none", display: "flex", alignItems: "center", justifyContent: "center",
                boxShadow: t.shadow, cursor: "pointer", zIndex: 15,
              }}
            >
              <Plus size={24} color={t.accentText} />
            </button>
          )}

          <BottomNav t={t} screen={screen} setScreen={setScreen} />

          {showAdd && (
            <AddTransactionSheet
              t={t}
              editing={editingTx}
              onClose={() => { setShowAdd(false); setEditingTx(null); }}
              onSave={saveTx}
            />
          )}
        </div>
      </div>
    </div>
  );
}
