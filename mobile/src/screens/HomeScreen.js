import React, { useCallback, useState } from "react";
import { View, Text, ScrollView, RefreshControl, SafeAreaView, ActivityIndicator } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/client";
import { Card, Pill, SectionLabel } from "../components/ui";
import { computeBalance, utilizationPct, thresholdFor, safeToSpend } from "../domain/financeEngine";

function money(n) {
  const v = Number(n) || 0;
  return (v < 0 ? "-" : "") + "\u20B9" + Math.abs(v).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export default function HomeScreen() {
  const { theme, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [transactions, setTransactions] = useState([]);
  const [budget, setBudget] = useState(null);
  const [utilRows, setUtilRows] = useState([]);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setError("");
    try {
      const now = new Date();
      const monthStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
      const [txRes, budgetRes] = await Promise.all([
        api.listTransactions({ month: monthStr }),
        api.getBudget(now.getFullYear(), now.getMonth() + 1),
      ]);
      setTransactions(txRes.transactions);
      setBudget(budgetRes.budget);
      if (budgetRes.budget) {
        const utilRes = await api.getUtilization(budgetRes.budget.id);
        setUtilRows(utilRes.utilization);
      } else {
        setUtilRows([]);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const active = transactions.filter((t) => t.status === "ACTIVE");
  const balance = computeBalance(active);
  const income = active.filter((t) => t.transaction_type === "INCOME").reduce((s, t) => s + t.amount, 0);
  const expense = active.filter((t) => t.transaction_type === "EXPENSE").reduce((s, t) => s + t.amount, 0);
  const totalAllocated = budget ? budget.allocations.reduce((s, a) => s + a.allocated_amount, 0) : 0;
  const overallUtilization = utilizationPct(expense, totalAllocated);

  const alerts = utilRows.filter((r) => r.threshold).sort((a, b) => b.utilizationPct - a.utilizationPct);

  if (loading) {
    return (
      <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg, alignItems: "center", justifyContent: "center" }}>
        <ActivityIndicator color={theme.accent} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <ScrollView
        contentContainerStyle={{ padding: 18 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={load} tintColor={theme.accent} />}
      >
        <Text style={{ color: theme.subtext, fontSize: 13 }}>Good day</Text>
        <Text style={{ color: theme.text, fontSize: 20, fontWeight: "700", marginBottom: 16 }}>{user?.displayName || "Wallet Scholer"}</Text>

        {error ? <Text style={{ color: theme.danger, marginBottom: 12 }}>{error}</Text> : null}

        <Card theme={theme} style={{ padding: 20 }}>
          <Text style={{ color: theme.subtext, fontSize: 12, fontWeight: "700", textTransform: "uppercase" }}>Current Balance</Text>
          <Text style={{ color: theme.text, fontSize: 34, fontWeight: "700", marginTop: 6 }}>{money(balance)}</Text>
          <View style={{ flexDirection: "row", gap: 20, marginTop: 14 }}>
            <View>
              <Text style={{ color: theme.faint, fontSize: 11 }}>SAFE TO SPEND</Text>
              <Text style={{ color: theme.success, fontSize: 16, fontWeight: "700" }}>
                {money(safeToSpend({ balance, essentialRemaining: Math.max(0, totalAllocated - expense), savingsCommitment: 0, emiCommitment: 0 }))}
              </Text>
            </View>
            <View>
              <Text style={{ color: theme.faint, fontSize: 11 }}>REMAINING BUDGET</Text>
              <Text style={{ color: totalAllocated - expense < 0 ? theme.danger : theme.text, fontSize: 16, fontWeight: "700" }}>
                {money(totalAllocated - expense)}
              </Text>
            </View>
          </View>
        </Card>

        <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
          <Card theme={theme} style={{ flex: 1 }}>
            <Text style={{ color: theme.subtext, fontSize: 12 }}>This month spent</Text>
            <Text style={{ color: theme.text, fontSize: 18, fontWeight: "700", marginTop: 4 }}>{money(expense)}</Text>
          </Card>
          <Card theme={theme} style={{ flex: 1 }}>
            <Text style={{ color: theme.subtext, fontSize: 12 }}>This month income</Text>
            <Text style={{ color: theme.text, fontSize: 18, fontWeight: "700", marginTop: 4 }}>{money(income)}</Text>
          </Card>
        </View>

        <Card theme={theme} style={{ marginTop: 12 }}>
          <Text style={{ color: theme.text, fontWeight: "700", fontSize: 15, marginBottom: 4 }}>Budget utilization</Text>
          <Text style={{ color: theme.subtext, fontSize: 12.5, marginBottom: 8 }}>{money(expense)} of {money(totalAllocated)} used</Text>
          <Pill theme={theme} color={overallUtilization >= 100 ? theme.danger : theme.success} bg={overallUtilization >= 100 ? theme.dangerSoft : theme.successSoft}>
            {overallUtilization.toFixed(0)}% used
          </Pill>
        </Card>

        {alerts.length > 0 && (
          <>
            <SectionLabel theme={theme}>Alerts</SectionLabel>
            {alerts.slice(0, 3).map((a) => (
              <Card key={a.categoryId} theme={theme} style={{ marginBottom: 8 }}>
                <Text style={{ color: theme.text, fontSize: 13.5 }}>
                  {a.threshold === "EXCEEDED"
                    ? `You exceeded your ${a.categoryName} budget by ${money(a.spent - a.allocated)}.`
                    : `You have used ${a.utilizationPct.toFixed(0)}% of your ${a.categoryName} budget.`}
                </Text>
              </Card>
            ))}
          </>
        )}

        {!budget && (
          <Card theme={theme} style={{ marginTop: 16 }}>
            <Text style={{ color: theme.subtext, fontSize: 13.5 }}>No budget set for this month yet. Head to the Budget tab to create one.</Text>
          </Card>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
