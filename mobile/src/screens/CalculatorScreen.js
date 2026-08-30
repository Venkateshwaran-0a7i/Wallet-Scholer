import React, { useState } from "react";
import { View, Text, ScrollView, SafeAreaView, TouchableOpacity } from "react-native";
import { useAuth } from "../context/AuthContext";
import { Field, AppInput, Card } from "../components/ui";
import {
  emiCalc, loanAffordability, simpleInterest, compoundInterest,
  requiredMonthlySavings, sipFutureValue, percentageOf, whatPercent, percentChange,
} from "../domain/financeEngine";

function money(n) {
  const v = Number(n) || 0;
  return "\u20B9" + v.toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

const MODULES = ["EMI", "Loan", "Simple Interest", "Compound Interest", "Savings", "SIP", "Percentage"];

export default function CalculatorScreen() {
  const { theme } = useAuth();
  const [tab, setTab] = useState("EMI");

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <View style={{ padding: 18, paddingBottom: 8 }}>
        <Text style={{ color: theme.text, fontSize: 22, fontWeight: "700", marginBottom: 12 }}>Financial Calculator</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={{ flexDirection: "row", gap: 6 }}>
            {MODULES.map((m) => (
              <TouchableOpacity
                key={m}
                onPress={() => setTab(m)}
                style={{ paddingHorizontal: 14, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: tab === m ? theme.accent : theme.border, backgroundColor: tab === m ? theme.accentSoft : "transparent" }}
              >
                <Text style={{ color: tab === m ? theme.accent : theme.subtext, fontWeight: "700", fontSize: 12.5 }}>{m}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </ScrollView>
      </View>
      <ScrollView contentContainerStyle={{ padding: 18, paddingTop: 6 }}>
        {tab === "EMI" && <EmiModule theme={theme} />}
        {tab === "Loan" && <LoanModule theme={theme} />}
        {tab === "Simple Interest" && <SiModule theme={theme} />}
        {tab === "Compound Interest" && <CiModule theme={theme} />}
        {tab === "Savings" && <SavingsModule theme={theme} />}
        {tab === "SIP" && <SipModule theme={theme} />}
        {tab === "Percentage" && <PctModule theme={theme} />}
      </ScrollView>
    </SafeAreaView>
  );
}

function ResultCard({ theme, rows, disclaimer }) {
  return (
    <Card theme={theme} style={{ backgroundColor: theme.accentSoft, marginTop: 6 }}>
      {rows.map((r, i) => (
        <View key={i} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 4 }}>
          <Text style={{ color: theme.subtext, fontSize: 13 }}>{r.label}</Text>
          <Text style={{ color: theme.text, fontWeight: "700", fontSize: r.big ? 18 : 14 }}>{r.value}</Text>
        </View>
      ))}
      {disclaimer && <Text style={{ color: theme.subtext, fontSize: 11.5, marginTop: 6 }}>{disclaimer}</Text>}
    </Card>
  );
}

function NumField({ theme, label, value, onChange, suffix }) {
  return (
    <Field theme={theme} label={`${label}${suffix ? ` (${suffix})` : ""}`}>
      <AppInput theme={theme} keyboardType="decimal-pad" value={value} onChangeText={(t) => onChange(t.replace(/[^0-9.]/g, ""))} placeholder="0" />
    </Field>
  );
}

function EmiModule({ theme }) {
  const [p, setP] = useState("500000"), [r, setR] = useState("9.5"), [y, setY] = useState("5");
  const res = emiCalc(parseFloat(p), parseFloat(r), parseFloat(y));
  return (
    <View>
      <NumField theme={theme} label="Loan Amount" value={p} onChange={setP} suffix="₹" />
      <NumField theme={theme} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Tenure" value={y} onChange={setY} suffix="years" />
      {res ? <ResultCard theme={theme} rows={[
        { label: "Monthly EMI", value: money(res.emi), big: true },
        { label: "Total Interest", value: money(res.totalInterest) },
        { label: "Total Payment", value: money(res.totalPayment) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function LoanModule({ theme }) {
  const [emi, setEmi] = useState("15000"), [r, setR] = useState("9.5"), [y, setY] = useState("5");
  const res = loanAffordability(parseFloat(emi), parseFloat(r), parseFloat(y));
  return (
    <View>
      <Text style={{ color: theme.subtext, fontSize: 12.5, marginBottom: 10 }}>Find how much you can borrow for a given monthly payment.</Text>
      <NumField theme={theme} label="Desired Monthly Payment" value={emi} onChange={setEmi} suffix="₹" />
      <NumField theme={theme} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Tenure" value={y} onChange={setY} suffix="years" />
      {res ? <ResultCard theme={theme} rows={[
        { label: "Max Loan Amount", value: money(res.principal), big: true },
        { label: "Total Interest", value: money(res.totalInterest) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function SiModule({ theme }) {
  const [p, setP] = useState("100000"), [r, setR] = useState("6"), [y, setY] = useState("2");
  const res = simpleInterest(parseFloat(p), parseFloat(r), parseFloat(y));
  return (
    <View>
      <NumField theme={theme} label="Principal" value={p} onChange={setP} suffix="₹" />
      <NumField theme={theme} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Time" value={y} onChange={setY} suffix="years" />
      {res ? <ResultCard theme={theme} rows={[
        { label: "Interest Earned", value: money(res.interest), big: true },
        { label: "Final Amount", value: money(res.total) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function CiModule({ theme }) {
  const [p, setP] = useState("100000"), [r, setR] = useState("7"), [y, setY] = useState("3"), [n, setN] = useState("12");
  const res = compoundInterest(parseFloat(p), parseFloat(r), parseFloat(y), parseFloat(n));
  return (
    <View>
      <NumField theme={theme} label="Principal" value={p} onChange={setP} suffix="₹" />
      <NumField theme={theme} label="Annual Rate" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Time" value={y} onChange={setY} suffix="years" />
      <Field theme={theme} label="Compounding Frequency">
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={{ flexDirection: "row", gap: 6 }}>
            {[["1", "Annually"], ["2", "Semi-annual"], ["4", "Quarterly"], ["12", "Monthly"], ["365", "Daily"]].map(([val, lbl]) => (
              <TouchableOpacity key={val} onPress={() => setN(val)} style={{ paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: n === val ? theme.accent : theme.border, backgroundColor: n === val ? theme.accentSoft : "transparent" }}>
                <Text style={{ color: n === val ? theme.accent : theme.subtext, fontSize: 12, fontWeight: "700" }}>{lbl}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </ScrollView>
      </Field>
      {res ? <ResultCard theme={theme} rows={[
        { label: "Interest Earned", value: money(res.interest), big: true },
        { label: "Final Amount", value: money(res.total) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function SavingsModule({ theme }) {
  const [target, setTarget] = useState("100000"), [r, setR] = useState("6"), [y, setY] = useState("2");
  const res = requiredMonthlySavings(parseFloat(target), parseFloat(r), parseFloat(y));
  return (
    <View>
      <Text style={{ color: theme.subtext, fontSize: 12.5, marginBottom: 10 }}>Find the monthly amount needed to hit a savings goal.</Text>
      <NumField theme={theme} label="Target Amount" value={target} onChange={setTarget} suffix="₹" />
      <NumField theme={theme} label="Expected Annual Return" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Duration" value={y} onChange={setY} suffix="years" />
      {res ? <ResultCard theme={theme} disclaimer="Projected returns are estimates and not guaranteed." rows={[
        { label: "Required Monthly Saving", value: money(res.monthly), big: true },
        { label: "Total Deposited", value: money(res.totalDeposited) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function SipModule({ theme }) {
  const [m, setM] = useState("5000"), [r, setR] = useState("12"), [y, setY] = useState("10");
  const res = sipFutureValue(parseFloat(m), parseFloat(r), parseFloat(y));
  return (
    <View>
      <NumField theme={theme} label="Monthly Investment" value={m} onChange={setM} suffix="₹" />
      <NumField theme={theme} label="Expected Annual Return" value={r} onChange={setR} suffix="%" />
      <NumField theme={theme} label="Duration" value={y} onChange={setY} suffix="years" />
      {res ? <ResultCard theme={theme} disclaimer="Projected returns are estimates and not guaranteed." rows={[
        { label: "Estimated Future Value", value: money(res.futureValue), big: true },
        { label: "Total Invested", value: money(res.invested) },
        { label: "Estimated Gain", value: money(res.gain) },
      ]} /> : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}

function PctModule({ theme }) {
  const [mode, setMode] = useState("of");
  const [x, setX] = useState("15"), [y, setY] = useState("2000");
  let result = null;
  if (mode === "of") result = percentageOf(parseFloat(x), parseFloat(y));
  if (mode === "what") result = whatPercent(parseFloat(x), parseFloat(y));
  if (mode === "increase") result = percentChange(parseFloat(y), parseFloat(x), "increase");
  if (mode === "decrease") result = percentChange(parseFloat(y), parseFloat(x), "decrease");

  return (
    <View>
      <Field theme={theme} label="Mode">
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={{ flexDirection: "row", gap: 6 }}>
            {[["of", "X% of Y"], ["what", "X is what % of Y"], ["increase", "Increase Y by X%"], ["decrease", "Decrease Y by X%"]].map(([val, lbl]) => (
              <TouchableOpacity key={val} onPress={() => setMode(val)} style={{ paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: mode === val ? theme.accent : theme.border, backgroundColor: mode === val ? theme.accentSoft : "transparent" }}>
                <Text style={{ color: mode === val ? theme.accent : theme.subtext, fontSize: 12, fontWeight: "700" }}>{lbl}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </ScrollView>
      </Field>
      <NumField theme={theme} label="X" value={x} onChange={setX} />
      <NumField theme={theme} label="Y" value={y} onChange={setY} />
      {typeof result === "number" && isFinite(result) ? (
        <ResultCard theme={theme} rows={[{ label: "Result", value: mode === "what" ? `${result.toFixed(2)}%` : money(result), big: true }]} />
      ) : <ResultCard theme={theme} rows={[{ label: "Enter valid values", value: "—" }]} />}
    </View>
  );
}
