package com.alieducational.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class AliEducationalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
        cleanupTempExports()
    }

    /**
     * Delete temporary shared exports, receipts, and camera captures older than 24 hours (Section SEC-06c).
     */
    private fun cleanupTempExports() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        listOf(
            File(cacheDir, "exports"),
            File(cacheDir, "receipts"),
            File(cacheDir, "barcode_exports"),
            File(cacheDir, "camera_temp"),
            getExternalFilesDir("exports")
        ).forEach { dir ->
            if (dir != null && dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoff) {
                        file.delete()
                    }
                }
            }
        }
    }
}
