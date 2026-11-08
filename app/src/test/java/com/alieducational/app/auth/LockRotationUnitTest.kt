package com.alieducational.app.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LockRotationUnitTest {

    private fun findSourceFile(relativePath: String): File {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, relativePath),
            File(userDir, "app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("File must exist at one of $candidates", file != null && file.exists())
        return file!!
    }

    @Test
    fun mainActivityDoesNotStoreIsAppLockedAsLocalField() {
        val mainActivityFile = findSourceFile("src/main/java/com/alieducational/app/MainActivity.kt")
        val content = mainActivityFile.readText()
        assertFalse(
            "MainActivity must not store isAppLocked as an Activity field (it must be in AliViewModel)",
            content.contains("private var isAppLocked by mutableStateOf") ||
                    content.contains("var isAppLocked by mutableStateOf")
        )
    }

    @Test
    fun mainActivityUsesMonotonicClockForBackgroundTimeout() {
        val mainActivityFile = findSourceFile("src/main/java/com/alieducational/app/MainActivity.kt")
        val content = mainActivityFile.readText()
        assertFalse(
            "MainActivity must not use wall-clock System.currentTimeMillis() for pause/lock timing",
            content.contains("System.currentTimeMillis()")
        )
        assertTrue(
            "MainActivity must use SystemClock.elapsedRealtime() for pause/lock timing",
            content.contains("SystemClock.elapsedRealtime()")
        )
    }

    @Test
    fun mainActivityConfiguresRecentsProtectionInOnCreate() {
        val mainActivityFile = findSourceFile("src/main/java/com/alieducational/app/MainActivity.kt")
        val content = mainActivityFile.readText()
        assertTrue(
            "MainActivity must configure setRecentsScreenshotEnabled or FLAG_SECURE",
            content.contains("setRecentsScreenshotEnabled") && content.contains("FLAG_SECURE")
        )
    }

    @Test
    fun aliViewModelContainsIsAppLockedProperty() {
        val viewModelFile = findSourceFile("src/main/java/com/alieducational/app/ui/AliViewModel.kt")
        val content = viewModelFile.readText()
        assertTrue(
            "AliViewModel must contain `isAppLocked` property to survive rotation",
            content.contains("var isAppLocked") && content.contains("mutableStateOf")
        )
    }
}
