import React, { useCallback, useState } from "react";
import { View, Text, ScrollView, SafeAreaView, TouchableOpacity, Alert } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/client";
import { Card, Field, AppInput, PrimaryButton, GhostButton, Pill, SectionLabel } from "../components/ui";
import { utilizationPct } from "../domain/financeEngine";

function money(n) {
  const v = Number(n) || 0;
  return "\u20B9" + v.toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export default function BudgetScreen() {
  const { theme } = useAuth();
  const [budget, setBudget] = useState(null);
  const [categories, setCategories] = useState([]);
  const [utilRows, setUtilRows] = useState([]);
  const [editing, setEditing] = useState(false);
  const [income, setIncome] = useState("0");
  const [allocations, setAllocations] = useState({});

  const now = new Date();
  const year = now.getFullYear(), month = now.getMonth() + 1;

  const load = useCallback(async () => {
    const [budgetRes, catRes] = await Promise.all([api.getBudget(year, month), api.listCategories()]);
    setBudget(budgetRes.budget);
    setCategories(catRes.categories.filter((c) => c.category_type === "EXPENSE"));
    if (budgetRes.budget) {
      setIncome(String(budgetRes.budget.income_amount));
      const map = {};
      budgetRes.budget.allocations.forEach((a) => (map[a.category_id] = String(a.allocated_amount)));
      setAllocations(map);
      const utilRes = await api.getUtilization(budgetRes.budget.id);
      setUtilRows(utilRes.utilization);
    }
  }, [year, month]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  async function createOrSave(confirmExceedsIncome = false) {
    const incomeNum = parseFloat(income) || 0;
    const allocList = Object.entries(allocations)
      .filter(([, v]) => parseFloat(v) > 0)
      .map(([categoryId, v]) => ({ categoryId, allocatedAmount: parseFloat(v) }));
    const payload = { periodYear: year, periodMonth: month, incomeAmount: incomeNum, allocations: allocList, confirmExceedsIncome };

    try {
      if (budget) await api.updateBudget(budget.id, payload);
      else await api.createBudget(payload);
      setEditing(false);
      load();
    } catch (e) {
      if (e.status === 409 && e.message.includes("exceeds income")) {
        Alert.alert(
          "Budget exceeds income",
          `Your allocated budget (${money(allocList.reduce((s, a) => s + a.allocatedAmount, 0))}) exceeds your income (${money(incomeNum)}). Save anyway?`,
          [{ text: "Cancel", style: "cancel" }, { text: "Save anyway", onPress: () => createOrSave(true) }]
        );
      } else {
        Alert.alert("Couldn't save budget", e.message);
      }
    }
  }

  async function useLastMonth() {
    try {
      await api.copyLastMonth(year, month);
      load();
    } catch (e) {
      Alert.alert("Couldn't copy last month", e.message);
    }
  }

  const totalAllocated = Object.values(allocations).reduce((s, v) => s + (parseFloat(v) || 0), 0);
  const exceedsIncome = totalAllocated > (parseFloat(income) || 0);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <ScrollView contentContainerStyle={{ padding: 18 }}>
        <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
          <Text style={{ color: theme.text, fontSize: 22, fontWeight: "700" }}>Budget</Text>
          <TouchableOpacity onPress={() => setEditing((e) => !e)}>
            <Text style={{ color: theme.accent, fontWeight: "700" }}>{editing ? "Done" : "Edit"}</Text>
          </TouchableOpacity>
        </View>

        {!budget && !editing && (
          <Card theme={theme} style={{ marginBottom: 14 }}>
            <Text style={{ color: theme.subtext, marginBottom: 12 }}>No budget set for this month.</Text>
            <PrimaryButton theme={theme} title="Create Budget" onPress={() => setEditing(true)} style={{ marginBottom: 10 }} />
            <GhostButton theme={theme} title="Use Last Month's Budget" onPress={useLastMonth} />
          </Card>
        )}

        {editing && (
          <Card theme={theme} style={{ marginBottom: 14 }}>
            <Field theme={theme} label="Monthly Income (₹)">
              <AppInput theme={theme} value={income} onChangeText={(t) => setIncome(t.replace(/[^0-9.]/g, ""))} keyboardType="decimal-pad" />
            </Field>
            {categories.map((c) => (
              <Field key={c.id} theme={theme} label={c.name}>
                <AppInput
                  theme={theme}
                  value={allocations[c.id] || ""}
                  onChangeText={(t) => setAllocations({ ...allocations, [c.id]: t.replace(/[^0-9.]/g, "") })}
                  keyboardType="decimal-pad"
                  placeholder="0"
                />
              </Field>
            ))}
            {exceedsIncome && (
              <Text style={{ color: theme.danger, fontSize: 12.5, marginBottom: 10 }}>
                Allocated ({money(totalAllocated)}) exceeds income ({money(parseFloat(income) || 0)}). You'll be asked to confirm.
              </Text>
            )}
            <PrimaryButton theme={theme} title="Save Budget" onPress={() => createOrSave(false)} />
          </Card>
        )}

        {budget && !editing && (
          <>
            <Card theme={theme} style={{ marginBottom: 14 }}>
              <Text style={{ color: theme.subtext, fontSize: 13 }}>Allocated {money(budget.total_budget)} of {money(budget.income_amount)} income</Text>
            </Card>
            <SectionLabel theme={theme}>Categories</SectionLabel>
            {utilRows.map((row) => (
              <Card key={row.categoryId} theme={theme} style={{ marginBottom: 8 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between", marginBottom: 4 }}>
                  <Text style={{ color: theme.text, fontWeight: "600" }}>{row.categoryName}</Text>
                  <Pill theme={theme} color={row.utilizationPct >= 100 ? theme.danger : theme.success} bg={row.utilizationPct >= 100 ? theme.dangerSoft : theme.successSoft}>
                    {row.utilizationPct.toFixed(0)}%
                  </Pill>
                </View>
                <Text style={{ color: theme.faint, fontSize: 12 }}>{money(row.spent)} of {money(row.allocated)}</Text>
              </Card>
            ))}
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
