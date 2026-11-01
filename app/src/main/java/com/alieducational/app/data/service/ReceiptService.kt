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
     * - Non-GST registered notice
     */
    suspend fun createReceiptPdf(sale: SaleEntity, lines: List<SaleLineEntity>): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        // Page dimensions: 380 pt width (standard POS receipt/mobile width)
        val pageWidth = 380
        val baseHeaderHeight = 165
        val itemRowHeight = 22
        val summaryHeight = 150
        val footerHeight = 65
        val estimatedTotal = baseHeaderHeight + (lines.size * itemRowHeight) + summaryHeight + footerHeight
        val pageHeight = maxOf(380, estimatedTotal)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background: pure white
        canvas.drawColor(Color.WHITE)

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

        var y = 18f

        // 1. Draw approved monochrome Ali logo
        try {
            val logoBitmap = context?.let { BitmapFactory.decodeResource(it.resources, R.drawable.ali_logo_receipt_mono) }
            if (logoBitmap != null) {
                val targetW = 80
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

        // 2. Header Shop Name & Subtitle
        val titleText = "ALI EDUCATIONAL"
        val titleWidth = titlePaint.measureText(titleText)
        canvas.drawText(titleText, (pageWidth - titleWidth) / 2f, y, titlePaint)
        y += 15f

        val subtitle = "Stationery & Educational Supplies"
        val subtitleW = smallPaint.measureText(subtitle)
        canvas.drawText(subtitle, (pageWidth - subtitleW) / 2f, y, smallPaint)
        y += 13f

        val addressText = "Ordinary Retail Receipt • Not GST Registered"
        val addressW = smallPaint.measureText(addressText)
        canvas.drawText(addressText, (pageWidth - addressW) / 2f, y, smallPaint)
        y += 15f

        // Separator
        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 15f

        // 3. Receipt Details (Line 1: Receipt No on left, Date right-aligned on right)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val dateLabel = "Date: $dateStr"
        val dateW = textPaint.measureText(dateLabel)

        canvas.drawText("Receipt No: ${sale.receiptNumber}", 16f, y, boldPaint)
        canvas.drawText(dateLabel, pageWidth - 16f - dateW, y, textPaint)
        y += 15f

        // Line 2: Customer Name on left, Sale Tag right-aligned on right
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

        // 4. Item Table Headers (Properly aligned with columns)
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

        // 5. Item Rows: Description, Qty, Rate, and Amount rendered on the EXACT SAME line/baseline!
        lines.forEach { line ->
            val displayName = line.productName.ifBlank { "Item" }
            val unitStr = line.unitLabel ?: if (line.isPack) "pk" else "pc"
            val qtyStr = "${line.quantityBase} $unitStr"
            val rateStr = CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)
            val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)

            val lineTotalW = textPaint.measureText(lineTotal)
            val rateW = smallPaint.measureText(rateStr)
            val qtyW = smallPaint.measureText(qtyStr)

            // Right-aligned amounts & quantities aligned on this row
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
        y += 16f

        // 6. Totals & Payment Summary (Clean right-aligned block)
        fun drawSummaryRow(label: String, value: String, isBold: Boolean = false, isLarge: Boolean = false) {
            val p = if (isBold) boldPaint else textPaint
            if (isLarge) p.textSize = 12f else p.textSize = 10f
            canvas.drawText(label, 175f, y, p)
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

        canvas.drawLine(175f, y - 4f, pageWidth - 16f, y - 4f, linePaint)
        y += 4f

        drawSummaryRow("Total Payable:", CostCalculator.formatPaiseToRupees(sale.netTotalPaise), isBold = true, isLarge = true)

        drawSummaryRow("Amount Paid:", CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise))

        if (sale.changeGivenPaise > 0) {
            drawSummaryRow("Change Returned:", CostCalculator.formatPaiseToRupees(sale.changeGivenPaise))
        }

        if (sale.remainingCreditPaise > 0) {
            drawSummaryRow("Balance Due (Credit):", CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise), isBold = true)
        }

        drawSummaryRow("Payment Mode:", sale.paymentMethod)

        // 7. Footer
        y += 8f
        canvas.drawLine(16f, y, pageWidth - 16f, y, linePaint)
        y += 18f

        val thanksText = "Thank you for visiting Ali Educational!"
        val thanksW = boldPaint.measureText(thanksText)
        canvas.drawText(thanksText, (pageWidth - thanksW) / 2f, y, boldPaint)
        y += 13f

        val footerNote = "Please retain this receipt for warranty and returns."
        val footerW = smallPaint.measureText(footerNote)
        canvas.drawText(footerNote, (pageWidth - footerW) / 2f, y, smallPaint)

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
