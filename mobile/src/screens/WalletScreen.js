import React, { useCallback, useState } from "react";
import { View, Text, FlatList, SafeAreaView, TouchableOpacity, Modal, ScrollView, Alert } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/client";
import { Card, Field, AppInput, PrimaryButton, GhostButton, ErrorText } from "../components/ui";

function money(n) {
  const v = Number(n) || 0;
  return (v < 0 ? "-" : "") + "\u20B9" + Math.abs(v).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}
const todayISO = () => new Date().toISOString().slice(0, 10);

export default function WalletScreen() {
  const { theme } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [showAdd, setShowAdd] = useState(false);

  const load = useCallback(async () => {
    const [txRes, catRes] = await Promise.all([api.listTransactions(), api.listCategories()]);
    setTransactions(txRes.transactions);
    setCategories(catRes.categories);
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  async function voidTx(tx) {
    Alert.alert("Void transaction?", "It will be removed from active totals but kept in history.", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Void", style: "destructive", onPress: async () => {
          await api.voidTransaction(tx.id);
          load();
        },
      },
    ]);
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", padding: 18, paddingBottom: 8 }}>
        <Text style={{ color: theme.text, fontSize: 22, fontWeight: "700" }}>Wallet</Text>
        <TouchableOpacity onPress={() => setShowAdd(true)} style={{ backgroundColor: theme.accent, width: 40, height: 40, borderRadius: 20, alignItems: "center", justifyContent: "center" }}>
          <Text style={{ color: theme.accentText, fontSize: 22, fontWeight: "700", marginTop: -2 }}>+</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={transactions}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ padding: 18, paddingTop: 6 }}
        ListEmptyComponent={<Text style={{ color: theme.subtext, textAlign: "center", marginTop: 40 }}>No transactions yet. Tap + to add one.</Text>}
        renderItem={({ item }) => {
          const cat = categories.find((c) => c.id === item.category_id);
          const isIncome = item.transaction_type === "INCOME";
          const voided = item.status === "VOIDED";
          return (
            <TouchableOpacity onLongPress={() => !voided && voidTx(item)}>
              <Card theme={theme} style={{ marginBottom: 8, opacity: voided ? 0.5 : 1 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <View style={{ flex: 1 }}>
                    <Text style={{ color: theme.text, fontWeight: "600", textDecorationLine: voided ? "line-through" : "none" }}>
                      {item.description || cat?.name || "Other"}
                    </Text>
                    <Text style={{ color: theme.faint, fontSize: 12 }}>{cat?.name || "Other"} · {item.transaction_date}{voided ? " · Voided" : ""}</Text>
                  </View>
                  <Text style={{ color: voided ? theme.faint : isIncome ? theme.success : theme.text, fontWeight: "700" }}>
                    {isIncome ? "+" : "-"}{money(item.amount)}
                  </Text>
                </View>
              </Card>
            </TouchableOpacity>
          );
        }}
      />

      <AddTransactionModal
        visible={showAdd}
        onClose={() => setShowAdd(false)}
        onSaved={() => { setShowAdd(false); load(); }}
        theme={theme}
        categories={categories}
      />
    </SafeAreaView>
  );
}

function AddTransactionModal({ visible, onClose, onSaved, theme, categories }) {
  const [type, setType] = useState("EXPENSE");
  const [amount, setAmount] = useState("");
  const [categoryId, setCategoryId] = useState(null);
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const filteredCats = categories.filter((c) => c.category_type === type);

  async function submit() {
    setError("");
    const amt = parseFloat(amount);
    if (!amount || isNaN(amt) || amt <= 0) { setError("Enter an amount greater than 0."); return; }
    setSaving(true);
    try {
      await api.addTransaction({
        transactionType: type, amount: amt,
        categoryId: categoryId || filteredCats[0]?.id || null,
        description: description.trim(), transactionDate: todayISO(),
      });
      setAmount(""); setDescription(""); setCategoryId(null);
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
            <Text style={{ color: theme.text, fontSize: 18, fontWeight: "700", marginBottom: 14 }}>Add Transaction</Text>

            <View style={{ flexDirection: "row", gap: 8, marginBottom: 16 }}>
              {["EXPENSE", "INCOME"].map((tp) => (
                <TouchableOpacity
                  key={tp}
                  onPress={() => { setType(tp); setCategoryId(null); }}
                  style={{ flex: 1, padding: 11, borderRadius: 12, borderWidth: 1.5, borderColor: type === tp ? theme.accent : theme.border, backgroundColor: type === tp ? theme.accentSoft : "transparent", alignItems: "center" }}
                >
                  <Text style={{ color: type === tp ? theme.accent : theme.subtext, fontWeight: "700" }}>{tp === "EXPENSE" ? "Expense" : "Income"}</Text>
                </TouchableOpacity>
              ))}
            </View>

            <Field theme={theme} label="Amount (₹)">
              <AppInput theme={theme} value={amount} onChangeText={(t) => setAmount(t.replace(/[^0-9.]/g, ""))} keyboardType="decimal-pad" placeholder="0.00" />
            </Field>

            <Field theme={theme} label="Category">
              <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                <View style={{ flexDirection: "row", gap: 8 }}>
                  {filteredCats.map((c) => (
                    <TouchableOpacity
                      key={c.id}
                      onPress={() => setCategoryId(c.id)}
                      style={{ paddingHorizontal: 12, paddingVertical: 8, borderRadius: 999, borderWidth: 1.5, borderColor: (categoryId || filteredCats[0]?.id) === c.id ? theme.accent : theme.border, backgroundColor: (categoryId || filteredCats[0]?.id) === c.id ? theme.accentSoft : "transparent" }}
                    >
                      <Text style={{ color: (categoryId || filteredCats[0]?.id) === c.id ? theme.accent : theme.subtext, fontWeight: "600", fontSize: 13 }}>{c.name}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </ScrollView>
            </Field>

            <Field theme={theme} label="Description (optional)">
              <AppInput theme={theme} value={description} onChangeText={setDescription} placeholder="e.g. Groceries" maxLength={200} />
            </Field>

            <ErrorText theme={theme}>{error}</ErrorText>

            <PrimaryButton theme={theme} title={saving ? "Saving…" : "Save Transaction"} onPress={submit} disabled={saving} style={{ marginBottom: 10 }} />
            <GhostButton theme={theme} title="Cancel" onPress={onClose} />
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
