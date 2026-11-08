package com.alieducational.app.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alieducational.app.ui.auth.UnlockScreen
import com.alieducational.app.util.BiometricAuthManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockScreenSecurityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenLockedNoNodeWithTextEnterShopDirectlyExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val biometricAuthManager = BiometricAuthManager(context)

        composeTestRule.setContent {
            UnlockScreen(
                biometricAuthManager = biometricAuthManager,
                onUnlocked = {}
            )
        }

        composeTestRule.onNodeWithText("Enter Shop Directly").assertDoesNotExist()
    }
}
