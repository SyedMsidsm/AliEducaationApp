package com.alieducational.app.auth

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.alieducational.app.util.BiometricAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UnlockSecurityUnitTest {

    @Test
    fun authenticatorsForApi30AndAboveRequiresStrongAndDeviceCredential() {
        val authenticators = BiometricAuthManager.getAllowedAuthenticators(Build.VERSION_CODES.R)
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, authenticators)
    }

    @Test
    fun authenticatorsBelowApi30UsesWeakAndDeviceCredential() {
        val authenticatorsApi29 = BiometricAuthManager.getAllowedAuthenticators(Build.VERSION_CODES.Q)
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, authenticatorsApi29)
        assertNotEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, authenticatorsApi29)
    }

    @Test
    fun authenticationFailedDoesNotCallOnError() {
        var errorCalled = false
        var successCalled = false

        val callback = BiometricAuthManager.createAuthCallback(
            onSuccess = { successCalled = true },
            onError = { _ ->
                errorCalled = true
            }
        )

        // onAuthenticationFailed represents non-terminal failure (e.g. wrong finger)
        callback.onAuthenticationFailed()

        assertFalse("onAuthenticationFailed must NOT trigger onError", errorCalled)
        assertFalse("onAuthenticationFailed must NOT trigger onSuccess", successCalled)
    }

    @Test
    fun unlockScreenSourceContainsNoEnterShopDirectlyBypass() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidatePaths = listOf(
            File(userDir, "src/main/java/com/alieducational/app/ui/auth/UnlockScreen.kt"),
            File(userDir, "app/src/main/java/com/alieducational/app/ui/auth/UnlockScreen.kt")
        )
        val unlockScreenFile = candidatePaths.firstOrNull { it.exists() }
        assertTrue("UnlockScreen.kt must exist at one of $candidatePaths", unlockScreenFile != null && unlockScreenFile.exists())
        val content = unlockScreenFile!!.readText()
        assertFalse(
            "UnlockScreen must NOT contain 'Enter Shop Directly' bypass button",
            content.contains("Enter Shop Directly")
        )
    }
}
