package com.alieducational.app.fakes

import androidx.fragment.app.FragmentActivity
import com.alieducational.app.util.BiometricAuthManager

class FakeBiometricAuthManager(
    var canAuth: Boolean = true,
    var shouldSucceed: Boolean = true,
    var errorMessage: String = "Biometric authentication failed"
) : BiometricAuthManager() {

    override fun canAuthenticate(): Boolean = canAuth

    override fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuth) {
            onError("Biometric authentication unavailable")
            return
        }
        if (shouldSucceed) {
            onSuccess()
        } else {
            onError(errorMessage)
        }
    }
}
