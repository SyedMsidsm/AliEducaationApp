package com.alieducational.app.sync

import com.alieducational.app.data.service.OwnerSyncService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OwnerSyncServiceTest {

    @Test
    fun testOwnerSyncServiceConstants() {
        assertEquals("sync", OwnerSyncService.CHANNEL_ID)
        assertEquals(2001, OwnerSyncService.NOTIFICATION_ID)
        assertEquals("port", OwnerSyncService.EXTRA_PORT)
    }

    @Test
    fun testManifestDeclaresForegroundServiceAndPermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml").let {
            if (it.exists()) it else File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val text = manifestFile.readText()

        assertTrue("Manifest must declare FOREGROUND_SERVICE", text.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue("Manifest must declare FOREGROUND_SERVICE_CONNECTED_DEVICE", text.contains("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"))
        assertTrue("Manifest must declare POST_NOTIFICATIONS", text.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue("Manifest must declare CHANGE_WIFI_STATE", text.contains("android.permission.CHANGE_WIFI_STATE"))
        assertTrue("Manifest must declare OwnerSyncService", text.contains("OwnerSyncService"))
        assertTrue("Manifest must set foregroundServiceType connectedDevice", text.contains("connectedDevice"))
    }
}
