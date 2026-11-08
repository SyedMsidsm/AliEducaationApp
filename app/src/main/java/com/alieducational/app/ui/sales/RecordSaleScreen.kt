package com.alieducational.app.ui.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.repository.SaleItemParam
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel

data class CartLineState(
    var productId: String = "",
    var productName: String = "",
    var packSize: Int = 1,
    var isPack: Boolean = false,
    var quantityStr: String = "1",
    var unitPriceStr: String = "0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSaleScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    val cart = remember { mutableStateListOf<CartLineState>() }

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerExpanded by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var showNewCustomerInput by remember { mutableStateOf(false) }

    var discountMode by remember { mutableStateOf("NONE") } // "NONE", "PERCENT", "FINAL_PRICE"
    var discountPercentStr by remember { mutableStateOf("0") }
    var agreedFinalPriceStr by remember { mutableStateOf("") }

    var amountReceivedStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    var showShortPaymentDialog by remember { mutableStateOf(false) }
    var completedSale by remember { mutableStateOf<SaleEntity?>(null) }

    // Computations
    val lineItems = cart.mapNotNull { line ->
        val qty = line.quantityStr.toIntOrNull() ?: 0
        val baseQty = if (line.isPack) qty * line.packSize else qty
        val pricePaise = ((line.unitPriceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (line.productId.isNotBlank() && baseQty > 0) {
            SaleItemParam(
                productId = line.productId,
                productName = line.productName,
                quantityBase = baseQty,
                isPack = line.isPack,
                unitSellingPricePaise = pricePaise
            )
        } else null
    }

    val grossTotalPaise = lineItems.sumOf { it.quantityBase.toLong() * it.unitSellingPricePaise }

    val discountValuePaise: Long = when (discountMode) {
        "PERCENT" -> {
            val pct = discountPercentStr.toDoubleOrNull() ?: 0.0
            ((grossTotalPaise * pct) / 100.0).toLong()
        }
        "FINAL_PRICE" -> {
            val finalPrice = ((agreedFinalPriceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
            (grossTotalPaise - finalPrice).coerceAtLeast(0L)
        }
        else -> 0L
    }

    val netTotalPaise = (grossTotalPaise - discountValuePaise).coerceAtLeast(0L)
    val computedDiscountPercent: Double = if (grossTotalPaise > 0) {
        (discountValuePaise.toDouble() / grossTotalPaise.toDouble()) * 100.0
    } else 0.0

    val amountReceivedPaise = if (amountReceivedStr.isBlank()) netTotalPaise
    else ((amountReceivedStr.toDoubleOrNull() ?: 0.0) * 100).toLong()

    val isShortPayment = amountReceivedPaise < netTotalPaise
    val balanceOwed = (netTotalPaise - amountReceivedPaise).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Sale", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Customer Selector (Optional for Cash, Required for Credit)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Customer (Optional for paid sales)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedCustomer != null) {
                                    Text(
                                        text = "Due: ${CostCalculator.formatPaiseToRupees(selectedCustomer!!.currentBalancePaise)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFE4B765)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (showNewCustomerInput) {
                                OutlinedTextField(
                                    value = newCustomerName,
                                    onValueChange = { newCustomerName = it },
                                    label = { Text("Customer Name") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (newCustomerName.isNotBlank()) {
                                                val c = CustomerEntity(name = newCustomerName.trim())
                                                viewModel.addCustomer(c) {
                                                    selectedCustomer = c
                                                    showNewCustomerInput = false
                                                }
                                            }
                                        }
                                    ) { Text("Save Customer") }
                                    OutlinedButton(onClick = { showNewCustomerInput = false }) { Text("Cancel") }
                                }
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = customerExpanded,
                                    onExpandedChange = { customerExpanded = !customerExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCustomer?.name ?: "Walk-in Cash Customer (No account)",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = customerExpanded,
                                        onDismissRequest = { customerExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Walk-in Cash Customer (No account)") },
                                            onClick = {
                                                selectedCustomer = null
                                                customerExpanded = false
                                            }
                                        )
                                        allCustomers.forEach { customer ->
                                            DropdownMenuItem(
                                                text = { Text("${customer.name} (Balance: ${CostCalculator.formatPaiseToRupees(customer.currentBalancePaise)})") },
                                                onClick = {
                                                    selectedCustomer = customer
                                                    customerExpanded = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("+ Add New Customer", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                customerExpanded = false
                                                showNewCustomerInput = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cart Items Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sale Items (${cart.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { cart.add(CartLineState()) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 12.sp)
                        }
                    }
                }

                itemsIndexed(cart) { index, line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Item #${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { cart.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Product Dropdown
                            var prodMenuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = prodMenuExpanded,
                                onExpandedChange = { prodMenuExpanded = !prodMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = line.productName.ifBlank { "Select Product..." },
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = prodMenuExpanded,
                                    onDismissRequest = { prodMenuExpanded = false }
                                ) {
                                    allProducts.forEach { product ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${product.name} (Stock: ${product.currentSellableStock} pcs • ₹${product.sellingPricePiecePaise / 100.0}/pc)")
                                            },
                                            onClick = {
                                                line.productId = product.id
                                                line.productName = product.name
                                                line.packSize = product.packSize
                                                line.unitPriceStr = (product.sellingPricePiecePaise / 100.0).toString()
                                                prodMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = line.quantityStr,
                                    onValueChange = { line.quantityStr = it },
                                    label = { Text("Qty (Pieces)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = line.unitPriceStr,
                                    onValueChange = { line.unitPriceStr = it },
                                    label = { Text("Price (₹/pc)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Wholesale Discount Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Wholesale Discount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { discountMode = "NONE" }
                                ) {
                                    RadioButton(selected = discountMode == "NONE", onClick = { discountMode = "NONE" })
                                    Text("None")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { discountMode = "PERCENT" }
                                ) {
                                    RadioButton(selected = discountMode == "PERCENT", onClick = { discountMode = "PERCENT" })
                                    Text("Percent (%)")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { discountMode = "FINAL_PRICE" }
                                ) {
                                    RadioButton(selected = discountMode == "FINAL_PRICE", onClick = { discountMode = "FINAL_PRICE" })
                                    Text("Final Price (₹)")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (discountMode == "PERCENT") {
                                OutlinedTextField(
                                    value = discountPercentStr,
                                    onValueChange = { discountPercentStr = it },
                                    label = { Text("Discount Percentage (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            } else if (discountMode == "FINAL_PRICE") {
                                OutlinedTextField(
                                    value = agreedFinalPriceStr,
                                    onValueChange = { agreedFinalPriceStr = it },
                                    label = { Text("Agreed Final Bill Amount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            if (discountValuePaise > 0L) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Discount: -${CostCalculator.formatPaiseToRupees(discountValuePaise)} (${String.format("%.1f", computedDiscountPercent)}%)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Payment Summary & Tender Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Payment & Tender",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gross Subtotal:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CostCalculator.formatPaiseToRupees(grossTotalPaise), fontWeight = FontWeight.Bold)
                            }

                            if (discountValuePaise > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-${CostCalculator.formatPaiseToRupees(discountValuePaise)}", color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Final Net Bill:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(CostCalculator.formatPaiseToRupees(netTotalPaise), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = amountReceivedStr,
                                onValueChange = { amountReceivedStr = it },
                                label = { Text("Amount Received (₹)") },
                                placeholder = { Text(CostCalculator.formatPaiseToRupees(netTotalPaise)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Short Payment Notice
                            if (isShortPayment) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Short payment by ${CostCalculator.formatPaiseToRupees(balanceOwed)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE4B765)
                                )
                            }
                        }
                    }
                }

                // Save Sale Button
                item {
                    Button(
                        onClick = {
                            if (lineItems.isEmpty()) return@Button

                            if (isShortPayment) {
                                showShortPaymentDialog = true
                            } else {
                                viewModel.recordSale(
                                    customerId = selectedCustomer?.id,
                                    customerName = selectedCustomer?.name,
                                    items = lineItems,
                                    discountType = discountMode,
                                    discountInput = if (discountMode == "PERCENT") discountPercentStr.toDoubleOrNull() ?: 0.0 else (discountValuePaise / 100.0),
                                    amountReceivedPaise = amountReceivedPaise,
                                    paymentMethod = paymentMethod,
                                    notes = notes.trim().ifBlank { null },
                                    onSuccess = { sale -> completedSale = sale }
                                )
                            }
                        },
                        enabled = lineItems.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Save Sale & Issue Receipt",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Short Payment Dialog (Spec requirement: Give discount vs Keep as credit)
    if (showShortPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showShortPaymentDialog = false },
            title = { Text("Payment is Short") },
            text = {
                Column {
                    Text("Total due: ${CostCalculator.formatPaiseToRupees(netTotalPaise)}")
                    Text("Received: ${CostCalculator.formatPaiseToRupees(amountReceivedPaise)}")
                    Text(
                        "Remaining: ${CostCalculator.formatPaiseToRupees(balanceOwed)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE4B765)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("How would you like to handle the remaining ${CostCalculator.formatPaiseToRupees(balanceOwed)}?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Option 1: Give discount (adjusts final price so balance = 0)
                        val extraDiscount = balanceOwed
                        val totalDisc = discountValuePaise + extraDiscount
                        showShortPaymentDialog = false

                        viewModel.recordSale(
                            customerId = selectedCustomer?.id,
                            customerName = selectedCustomer?.name,
                            items = lineItems,
                            discountType = "FIXED",
                            discountInput = totalDisc / 100.0,
                            amountReceivedPaise = amountReceivedPaise,
                            paymentMethod = paymentMethod,
                            notes = "Short payment discounted. $notes",
                            onSuccess = { sale -> completedSale = sale }
                        )
                    }
                ) {
                    Text("Give Discount")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        // Option 2: Keep remaining as credit
                        showShortPaymentDialog = false
                        viewModel.recordSale(
                            customerId = selectedCustomer?.id,
                            customerName = selectedCustomer?.name ?: "Credit Customer",
                            items = lineItems,
                            discountType = discountMode,
                            discountInput = if (discountMode == "PERCENT") discountPercentStr.toDoubleOrNull() ?: 0.0 else (discountValuePaise / 100.0),
                            amountReceivedPaise = amountReceivedPaise,
                            paymentMethod = "CREDIT",
                            notes = "Credit sale balance ${CostCalculator.formatPaiseToRupees(balanceOwed)}. $notes",
                            onSuccess = { sale -> completedSale = sale }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Keep as Credit")
                }
            }
        )
    }

    // Receipt Completed Dialog
    if (completedSale != null) {
        val sale = completedSale!!
        AlertDialog(
            onDismissRequest = {
                completedSale = null
                onNavigateBack()
            },
            title = { Text("Sale Saved! Receipt: ${sale.receiptNumber}") },
            text = {
                Column {
                    Text("Amount Paid: ${CostCalculator.formatPaiseToRupees(sale.amountReceivedPaise)}")
                    if (sale.remainingCreditPaise > 0) {
                        Text(
                            "Credit Balance: ${CostCalculator.formatPaiseToRupees(sale.remainingCreditPaise)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4B765)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Stock and account balances updated together offline.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = viewModel.receiptService.generateReceiptText(sale, emptyList())
                        viewModel.receiptService.shareReceipt(text)
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share WhatsApp")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        completedSale = null
                        onNavigateBack()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}