package com.alieducational.app.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.alieducational.app.domain.barcode.ShopBarcode
import com.alieducational.app.domain.calculation.CostCalculator
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class LabelItem(
    val productName: String,
    val barcode: String,
    val unitLabel: String = "PIECE",
    val pricePaise: Long? = null,
    val copies: Int = 1
)

enum class LabelPreset(
    val title: String,
    val pageWidthPt: Int,
    val pageHeightPt: Int,
    val cols: Int,
    val rows: Int,
    val labelWidthPt: Float,
    val labelHeightPt: Float
) {
    A4_3X8("A4 (3 × 8)", 595, 842, 3, 8, 180f, 95f),
    A4_4X10("A4 (4 × 10)", 595, 842, 4, 10, 135f, 75f),
    THERMAL_50X25("Thermal 50 × 25 mm", 142, 71, 1, 1, 136f, 65f),
    THERMAL_38X25("Thermal 38 × 25 mm", 108, 71, 1, 1, 102f, 65f)
}

@Singleton
class BarcodeGeneratorService @Inject constructor() {

    /**
     * Backward-compatible payload generator for legacy calls.
     * New codes should use ShopBarcode.newPayload(...).
     */
    fun createBarcodePayload(productId: String, unitType: String, packQty: Int): String {
        return ShopBarcode.newPayload(unitType, packQty)
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
     * Single-product label sheet generator for backward compatibility.
     */
    fun generateLabelsPdf(
        context: Context,
        productName: String,
        barcode: String,
        unitLabel: String,
        pricePaise: Long?,
        copies: Int
    ): File {
        return generateMultiLabelsPdf(
            context = context,
            items = listOf(LabelItem(productName, barcode, unitLabel, pricePaise, copies)),
            preset = LabelPreset.A4_3X8
        )
    }

    /**
     * Multi-product label sheet generator supporting multiple presets and vector bar drawing.
     */
    fun generateMultiLabelsPdf(
        context: Context,
        items: List<LabelItem>,
        preset: LabelPreset = LabelPreset.A4_3X8
    ): File {
        val allLabels = items.flatMap { item ->
            val safeCopies = item.copies.coerceIn(1, 500)
            List(safeCopies) { item }
        }

        if (allLabels.isEmpty()) {
            throw IllegalArgumentException("No label items provided")
        }

        val pdfDocument = PdfDocument()
        val cols = preset.cols
        val rows = preset.rows
        val labelsPerPage = cols * rows

        val marginX = (preset.pageWidthPt - (cols * preset.labelWidthPt)) / 2f
        val marginY = (preset.pageHeightPt - (rows * preset.labelHeightPt)) / 2f

        val totalPages = (allLabels.size + labelsPerPage - 1) / labelsPerPage
        var labelIndex = 0

        val writer = MultiFormatWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 0)

        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false // crisp vector edges
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
        }

        val pricePaint = Paint().apply {
            color = Color.BLACK
            isFakeBoldText = true
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        for (pageIdx in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(preset.pageWidthPt, preset.pageHeightPt, pageIdx).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (labelIndex >= allLabels.size) break
                    val item = allLabels[labelIndex]

                    val left = marginX + col * preset.labelWidthPt
                    val top = marginY + row * preset.labelHeightPt
                    val right = left + preset.labelWidthPt
                    val bottom = top + preset.labelHeightPt

                    // Cell cut border for multi-cell pages
                    if (preset.cols > 1 || preset.rows > 1) {
                        canvas.drawRect(left, top, right, bottom, borderPaint)
                    }

                    val padX = 6f
                    val availWidth = preset.labelWidthPt - (padX * 2f)

                    // 1. Header: Shop Name + Product Name
                    val isSmall = preset == LabelPreset.A4_4X10 || preset.pageHeightPt < 100
                    val shopTextSize = if (isSmall) 6.5f else 7.5f
                    metaPaint.textSize = shopTextSize
                    canvas.drawText("Ali Educational", left + padX, top + shopTextSize + 2f, metaPaint)

                    var titleTextSize = if (isSmall) 8f else 9.5f
                    titlePaint.textSize = titleTextSize
                    while (titlePaint.measureText(item.productName) > availWidth && titleTextSize > 5.5f) {
                        titleTextSize -= 0.5f
                        titlePaint.textSize = titleTextSize
                    }
                    val displayTitle = if (titlePaint.measureText(item.productName) > availWidth) {
                        var trunc = item.productName
                        while (trunc.length > 3 && titlePaint.measureText("$trunc...") > availWidth) {
                            trunc = trunc.dropLast(1)
                        }
                        "$trunc..."
                    } else {
                        item.productName
                    }
                    val titleY = top + shopTextSize + 2f + titleTextSize + 2f
                    canvas.drawText(displayTitle, left + padX, titleY, titlePaint)

                    // 2. Vector Barcode Bars
                    val barcodeTop = titleY + 3f
                    val barcodeHeight = if (isSmall) 22f else 32f

                    try {
                        val bitMatrix = writer.encode(item.barcode, BarcodeFormat.CODE_128, 0, 1, hints)
                        val numModules = bitMatrix.width
                        // 10 modules quiet zone each side => total modules = numModules + 20
                        val totalModules = numModules + 20
                        val moduleWidth = availWidth / totalModules.toFloat()
                        val drawnBarcodeWidth = numModules * moduleWidth
                        val barcodeLeft = left + padX + ((availWidth - drawnBarcodeWidth) / 2f)

                        for (x in 0 until numModules) {
                            if (bitMatrix.get(x, 0)) {
                                val barLeft = barcodeLeft + x * moduleWidth
                                val barRight = barcodeLeft + (x + 1) * moduleWidth
                                canvas.drawRect(barLeft, barcodeTop, barRight, barcodeTop + barcodeHeight, barPaint)
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback text if encoding fails
                        metaPaint.textSize = 8f
                        canvas.drawText(item.barcode, left + padX, barcodeTop + 14f, metaPaint)
                    }

                    // 3. Human-readable Barcode & Unit
                    val codeY = barcodeTop + barcodeHeight + (if (isSmall) 8f else 10f)
                    var codeTextSize = if (isSmall) 7f else 8f
                    metaPaint.textSize = codeTextSize
                    while (metaPaint.measureText(item.barcode) > availWidth && codeTextSize > 5f) {
                        codeTextSize -= 0.5f
                        metaPaint.textSize = codeTextSize
                    }
                    canvas.drawText(item.barcode, left + padX, codeY, metaPaint)

                    val unitY = codeY + (if (isSmall) 7f else 9f)
                    if (unitY < bottom - 2f) {
                        val unitStr = if (item.unitLabel.isNotBlank()) item.unitLabel.uppercase() else "PIECE"
                        metaPaint.textSize = if (isSmall) 6f else 7f
                        canvas.drawText(unitStr, left + padX, unitY, metaPaint)
                    }

                    // 4. Selling Price
                    if (item.pricePaise != null && item.pricePaise > 0L) {
                        val priceText = CostCalculator.formatPaiseToRupees(item.pricePaise)
                        var priceSize = if (isSmall) 8.5f else 10f
                        pricePaint.textSize = priceSize
                        val priceWidth = pricePaint.measureText(priceText)
                        canvas.drawText(priceText, right - padX - priceWidth, unitY.coerceAtMost(bottom - 3f), pricePaint)
                    }

                    labelIndex++
                }
            }

            pdfDocument.finishPage(page)
        }

        val exportDir = File(context.cacheDir, "barcode_exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val outputFile = File(exportDir, "Labels_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }
}
