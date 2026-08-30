import React, { useCallback, useState } from "react";
import { View, Text, FlatList, SafeAreaView, TouchableOpacity, Modal, ScrollView, Alert } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/client";
import { Card, Field, AppInput, PrimaryButton, GhostButton, ErrorText, Pill } from "../components/ui";

function money(n) {
  const v = Number(n) || 0;
  return "\u20B9" + v.toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export default function GoalScreen() {
  const { theme } = useAuth();
  const [goals, setGoals] = useState([]);
  const [showAdd, setShowAdd] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await api.listGoals();
      setGoals(res.goals);
    } catch (e) {
      console.error(e);
    }
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  async function deleteGoal(id) {
    Alert.alert("Delete Goal?", "This cannot be undone.", [
      { text: "Cancel", style: "cancel" },
      { text: "Delete", style: "destructive", onPress: async () => {
          await api.deleteGoal(id);
          load();
        }
      }
    ]);
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", padding: 18, paddingBottom: 8 }}>
        <Text style={{ color: theme.text, fontSize: 22, fontWeight: "700" }}>Goals</Text>
        <TouchableOpacity onPress={() => setShowAdd(true)} style={{ backgroundColor: theme.accent, width: 40, height: 40, borderRadius: 20, alignItems: "center", justifyContent: "center" }}>
          <Text style={{ color: theme.accentText, fontSize: 22, fontWeight: "700", marginTop: -2 }}>+</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={goals}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 18, paddingTop: 6 }}
        ListEmptyComponent={<Text style={{ color: theme.subtext, textAlign: "center", marginTop: 40 }}>No goals set. Plan for your future today!</Text>}
        renderItem={({ item }) => {
          const pct = Math.min(100, Math.round((item.current_amount / item.target_amount) * 100));
          return (
            <TouchableOpacity onLongPress={() => deleteGoal(item.id)}>
              <Card theme={theme} style={{ marginBottom: 12 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between", marginBottom: 10 }}>
                  <View style={{ flex: 1 }}>
                    <Text style={{ color: theme.text, fontWeight: "700", fontSize: 16 }}>{item.name}</Text>
                    {item.target_date && <Text style={{ color: theme.faint, fontSize: 12 }}>Target: {item.target_date}</Text>}
                  </View>
                  <Pill theme={theme} color={pct >= 100 ? theme.success : theme.accent} bg={pct >= 100 ? theme.successSoft : theme.accentSoft}>
                    {pct}%
                  </Pill>
                </View>
                <View style={{ height: 6, backgroundColor: theme.borderSoft, borderRadius: 3, overflow: "hidden", marginBottom: 10 }}>
                  <View style={{ height: "100%", width: `${pct}%`, backgroundColor: pct >= 100 ? theme.success : theme.accent }} />
                </View>
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <Text style={{ color: theme.text, fontWeight: "600" }}>{money(item.current_amount)}</Text>
                  <Text style={{ color: theme.subtext, fontSize: 13 }}>of {money(item.target_amount)}</Text>
                </View>
              </Card>
            </TouchableOpacity>
          );
        }}
      />

      <AddGoalModal
        visible={showAdd}
        onClose={() => setShowAdd(false)}
        onSaved={() => { setShowAdd(false); load(); }}
        theme={theme}
      />
    </SafeAreaView>
  );
}

function AddGoalModal({ visible, onClose, onSaved, theme }) {
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [current, setCurrent] = useState("");
  const [date, setDate] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit() {
    setError("");
    const t = parseFloat(target);
    const c = parseFloat(current || "0");
    if (!name.trim()) return setError("Name is required.");
    if (isNaN(t) || t <= 0) return setError("Target must be greater than 0.");

    setSaving(true);
    try {
      await api.addGoal({
        name: name.trim(),
        targetAmount: t,
        currentAmount: c,
        targetDate: date || null,
      });
      setName(""); setTarget(""); setCurrent(""); setDate("");
      onSaved();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.5)", justifyContent: "flex-end" }}>
        <View style={{ backgroundColor: theme.surface, borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 20, maxHeight: "85%" }}>
          <ScrollView>
            <Text style={{ color: theme.text, fontSize: 18, fontWeight: "700", marginBottom: 14 }}>New Financial Goal</Text>

            <Field theme={theme} label="Goal Name">
              <AppInput theme={theme} value={name} onChangeText={setName} placeholder="e.g. New Laptop" />
            </Field>

            <Field theme={theme} label="Target Amount (₹)">
              <AppInput theme={theme} value={target} onChangeText={(t) => setTarget(t.replace(/[^0-9.]/g, ""))} keyboardType="decimal-pad" placeholder="0" />
            </Field>

            <Field theme={theme} label="Current Savings (₹)">
              <AppInput theme={theme} value={current} onChangeText={(t) => setCurrent(t.replace(/[^0-9.]/g, ""))} keyboardType="decimal-pad" placeholder="0" />
            </Field>

            <Field theme={theme} label="Target Date (YYYY-MM-DD)">
              <AppInput theme={theme} value={date} onChangeText={setDate} placeholder="2026-12-31" />
            </Field>

            <ErrorText theme={theme}>{error}</ErrorText>

            <PrimaryButton theme={theme} title={saving ? "Saving…" : "Create Goal"} onPress={submit} disabled={saving} style={{ marginBottom: 10 }} />
            <GhostButton theme={theme} title="Cancel" onPress={onClose} />
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
