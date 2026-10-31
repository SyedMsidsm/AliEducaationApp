package com.alieducational.app.ui.accounts

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AliViewModel,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }
    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = We Owe (Suppliers), 1 = Customers Owe Us
    val suppliers by viewModel.allSuppliers.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val totalWeOwe by viewModel.totalWeOweSuppliersPaise.collectAsState()
    val totalCustomersOwe by viewModel.totalCustomersOweUsPaise.collectAsState()

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var paymentTargetSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var paymentTargetCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var completedVoucher by remember { mutableStateOf<PaymentVoucher?>(null) }
    val shopName by viewModel.shopName.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Row: We Owe vs Customers Owe Us
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("We Owe (${suppliers.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = Color(0xFF75B890), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Customers Owe (${customers.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Balance Banner Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedTab == 0) Color(0xFFE57373).copy(alpha = 0.15f)
                        else Color(0xFF75B890).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedTab == 0) "Total We Owe Suppliers" else "Total Customers Owe Us",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CostCalculator.formatPaiseToRupees(
                                    if (selectedTab == 0) totalWeOwe ?: 0L else totalCustomersOwe ?: 0L
                                ),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == 0) Color(0xFFE57373) else Color(0xFF75B890)
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedTab == 0) showAddSupplierDialog = true
                                else showAddCustomerDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) Color(0xFFE57373) else Color(0xFF75B890),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedTab == 0) "Add Supplier" else "Add Customer", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Accounts List
                if (selectedTab == 0) {
                    // Suppliers List
                    if (suppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No suppliers added yet. Tap + to add a supplier.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(suppliers) { supplier ->
                                SupplierAccountCard(
                                    supplier = supplier,
                                    onRecordPayment = { paymentTargetSupplier = supplier }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                } else {
                    // Customers List
                    if (customers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No customer credit accounts. Tap + to add a customer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(customers) { customer ->
                                CustomerAccountCard(
                                    customer = customer,
                                    onRecordRepayment = { paymentTargetCustomer = customer }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var openingBalanceStr by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("Add Supplier Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Supplier / Company Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openingBalanceStr,
                        onValueChange = { openingBalanceStr = it },
                        label = { Text("Old Outstanding Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val balancePaise = ((openingBalanceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        viewModel.addSupplier(
                            SupplierEntity(
                                name = name.trim(),
                                phone = phone.trim().ifBlank { null },
                                currentBalancePaise = balancePaise
                            )
                        ) {
                            showAddSupplierDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddSupplierDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var openingBalanceStr by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Customer Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openingBalanceStr,
                        onValueChange = { openingBalanceStr = it },
                        label = { Text("Old Credit Due (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val balancePaise = ((openingBalanceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        viewModel.addCustomer(
                            CustomerEntity(
                                name = name.trim(),
                                phone = phone.trim().ifBlank { null },
                                currentBalancePaise = balancePaise
                            )
                        ) {
                            showAddCustomerDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCustomerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Record Payment Dialog
    if (paymentTargetSupplier != null || paymentTargetCustomer != null) {
        val isSupplier = paymentTargetSupplier != null
        val accountName = paymentTargetSupplier?.name ?: paymentTargetCustomer?.name ?: ""
        val currentBalance = paymentTargetSupplier?.currentBalancePaise ?: paymentTargetCustomer?.currentBalancePaise ?: 0L

        var amountStr by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("CASH") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                paymentTargetSupplier = null
                paymentTargetCustomer = null
            },
            title = {
                Text(if (isSupplier) "Pay Supplier: $accountName" else "Receive Repayment: $accountName")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Balance: ${CostCalculator.formatPaiseToRupees(currentBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = if (isSupplier) Color(0xFFE57373) else Color(0xFF75B890)
                    )

                    // Payment Method FilterChips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentMethod == "CASH",
                            onClick = { paymentMethod = "CASH" },
                            label = { Text("Cash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == "UPI",
                            onClick = { paymentMethod = "UPI" },
                            label = { Text("UPI") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == "BANK",
                            onClick = { paymentMethod = "BANK" },
                            label = { Text("Bank") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Payment Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Reference (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = ((amountStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        if (amountPaise <= 0) return@Button

                        val prevBal = currentBalance
                        val remBal = prevBal - amountPaise

                        if (isSupplier) {
                            val sup = paymentTargetSupplier!!
                            viewModel.recordAccountPayment(
                                accountType = "SUPPLIER",
                                accountId = sup.id,
                                amountPaise = amountPaise,
                                paymentMethod = paymentMethod,
                                notes = notes.trim().ifBlank { null },
                                receiptRef = null,
                                onDone = {
                                    paymentTargetSupplier = null
                                    completedVoucher = PaymentVoucher(
                                        accountName = accountName,
                                        accountType = "SUPPLIER",
                                        amountPaise = amountPaise,
                                        paymentMethod = paymentMethod,
                                        previousBalancePaise = prevBal,
                                        remainingBalancePaise = remBal,
                                        notes = notes.trim().ifBlank { null }
                                    )
                                }
                            )
                        } else {
                            val cust = paymentTargetCustomer!!
                            viewModel.recordAccountPayment(
                                accountType = "CUSTOMER",
                                accountId = cust.id,
                                amountPaise = amountPaise,
                                paymentMethod = paymentMethod,
                                notes = notes.trim().ifBlank { null },
                                receiptRef = null,
                                onDone = {
                                    paymentTargetCustomer = null
                                    completedVoucher = PaymentVoucher(
                                        accountName = accountName,
                                        accountType = "CUSTOMER",
                                        amountPaise = amountPaise,
                                        paymentMethod = paymentMethod,
                                        previousBalancePaise = prevBal,
                                        remainingBalancePaise = remBal,
                                        notes = notes.trim().ifBlank { null }
                                    )
                                }
                            )
                        }
                    }
                ) { Text("Confirm Payment") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        paymentTargetSupplier = null
                        paymentTargetCustomer = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    // Payment Voucher Dialog (Option 2)
    completedVoucher?.let { voucher ->
        PaymentVoucherDialog(
            voucher = voucher,
            shopName = shopName,
            onDismiss = { completedVoucher = null }
        )
    }
}

data class PaymentVoucher(
    val accountName: String,
    val accountType: String, // "CUSTOMER" or "SUPPLIER"
    val amountPaise: Long,
    val paymentMethod: String,
    val previousBalancePaise: Long,
    val remainingBalancePaise: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Composable
fun PaymentVoucherDialog(
    voucher: PaymentVoucher,
    shopName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSupplier = voucher.accountType == "SUPPLIER"
    val formattedDate = remember(voucher.timestamp) {
        SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(voucher.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.voucher_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = shopName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isSupplier) "Supplier Payment Paid" else "Debt Repayment Received",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${if (isSupplier) "Supplier" else "Customer"}: ${voucher.accountName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Date: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stringResource(R.string.voucher_payment_mode)}: ${voucher.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.voucher_amount_paid), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            CostCalculator.formatPaiseToRupees(voucher.amountPaise),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Previous Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CostCalculator.formatPaiseToRupees(voucher.previousBalancePaise), style = MaterialTheme.typography.bodySmall)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.voucher_remaining_balance), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            CostCalculator.formatPaiseToRupees(voucher.remainingBalancePaise),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (voucher.remainingBalancePaise > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                        )
                    }

                    if (!voucher.notes.isNullOrBlank()) {
                        Text(
                            text = "Note: ${voucher.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareMsg = """
                        *$shopName - Payment Voucher*
                        ${if (isSupplier) "Supplier" else "Customer"}: ${voucher.accountName}
                        Date: $formattedDate
                        Payment Mode: ${voucher.paymentMethod}
                        Amount Paid: ${CostCalculator.formatPaiseToRupees(voucher.amountPaise)}
                        Previous Balance: ${CostCalculator.formatPaiseToRupees(voucher.previousBalancePaise)}
                        Remaining Balance: ${CostCalculator.formatPaiseToRupees(voucher.remainingBalancePaise)}
                        Thank you! Stored safely offline.
                    """.trimIndent()
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Payment Voucher"))
                }
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.voucher_share_button))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.voucher_done_button))
            }
        }
    )
}

@Composable
fun SupplierAccountCard(
    supplier: SupplierEntity,
    onRecordPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!supplier.phone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = supplier.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CostCalculator.formatPaiseToRupees(supplier.currentBalancePaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (supplier.currentBalancePaise > 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onRecordPayment,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Bill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomerAccountCard(
    customer: CustomerEntity,
    onRecordRepayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!customer.phone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CostCalculator.formatPaiseToRupees(customer.currentBalancePaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (customer.currentBalancePaise > 0) Color(0xFF75B890) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onRecordRepayment,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}