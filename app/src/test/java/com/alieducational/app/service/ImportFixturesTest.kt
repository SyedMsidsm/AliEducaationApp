package com.alieducational.app.service

import com.alieducational.app.data.service.ColumnMapping
import com.alieducational.app.data.service.ImportExportService
import com.alieducational.app.data.service.ImportMode
import com.alieducational.app.data.service.RowStatus
import com.alieducational.app.data.service.Severity
import com.alieducational.app.data.service.`import`.AutoMapper
import com.alieducational.app.data.service.`import`.CellReader
import com.alieducational.app.data.service.`import`.CsvReader
import com.alieducational.app.data.service.`import`.ImportParsers
import com.alieducational.app.data.service.`import`.Parsed
import kotlinx.coroutines.runBlocking
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ImportFixturesTest {

    private val importService = ImportExportService()

    @Test
    fun testFormulaEvaluationInExcel() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("FormulaTest")

        // Row 0: Headers
        val r0 = sheet.createRow(0)
        r0.createCell(0).setCellValue("Qty")
        r0.createCell(1).setCellValue("Rate")
        r0.createCell(2).setCellValue("Total")

        // Row 1: Data with formula =A2*B2
        val r1 = sheet.createRow(1)
        r1.createCell(0).setCellValue(5.0)
        r1.createCell(1).setCellValue(20.0)
        val formulaCell = r1.createCell(2)
        formulaCell.cellFormula = "A2*B2"

        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val formatter = DataFormatter()

        val readValues = CellReader.readRow(r1, evaluator, formatter)
        assertEquals(3, readValues.size)
        assertEquals("5", readValues[0])
        assertEquals("20", readValues[1])
        assertEquals("100", readValues[2])

        workbook.close()
    }

    @Test
    fun testPriceParsingVariousFormats() {
        // Valid Indian currency formats
        val p1 = ImportParsers.parseMoney("₹ 1,20,000")
        assertTrue(p1 is Parsed.Ok && (p1 as Parsed.Ok).v == 12_000_000L)

        val p2 = ImportParsers.parseMoney("12.50")
        assertTrue(p2 is Parsed.Ok && (p2 as Parsed.Ok).v == 1250L)

        val p3 = ImportParsers.parseMoney(".50")
        assertTrue(p3 is Parsed.Ok && (p3 as Parsed.Ok).v == 50L)

        val p4 = ImportParsers.parseMoney("12 /-")
        assertTrue(p4 is Parsed.Ok && (p4 as Parsed.Ok).v == 1200L)

        val p5 = ImportParsers.parseMoney("Rs. 450")
        assertTrue(p5 is Parsed.Ok && (p5 as Parsed.Ok).v == 45000L)

        // Invalid formats
        val pErr1 = ImportParsers.parseMoney("N/A")
        assertTrue(pErr1 is Parsed.Err)

        val pErr2 = ImportParsers.parseMoney("-5")
        assertTrue(pErr2 is Parsed.Err)
    }

    @Test
    fun testQuantityParsingFormats() {
        val q1 = ImportParsers.parseQty("12 pcs")
        assertTrue(q1 is Parsed.Ok && (q1 as Parsed.Ok).v == 12)

        val q2 = ImportParsers.parseQty("1,200")
        assertTrue(q2 is Parsed.Ok && (q2 as Parsed.Ok).v == 1200)

        val q3 = ImportParsers.parseQty("12.0")
        assertTrue(q3 is Parsed.Ok && (q3 as Parsed.Ok).v == 12)

        val qErr = ImportParsers.parseQty("abc")
        assertTrue(qErr is Parsed.Err)
    }

    @Test
    fun testCsvReaderBOMAndSemicolonDialects() {
        // UTF-8 BOM
        val bomCsv = "\uFEFFProduct Name,Category,Price\nBook,Stationery,50.00"
        val rowsBom = CsvReader.read(ByteArrayInputStream(bomCsv.toByteArray(Charsets.UTF_8)))
        assertEquals(2, rowsBom.size)
        assertEquals("Product Name", rowsBom[0][0]) // BOM stripped cleanly

        // Semicolon delimited
        val semiCsv = "Item;Category;Price\nPen;Office;10.00"
        val rowsSemi = CsvReader.read(ByteArrayInputStream(semiCsv.toByteArray()))
        assertEquals(2, rowsSemi.size)
        assertEquals(3, rowsSemi[0].size)
        assertEquals("Item", rowsSemi[0][0])
        assertEquals("Price", rowsSemi[0][2])

        // Quoted field with embedded newline
        val multilineCsv = "\"Classmate\nNotebook\",Stationery,60.00\nPencil,Art,5.00"
        val rowsMulti = CsvReader.read(ByteArrayInputStream(multilineCsv.toByteArray()))
        assertEquals(2, rowsMulti.size)
        assertEquals("Classmate\nNotebook", rowsMulti[0][0])
    }

    @Test
    fun testFormulaInjectionSanitization() {
        assertEquals("'=HYPERLINK(\"evil\")", importService.sanitizeForSpreadsheet("=HYPERLINK(\"evil\")"))
        assertEquals("'+CMD", importService.sanitizeForSpreadsheet("+CMD"))
        assertEquals("'-2+3*cmd", importService.sanitizeForSpreadsheet("-2+3*cmd"))
        assertEquals("'@SUM(A1:A10)", importService.sanitizeForSpreadsheet("@SUM(A1:A10)"))
        assertEquals("Regular Name", importService.sanitizeForSpreadsheet("Regular Name"))
    }

    @Test
    fun testAutoMapperSuggestionsAndHash() {
        val headers = listOf("Item Name", "Cat", "C.P.", "S.P.", "Qty in pcs", "EAN")
        val suggestions = AutoMapper.suggest(headers)

        assertTrue(suggestions.containsKey("name"))
        assertEquals(0, suggestions["name"]!!.columnIndex)
        assertTrue(suggestions["name"]!!.confidence >= 90)

        assertTrue(suggestions.containsKey("category"))
        assertEquals(1, suggestions["category"]!!.columnIndex)

        assertTrue(suggestions.containsKey("purchaseCost"))
        assertEquals(2, suggestions["purchaseCost"]!!.columnIndex)

        assertTrue(suggestions.containsKey("sellingPrice"))
        assertEquals(3, suggestions["sellingPrice"]!!.columnIndex)

        assertTrue(suggestions.containsKey("openingQty"))
        assertEquals(4, suggestions["openingQty"]!!.columnIndex)

        assertTrue(suggestions.containsKey("barcode"))
        assertEquals(5, suggestions["barcode"]!!.columnIndex)

        val hash1 = AutoMapper.hashHeaders(headers)
        val hash2 = AutoMapper.hashHeaders(headers)
        assertEquals("Header hash must be deterministic", hash1, hash2)
        assertEquals(16, hash1.length)
    }

    @Test
    fun testStagedPlanInFileDuplicateDetection(): Unit = runBlocking {
        val rows = listOf(
            listOf("Product Name", "Barcode", "Price", "Qty"),
            listOf("Item A", "AE-P11111111", "50", "10"),
            listOf("Item B", "AE-P11111111", "60", "20"), // Duplicate barcode in file
            listOf("", "AE-P22222222", "30", "5") // Empty name
        )

        val mapping = ColumnMapping(
            nameIndex = 0,
            barcodeIndex = 1,
            sellingPricePieceIndex = 2,
            openingQtyIndex = 3,
            headerRowIndex = 0
        )

        val plan = importService.planImport(rows, mapping, ImportMode.ADD_STOCK)
        assertEquals(3, plan.size)

        // Row 1: Item A is valid
        assertEquals(RowStatus.NEW, plan[0].status)
        assertFalse(plan[0].issues.any { it.severity == Severity.ERROR })

        // Row 2: Item B has duplicate barcode error
        assertEquals(RowStatus.ERROR, plan[1].status)
        assertTrue(plan[1].issues.any { it.field == "barcode" && it.severity == Severity.ERROR })

        // Row 3: Empty name error
        assertEquals(RowStatus.ERROR, plan[2].status)
        assertTrue(plan[2].issues.any { it.field == "name" && it.severity == Severity.ERROR })
    }
}
