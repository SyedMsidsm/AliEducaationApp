package com.alieducational.app.util

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        fun getAllowedAuthenticators(sdkInt: Int = Build.VERSION.SDK_INT): Int {
            return if (sdkInt >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            }
        }

        fun createAuthCallback(
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ): BiometricPrompt.AuthenticationCallback {
            return object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Non-terminal attempt; do NOT call onError so user can retry on prompt
                }
            }
        }
    }

    open fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(getAllowedAuthenticators())
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    open fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Unlock Ali Educational",
        subtitle: String = "Confirm your fingerprint or device credential",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            createAuthCallback(onSuccess, onError)
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(getAllowedAuthenticators())
            .build()

        prompt.authenticate(promptInfo)
    }
}