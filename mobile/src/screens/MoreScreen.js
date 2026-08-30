import React, { useCallback, useState } from "react";
import { View, Text, ScrollView, SafeAreaView, Switch, Linking, ActivityIndicator, Alert } from "react-native";
import { useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { api } from "../api/client";
import { Card, SectionLabel, PrimaryButton, GhostButton, Pill } from "../components/ui";

export default function MoreScreen() {
  const { theme, user, logout, isDark, setIsDark } = useAuth();
  const [prefs, setPrefs] = useState(null);
  const [integration, setIntegration] = useState(null);
  const [connecting, setConnecting] = useState(false);

  const load = useCallback(async () => {
    const [prefsRes, statusRes] = await Promise.all([
      api.getNotificationPrefs().catch(() => ({ preferences: null })),
      api.getSheetsStatus().catch(() => ({ integration: null })),
    ]);
    setPrefs(prefsRes.preferences);
    setIntegration(statusRes.integration);
  }, []);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  async function togglePref(key, value) {
    const patch = { [key]: value };
    setPrefs((p) => ({ ...p, [snakeToCamelMap[key]]: value }));
    await api.updateNotificationPrefs(patch);
  }

  async function connectSheets() {
    setConnecting(true);
    try {
      const { url } = await api.getSheetsConnectUrl();
      await Linking.openURL(url); // opens system browser for the Google consent screen
    } catch (e) {
      Alert.alert("Couldn't start backup setup", e.message);
    } finally {
      setConnecting(false);
    }
  }

  async function disconnectSheets() {
    await api.disconnectSheets();
    load();
  }

  async function syncNow() {
    try {
      await api.syncSheetsNow();
      load();
    } catch (e) {
      Alert.alert("Sync failed", e.message);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <ScrollView contentContainerStyle={{ padding: 18 }}>
        <Text style={{ color: theme.text, fontSize: 22, fontWeight: "700", marginBottom: 16 }}>More</Text>

        <Card theme={theme} style={{ marginBottom: 4 }}>
          <Text style={{ color: theme.text, fontWeight: "700" }}>{user?.displayName}</Text>
          <Text style={{ color: theme.subtext, fontSize: 12, marginBottom: 12 }}>{user?.email}</Text>
          <GhostButton theme={theme} title="Sign out" onPress={logout} />
        </Card>

        <SectionLabel theme={theme}>Notifications</SectionLabel>
        <Card theme={theme}>
          <PrefRow theme={theme} label="Budget alerts" value={!!prefs?.master_enabled} onChange={(v) => togglePref("masterEnabled", v)} />
          <PrefRow theme={theme} label="75% used" value={!!prefs?.threshold_75} onChange={(v) => togglePref("threshold75", v)} sub />
          <PrefRow theme={theme} label="90% used" value={!!prefs?.threshold_90} onChange={(v) => togglePref("threshold90", v)} sub />
          <PrefRow theme={theme} label="100% used" value={!!prefs?.threshold_100} onChange={(v) => togglePref("threshold100", v)} sub />
          <PrefRow theme={theme} label="Over budget" value={!!prefs?.threshold_exceeded} onChange={(v) => togglePref("thresholdExceeded", v)} sub />
        </Card>

        <SectionLabel theme={theme}>Backup & Sync</SectionLabel>
        <Card theme={theme} style={{ marginBottom: 4 }}>
          {(!integration || integration.sync_status === "DISCONNECTED") && (
            <>
              <Text style={{ color: theme.subtext, fontSize: 13, marginBottom: 12 }}>
                Back up Transactions, Budgets, Income, and Monthly Summary to your own Google Sheet. Only these structured records are sent.
              </Text>
              <PrimaryButton theme={theme} title={connecting ? "Opening Google…" : "Enable Google Sheets Backup"} onPress={connectSheets} disabled={connecting} />
            </>
          )}
          {integration?.sync_status === "CONNECTING" && (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
              <ActivityIndicator color={theme.accent} />
              <Text style={{ color: theme.subtext }}>Syncing…</Text>
            </View>
          )}
          {integration?.sync_status === "CONNECTED" && (
            <>
              <Pill theme={theme} color={theme.success} bg={theme.successSoft}>Up to date</Pill>
              <Text style={{ color: theme.faint, fontSize: 11, marginTop: 8 }}>Last synced: {integration.last_synced_at || "just now"}</Text>
              <View style={{ flexDirection: "row", gap: 8, marginTop: 12 }}>
                <GhostButton theme={theme} title="Sync now" onPress={syncNow} style={{ flex: 1 }} />
                <GhostButton theme={theme} title="Disconnect" onPress={disconnectSheets} style={{ flex: 1, borderColor: theme.danger }} />
              </View>
            </>
          )}
          {integration?.sync_status === "FAILED" && (
            <>
              <Pill theme={theme} color={theme.danger} bg={theme.dangerSoft}>Sync failed — retrying</Pill>
              <GhostButton theme={theme} title="Retry now" onPress={syncNow} style={{ marginTop: 12 }} />
            </>
          )}
        </Card>

        <SectionLabel theme={theme}>Appearance</SectionLabel>
        <Card theme={theme}>
          <PrefRow theme={theme} label="Dark theme" value={isDark} onChange={setIsDark} />
        </Card>
      </ScrollView>
    </SafeAreaView>
  );
}

const snakeToCamelMap = {
  masterEnabled: "master_enabled", threshold75: "threshold_75", threshold90: "threshold_90",
  threshold100: "threshold_100", thresholdExceeded: "threshold_exceeded",
};

function PrefRow({ theme, label, value, onChange, sub }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingVertical: sub ? 8 : 10, paddingLeft: sub ? 12 : 0 }}>
      <Text style={{ color: theme.text, fontSize: sub ? 13 : 14, fontWeight: sub ? "500" : "600" }}>{label}</Text>
      <Switch value={value} onValueChange={onChange} trackColor={{ true: theme.accent, false: theme.borderSoft }} />
    </View>
  );
}
