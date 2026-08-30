import React from "react";
import { View, Text, ActivityIndicator } from "react-native";
import { NavigationContainer, DarkTheme, DefaultTheme } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { useAuth } from "../context/AuthContext";
import LoginScreen from "../screens/LoginScreen";
import HomeScreen from "../screens/HomeScreen";
import WalletScreen from "../screens/WalletScreen";
import GoalScreen from "../screens/GoalScreen";
import CalculatorScreen from "../screens/CalculatorScreen";
import BudgetScreen from "../screens/BudgetScreen";
import MoreScreen from "../screens/MoreScreen";

const Tab = createBottomTabNavigator();

function TabIcon({ label, focused, theme }) {
  return (
    <Text style={{ fontSize: 10.5, fontWeight: "700", color: focused ? theme.accent : theme.faint }}>{label}</Text>
  );
}

export default function RootNavigator() {
  const { user, booting, theme, isDark } = useAuth();

  if (booting) {
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: theme.bg }}>
        <ActivityIndicator color={theme.accent} />
      </View>
    );
  }

  const navTheme = {
    ...(isDark ? DarkTheme : DefaultTheme),
    colors: { ...(isDark ? DarkTheme.colors : DefaultTheme.colors), background: theme.bg, card: theme.navBg, border: theme.border, text: theme.text, primary: theme.accent },
  };

  if (!user) {
    return (
      <NavigationContainer theme={navTheme}>
        <LoginScreen />
      </NavigationContainer>
    );
  }

  return (
    <NavigationContainer theme={navTheme}>
      <Tab.Navigator
        screenOptions={{
          headerShown: false,
          tabBarStyle: { backgroundColor: theme.navBg, borderTopColor: theme.border },
          tabBarShowLabel: true,
          tabBarActiveTintColor: theme.accent,
          tabBarInactiveTintColor: theme.faint,
        }}
      >
        <Tab.Screen name="Home" component={HomeScreen} />
        <Tab.Screen name="Wallet" component={WalletScreen} />
        <Tab.Screen name="Goals" component={GoalScreen} />
        <Tab.Screen name="Calculator" component={CalculatorScreen} />
        <Tab.Screen name="Budget" component={BudgetScreen} />
        <Tab.Screen name="More" component={MoreScreen} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}
