package com.alieducational.app.data.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.domain.calculation.CostCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun generateReceiptText(sale: SaleEntity, lines: List<SaleLineEntity>, isUrdu: Boolean = false): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val sb = StringBuilder()

        if (isUrdu) {
            sb.append("==============================\n")
            sb.append("       علی ایجوکیشنل\n")
            sb.append("   اسٹیشنری و تعلیمی کتب\n")
            sb.append("==============================\n")
            sb.append("رسید نمبر: ${sale.receiptNumber}\n")
            sb.append("تاریخ: $dateStr\n")
            if (!sale.customerName.isNullOrBlank()) {
                sb.append("گاہک: ${sale.customerName}\n")
            }
            sb.append("------------------------------\n")
            lines.forEach { line ->
                val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)
                sb.append("${line.productName}\n")
                sb.append("  ${line.quantityBase} x ${CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)} = $lineTotal\n")
            }
            sb.append("------------------------------\n")
            sb.append("کل رقم: ${CostCalculator.formatPaiseToRupees(sale.grossTotalPaise)}\n")
            if (sale.discountValuePaise > 0) {
                sb.append("رعایت: -${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)} (${String.format("%.1f", sale.discountPercent)}%)\n")
            }
            sb.append("حتمی رقم: ${CostCalculator.formatPaiseToRupees(sale.netTotalPaise)}\n")
            sb.append("وصول شدہ: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}\n")
            if (sale.remainingCreditPaise > 0) {
                sb.append("باقی ادھار: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}\n")
            }
            sb.append("==============================\n")
            sb.append("   شکریہ! دوبارہ تشریف لائیں\n")
        } else {
            sb.append("==============================\n")
            sb.append("       ALI EDUCATIONAL\n")
            sb.append("  Stationery & Books Shop\n")
            sb.append("==============================\n")
            sb.append("Receipt: ${sale.receiptNumber}\n")
            sb.append("Date: $dateStr\n")
            if (!sale.customerName.isNullOrBlank()) {
                sb.append("Customer: ${sale.customerName}\n")
            }
            sb.append("------------------------------\n")
            lines.forEach { line ->
                val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)
                sb.append("${line.productName}\n")
                sb.append("  ${line.quantityBase} x ${CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)} = $lineTotal\n")
            }
            sb.append("------------------------------\n")
            sb.append("Subtotal: ${CostCalculator.formatPaiseToRupees(sale.grossTotalPaise)}\n")
            if (sale.discountValuePaise > 0) {
                sb.append("Discount: -${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)} (${String.format("%.1f", sale.discountPercent)}%)\n")
            }
            sb.append("Total: ${CostCalculator.formatPaiseToRupees(sale.netTotalPaise)}\n")
            sb.append("Paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}\n")
            if (sale.remainingCreditPaise > 0) {
                sb.append("Remaining Balance: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}\n")
            }
            sb.append("==============================\n")
            sb.append("    Thank you for visiting!\n")
        }

        return sb.toString()
    }

    suspend fun createReceiptPdf(sale: SaleEntity, lines: List<SaleLineEntity>, isUrdu: Boolean = false): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 30f
        canvas.drawText("ALI EDUCATIONAL", 85f, y, boldPaint)
        y += 18f
        paint.textSize = 9f
        canvas.drawText("Stationery & Educational Supplies", 65f, y, paint)
        y += 20f

        canvas.drawLine(10f, y, 290f, y, paint)
        y += 16f

        canvas.drawText("Receipt: ${sale.receiptNumber}", 10f, y, paint)
        y += 14f
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))
        canvas.drawText("Date: $dateStr", 10f, y, paint)
        y += 14f

        if (!sale.customerName.isNullOrBlank()) {
            canvas.drawText("Customer: ${sale.customerName}", 10f, y, paint)
            y += 14f
        }

        canvas.drawLine(10f, y, 290f, y, paint)
        y += 18f

        lines.forEach { line ->
            canvas.drawText(line.productName, 10f, y, boldPaint)
            y += 14f
            val itemLine = "${line.quantityBase} x ${CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)}"
            val lineTotal = CostCalculator.formatPaiseToRupees(line.lineNetPaise)
            canvas.drawText(itemLine, 15f, y, paint)
            canvas.drawText(lineTotal, 220f, y, paint)
            y += 16f
        }

        canvas.drawLine(10f, y, 290f, y, paint)
        y += 18f

        canvas.drawText("Subtotal:", 120f, y, paint)
        canvas.drawText(CostCalculator.formatPaiseToRupees(sale.grossTotalPaise), 220f, y, paint)
        y += 16f

        if (sale.discountValuePaise > 0) {
            canvas.drawText("Discount:", 120f, y, paint)
            canvas.drawText("-${CostCalculator.formatPaiseToRupees(sale.discountValuePaise)}", 220f, y, paint)
            y += 16f
        }

        canvas.drawText("Final Total:", 120f, y, boldPaint)
        canvas.drawText(CostCalculator.formatPaiseToRupees(sale.netTotalPaise), 220f, y, boldPaint)
        y += 18f

        canvas.drawText("Amount Paid:", 120f, y, paint)
        canvas.drawText(CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise), 220f, y, paint)
        y += 16f

        if (sale.remainingCreditPaise > 0) {
            canvas.drawText("Credit Due:", 120f, y, boldPaint)
            canvas.drawText(CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise), 220f, y, boldPaint)
            y += 18f
        }

        canvas.drawLine(10f, y, 290f, y, paint)
        y += 24f
        canvas.drawText("Thank you for your business!", 75f, y, paint)

        document.finishPage(page)

        val receiptsDir = File(context.cacheDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()
        val pdfFile = File(receiptsDir, "${sale.receiptNumber}.pdf")
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return@withContext pdfFile
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
        context.startActivity(chooser)
    }
}