import React, { useState } from "react";
import { View, Text, ScrollView, ActivityIndicator, SafeAreaView } from "react-native";
import { useAuth } from "../context/AuthContext";
import { Field, AppInput, PrimaryButton, GhostButton, ErrorText } from "../components/ui";

// Google Sign-In on mobile: use expo-auth-session (or @react-native-google-signin/google-signin
// for a native modal) to obtain an ID token, then hand it to loginWithGoogleIdToken().
// Wiring the real Google client ID is an app-config step — see mobile/README.md.
async function getGoogleIdTokenStub() {
  throw new Error("Configure GOOGLE_EXPO_CLIENT_ID in app.json and wire expo-auth-session to enable Google Sign-In.");
}

export default function LoginScreen() {
  const { login, register, loginWithGoogleIdToken, theme } = useAuth();
  const [mode, setMode] = useState("login"); // login | register
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit() {
    setError("");
    setLoading(true);
    try {
      if (mode === "login") await login(email.trim(), password);
      else await register(displayName.trim(), email.trim(), password);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function googleSignIn() {
    setError("");
    try {
      const idToken = await getGoogleIdTokenStub();
      await loginWithGoogleIdToken(idToken);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: theme.bg }}>
      <ScrollView contentContainerStyle={{ padding: 24, flexGrow: 1, justifyContent: "center" }}>
        <Text style={{ color: theme.text, fontSize: 26, fontWeight: "700", marginBottom: 4 }}>Wallet Scholer</Text>
        <Text style={{ color: theme.subtext, fontSize: 14, marginBottom: 28 }}>
          {mode === "login" ? "Sign in to continue" : "Create your account"}
        </Text>

        {mode === "register" && (
          <Field theme={theme} label="Name">
            <AppInput theme={theme} value={displayName} onChangeText={setDisplayName} placeholder="Alex Kumar" />
          </Field>
        )}
        <Field theme={theme} label="Email">
          <AppInput theme={theme} value={email} onChangeText={setEmail} placeholder="you@example.com" autoCapitalize="none" keyboardType="email-address" />
        </Field>
        <Field theme={theme} label="Password">
          <AppInput theme={theme} value={password} onChangeText={setPassword} placeholder="At least 8 characters" secureTextEntry />
        </Field>

        <ErrorText theme={theme}>{error}</ErrorText>

        <PrimaryButton theme={theme} onPress={submit} disabled={loading} style={{ marginBottom: 10 }}>
          {loading ? <ActivityIndicator color={theme.accentText} /> : <Text style={{ color: theme.accentText, fontWeight: "700" }}>{mode === "login" ? "Sign In" : "Create Account"}</Text>}
        </PrimaryButton>

        <GhostButton theme={theme} title="Continue with Google" onPress={googleSignIn} style={{ marginBottom: 18 }} />

        <Text
          onPress={() => setMode(mode === "login" ? "register" : "login")}
          style={{ color: theme.accent, textAlign: "center", fontWeight: "600" }}
        >
          {mode === "login" ? "New here? Create an account" : "Already have an account? Sign in"}
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}
