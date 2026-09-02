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
 * Checks biometric/device credential availability and triggers the system biometric prompt.
 * Works on API 26+ (minSdk = 26 in this project).
 */
object BiometricLockManager {

    /**
     * Returns true if the device has any usable authentication method
     * (fingerprint, face, or device PIN/pattern/password as fallback).
     */
    fun isAvailable(context: Context): Boolean {
        val mgr = BiometricManager.from(context)
        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mgr.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            } else {
                @Suppress("DEPRECATION")
                mgr.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            }
            result == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shows the biometric prompt with device credential fallback. Callbacks fire on the main thread.
     *
     * @param activity      A FragmentActivity (your MainActivity)
     * @param onSuccess     Called when authentication succeeds
     * @param onError       Called with a user-readable error message
     * @param onFailed      Called when the biometric attempt was not recognised (try again)
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Wallet Scholar",
        subtitle: String = "Use your fingerprint, face, or device PIN to continue",
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
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onError(errString.toString())
                } else {
                    onError("") // User cancelled
                }
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            infoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            infoBuilder.setDeviceCredentialAllowed(true)
        }

        try {
            prompt.authenticate(infoBuilder.build())
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Authentication failed to start")
        }
    }
}
