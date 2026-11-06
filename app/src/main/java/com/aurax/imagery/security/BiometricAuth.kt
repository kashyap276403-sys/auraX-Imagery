package com.aurax.imagery.security

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/**
 * Thin wrapper over the platform BiometricPrompt (no extra dependency). Accepts a strong biometric or
 * the device PIN / pattern / password, so a phone without a fingerprint sensor can still use App Lock.
 */
object BiometricAuth {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isAvailable(context: Context): Boolean {
        // Never let a missing permission or odd OEM build take the Settings screen down.
        return runCatching {
            val manager = context.getSystemService(BiometricManager::class.java) ?: return false
            manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    /** Shows the system prompt; [onResult] gets true only on a successful authentication. */
    fun prompt(activity: Activity, title: String, onResult: (Boolean) -> Unit): CancellationSignal {
        val signal = CancellationSignal()
        try {
            BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
                .authenticate(
                    signal,
                    activity.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = onResult(false)
                    },
                )
        } catch (e: Exception) {
            // Stay locked rather than crash; the lock screen's Unlock button can try again.
            onResult(false)
        }
        return signal
    }
}
