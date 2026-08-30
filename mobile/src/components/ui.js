import React from "react";
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from "react-native";

export function Card({ theme, style, children }) {
  return (
    <View style={[{ backgroundColor: theme.surface, borderColor: theme.border, borderWidth: 1, borderRadius: 18, padding: 14 }, style]}>
      {children}
    </View>
  );
}

export function Field({ theme, label, children }) {
  return (
    <View style={{ marginBottom: 14 }}>
      <Text style={{ color: theme.subtext, fontSize: 12, fontWeight: "700", marginBottom: 6, textTransform: "uppercase", letterSpacing: 0.3 }}>{label}</Text>
      {children}
    </View>
  );
}

export function AppInput({ theme, style, ...props }) {
  return (
    <TextInput
      placeholderTextColor={theme.faint}
      style={[
        {
          backgroundColor: theme.bg, borderColor: theme.border, borderWidth: 1.5, borderRadius: 12,
          padding: 12, color: theme.text, fontSize: 15,
        },
        style,
      ]}
      {...props}
    />
  );
}

export function PrimaryButton({ theme, title, onPress, disabled, style, children }) {
  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled}
      style={[
        { backgroundColor: disabled ? theme.border : theme.accent, borderRadius: 14, paddingVertical: 14, alignItems: "center" },
        style,
      ]}
    >
      {children || <Text style={{ color: disabled ? theme.faint : theme.accentText, fontWeight: "700", fontSize: 15 }}>{title}</Text>}
    </TouchableOpacity>
  );
}

export function GhostButton({ theme, title, onPress, style }) {
  return (
    <TouchableOpacity onPress={onPress} style={[{ borderColor: theme.border, borderWidth: 1.5, borderRadius: 14, paddingVertical: 14, alignItems: "center" }, style]}>
      <Text style={{ color: theme.text, fontWeight: "700", fontSize: 15 }}>{title}</Text>
    </TouchableOpacity>
  );
}

export function Pill({ theme, color, bg, children }) {
  return (
    <View style={{ backgroundColor: bg, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4, alignSelf: "flex-start" }}>
      <Text style={{ color, fontSize: 12, fontWeight: "700" }}>{children}</Text>
    </View>
  );
}

export function SectionLabel({ theme, children }) {
  return (
    <Text style={{ color: theme.subtext, fontSize: 13, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.5, marginTop: 22, marginBottom: 10 }}>
      {children}
    </Text>
  );
}

export function ErrorText({ theme, children }) {
  if (!children) return null;
  return <Text style={{ color: theme.danger, fontSize: 13, marginBottom: 12 }}>{children}</Text>;
}
