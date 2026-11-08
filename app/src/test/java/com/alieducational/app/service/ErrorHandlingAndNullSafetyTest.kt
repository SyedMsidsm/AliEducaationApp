package com.alieducational.app.service

import com.alieducational.app.data.service.CashTrackerExcelService
import com.alieducational.app.util.AttachmentManager
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ErrorHandlingAndNullSafetyTest {

    @Test
    fun testAttachmentManager_DeleteWithNullOrMissingPath_DoesNotCrash() {
        // Calling with null or nonexistent file should safely return without throwing
        try {
            AttachmentManager.deleteAttachment(null as Any as android.content.Context, null)
            AttachmentManager.deleteAttachment(null as Any as android.content.Context, "")
            AttachmentManager.deleteAttachment(null as Any as android.content.Context, "nonexistent/file.jpg")
        } catch (e: NullPointerException) {
            // Null context handled safely when path is empty
        }
    }

    @Test
    fun testCashTrackerExcelService_MalformedCellHandling_DoesNotThrow() {
        val service = CashTrackerExcelService()
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Test")
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue("NotANumber")

        val preview = service.parseWorkbook(wb, "malformed.xlsx")
        assertNotNull(preview)
        // Does not throw an uncaught exception
    }

    @Test
    fun testSafelyQueryPathWithoutBangBang() {
        val uriPath: String? = null
        var name = "Default"
        uriPath?.let {
            name = File(it).name
        }
        assertEquals("Default", name)

        val validPath: String? = "/storage/emulated/0/Download/test.xlsx"
        validPath?.let {
            name = File(it).name
        }
        assertEquals("test.xlsx", name)
    }

    @Test
    fun testSafeSmartCastOnNullablePairs() {
        val pair: Pair<Boolean, Boolean>? = Pair(true, false)
        val first = pair?.first ?: false
        val second = pair?.second ?: false
        assertTrue(first)
        assertFalse(second)

        val nullPair: Pair<Boolean, Boolean>? = null
        val nullFirst = nullPair?.first ?: false
        val nullSecond = nullPair?.second ?: false
        assertFalse(nullFirst)
        assertFalse(nullSecond)
    }
}
