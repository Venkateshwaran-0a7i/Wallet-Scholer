# Setting Up Real Google Sign-In & Google Sheets Sync

This covers the steps **you** need to do in Google Cloud Console before the updated
`app/` (Android) project will actually sign you in for real and sync to a real
spreadsheet. None of this can be done from code — it's account/console setup on your end.

---

## 1. Find your app's package name and SHA-1 fingerprint

**Package name** — already set in `app/build.gradle.kts`:
```
applicationId = "com.aistudio.walletscholer.a7ik"
```
(Use whatever value is actually in your `build.gradle.kts` — copy it exactly.)

**SHA-1 fingerprint** — open a terminal in the project folder and run:

- **Mac/Linux (debug key, used automatically by Android Studio while developing):**
  ```
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```
- **Windows:**
  ```
  keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

Copy the line that starts with `SHA1:` (looks like `AA:BB:CC:...`).

> If you later build a signed release APK with a different keystore, you'll need to repeat
> this step with that keystore and add a *second* Android OAuth client for it.

---

## 2. Create a Google Cloud project (or reuse one you already have)

1. Go to [console.cloud.google.com](https://console.cloud.google.com/).
2. Top-left project dropdown → **New Project** → give it any name (e.g. "Wallet Scholar") → **Create**.
3. Make sure the new project is selected in the top bar before continuing.

---

## 3. Enable the Google Sheets API

1. In the left sidebar: **APIs & Services → Library**.
2. Search for **"Google Sheets API"** → open it → click **Enable**.

---

## 4. Configure the OAuth consent screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** → Create.
3. Fill in the required fields (App name, your email as support contact, your email as developer contact). You can leave logo/domain fields blank for personal use.
4. On the **Scopes** step, click **Add or Remove Scopes** and add:
   - `.../auth/spreadsheets` (Google Sheets API)
5. On the **Test users** step, click **Add Users** and add **your own Google account email** (the one you'll sign in with in the app). This is required — without it, sign-in will fail with an "access blocked" error.
6. Save. Leave the app in **Testing** publishing status — you don't need to submit for verification for personal use.

> **Note on Testing mode:** while your app is in "Testing" status, only the test users you
> listed can sign in, and Google may require you to re-consent roughly every 7 days. That's
> fine for personal use. If you ever want other people to use it, you'd need to move to
> "Production" and go through Google's verification process for the Sheets scope (it's a
> "sensitive scope").

---

## 5. Create the OAuth 2.0 Client ID (Android)

1. **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. Application type: **Android**.
3. Paste in your **package name** (from Step 1).
4. Paste in your **SHA-1 fingerprint** (from Step 1).
5. Click **Create**.

You do **not** need to copy any client ID string into the code — Google matches sign-in
requests to this client automatically using your app's package name + signing certificate.

---

## 6. Create your own Google Sheet

The app can only write to a spreadsheet your Google account can actually edit — it cannot
write to anyone else's sheet.

1. Go to [sheets.google.com](https://sheets.google.com) → create a new blank spreadsheet.
2. Rename the sheet tabs (bottom tabs) to exactly: **Transactions**, **Budget**, **Goals**
   (create two more tabs with the `+` button if needed — a new sheet only has one tab called "Sheet1").
3. Copy the **Sheet ID** from the URL:
   ```
   https://docs.google.com/spreadsheets/d/  1AbCDefGhIJKLmnoPQRstuVWxyz1234567890 /edit
                                              ^^^^^^^^^^^^^^^^ this part is the Sheet ID
   ```
4. Keep that ID handy — you'll paste it into the app's "Your Spreadsheet" field after signing in.

---

## 7. Build and run

1. Open the project in Android Studio, let Gradle sync (it will download the new
   dependencies added to `app/build.gradle.kts`).
2. Run on an emulator or device that has **Google Play Services** installed (a plain AOSP
   emulator image without Play Store won't support Google Sign-In — pick an emulator image
   that shows the Play Store icon when creating it).
3. Open the app → **More → Continue with Google** (or the Account/Sync sheet) → pick your
   Google account → grant the Sheets permission when prompted.
4. Paste your Sheet ID from Step 6 and tap **Save Sheet ID**, then **Sync Now**.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| "Sign-in failed (code 10)" | SHA-1 or package name doesn't match what's registered in Step 5. Double-check both, and make sure you used the debug keystore's SHA-1 if you're running a debug build. |
| "Sign-in failed (code 12501)" | You cancelled the account picker — just try again. |
| Google shows "Access blocked: this app's request is invalid" | Your account isn't added as a Test user (Step 4.5), or the OAuth consent screen isn't fully saved. |
| Sync fails with a 403 / permission error | The signed-in account doesn't have edit access to that Sheet ID — make sure you created the sheet with the *same* Google account you signed into the app with. |
| Sync fails with a 404 | The Sheet ID is wrong, or the tab names don't exactly match `Transactions` / `Budget` / `Goals`. |
| No emulator prompt appears at all | The emulator image doesn't include Google Play Services — recreate it using a Play Store–enabled system image. |

---

## What changed in the code (for reference)

- `GoogleAuthManager.kt` (new) — real `GoogleSignInClient` requesting the Sheets scope.
- `MainActivity.kt` — launches the real sign-in intent, stores the authenticated account, restores it on relaunch.
- `GoogleSheetsSyncEngine.kt` — now performs real Sheets API `clear` + `update` calls instead of a fake delay.
- `AccountSyncSheet.kt` / `MoreScreen.kt` — buttons now trigger real sign-in/out; added a field to enter your own Sheet ID.
- `UserSettingsEntity.kt` — removed hardcoded "already logged in as [name]" defaults; new installs start logged out with no data.
- `WalletScholarRepository.kt` — removed automatic fake demo transactions/goals; presets no longer overwrite your real identity.
- `app/build.gradle.kts` — added `play-services-auth`, `google-api-client-android`, `google-api-services-sheets`, `google-http-client-gson`.
