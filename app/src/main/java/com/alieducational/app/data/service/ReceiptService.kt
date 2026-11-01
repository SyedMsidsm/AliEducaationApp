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
        sb.append("------------------------------\n")
        sb.append("Receipt: ${sale.receiptNumber}\n")
        sb.append("Date: $dateStr\n")
        if (!sale.customerName.isNullOrBlank()) {
            sb.append("Customer: ${sale.customerName}\n")
        }
        sb.append("------------------------------\n")
        lines.forEach { line ->
            val unit = if (line.isPack) "pack" else "pc"
            val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)
            sb.append("• ${line.productName}: ${line.quantityBase} $unit @ ${CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)} = $lineTotal\n")
        }
        sb.append("------------------------------\n")
        sb.append("Subtotal: ${CostCalculator.formatPaiseToRupees(sale.grossTotalPaise)}\n")
        if (sale.discountValuePaise > 0) {
            sb.append("Discount: -${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)} (${String.format(Locale.ROOT, "%.1f", sale.discountPercent)}%)\n")
        }
        sb.append("Total: ${CostCalculator.formatPaiseToRupees(sale.netTotalPaise)}\n")
        sb.append("Paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}\n")
        if (sale.changeGivenPaise > 0) {
            sb.append("Change Given: ${CostCalculator.formatPaiseToRupees(sale.changeGivenPaise)}\n")
        }
        if (sale.remainingCreditPaise > 0) {
            sb.append("Balance Due: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}\n")
        }
        sb.append("Payment Mode: ${sale.paymentMethod}\n")
        if (sale.isVoided) {
            sb.append("STATUS: CANCELLED & REFUNDED\n")
        }
        sb.append("------------------------------\n")
        sb.append("Thank you for your visit!\n")

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
    /**
     * Creates a high-quality branded PDF receipt document on white background.
     * Features:
     * - Approved monochrome Ali Educational logo
     * - English headers and layout with Unicode Urdu shaping for product names
     * - Itemized table, subtotal, discount, final total, tender/change, remaining balance
     * - Clean, balanced margins with no GST statement and no customer retention footer
     */
    suspend fun createReceiptPdf(sale: SaleEntity, lines: List<SaleLineEntity>): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val smallPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            isAntiAlias = true
        }

        // Helper to count wrapped lines for text within maxW
        fun countWrappedLines(text: String, paint: Paint, maxW: Float): Int {
            if (paint.measureText(text) <= maxW) return 1
            val words = text.split(" ")
            var linesCount = 0
            var cur = ""
            words.forEach { word ->
                val test = if (cur.isEmpty()) word else "$cur $word"
                if (paint.measureText(test) <= maxW) {
                    cur = test
                } else {
                    linesCount++
                    cur = word
                }
            }
            if (cur.isNotEmpty()) linesCount++
            return maxOf(1, linesCount)
        }

        // Measure exact content height dynamically to eliminate any bottom white void
        var estimatedH = 16f // top margin
        estimatedH += 46f // logo + spacing
        estimatedH += 16f + 13f + 13f + 14f // shop name, subtitle, retail receipt, divider
        estimatedH += 15f + 16f + 12f // metadata row 1, row 2, divider
        if (sale.isVoided) estimatedH += 28f
        estimatedH += 14f + 6f + 12f // table headers + subline
        lines.forEach { line ->
            val desc = line.productName.ifBlank { "Item" }
            val wrapped = countWrappedLines(desc, textPaint, 160f)
            estimatedH += (wrapped * 13f) + 4f
        }
        estimatedH += 12f // divider after items
        estimatedH += 14f // subtotal
        if (sale.discountValuePaise > 0) estimatedH += 14f
        estimatedH += 4f + 17f + 4f // divider + total payable + divider
        estimatedH += 14f // amount paid
        if (sale.changeGivenPaise > 0) estimatedH += 14f
        if (sale.remainingCreditPaise > 0) estimatedH += 14f
        estimatedH += 14f // payment mode
        estimatedH += 14f // divider before footer
        estimatedH += 16f // thank you
        estimatedH += 18f // bottom margin

        val pageWidth = 380
        val pageHeight = maxOf(260, estimatedH.toInt())

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background: pure white
        canvas.drawColor(Color.WHITE)

        var y = 16f

        // 1. Draw approved monochrome Ali logo
        try {
            val logoBitmap = context?.let { BitmapFactory.decodeResource(it.resources, R.drawable.ali_logo_receipt_mono) }
            if (logoBitmap != null) {
                val targetW = 75
                val targetH = (targetW * logoBitmap.height) / logoBitmap.width
                val logoX = (pageWidth - targetW) / 2f
                val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val dstRect = Rect(logoX.toInt(), y.toInt(), (logoX + targetW).toInt(), (y + targetH).toInt())
                canvas.drawBitmap(logoBitmap, srcRect, dstRect, textPaint)
                y += targetH + 8f
            }
        } catch (_: Exception) {
            y += 8f
        }

        // 2. Header Shop Name & Subtitle (Clean & Compact)
        val titleText = "ALI EDUCATIONAL"
        val titleWidth = titlePaint.measureText(titleText)
        canvas.drawText(titleText, (pageWidth - titleWidth) / 2f, y, titlePaint)
        y += 15f

        val subtitle = "Stationery & Educational Supplies"
        val subtitleW = smallPaint.measureText(subtitle)
        canvas.drawText(subtitle, (pageWidth - subtitleW) / 2f, y, smallPaint)
        y += 13f

        val addressText = "Ordinary Retail Receipt"
        val addressW = smallPaint.measureText(addressText)
        canvas.drawText(addressText, (pageWidth - addressW) / 2f, y, smallPaint)
        y += 14f

        // Separator
        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 14f

        // 3. Receipt Details (Receipt No on left, Date right-aligned)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val dateLabel = "Date: $dateStr"
        val dateW = textPaint.measureText(dateLabel)

        canvas.drawText("Receipt No: ${sale.receiptNumber}", 16f, y, boldPaint)
        canvas.drawText(dateLabel, pageWidth - 16f - dateW, y, textPaint)
        y += 15f

        // Customer Name on left, Sale Tag right-aligned
        val customerName = if (!sale.customerName.isNullOrBlank()) sale.customerName else "Walk-in Customer"
        canvas.drawText("Customer: $customerName", 16f, y, textPaint)

        val memoTag = when {
            sale.isVoided -> "VOIDED / REFUNDED"
            sale.paymentMethod == "CREDIT" -> "CREDIT SALE"
            sale.paymentMethod == "UPI" -> "UPI PAYMENT"
            else -> "CASH SALE"
        }
        val tagW = boldPaint.measureText(memoTag)
        canvas.drawText(memoTag, pageWidth - 16f - tagW, y, boldPaint)
        y += 16f

        // Voided Alert Banner
        if (sale.isVoided) {
            val voidPaint = Paint().apply {
                color = 0xFFD32F2F.toInt()
                isFakeBoldText = true
                textSize = 10f
                isAntiAlias = true
            }
            canvas.drawText("CANCELLED & REFUNDED: Refund ₹${CostCalculator.formatPaiseToRupees(sale.refundPayoutPaise)}", 16f, y, voidPaint)
            y += 14f
            if (!sale.voidReason.isNullOrBlank()) {
                canvas.drawText("Reason: ${sale.voidReason}", 16f, y, smallPaint)
                y += 14f
            }
        }

        // 4. Item Table Headers
        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 13f

        canvas.drawText("Item / Particulars", 16f, y, boldPaint)
        val qtyHdr = "Qty"
        canvas.drawText(qtyHdr, 215f - boldPaint.measureText(qtyHdr) / 2f, y, boldPaint)
        val rateHdr = "Rate"
        canvas.drawText(rateHdr, 290f - boldPaint.measureText(rateHdr), y, boldPaint)
        val amtHdr = "Amount"
        canvas.drawText(amtHdr, pageWidth - 16f - boldPaint.measureText(amtHdr), y, boldPaint)
        y += 7f

        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 15f

        // 5. Item Rows (Description, Qty, Rate, and Amount on EXACT SAME baseline)
        lines.forEach { line ->
            val displayName = line.productName.ifBlank { "Item" }
            val unitStr = line.unitLabel ?: if (line.isPack) "pk" else "pc"
            val qtyStr = "${line.quantityBase} $unitStr"
            val rateStr = CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)
            val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)

            val lineTotalW = textPaint.measureText(lineTotal)
            val rateW = smallPaint.measureText(rateStr)
            val qtyW = smallPaint.measureText(qtyStr)

            val maxDescW = 160f
            if (textPaint.measureText(displayName) > maxDescW) {
                val words = displayName.split(" ")
                var currentLine = ""
                var isFirst = true
                words.forEach { word ->
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (textPaint.measureText(testLine) <= maxDescW) {
                        currentLine = testLine
                    } else {
                        canvas.drawText(currentLine, 16f, y, textPaint)
                        if (isFirst) {
                            canvas.drawText(qtyStr, 215f - qtyW / 2f, y, smallPaint)
                            canvas.drawText(rateStr, 290f - rateW, y, smallPaint)
                            canvas.drawText(lineTotal, pageWidth - 16f - lineTotalW, y, textPaint)
                            isFirst = false
                        }
                        y += 13f
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 16f, y, textPaint)
                    if (isFirst) {
                        canvas.drawText(qtyStr, 215f - qtyW / 2f, y, smallPaint)
                        canvas.drawText(rateStr, 290f - rateW, y, smallPaint)
                        canvas.drawText(lineTotal, pageWidth - 16f - lineTotalW, y, textPaint)
                    }
                    y += 15f
                } else {
                    y += 4f
                }
            } else {
                canvas.drawText(displayName, 16f, y, textPaint)
                canvas.drawText(qtyStr, 215f - qtyW / 2f, y, smallPaint)
                canvas.drawText(rateStr, 290f - rateW, y, smallPaint)
                canvas.drawText(lineTotal, pageWidth - 16f - lineTotalW, y, textPaint)
                y += 17f
            }
        }

        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 15f

        // 6. Totals & Payment Summary (Clean right-aligned block)
        val summaryX = 165f
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false, isLarge: Boolean = false) {
            val p = if (isBold) boldPaint else textPaint
            if (isLarge) p.textSize = 12f else p.textSize = 10f
            canvas.drawText(label, summaryX, y, p)
            val valW = p.measureText(value)
            canvas.drawText(value, pageWidth - 16f - valW, y, p)
            y += if (isLarge) 17f else 14f
        }

        drawSummaryRow("Subtotal:", CostCalculator.formatPaiseToRupees(sale.grossTotalPaise))

        if (sale.discountValuePaise > 0) {
            val discountText = "-${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}"
            val pctLabel = if (sale.discountPercent > 0.0) "Discount (${String.format(Locale.ROOT, "%.1f", sale.discountPercent)}%):" else "Discount:"
            drawSummaryRow(pctLabel, discountText)
        }

        canvas.drawLine(summaryX, y - 4f, pageWidth - 16f, y - 4f, linePaint)
        y += 4f

        drawSummaryRow("Total Payable:", CostCalculator.formatPaiseToRupees(sale.netTotalPaise), isBold = true, isLarge = true)

        canvas.drawLine(summaryX, y - 4f, pageWidth - 16f, y - 4f, linePaint)
        y += 4f

        drawSummaryRow("Amount Paid:", CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise))

        if (sale.changeGivenPaise > 0) {
            drawSummaryRow("Change Returned:", CostCalculator.formatPaiseToRupees(sale.changeGivenPaise))
        }

        if (sale.remainingCreditPaise > 0) {
            drawSummaryRow("Balance Due (Credit):", CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise), isBold = true)
        }

        drawSummaryRow("Payment Mode:", sale.paymentMethod)

        // 7. Footer (Clean, compact, no customer retention note)
        y += 6f
        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 16f

        val thanksText = "Thank you for visiting Ali Educational!"
        val thanksW = boldPaint.measureText(thanksText)
        canvas.drawText(thanksText, (pageWidth - thanksW) / 2f, y, boldPaint)

        document.finishPage(page)

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
