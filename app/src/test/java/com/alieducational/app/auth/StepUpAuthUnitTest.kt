package com.alieducational.app.auth

import androidx.fragment.app.FragmentActivity
import com.alieducational.app.util.BiometricAuthManager
import com.alieducational.app.util.StepUpAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StepUpAuthUnitTest {

    private fun createDummyActivity(): FragmentActivity {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstanceMethod.invoke(unsafe, FragmentActivity::class.java) as FragmentActivity
    }

    private class FakeBiometricAuthManager(
        var canAuth: Boolean = true,
        var shouldSucceed: Boolean = true
    ) : BiometricAuthManager(createDummyContext()) {
        var promptCount = 0
        var lastTitle: String? = null

        override fun canAuthenticate(): Boolean = canAuth

        override fun promptBiometric(
            activity: FragmentActivity,
            title: String,
            subtitle: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) {
            promptCount++
            lastTitle = title
            if (shouldSucceed) {
                onSuccess()
            } else {
                onError("User cancelled")
            }
        }

        companion object {
            private fun createDummyContext(): android.content.Context {
                val unsafeClass = Class.forName("sun.misc.Unsafe")
                val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
                val unsafe = theUnsafeField.get(null)
                val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
                return allocateInstanceMethod.invoke(unsafe, android.content.ContextWrapper::class.java) as android.content.Context
            }
        }
    }

    @Test
    fun freshWindowSkipsPrompt() {
        var fakeTime = 100_000L
        val fakeBio = FakeBiometricAuthManager()
        val stepUp = StepUpAuth(bio = fakeBio, clock = { fakeTime })
        val dummyActivity = createDummyActivity()

        var firstOk = false
        stepUp.require(dummyActivity, "Title 1", onOk = { firstOk = true }, onFail = {})
        assertTrue("First auth should succeed", firstOk)
        assertEquals("First auth should trigger prompt", 1, fakeBio.promptCount)

        // Advance time by 30 seconds (within 60s window)
        fakeTime += 30_000L

        var secondOk = false
        stepUp.require(dummyActivity, "Title 2", windowMs = 60_000L, onOk = { secondOk = true }, onFail = {})
        assertTrue("Second call within window should succeed without prompt", secondOk)
        assertEquals("Prompt count must remain 1 (skipped)", 1, fakeBio.promptCount)
    }

    @Test
    fun expiredWindowPrompts() {
        var fakeTime = 100_000L
        val fakeBio = FakeBiometricAuthManager()
        val stepUp = StepUpAuth(bio = fakeBio, clock = { fakeTime })
        val dummyActivity = createDummyActivity()

        var firstOk = false
        stepUp.require(dummyActivity, "Title 1", onOk = { firstOk = true }, onFail = {})
        assertTrue(firstOk)
        assertEquals(1, fakeBio.promptCount)

        // Advance time by 61 seconds (past 60s window)
        fakeTime += 61_000L

        var secondOk = false
        stepUp.require(dummyActivity, "Title 2", windowMs = 60_000L, onOk = { secondOk = true }, onFail = {})
        assertTrue(secondOk)
        assertEquals("Expired window must trigger prompt again", 2, fakeBio.promptCount)
    }

    @Test
    fun resetForcesPrompt() {
        var fakeTime = 100_000L
        val fakeBio = FakeBiometricAuthManager()
        val stepUp = StepUpAuth(bio = fakeBio, clock = { fakeTime })
        val dummyActivity = createDummyActivity()

        stepUp.require(dummyActivity, "Title 1", onOk = {}, onFail = {})
        assertEquals(1, fakeBio.promptCount)

        // Reset forces prompt even within the same millisecond
        stepUp.reset()

        stepUp.require(dummyActivity, "Title 2", windowMs = 60_000L, onOk = {}, onFail = {})
        assertEquals("After reset(), prompt must be triggered even within window", 2, fakeBio.promptCount)
    }

    @Test
    fun deviceWithoutScreenLockFailsWithActionableMessage() {
        val fakeBio = FakeBiometricAuthManager(canAuth = false)
        val stepUp = StepUpAuth(fakeBio)
        val dummyActivity = createDummyActivity()

        var okCalled = false
        var failureMessage: String? = null
        stepUp.require(
            dummyActivity,
            "Title",
            onOk = { okCalled = true },
            onFail = { msg -> failureMessage = msg }
        )

        assertFalse("Must not succeed if device cannot authenticate", okCalled)
        assertEquals(0, fakeBio.promptCount)
        assertEquals("Set up a screen lock first", failureMessage)
    }

    @Test
    fun verifySettingsScreenProtectsLockToggleWithStepUp() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, "src/main/java/com/alieducational/app/ui/settings/SettingsScreen.kt"),
            File(userDir, "app/src/main/java/com/alieducational/app/ui/settings/SettingsScreen.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("SettingsScreen.kt must exist", file != null && file.exists())
        val text = file!!.readText()
        assertTrue("SettingsScreen must protect turning off lock with stepUp.require", text.contains("stepUp.require"))
    }

    @Test
    fun verifyBackupRestoreScreenProtectsRestoreWithStepUp() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, "src/main/java/com/alieducational/app/ui/more/BackupRestoreScreen.kt"),
            File(userDir, "app/src/main/java/com/alieducational/app/ui/more/BackupRestoreScreen.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("BackupRestoreScreen.kt must exist", file != null && file.exists())
        val text = file!!.readText()
        assertTrue("BackupRestoreScreen must protect restore with stepUp.require", text.contains("stepUp.require"))
    }
}
