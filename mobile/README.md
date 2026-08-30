# Wallet Scholer — Mobile App

Expo (React Native) app. Bundled successfully through Metro (734 modules,
real Android JS bundle produced) before delivery — see the root README.

## Setup

```bash
npm install
npx expo start
```

Press `a` for Android emulator, `i` for iOS simulator, or scan the QR code
with the Expo Go app on a physical device.

## Pointing at the backend

Edit `app.json` → `expo.extra.apiBaseUrl`:

- **Simulator/emulator on the same machine as the backend**: `http://localhost:4000` (default) works for iOS simulator; Android emulator needs `http://10.0.2.2:4000` instead of `localhost`.
- **Physical device**: use your computer's LAN IP, e.g. `http://192.168.1.20:4000`, and make sure your phone is on the same network.
- **Deployed backend**: use its public HTTPS URL.

## Screens

| Screen | Talks to backend? |
|---|---|
| Login / Register | Yes — `/api/auth/*` |
| Home (dashboard) | Yes — transactions + budget utilization |
| Wallet | Yes — transaction CRUD + void |
| Financial Calculator | No — all 7 modules (EMI, Loan, Simple Interest, Compound Interest, Savings, SIP, Percentage) compute instantly on-device using the exact same formulas as the backend (`src/domain/financeEngine.js` is a byte-for-byte copy of the backend's) |
| Budget | Yes — create/edit, exceeds-income confirmation, copy-last-month, per-category utilization |
| More | Yes — notification preferences, Google Sheets backup status |

## Google Sign-In — what's wired vs. what you need to add

`src/screens/LoginScreen.js` has a `getGoogleIdTokenStub()` placeholder that
throws a clear error until you wire real native Google Sign-In. To finish it:

1. Install `expo-auth-session` (already in `package.json`) or
   `@react-native-google-signin/google-signin` for a native modal.
2. Register an OAuth client (Android/iOS type) in Google Cloud Console for
   your Expo project — see `backend/README.md` for the Cloud Console steps.
3. Replace `getGoogleIdTokenStub()` with the real token-acquisition call, then
   pass the resulting ID token into `loginWithGoogleIdToken()` (already wired
   through `AuthContext` → `POST /api/auth/google`).

## Google Sheets backup

`MoreScreen.js` calls `GET /api/integrations/google-sheets/connect`, which
returns a Google consent URL, and opens it via `Linking.openURL` (system
browser). After the user grants access, the backend's callback creates their
spreadsheet and redirects to `walletscholer://backup-connected` — add a deep
link handler in `App.js` if you want to catch that redirect and show a
success screen instead of leaving the user in the browser.

## What's not implemented

- Native push notifications (budget alerts currently surface as in-app cards
  fetched from `/api/budgets/:id/utilization`, not system push). Wiring
  `expo-notifications` + a device token registered against the backend is the
  natural next step once you want alerts to arrive when the app is closed.
- Offline queueing of transactions created without network — the app assumes
  a reachable backend for every write today, unlike the original app's
  offline-first local SQLite design.
