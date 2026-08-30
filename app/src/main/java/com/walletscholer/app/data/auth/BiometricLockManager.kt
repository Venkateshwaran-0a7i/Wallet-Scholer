package com.walletscholer.app.data.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Checks biometric availability and triggers the system biometric prompt.
 * Works on API 26+ (minSdk = 26 in this project).
 */
object BiometricLockManager {

    /**
     * Returns true if the device has any usable authentication method
     * (fingerprint, face, or device PIN/pattern/password as fallback).
     */
    fun isAvailable(context: Context): Boolean {
        val mgr = BiometricManager.from(context)
        val result = mgr.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the biometric prompt. Callbacks fire on the main thread.
     *
     * @param activity      A FragmentActivity (your MainActivity)
     * @param onSuccess     Called when authentication succeeds
     * @param onError       Called with a user-readable error message
     * @param onFailed      Called when the biometric attempt was not recognised (try again)
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Wallet Scholar",
        subtitle: String = "Use your fingerprint or face to open the app",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // errorCode 10 = user pressed Cancel / back; 13 = lockout
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onError(errString.toString())
                } else {
                    onError("") // Silent cancel — caller decides what to do
                }
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(info)
    }
}
