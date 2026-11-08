package com.alieducational.app.ui.accounts

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AliViewModel,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit = {},
    onNavigateToBillHistory: () -> Unit = {},
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
    val distinctCreditors by viewModel.distinctCreditorsCount.collectAsState(initial = 0)
    val distinctDebtors by viewModel.distinctDebtorsCount.collectAsState(initial = 0)

    var showOnlyOutstanding by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val customerTarget by viewModel.accountCustomerTarget.collectAsState()
    androidx.compose.runtime.LaunchedEffect(customerTarget, customers) {
        customerTarget?.let { id -> customers.firstOrNull { it.id == id }?.let { customer ->
            selectedTab = 1; searchQuery = customer.name; showOnlyOutstanding = false
            viewModel.clearCustomerAccountTarget()
        } }
    }

    val filteredSuppliers = remember(suppliers, showOnlyOutstanding, searchQuery) {
        suppliers.filter { sup ->
            val matchesOutstanding = if (showOnlyOutstanding) sup.currentBalancePaise > 0L else true
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sup.name.contains(searchQuery, ignoreCase = true) || (sup.phone?.contains(searchQuery) == true)
            }
            matchesOutstanding && matchesSearch
        }
    }

    val filteredCustomers = remember(customers, showOnlyOutstanding, searchQuery) {
        customers.filter { cust ->
            val matchesOutstanding = if (showOnlyOutstanding) cust.currentBalancePaise > 0L else true
            val matchesSearch = if (searchQuery.isBlank()) true else {
                cust.name.contains(searchQuery, ignoreCase = true) || (cust.phone?.contains(searchQuery) == true)
            }
            matchesOutstanding && matchesSearch
        }
    }

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var paymentTargetSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var paymentTargetCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var removeTargetSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var removeTargetCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var completedVoucher by remember { mutableStateOf<PaymentVoucher?>(null) }
    val shopName by viewModel.shopName.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val positiveColor = if (isDarkTheme) PositiveGreenDark else PositiveGreenLight
    val negativeColor = if (isDarkTheme) NegativeRedDark else NegativeRedLight

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
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = negativeColor, modifier = Modifier.size(16.dp))
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
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = positiveColor, modifier = Modifier.size(16.dp))
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            if (selectedTab == 0) NegativeRedDark.copy(alpha = 0.15f)
                            else PositiveGreenDark.copy(alpha = 0.15f)
                        } else {
                            if (selectedTab == 0) Color(0xFFFFEBEE)
                            else Color(0xFFE8F5E9)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == 0) negativeColor.copy(alpha = 0.35f)
                        else positiveColor.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "Amount We Owe Suppliers" else "Amount Customers Owe Us",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CostCalculator.formatPaiseToRupees(
                                if (selectedTab == 0) totalWeOwe ?: 0L else totalCustomersOwe ?: 0L
                            ),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedTab == 0) negativeColor else positiveColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTab == 0) {
                                if (distinctCreditors > 0) "$distinctCreditors suppliers with outstanding dues" else "No outstanding supplier balances"
                            } else {
                                if (distinctDebtors > 0) "$distinctDebtors customers with outstanding dues" else "No outstanding customer balances"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dedicated Full-Width Add Action Button Row
                Button(
                    onClick = {
                        if (selectedTab == 0) showAddSupplierDialog = true
                        else showAddCustomerDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) negativeColor else positiveColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(46.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedTab == 0) "Add Supplier" else "Add Customer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Dedicated Full-Width Search Field Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (selectedTab == 0) "Search suppliers..." else "Search customers...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                // Dedicated Filter Chips Row (All vs Outstanding)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showOnlyOutstanding,
                        onClick = { showOnlyOutstanding = false },
                        label = { Text("All", fontWeight = if (!showOnlyOutstanding) FontWeight.Bold else FontWeight.Normal) }
                    )
                    FilterChip(
                        selected = showOnlyOutstanding,
                        onClick = { showOnlyOutstanding = true },
                        label = { Text("Outstanding", fontWeight = if (showOnlyOutstanding) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Accounts List or Explanatory Empty State
                if (selectedTab == 0) {
                    // Suppliers List
                    if (filteredSuppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp, start = 24.dp, end = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMsg = when {
                                suppliers.isEmpty() -> "No suppliers added yet.\nTap 'Add Supplier' above to create one."
                                showOnlyOutstanding && suppliers.none { it.currentBalancePaise > 0L } -> "No outstanding supplier balances.\nAll supplier dues are cleared."
                                else -> "No matching suppliers found."
                            }
                            Text(
                                emptyMsg,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredSuppliers) { supplier ->
                                SupplierAccountCard(
                                    supplier = supplier,
                                    isDarkTheme = isDarkTheme,
                                    onRecordPayment = { paymentTargetSupplier = supplier },
                                    onRemoveSupplier = { removeTargetSupplier = supplier }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                } else {
                    // Customers List
                    if (filteredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 36.dp, start = 24.dp, end = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMsg = when {
                                customers.isEmpty() -> "No customers added yet.\nTap 'Add Customer' above to create one."
                                showOnlyOutstanding && customers.none { it.currentBalancePaise > 0L } -> "No outstanding customer balances.\nAll customer credit dues are cleared."
                                else -> "No matching customers found."
                            }
                            Text(
                                emptyMsg,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredCustomers) { customer ->
                                CustomerAccountCard(
                                    customer = customer,
                                    isDarkTheme = isDarkTheme,
                                    onRecordRepayment = { paymentTargetCustomer = customer },
                                    onNavigateToBillHistory = onNavigateToBillHistory,
                                    onRemoveCustomer = { removeTargetCustomer = customer }
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
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
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

    // Remove Supplier Confirmation Dialog
    if (removeTargetSupplier != null) {
        val sup = removeTargetSupplier!!
        AlertDialog(
            onDismissRequest = { removeTargetSupplier = null },
            title = { Text("Remove Supplier Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to remove supplier '${sup.name}'?")
                    if (sup.currentBalancePaise > 0) {
                        Text(
                            "Warning: You currently owe this supplier ${CostCalculator.formatPaiseToRupees(sup.currentBalancePaise)}. Removing this account will clear the balance from active records.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplier(sup.id) {
                            removeTargetSupplier = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { removeTargetSupplier = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Customer Confirmation Dialog
    if (removeTargetCustomer != null) {
        val cust = removeTargetCustomer!!
        AlertDialog(
            onDismissRequest = { removeTargetCustomer = null },
            title = { Text("Remove Customer Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to remove customer '${cust.name}'?")
                    if (cust.currentBalancePaise > 0) {
                        Text(
                            "Warning: This customer currently owes you ${CostCalculator.formatPaiseToRupees(cust.currentBalancePaise)}. Removing this account will clear the balance from active records.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust.id) {
                            removeTargetCustomer = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { removeTargetCustomer = null }) {
                    Text("Cancel")
                }
            }
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
    isDarkTheme: Boolean = true,
    onRecordPayment: () -> Unit,
    onRemoveSupplier: () -> Unit = {}
) {
    val negColor = if (isDarkTheme) NegativeRedDark else NegativeRedLight
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.3f else 0.5f)),
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
                    color = if (supplier.currentBalancePaise > 0) negColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemoveSupplier,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Remove Supplier",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
}

@Composable
fun CustomerAccountCard(
    customer: CustomerEntity,
    isDarkTheme: Boolean = true,
    onRecordRepayment: () -> Unit,
    onNavigateToBillHistory: () -> Unit = {},
    onRemoveCustomer: () -> Unit = {}
) {
    val posColor = if (isDarkTheme) PositiveGreenDark else PositiveGreenLight
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.3f else 0.5f)),
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
                    color = if (customer.currentBalancePaise > 0) posColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRemoveCustomer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Remove Customer",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToBillHistory,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bills", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
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
}