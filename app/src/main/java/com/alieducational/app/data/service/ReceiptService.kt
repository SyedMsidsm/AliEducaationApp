package com.alieducational.app.data.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.domain.calculation.CostCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptService @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    constructor() : this(null)

    /**
     * Generate short accompanying message text for WhatsApp or SMS.
     */
    fun generateReceiptSummaryText(sale: SaleEntity, lines: List<SaleLineEntity>): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val sb = StringBuilder()

        sb.append("Ali Educational\n")
        sb.append("Stationery & Educational Supplies\n")
        sb.append("RECEIPT\n")
        sb.append("------------------------------\n")
        sb.append("Receipt No: ${sale.receiptNumber}\n")
        sb.append("Date: $dateStr\n")
        val customer = if (!sale.customerName.isNullOrBlank()) sale.customerName else "Walk-in Customer"
        sb.append("Customer: $customer\n")
        val paymentMode = when (sale.paymentMethod) {
            "CASH" -> "Cash"
            "UPI" -> "UPI"
            "CREDIT" -> "Credit"
            else -> sale.paymentMethod
        }
        sb.append("Payment: $paymentMode\n")
        sb.append("------------------------------\n")
        lines.forEach { line ->
            val unit = if (!line.unitLabel.isNullOrBlank()) {
                if (line.quantityBase == 1 && line.unitLabel.equals("copies", ignoreCase = true)) "copy"
                else if (line.quantityBase == 1 && line.unitLabel.equals("sheets", ignoreCase = true)) "sheet"
                else line.unitLabel
            } else if (line.isPack) {
                "pcs (pack sale)"
            } else {
                if (line.quantityBase == 1) "pc" else "pcs"
            }
            val lineGross = if (line.lineGrossPaise > 0L) line.lineGrossPaise else line.quantityBase * line.unitSellingPricePaise
            val lineTotal = CostCalculator.formatPaiseToRupees(lineGross)
            sb.append("• ${line.productName}: ${com.alieducational.app.domain.calculation.ReceiptUnits.quantity(line)} @ ${CostCalculator.formatPaiseToRupees(com.alieducational.app.domain.calculation.ReceiptUnits.ratePaise(line))} = $lineTotal\n")
        }
        sb.append("------------------------------\n")
        sb.append("Subtotal: ${CostCalculator.formatPaiseToRupees(sale.grossTotalPaise)}\n")
        if (sale.discountValuePaise > 0) {
            val pctStr = if (sale.discountPercent > 0.0) {
                if (sale.discountPercent % 1.0 == 0.0) String.format(Locale.ROOT, "%.0f%%", sale.discountPercent) else String.format(Locale.ROOT, "%.1f%%", sale.discountPercent)
            } else ""
            val pctLabel = if (pctStr.isNotEmpty()) "Discount ($pctStr):" else "Discount:"
            sb.append("$pctLabel -${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}\n")
        }
        sb.append("TOTAL PAYABLE: ${CostCalculator.formatPaiseToRupees(sale.netTotalPaise)}\n")
        sb.append("Amount paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}\n")
        if (sale.changeGivenPaise > 0) {
            sb.append("Change: ${CostCalculator.formatPaiseToRupees(sale.changeGivenPaise)}\n")
        }
        if (sale.remainingCreditPaise > 0) {
            sb.append("Balance due: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}\n")
        } else {
            sb.append("Balance due: ₹0.00\n")
        }
        val badge = when {
            sale.isVoided -> "VOIDED"
            sale.remainingCreditPaise > 0L && sale.amountReceivedPaise > 0L -> "PARTIAL"
            sale.remainingCreditPaise > 0L -> "DUE"
            else -> "PAID"
        }
        sb.append("Status: $badge\n")
        sb.append("------------------------------\n")
        sb.append("Thank you for visiting Ali Educational!\n")

        return sb.toString()
    }

    /**
     * Backward-compatible helper for legacy text sharing.
     */
    fun generateReceiptText(sale: SaleEntity, lines: List<SaleLineEntity>, isUrdu: Boolean = false): String {
        return generateReceiptSummaryText(sale, lines)
    }

    fun shareReceipt(receiptText: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Receipt")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(chooser)
    }

    private data class ProcessedItem(
        val entity: SaleLineEntity,
        val displayNameLines: List<String>,
        val qtyStr: String,
        val rateStr: String,
        val amountStr: String,
        val height: Float
    )

    /**
     * Creates a high-quality branded PDF receipt document on white background.
     * Features:
     * - Approved monochrome Ali Educational logo with proper vertical spacing
     * - "ALI EDUCATIONAL" and "Stationery & Educational Supplies" centered
     * - Clean "RECEIPT" document title without unwanted GST statements
     * - Consistent 2-column metadata with right-aligned values
     * - Clean tabular items layout with right-aligned amounts and wrapped descriptions
     * - Single discount deduction shown in the totals section
     * - Light grey shaded "TOTAL PAYABLE" banner
     * - Centered status badge: [ PAID ], [ PARTIAL ], [ DUE ], [ VOIDED ]
     * - Responsive text measurement and multi-page pagination for long bills
     * - Snug fit dynamically sized page for short receipts with zero trailing void
     */
    suspend fun createReceiptPdf(sale: SaleEntity, lines: List<SaleLineEntity>): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 17f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 45, 60)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val docTypePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 85, 99)
            textSize = 9.5f
            letterSpacing = 0.18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val metaKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 65, 81)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 213, 219)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 65, 81)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val summaryLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 65, 81)
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val summaryValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(243, 244, 246)
            style = Paint.Style.FILL
        }

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11.5f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13.5f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (sale.isVoided) Color.rgb(220, 38, 38) else Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (sale.isVoided) Color.rgb(220, 38, 38) else Color.BLACK
            textSize = 9.5f
            isFakeBoldText = true
            letterSpacing = 0.2f
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11.5f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (paint.measureText(text) <= maxWidth) return listOf(text)
            val words = text.split(" ")
            val result = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val test = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(test) <= maxWidth) {
                    current = test
                } else {
                    if (current.isNotEmpty()) result.add(current)
                    current = word
                }
            }
            if (current.isNotEmpty()) result.add(current)
            return if (result.isEmpty()) listOf(text) else result
        }

        // Layout constants
        val pageWidth = 380
        val margin = 24f
        val qtyColX = 230f
        val rateColX = 288f
        val rightColX = pageWidth - margin
        val summaryX = 170f

        // Process item lines and measure heights
        val processedItems = lines.map { line ->
            val desc = line.productName.ifBlank { "Item" }
            val wrapped = wrapText(desc, itemTextPaint, 160f)
            val unitStr = if (!line.unitLabel.isNullOrBlank()) {
                if (line.quantityBase == 1 && line.unitLabel.equals("copies", ignoreCase = true)) "copy"
                else if (line.quantityBase == 1 && line.unitLabel.equals("sheets", ignoreCase = true)) "sheet"
                else line.unitLabel
            } else if (line.isPack) {
                "pcs (pack sale)"
            } else {
                if (line.quantityBase == 1) "pc" else "pcs"
            }
            val qtyStr = com.alieducational.app.domain.calculation.ReceiptUnits.quantity(line)
            val rateStr = CostCalculator.formatPaiseToRupees(com.alieducational.app.domain.calculation.ReceiptUnits.ratePaise(line))
            val lineGross = if (line.lineGrossPaise > 0L) line.lineGrossPaise else line.quantityBase * line.unitSellingPricePaise
            val amountStr = CostCalculator.formatPaiseToRupees(lineGross)
            val rowHeight = 18f + (wrapped.size - 1) * 13f
            ProcessedItem(line, wrapped, qtyStr, rateStr, amountStr, rowHeight)
        }

        // Decode logo bitmap
        val logoBitmap = try {
            context?.let { BitmapFactory.decodeResource(it.resources, R.drawable.ali_logo_receipt_mono) }
        } catch (_: Exception) {
            null
        }

        val logoW = 74f
        val logoH = if (logoBitmap != null) (logoW * logoBitmap.height) / logoBitmap.width else 39f

        // Header height
        val fullHeaderHeight = 24f + logoH + 18f + 18f + 16f + 24f + (4 * 16f) + 8f + 16f + 18f + 8f + 16f
        val summaryBlockHeight = 16f + (if (sale.discountValuePaise > 0L) 16f else 0f) + 12f + 28f + 14f + 16f + 16f + 22f + 20f + 16f + 28f
        val totalItemsHeight = processedItems.sumOf { it.height.toDouble() }.toFloat()
        val totalEstimatedHeight = fullHeaderHeight + totalItemsHeight + summaryBlockHeight

        val MAX_SINGLE_PAGE_HEIGHT = 650f

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val customerName = if (!sale.customerName.isNullOrBlank()) sale.customerName else "Walk-in Customer"
        val paymentLabel = when (sale.paymentMethod) {
            "CASH" -> "Cash"
            "UPI" -> "UPI"
            "CREDIT" -> "Credit"
            else -> sale.paymentMethod
        }

        val badgeText = when {
            sale.isVoided -> "V O I D E D"
            sale.remainingCreditPaise > 0L && sale.amountReceivedPaise > 0L -> "P A R T I A L"
            sale.remainingCreditPaise > 0L -> "D U E"
            else -> "P A I D"
        }

        // Draw Helper Functions
        fun drawReceiptHeader(canvas: Canvas): Float {
            var y = 24f
            if (logoBitmap != null) {
                val logoX = (pageWidth - logoW) / 2f
                val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val dstRect = RectF(logoX, y, logoX + logoW, y + logoH)
                canvas.drawBitmap(logoBitmap, srcRect, dstRect, null)
                y += logoH + 18f
            } else {
                y += 12f
            }

            val titleText = "ALI EDUCATIONAL"
            val titleW = titlePaint.measureText(titleText)
            canvas.drawText(titleText, (pageWidth - titleW) / 2f, y, titlePaint)
            y += 18f

            val subtitleText = "Stationery & Educational Supplies"
            val subtitleW = subtitlePaint.measureText(subtitleText)
            canvas.drawText(subtitleText, (pageWidth - subtitleW) / 2f, y, subtitlePaint)
            y += 16f

            val docTypeText = "R E C E I P T"
            val docTypeW = docTypePaint.measureText(docTypeText)
            canvas.drawText(docTypeText, (pageWidth - docTypeW) / 2f, y, docTypePaint)
            y += 24f

            // Metadata: 4 rows
            val metaRows = listOf(
                "Receipt No." to sale.receiptNumber,
                "Date" to dateStr,
                "Customer" to customerName,
                "Payment" to paymentLabel
            )

            for ((k, v) in metaRows) {
                canvas.drawText(k, margin, y, metaKeyPaint)
                val vW = metaValPaint.measureText(v)
                canvas.drawText(v, rightColX - vW, y, metaValPaint)
                y += 16f
            }
            y += 6f

            // Voided Banner if cancelled
            if (sale.isVoided) {
                val voidBg = RectF(margin, y, rightColX, y + 26f)
                val voidBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(254, 226, 226); style = Paint.Style.FILL }
                canvas.drawRoundRect(voidBg, 4f, 4f, voidBgPaint)
                val voidTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 38, 38); textSize = 9.5f; isFakeBoldText = true }
                val vText = "CANCELLED & REFUNDED: Refund ₹${CostCalculator.formatPaiseToRupees(sale.refundPayoutPaise)}"
                canvas.drawText(vText, margin + 8f, y + 17f, voidTextPaint)
                y += 34f
            }

            return y
        }

        fun drawTableHeader(canvas: Canvas, startY: Float): Float {
            var y = startY
            canvas.drawLine(margin, y, rightColX, y, linePaint)
            y += 16f

            canvas.drawText("Item", margin, y, tableHeaderPaint)
            val qtyHdrW = tableHeaderPaint.measureText("Qty")
            canvas.drawText("Qty", qtyColX - qtyHdrW, y, tableHeaderPaint)
            val rateHdrW = tableHeaderPaint.measureText("Rate")
            canvas.drawText("Rate", rateColX - rateHdrW, y, tableHeaderPaint)
            val amtHdrW = tableHeaderPaint.measureText("Amount")
            canvas.drawText("Amount", rightColX - amtHdrW, y, tableHeaderPaint)
            y += 7f

            canvas.drawLine(margin, y, rightColX, y, linePaint)
            y += 17f
            return y
        }

        fun drawItemRow(canvas: Canvas, item: ProcessedItem, startY: Float): Float {
            var y = startY
            val baseBaseline = y
            val firstLine = item.displayNameLines.firstOrNull() ?: ""
            canvas.drawText(firstLine, margin, baseBaseline, itemTextPaint)

            val qtyW = itemSubPaint.measureText(item.qtyStr)
            canvas.drawText(item.qtyStr, qtyColX - qtyW, baseBaseline, itemSubPaint)

            val rateW = itemSubPaint.measureText(item.rateStr)
            canvas.drawText(item.rateStr, rateColX - rateW, baseBaseline, itemSubPaint)

            val amtW = itemTextPaint.measureText(item.amountStr)
            canvas.drawText(item.amountStr, rightColX - amtW, baseBaseline, itemTextPaint)

            if (item.displayNameLines.size > 1) {
                for (i in 1 until item.displayNameLines.size) {
                    y += 13f
                    canvas.drawText(item.displayNameLines[i], margin, y, itemTextPaint)
                }
            }
            y += 18f
            return y
        }

        fun drawTotalsAndFooter(canvas: Canvas, startY: Float) {
            var y = startY
            canvas.drawLine(margin, y, rightColX, y, linePaint)
            y += 18f

            // Subtotal
            canvas.drawText("Subtotal", summaryX, y, summaryLabelPaint)
            val subVal = CostCalculator.formatPaiseToRupees(sale.grossTotalPaise)
            canvas.drawText(subVal, rightColX - summaryValPaint.measureText(subVal), y, summaryValPaint)
            y += 16f

            // Discount
            if (sale.discountValuePaise > 0L) {
                val pctStr = if (sale.discountPercent > 0.0) {
                    val formatted = if (sale.discountPercent % 1.0 == 0.0) {
                        String.format(Locale.ROOT, "%.0f", sale.discountPercent)
                    } else {
                        String.format(Locale.ROOT, "%.1f", sale.discountPercent)
                    }
                    "Discount ($formatted%)"
                } else {
                    "Discount"
                }
                canvas.drawText(pctStr, summaryX, y, summaryLabelPaint)
                val discVal = "-${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}"
                canvas.drawText(discVal, rightColX - summaryValPaint.measureText(discVal), y, summaryValPaint)
                y += 16f
            }

            // TOTAL PAYABLE Shaded Banner
            val bannerH = 28f
            val bannerRect = RectF(margin - 8f, y, rightColX + 8f, y + bannerH)
            canvas.drawRoundRect(bannerRect, 3f, 3f, totalBgPaint)
            val bannerTextY = y + 19f
            canvas.drawText("TOTAL PAYABLE", summaryX, bannerTextY, totalLabelPaint)
            val totalVal = CostCalculator.formatPaiseToRupees(sale.netTotalPaise)
            canvas.drawText(totalVal, rightColX - totalValPaint.measureText(totalVal), bannerTextY, totalValPaint)
            y += bannerH + 16f

            // Amount paid
            canvas.drawText("Amount paid", summaryX, y, summaryLabelPaint)
            val paidVal = CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)
            canvas.drawText(paidVal, rightColX - summaryValPaint.measureText(paidVal), y, summaryValPaint)
            y += 16f

            // Balance due or Change
            if (sale.remainingCreditPaise > 0L) {
                canvas.drawText("Balance due", summaryX, y, summaryLabelPaint)
                val dueVal = CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)
                canvas.drawText(dueVal, rightColX - summaryValPaint.measureText(dueVal), y, summaryValPaint)
                y += 22f
            } else if (sale.changeGivenPaise > 0L) {
                canvas.drawText("Change", summaryX, y, summaryLabelPaint)
                val changeVal = CostCalculator.formatPaiseToRupees(sale.changeGivenPaise)
                canvas.drawText(changeVal, rightColX - summaryValPaint.measureText(changeVal), y, summaryValPaint)
                y += 16f
                canvas.drawText("Balance due", summaryX, y, summaryLabelPaint)
                canvas.drawText("₹0.00", rightColX - summaryValPaint.measureText("₹0.00"), y, summaryValPaint)
                y += 22f
            } else {
                canvas.drawText("Balance due", summaryX, y, summaryLabelPaint)
                canvas.drawText("₹0.00", rightColX - summaryValPaint.measureText("₹0.00"), y, summaryValPaint)
                y += 22f
            }

            // Status Badge [ PAID ] / [ PARTIAL ] / [ DUE ] / [ VOIDED ]
            val bTextW = badgeTextPaint.measureText(badgeText)
            val bBoxW = maxOf(64f, bTextW + 20f)
            val bBoxH = 20f
            val bBoxX = (pageWidth - bBoxW) / 2f
            val bRect = RectF(bBoxX, y, bBoxX + bBoxW, y + bBoxH)
            canvas.drawRoundRect(bRect, 2f, 2f, badgeBorderPaint)
            canvas.drawText(badgeText, bBoxX + (bBoxW - bTextW) / 2f, y + 14f, badgeTextPaint)
            y += bBoxH + 18f

            canvas.drawLine(margin, y, rightColX, y, linePaint)
            y += 22f

            // Footer
            val thanksText = "Thank you for visiting Ali Educational!"
            val thanksW = footerPaint.measureText(thanksText)
            canvas.drawText(thanksText, (pageWidth - thanksW) / 2f, y, footerPaint)
        }

        if (totalEstimatedHeight <= MAX_SINGLE_PAGE_HEIGHT) {
            // SINGLE PAGE RECEIPT (Snug fit, zero blank void)
            val singlePageHeight = maxOf(340, totalEstimatedHeight.toInt())
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, singlePageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var curY = drawReceiptHeader(canvas)
            curY = drawTableHeader(canvas, curY)
            for (item in processedItems) {
                curY = drawItemRow(canvas, item, curY)
            }
            drawTotalsAndFooter(canvas, curY)

            document.finishPage(page)
        } else {
            // MULTI-PAGE RECEIPT (Responsive pagination for long bills)
            val standardPageHeight = 600
            val usableBottom = 540f

            // Partition items across pages
            val pagesItems = mutableListOf<MutableList<ProcessedItem>>()
            var curList = mutableListOf<ProcessedItem>()
            var curPageAvailableY = fullHeaderHeight

            for (item in processedItems) {
                if (curPageAvailableY + item.height > usableBottom && curList.isNotEmpty()) {
                    pagesItems.add(curList)
                    curList = mutableListOf()
                    curPageAvailableY = 90f // Continuation header + table header height
                }
                curList.add(item)
                curPageAvailableY += item.height
            }
            if (curList.isNotEmpty()) {
                pagesItems.add(curList)
            }

            // Determine if summary fits on last page or needs a final page
            val summaryNeedsNewPage = (curPageAvailableY + summaryBlockHeight > usableBottom)
            val totalPages = pagesItems.size + (if (summaryNeedsNewPage) 1 else 0)

            for (pIdx in pagesItems.indices) {
                val pageNum = pIdx + 1
                val pItems = pagesItems[pIdx]
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, standardPageHeight, pageNum).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                var curY: Float
                if (pageNum == 1) {
                    curY = drawReceiptHeader(canvas)
                    curY = drawTableHeader(canvas, curY)
                } else {
                    // Compact continuation header
                    curY = 22f
                    canvas.drawText("ALI EDUCATIONAL", margin, curY, titlePaint)
                    val contMeta = "Receipt #${sale.receiptNumber}  •  Page $pageNum of $totalPages"
                    canvas.drawText(contMeta, rightColX - subtitlePaint.measureText(contMeta), curY, subtitlePaint)
                    curY += 10f
                    curY = drawTableHeader(canvas, curY)
                }

                for (item in pItems) {
                    curY = drawItemRow(canvas, item, curY)
                }

                val isLastItemPage = (pIdx == pagesItems.size - 1)
                if (isLastItemPage && !summaryNeedsNewPage) {
                    drawTotalsAndFooter(canvas, curY)
                } else {
                    // Page footer indicator
                    canvas.drawLine(margin, usableBottom, rightColX, usableBottom, linePaint)
                    val contText = "Page $pageNum of $totalPages (Continued...)"
                    canvas.drawText(contText, (pageWidth - subtitlePaint.measureText(contText)) / 2f, usableBottom + 16f, subtitlePaint)
                }

                document.finishPage(page)
            }

            if (summaryNeedsNewPage) {
                val finalPageNum = totalPages
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, standardPageHeight, finalPageNum).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                var curY = 22f
                canvas.drawText("ALI EDUCATIONAL", margin, curY, titlePaint)
                val contMeta = "Receipt #${sale.receiptNumber}  •  Page $finalPageNum of $totalPages"
                canvas.drawText(contMeta, rightColX - subtitlePaint.measureText(contMeta), curY, subtitlePaint)
                curY += 14f

                drawTotalsAndFooter(canvas, curY)
                document.finishPage(page)
            }
        }

        // Save generated PDF to cacheDir/receipts
        val ctx = context ?: throw IllegalStateException("Context required to generate PDF file on device")
        val receiptsDir = File(ctx.cacheDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()
        val pdfFile = File(receiptsDir, "Receipt_${sale.receiptNumber}.pdf")
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return@withContext pdfFile
    }

    /**
     * Create Android Share Intent for sending the receipt PDF directly to WhatsApp or Android Chooser.
     */
    suspend fun createWhatsAppReceiptIntent(sale: SaleEntity, lines: List<SaleLineEntity>): Intent {
        val ctx = context ?: throw IllegalStateException("Context required to share receipt")
        val pdfFile = createReceiptPdf(sale, lines)
        val uri: Uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            pdfFile
        )
        val summaryText = generateReceiptSummaryText(sale, lines)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, summaryText)
            putExtra(Intent.EXTRA_SUBJECT, "Receipt #${sale.receiptNumber} - Ali Educational")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Try direct WhatsApp package if installed
        val pm = ctx.packageManager
        val isWhatsAppInstalled = isPackageInstalled("com.whatsapp", pm) || isPackageInstalled("com.whatsapp.w4b", pm)
        if (isWhatsAppInstalled) {
            val targetPackage = if (isPackageInstalled("com.whatsapp", pm)) "com.whatsapp" else "com.whatsapp.w4b"
            intent.setPackage(targetPackage)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        // Fallback: standard Android share sheet
        val chooser = Intent.createChooser(intent, "Share Receipt (WhatsApp)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return chooser
    }

    /**
     * Print receipt directly via Android PrintManager.
     */
    fun printReceipt(activity: Activity, sale: SaleEntity, lines: List<SaleLineEntity>) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "AliEducational_Receipt_${sale.receiptNumber}"

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                var pdfFile: File? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder("receipt_${sale.receiptNumber}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()

                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        kotlinx.coroutines.runBlocking {
                            pdfFile = createReceiptPdf(sale, lines)
                        }
                        val input = FileInputStream(pdfFile)
                        val output = FileOutputStream(destination?.fileDescriptor)
                        input.copyTo(output)
                        input.close()
                        output.close()
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            },
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()
        )
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
