package com.alieducational.app.service

import com.alieducational.app.data.service.CashTrackerExcelService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CashTrackerExcelServiceTest {

    private lateinit var service: CashTrackerExcelService

    @Before
    fun setup() {
        service = CashTrackerExcelService()
    }

    @Test
    fun testParseWorkbook_StandardUserFormat() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Cash In Out")

        // Row 0: Headers (Standard format selected by user)
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Date")
        headerRow.createCell(1).setCellValue("Description")
        headerRow.createCell(2).setCellValue("Cash In (Income)")
        headerRow.createCell(3).setCellValue("Cash Out (Expense)")
        headerRow.createCell(4).setCellValue("Payment Mode")

        // Row 1: Income entry
        val r1 = sheet.createRow(1)
        r1.createCell(0).setCellValue("31-Oct-2026")
        r1.createCell(1).setCellValue("Sales Collection")
        r1.createCell(2).setCellValue(14084.0)
        r1.createCell(3).setCellValue(0.0)
        r1.createCell(4).setCellValue("CASH")

        // Row 2: Expense entry
        val r2 = sheet.createRow(2)
        r2.createCell(0).setCellValue("31-Oct-2026")
        r2.createCell(1).setCellValue("Shop Rent")
        r2.createCell(2).setCellValue(0.0)
        r2.createCell(3).setCellValue(2000.0)
        r2.createCell(4).setCellValue("BANK")

        // Row 3: Expense with Currency formatted string
        val r3 = sheet.createRow(3)
        r3.createCell(0).setCellValue("31-Oct-2026")
        r3.createCell(1).setCellValue("Pigmy - 4")
        r3.createCell(2).setCellValue("")
        r3.createCell(3).setCellValue("₹ 1,000.00")
        r3.createCell(4).setCellValue("GPAY")

        // Row 4: Empty row
        val r4 = sheet.createRow(4)
        r4.createCell(0).setCellValue("")

        val preview = service.parseWorkbook(workbook, "test_cash.xlsx")

        assertNull(preview.errorMessage)
        assertEquals(3, preview.items.size)
        assertEquals(1, preview.incomeCount)
        assertEquals(2, preview.expenseCount)
        assertEquals(1408400L, preview.totalIncomePaise) // 14,084.00 * 100
        assertEquals(300000L, preview.totalExpensePaise) // (2,000 + 1,000) * 100

        // Check individual items
        val salesItem = preview.items[0]
        assertEquals("Sales Collection", salesItem.category)
        assertTrue(salesItem.isIncome)
        assertEquals(1408400L, salesItem.amountPaise)
        assertEquals("CASH", salesItem.notes)

        val rentItem = preview.items[1]
        assertEquals("Shop Rent", rentItem.category)
        assertTrue(!rentItem.isIncome)
        assertEquals(200000L, rentItem.amountPaise)
        assertEquals("BANK", rentItem.notes)

        val pigmyItem = preview.items[2]
        assertEquals("Pigmy - 4", pigmyItem.category)
        assertTrue(!pigmyItem.isIncome)
        assertEquals(100000L, pigmyItem.amountPaise)
        assertEquals("GPAY", pigmyItem.notes)

        workbook.close()
    }
}
