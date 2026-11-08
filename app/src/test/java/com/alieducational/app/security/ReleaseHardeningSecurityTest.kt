package com.alieducational.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class ReleaseHardeningSecurityTest {

    private fun locateFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("..", relativePath),
            File("../app", relativePath)
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw FileNotFoundException("Could not locate $relativePath from working dir: ${File(".").absolutePath}")
    }

    @Test
    fun testProguardAssumenosideeffectsConfigured_SEC10() {
        val proguardFile = locateFile("proguard-rules.pro")
        val content = proguardFile.readText()

        assertTrue(
            "proguard-rules.pro must contain -assumenosideeffects rule for android.util.Log",
            content.contains("-assumenosideeffects class android.util.Log")
        )
        assertTrue(
            "proguard-rules.pro must strip Log.d",
            content.contains("public static *** d(")
        )
        assertTrue(
            "proguard-rules.pro must strip Log.v",
            content.contains("public static *** v(")
        )
        assertTrue(
            "proguard-rules.pro must strip Log.i",
            content.contains("public static *** i(")
        )
    }

    @Test
    fun testGradleWrapperPinnedChecksum_SEC10() {
        val wrapperPropsFile = locateFile("gradle/wrapper/gradle-wrapper.properties")
        val content = wrapperPropsFile.readText()

        assertTrue(
            "gradle-wrapper.properties must pin official distributionSha256Sum for Gradle 8.11.1",
            content.contains("distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6")
        )
        assertTrue(
            "gradle-wrapper.properties must set validateDistributionUrl=true",
            content.contains("validateDistributionUrl=true")
        )
    }

    @Test
    fun testKeystorePropertiesIgnoredInGit_SEC10() {
        val gitignoreFile = locateFile(".gitignore")
        val content = gitignoreFile.readText()

        assertTrue(
            ".gitignore must ignore keystore.properties",
            content.contains("keystore.properties")
        )
    }

    @Test
    fun testUiFilesDoNotLeakRawExceptionMessages_SEC10() {
        val uiFiles = listOf(
            locateFile("src/main/java/com/alieducational/app/ui/billing/BillReceiptScreen.kt"),
            locateFile("src/main/java/com/alieducational/app/ui/reports/CashTrackerScreen.kt"),
            locateFile("src/main/java/com/alieducational/app/ui/stock/StockScreen.kt")
        )

        for (file in uiFiles) {
            val content = file.readText()
            assertFalse(
                "${file.name} must not expose raw e.message in user Toast messages",
                content.contains("Toast.makeText(context, \"Failed: \${e.message}\"") ||
                        content.contains("Toast.makeText(context, \"Failed to share: \${e.message}\"") ||
                        content.contains("Toast.makeText(context, \"Failed to share: \${e.localizedMessage}\"") ||
                        content.contains("Error creating PDF: \${e.message}")
            )
        }
    }
}
