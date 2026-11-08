package com.alieducational.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alieducational.app.ui.components.AliLogo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AliLogoTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aliLogoRendersInLightAndDarkThemes() {
        composeTestRule.setContent {
            AliLogo(isDark = false)
        }
        composeTestRule.onNodeWithContentDescription("Ali Educational logo").assertIsDisplayed()

        composeTestRule.setContent {
            AliLogo(isDark = true)
        }
        composeTestRule.onNodeWithContentDescription("Ali Educational logo").assertIsDisplayed()
    }
}
