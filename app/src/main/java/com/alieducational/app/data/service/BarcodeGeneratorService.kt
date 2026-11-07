package com.alieducational.app.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.alieducational.app.domain.calculation.CostCalculator
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BarcodeGeneratorService @Inject constructor() {

    /**
     * Generates a stable, unique shop barcode payload following Code 128 standard:
     * Format: AE-P{idPrefix}-PC for piece, AE-P{idPrefix}-PK{packQty} for pack.
     */
    fun createBarcodePayload(productId: String, unitType: String, packQty: Int): String {
        val cleanId = productId.replace("-", "").take(6).uppercase()
        return if (unitType.equals("PACK", ignoreCase = true)) {
            val safePack = if (packQty > 1) packQty else 10
            "AE-P$cleanId-PK$safePack"
        } else {
            "AE-P$cleanId-PC"
        }
    }

    /**
     * Renders a Code 128 linear barcode into an Android Bitmap (100% offline via ZXing).
     */
    fun generateBarcodeBitmap(
        code: String,
        width: Int = 400,
        height: Int = 140
    ): Bitmap {
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(
            code,
            BarcodeFormat.CODE_128,
            width,
            height
        )

        val bmWidth = bitMatrix.width
        val bmHeight = bitMatrix.height
        val bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)

        for (x in 0 until bmWidth) {
            for (y in 0 until bmHeight) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Generates a printable PDF sheet of barcode labels (Section 16).
     * Renders crisp black bars on white with clear quiet zones, product name (Urdu/English),
     * unit label, human-readable barcode text, and optional selling price.
     */
    fun generateLabelsPdf(
        context: Context,
        productName: String,
        barcode: String,
        unitLabel: String,
        pricePaise: Long?,
        copies: Int
    ): File {
        val safeCopies = copies.coerceIn(1, 100)
        val pdfDocument = PdfDocument()

        // Standard A4 dimensions at 72 DPI: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842

        // Grid layout: 3 columns x 8 rows = 24 labels per page
        val cols = 3
        val rows = 8
        val labelsPerPage = cols * rows

        val labelWidth = 180f
        val labelHeight = 95f
        val marginX = (pageWidth - (cols * labelWidth)) / 2f
        val marginY = (pageHeight - (rows * labelHeight)) / 2f

        val totalPages = (safeCopies + labelsPerPage - 1) / labelsPerPage
        var labelsPlaced = 0

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
            isAntiAlias = true
        }

        val pricePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        val barcodeBitmap = generateBarcodeBitmap(barcode, 360, 100)

        for (pageIdx in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (labelsPlaced >= safeCopies) break

                    val left = marginX + col * labelWidth
                    val top = marginY + row * labelHeight
                    val right = left + labelWidth
                    val bottom = top + labelHeight

                    // Subtle border / cut line
                    canvas.drawRect(left, top, right, bottom, borderPaint)

                    // 1. Shop name & Product Name
                    val clippedName = if (productName.length > 24) productName.take(22) + "..." else productName
                    canvas.drawText("Ali Educational", left + 8f, top + 13f, metaPaint)
                    canvas.drawText(clippedName, left + 8f, top + 26f, titlePaint)

                    // 2. Barcode Image (width: 140, height: 36)
                    val barcodeDest = Rect(
                        (left + 8f).toInt(),
                        (top + 30f).toInt(),
                        (left + 172f).toInt(),
                        (top + 68f).toInt()
                    )
                    canvas.drawBitmap(barcodeBitmap, null, barcodeDest, null)

                    // 3. Human readable code & Unit
                    canvas.drawText(barcode, left + 8f, top + 78f, metaPaint)
                    canvas.drawText(unitLabel, left + 8f, top + 88f, metaPaint)

                    // 4. Optional Price
                    if (pricePaise != null && pricePaise > 0L) {
                        val priceText = CostCalculator.formatPaiseToRupees(pricePaise)
                        val textWidth = pricePaint.measureText(priceText)
                        canvas.drawText(priceText, right - textWidth - 8f, top + 88f, pricePaint)
                    }

                    labelsPlaced++
                }
            }

            pdfDocument.finishPage(page)
        }

        val exportDir = File(context.cacheDir, "barcode_exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val outputFile = File(exportDir, "Labels_${barcode}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }
}
