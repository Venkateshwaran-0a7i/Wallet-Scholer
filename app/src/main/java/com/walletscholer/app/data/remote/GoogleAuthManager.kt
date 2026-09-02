package com.walletscholer.app.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes

/**
 * Wraps real Google Sign-In (com.google.android.gms.auth.api.signin). This is the
 * legacy-but-fully-supported Google Sign-In API; it is the simplest reliable way to get
 * BOTH an authenticated Google identity (name/email) AND an OAuth grant for the
 * Sheets API scope in a single consent screen.
 *
 * IMPORTANT (must be done once in Google Cloud Console before this works — see the
 * setup notes shared alongside this code):
 *   1. Create/select a project, enable the "Google Sheets API".
 *   2. Configure the OAuth consent screen (External, Testing is fine for personal use)
 *      and add your own Google account under "Test users".
 *   3. Create an OAuth 2.0 Client ID of type "Android", using this app's package name
 *      (applicationId in build.gradle.kts) and your signing certificate's SHA-1
 *      fingerprint (debug keystore SHA-1 while developing).
 * No client ID string needs to be embedded in code for this flow — Google matches the
 * request to your OAuth client using the app's package name + signing SHA-1 automatically.
 */
object GoogleAuthManager {

    fun getClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getSignInIntent(context: Context): Intent = getClient(context).signInIntent

    /** The account from the most recent successful sign-in, if any (survives app restarts). */
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? =
        try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (_: Exception) {
            null
        }

    /** True once the signed-in account has actually granted the Sheets scope. */
    fun hasSheetsPermission(account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        return try {
            GoogleSignIn.hasPermissions(account, Scope(SheetsScopes.SPREADSHEETS))
        } catch (_: Exception) {
            false
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        getClient(context).signOut().addOnCompleteListener { onComplete() }
    }
}
