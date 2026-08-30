import AsyncStorage from "@react-native-async-storage/async-storage";
import Constants from "expo-constants";

const BASE_URL = Constants.expoConfig?.extra?.apiBaseUrl || "http://localhost:4000";
const TOKEN_KEY = "wallet_scholer_token";

async function getToken() {
  return AsyncStorage.getItem(TOKEN_KEY);
}

export async function setToken(token) {
  if (token) await AsyncStorage.setItem(TOKEN_KEY, token);
  else await AsyncStorage.removeItem(TOKEN_KEY);
}

async function request(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth) {
    const token = await getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const isJson = res.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await res.json() : null;
  if (!res.ok) {
    const err = new Error(data?.error || `Request failed (${res.status})`);
    err.status = res.status;
    err.details = data?.details;
    throw err;
  }
  return data;
}

export const api = {
  register: (payload) => request("/api/auth/register", { method: "POST", body: payload, auth: false }),
  login: (payload) => request("/api/auth/login", { method: "POST", body: payload, auth: false }),
  loginWithGoogle: (idToken) => request("/api/auth/google", { method: "POST", body: { idToken }, auth: false }),
  me: () => request("/api/auth/me"),

  listCategories: () => request("/api/categories"),
  addCategory: (payload) => request("/api/categories", { method: "POST", body: payload }),

  listTransactions: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/api/transactions${qs ? `?${qs}` : ""}`);
  },
  addTransaction: (payload) => request("/api/transactions", { method: "POST", body: payload }),
  updateTransaction: (id, payload) => request(`/api/transactions/${id}`, { method: "PATCH", body: payload }),
  voidTransaction: (id) => request(`/api/transactions/${id}/void`, { method: "PATCH" }),

  getBudget: (year, month) => request(`/api/budgets?year=${year}&month=${month}`),
  createBudget: (payload) => request("/api/budgets", { method: "POST", body: payload }),
  updateBudget: (id, payload) => request(`/api/budgets/${id}`, { method: "PATCH", body: payload }),
  deleteBudget: (id) => request(`/api/budgets/${id}`, { method: "DELETE" }),
  copyLastMonth: (year, month) => request("/api/budgets/copy-last-month", { method: "POST", body: { periodYear: year, periodMonth: month } }),
  getUtilization: (budgetId) => request(`/api/budgets/${budgetId}/utilization`),

  listGoals: () => request("/api/goals"),
  addGoal: (payload) => request("/api/goals", { method: "POST", body: payload }),
  updateGoal: (id, payload) => request(`/api/goals/${id}`, { method: "PATCH", body: payload }),
  deleteGoal: (id) => request(`/api/goals/${id}`, { method: "DELETE" }),

  getNotificationPrefs: () => request("/api/notifications/preferences"),
  updateNotificationPrefs: (payload) => request("/api/notifications/preferences", { method: "PATCH", body: payload }),

  getSheetsConnectUrl: () => request("/api/integrations/google-sheets/connect"),
  getSheetsStatus: () => request("/api/integrations/google-sheets/status"),
  syncSheetsNow: () => request("/api/integrations/google-sheets/sync", { method: "POST" }),
  disconnectSheets: () => request("/api/integrations/google-sheets/disconnect", { method: "POST" }),
};
