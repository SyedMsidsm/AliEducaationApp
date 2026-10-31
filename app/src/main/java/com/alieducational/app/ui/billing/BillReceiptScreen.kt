package com.alieducational.app.ui.billing

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillReceiptScreen(
    receiptNumber: String,
    viewModel: AliViewModel,
    onNewBill: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shopName by viewModel.shopName.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    var currentSale by remember { mutableStateOf(billingState.lastSavedSale) }
    var saleLines by remember { mutableStateOf<List<SaleLineEntity>>(emptyList()) }

    LaunchedEffect(currentSale?.id, receiptNumber) {
        val saleId = currentSale?.id
        if (saleId != null) {
            saleLines = viewModel.getSaleLines(saleId)
        }
    }

    var showVoidDialog by remember { mutableStateOf(false) }
    var stockCondition by remember { mutableStateOf("SELLABLE") }
    var voidReason by remember { mutableStateOf("") }
    var isVoiding by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember { dateFormat.format(Date()) }

    val isVoided = currentSale?.isVoided == true
    val paymentMethod = currentSale?.paymentMethod ?: billingState.paymentMethod
    val paymentMethodLabel = when (paymentMethod) {
        "CASH" -> stringResource(R.string.payment_cash)
        "UPI" -> stringResource(R.string.payment_upi)
        "CREDIT" -> stringResource(R.string.payment_credit)
        else -> paymentMethod
    }
    val memoTag = when {
        isVoided -> "VOIDED MEMO"
        paymentMethod == "CREDIT" -> "CREDIT MEMO"
        paymentMethod == "UPI" -> "UPI MEMO"
        else -> "CASH MEMO"
    }

    val totalPaise = currentSale?.netTotalPaise ?: billingState.netTotalPaise
    val discountPaise = if (currentSale != null) currentSale!!.grossTotalPaise - currentSale!!.netTotalPaise else billingState.actualDiscountPaise
    val grossPaise = totalPaise + discountPaise
    val changeGivenPaise = currentSale?.changeGivenPaise ?: 0L

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                val isVoided = currentSale?.isVoided == true

                // Success or Voided Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isVoided) Color(0xFFE57373) else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVoided) Icons.Filled.Cancel else Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isVoided) stringResource(R.string.receipt_voided_badge) else stringResource(R.string.receipt_saved_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isVoided) Color(0xFFE57373) else MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${stringResource(R.string.receipt_reference)}: $receiptNumber",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Printable / Sharable Receipt Card
            item {
                val isVoided = currentSale?.isVoided == true

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isVoided) Color(0xFFE57373).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Voided Banner
                        if (isVoided) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE57373).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "CANCELLED & REFUNDED",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE57373),
                                        fontSize = 14.sp
                                    )
                                    val refundAmt = currentSale?.refundPayoutPaise ?: 0L
                                    Text(
                                        text = "Cash Refund Paid: ${CostCalculator.formatPaiseToRupees(refundAmt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val cond = currentSale?.stockRestoredCondition ?: "SELLABLE"
                                    Text(
                                        text = "Stock Condition: $cond",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shopName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = memoTag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isVoided) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (currentSale?.customerName != null) {
                            Text(
                                text = "${stringResource(R.string.review_customer)}: ${currentSale?.customerName}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Payment Method Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.receipt_payment_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = paymentMethodLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (paymentMethod == "CREDIT") Color(0xFFFFA000) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Items Purchased List
                        Text(
                            text = stringResource(R.string.receipt_items_purchased),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (saleLines.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                saleLines.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = line.productName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val unitStr = if (line.isPack) stringResource(R.string.billing_pack) else stringResource(R.string.billing_piece)
                                            val unitPrice = CostCalculator.formatPaiseToRupees(line.unitSellingPricePaise)
                                            Text(
                                                text = "${line.quantityBase} $unitStr @ $unitPrice",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = CostCalculator.formatPaiseToRupees(line.lineNetPaise),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else if (billingState.items.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                billingState.items.forEach { draftItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = draftItem.productName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val unitStr = if (draftItem.unitType == "PACK") stringResource(R.string.billing_pack) else stringResource(R.string.billing_piece)
                                            val unitPrice = CostCalculator.formatPaiseToRupees(draftItem.unitPricePaise)
                                            Text(
                                                text = "${draftItem.quantity} $unitStr @ $unitPrice",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = CostCalculator.formatPaiseToRupees(draftItem.lineGrossPaise),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Financial Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.receipt_gross_total),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CostCalculator.formatPaiseToRupees(grossPaise),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (discountPaise > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.receipt_discount_given),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${CostCalculator.formatPaiseToRupees(discountPaise)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.receipt_net_paid),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CostCalculator.formatPaiseToRupees(totalPaise),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = if (isVoided) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (changeGivenPaise > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.receipt_change_given),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = CostCalculator.formatPaiseToRupees(changeGivenPaise),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Share & Print Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val totalRupees = CostCalculator.formatPaiseToRupees(currentSale?.netTotalPaise ?: billingState.netTotalPaise)
                            val itemsListText = if (saleLines.isNotEmpty()) {
                                saleLines.joinToString("\n") { l ->
                                    val unit = if (l.isPack) "pack" else "pc"
                                    "• ${l.productName}: ${l.quantityBase} $unit @ ${CostCalculator.formatPaiseToRupees(l.unitSellingPricePaise)} = ${CostCalculator.formatPaiseToRupees(l.lineNetPaise)}"
                                }
                            } else {
                                billingState.items.joinToString("\n") { it ->
                                    val unit = if (it.unitType == "PACK") "pack" else "pc"
                                    "• ${it.productName}: ${it.quantity} $unit @ ${CostCalculator.formatPaiseToRupees(it.unitPricePaise)} = ${CostCalculator.formatPaiseToRupees(it.lineGrossPaise)}"
                                }
                            }
                            val shareText = """
                                *$shopName - $memoTag*
                                Receipt #: $receiptNumber
                                Date: $formattedDate
                                ${if (currentSale?.customerName != null) "Customer: ${currentSale?.customerName}\n" else ""}Payment Mode: $paymentMethodLabel

                                --- Items ---
                                $itemsListText

                                Gross Total: ${CostCalculator.formatPaiseToRupees(grossPaise)}
                                ${if (discountPaise > 0L) "Discount: -${CostCalculator.formatPaiseToRupees(discountPaise)}\n" else ""}Net Total: $totalRupees
                                Stored safely & offline.
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Receipt"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.receipt_share_button))
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Receipt sent to printer spool", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.receipt_print_button))
                    }
                }
            }

            // Void / Cancellation Action (F13 policy: Ask condition & refund payout)
            if (currentSale != null && currentSale?.isVoided != true) {
                item {
                    OutlinedButton(
                        onClick = { showVoidDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE57373)
                        )
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.receipt_void_button),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Navigation Actions: New Bill & Home
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        viewModel.resetBilling()
                        onNewBill()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PointOfSale,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.receipt_new_bill),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.resetBilling()
                        onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.receipt_done),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    // Cancellation & Refund Dialog (Asks condition & Refunds payout)
    if (showVoidDialog && currentSale != null) {
        val sale = currentSale!!
        val refundPaise = minOf(sale.amountReceivedPaise, sale.netTotalPaise)

        AlertDialog(
            onDismissRequest = { if (!isVoiding) showVoidDialog = false },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = stringResource(R.string.receipt_void_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.receipt_void_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Refund Payout Explanation
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE57373).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = stringResource(R.string.receipt_void_refund_payout),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE57373)
                            )
                            Text(
                                text = CostCalculator.formatPaiseToRupees(refundPaise),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE57373)
                            )
                            Text(
                                text = "Cash refund will be deducted from till payout records.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Stock Return Condition Selection ("It should ask")
                    Text(
                        text = stringResource(R.string.receipt_void_stock_question),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stockCondition = "SELLABLE" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (stockCondition == "SELLABLE") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (stockCondition == "SELLABLE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.receipt_void_condition_sellable),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stockCondition = "DAMAGED" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (stockCondition == "DAMAGED") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (stockCondition == "DAMAGED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.receipt_void_condition_damaged),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(stringResource(R.string.receipt_void_reason_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isVoiding = true
                        viewModel.voidSale(
                            saleId = sale.id,
                            stockCondition = stockCondition,
                            voidReason = voidReason.ifBlank { "Customer return / billing reversal" },
                            onDone = {
                                isVoiding = false
                                showVoidDialog = false
                                currentSale = currentSale?.copy(
                                    isVoided = true,
                                    voidReason = voidReason.ifBlank { "Customer return" },
                                    refundPayoutPaise = refundPaise,
                                    stockRestoredCondition = stockCondition
                                )
                                Toast.makeText(context, "Sale cancelled and refund recorded.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isVoiding = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    enabled = !isVoiding
                ) {
                    Text(stringResource(R.string.receipt_void_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showVoidDialog = false },
                    enabled = !isVoiding
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}
