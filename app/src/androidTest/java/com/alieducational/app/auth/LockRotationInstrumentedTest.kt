package com.alieducational.app.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alieducational.app.MainActivity
import com.alieducational.app.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockRotationInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setup() = runBlocking {
        val prefs = AppPreferences(context)
        prefs.setFirstLaunch(false)
        prefs.setLockEnabled(true)
        prefs.setLockTimeoutSeconds(120)
    }

    @After
    fun cleanup() {
        scenario?.close()
    }

    private fun findText(text: String): android.view.accessibility.AccessibilityNodeInfo? {
        fun walk(node: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.text?.toString() == text || node.contentDescription?.toString() == text) return node
            for (i in 0 until node.childCount) {
                walk(node.getChild(i))?.let { return it }
            }
            return null
        }
        return walk(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
    }

    @Test
    fun rotationWhileUnlockedPreservesUnlockedState() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario?.onActivity { activity ->
            activity.getAppViewModel().isAppLocked = false
        }

        android.os.SystemClock.sleep(500)

        scenario?.recreate()

        android.os.SystemClock.sleep(1000)

        val unlockPrompt = findText("Confirm your fingerprint or device credential")
        assertNull("Unlock prompt must not be displayed after rotation while unlocked", unlockPrompt)
    }
}
