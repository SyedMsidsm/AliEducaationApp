package com.alieducational.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.io.FileNotFoundException
import javax.xml.parsers.DocumentBuilderFactory

class ManifestAndFileProviderSecurityTest {

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

    private fun parseXml(file: File): Element {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(file).documentElement
    }

    @Test
    fun testUsesCleartextTrafficIsDisabled_SEC08() {
        val manifestFile = locateFile("src/main/AndroidManifest.xml")
        val manifestXml = parseXml(manifestFile)
        val appNodeList = manifestXml.getElementsByTagName("application")
        assertTrue("Manifest must have <application> tag", appNodeList.length > 0)
        val appElement = appNodeList.item(0) as Element

        val usesCleartext = appElement.getAttributeNS("http://schemas.android.com/apk/res/android", "usesCleartextTraffic")
        assertEquals(
            "usesCleartextTraffic must be set to 'false' for SEC-08 hygiene",
            "false",
            usesCleartext
        )
    }

    @Test
    fun testChangeWifiMulticastStatePermissionRemoved_SEC08() {
        val manifestFile = locateFile("src/main/AndroidManifest.xml")
        val manifestXml = parseXml(manifestFile)
        val permissions = manifestXml.getElementsByTagName("uses-permission")

        var hasMulticast = false
        for (i in 0 until permissions.length) {
            val element = permissions.item(i) as Element
            val name = element.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
            if (name == "android.permission.CHANGE_WIFI_MULTICAST_STATE") {
                hasMulticast = true
                break
            }
        }
        assertFalse(
            "android.permission.CHANGE_WIFI_MULTICAST_STATE must be removed as it is unused",
            hasMulticast
        )
    }

    @Test
    fun testFilePathsContainsNoDotPathsOrExternalPath_SEC08() {
        val filePathsFile = locateFile("src/main/res/xml/file_paths.xml")
        val filePathsXml = parseXml(filePathsFile)

        val childNodes = filePathsXml.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element) {
                // Assert no element exposes arbitrary external root
                assertFalse(
                    "external-path exposes entire external storage and must not be present in file_paths.xml: <${node.tagName}>",
                    node.tagName == "external-path"
                )

                val pathAttr = node.getAttribute("path")
                assertFalse(
                    "file_paths.xml must not expose root folders via '.' or empty path in <${node.tagName} path=\"$pathAttr\">",
                    pathAttr == "." || pathAttr.isEmpty()
                )
            }
        }
    }

    @Test
    fun testFilePathsExposesOnlyRequiredSubfolders_SEC08() {
        val filePathsFile = locateFile("src/main/res/xml/file_paths.xml")
        val filePathsXml = parseXml(filePathsFile)

        val paths = mutableListOf<Pair<String, String>>()
        val childNodes = filePathsXml.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node is Element) {
                paths.add(node.tagName to node.getAttribute("path"))
            }
        }

        // Must configure isolated subdirectories for receipts, exports, camera, backups
        assertTrue(
            "Must configure receipts folder in file_paths",
            paths.any { it.second.startsWith("receipts") }
        )
        assertTrue(
            "Must configure exports folder in file_paths",
            paths.any { it.second.startsWith("exports") }
        )
        assertTrue(
            "Must configure camera_temp folder in file_paths",
            paths.any { it.second.startsWith("camera_temp") }
        )
        assertTrue(
            "Must configure backups folder in file_paths",
            paths.any { it.second.startsWith("backups") }
        )
    }

    @Test
    fun testWidgetActionValidation_SEC08() {
        // Assert that only ACTION_WIDGET_CLICK is considered valid
        assertTrue(
            com.alieducational.app.MainActivity.isValidWidgetClick(
                "com.alieducational.app.action.WIDGET_CLICK",
                "aliedu"
            )
        )
        // Missing scheme from internal intent is allowed
        assertTrue(
            com.alieducational.app.MainActivity.isValidWidgetClick(
                "com.alieducational.app.action.WIDGET_CLICK",
                null
            )
        )
        // External intent with invalid action must be rejected
        assertFalse(
            com.alieducational.app.MainActivity.isValidWidgetClick(
                "android.intent.action.VIEW",
                "aliedu"
            )
        )
        assertFalse(
            com.alieducational.app.MainActivity.isValidWidgetClick(
                null,
                "aliedu"
            )
        )
        // Intent with unexpected scheme must be rejected
        assertFalse(
            com.alieducational.app.MainActivity.isValidWidgetClick(
                "com.alieducational.app.action.WIDGET_CLICK",
                "http"
            )
        )
    }
}
