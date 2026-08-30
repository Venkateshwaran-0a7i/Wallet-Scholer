import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, setToken } from "../api/client";
import { darkTheme, lightTheme } from "../theme/theme";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [booting, setBooting] = useState(true);
  const [isDark, setIsDark] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const { user } = await api.me();
        setUser(user);
      } catch (e) {
        setUser(null);
      } finally {
        setBooting(false);
      }
    })();
  }, []);

  async function register(displayName, email, password) {
    const { token, user } = await api.register({ displayName, email, password });
    await setToken(token);
    setUser(user);
  }

  async function login(email, password) {
    const { token, user } = await api.login({ email, password });
    await setToken(token);
    setUser(user);
  }

  async function loginWithGoogleIdToken(idToken) {
    const { token, user } = await api.loginWithGoogle(idToken);
    await setToken(token);
    setUser(user);
  }

  async function logout() {
    await setToken(null);
    setUser(null);
  }

  const theme = isDark ? darkTheme : lightTheme;

  const value = useMemo(
    () => ({ user, booting, register, login, loginWithGoogleIdToken, logout, theme, isDark, setIsDark }),
    [user, booting, isDark]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
