package com.alieducational.app.ui.stock

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
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.data.repository.PurchaseItemParam
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.domain.calculation.PurchaseLineInput
import com.alieducational.app.ui.AliViewModel

data class ReceiveLineState(
    var productId: String = "",
    var productName: String = "",
    var packSize: Int = 1,
    var isPack: Boolean = false,
    var quantityStr: String = "1",
    var unitPriceStr: String = "0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.allSuppliers.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var showNewSupplierInput by remember { mutableStateOf(false) }

    var invoiceNumber by remember { mutableStateOf("") }
    var deliveryChargeStr by remember { mutableStateOf("0") }
    var deliveryPaidTo by remember { mutableStateOf("SUPPLIER") } // SUPPLIER, CARRIER, SETTLED
    var amountPaidStr by remember { mutableStateOf("0") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    val items = remember { mutableStateListOf<ReceiveLineState>() }

    // Computations
    val totalDeliveryPaise = ((deliveryChargeStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
    val amountPaidPaise = ((amountPaidStr.toDoubleOrNull() ?: 0.0) * 100).toLong()

    val lineInputs = items.mapNotNull { line ->
        val qty = line.quantityStr.toIntOrNull() ?: 0
        val baseQty = if (line.isPack) qty * line.packSize else qty
        val pricePaise = ((line.unitPriceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (line.productId.isNotBlank() && baseQty > 0) {
            PurchaseLineInput(id = line.productId, quantity = baseQty, unitPricePaise = pricePaise)
        } else null
    }

    val totalGoodsValuePaise = lineInputs.sumOf { it.totalValuePaise }
    val allocatedDelivery = CostCalculator.allocateDelivery(lineInputs, totalDeliveryPaise)
    val totalLandedCostPaise = totalGoodsValuePaise + totalDeliveryPaise

    val supplierDeliveryLiability = if (deliveryPaidTo == "SUPPLIER") totalDeliveryPaise else 0L
    val totalBilledBySupplier = totalGoodsValuePaise + supplierDeliveryLiability
    val remainingPayablePaise = (totalBilledBySupplier - amountPaidPaise).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Goods & Stock", fontWeight = FontWeight.Bold) },
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
                // Supplier Selection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Supplier Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (showNewSupplierInput) {
                                OutlinedTextField(
                                    value = newSupplierName,
                                    onValueChange = { newSupplierName = it },
                                    label = { Text("New Supplier Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (newSupplierName.isNotBlank()) {
                                                val s = SupplierEntity(name = newSupplierName.trim())
                                                viewModel.addSupplier(s) {
                                                    selectedSupplier = s
                                                    showNewSupplierInput = false
                                                }
                                            }
                                        }
                                    ) { Text("Save Supplier") }
                                    OutlinedButton(onClick = { showNewSupplierInput = false }) { Text("Cancel") }
                                }
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = supplierExpanded,
                                    onExpandedChange = { supplierExpanded = !supplierExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedSupplier?.name ?: "Select Supplier...",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = supplierExpanded,
                                        onDismissRequest = { supplierExpanded = false }
                                    ) {
                                        suppliers.forEach { supplier ->
                                            DropdownMenuItem(
                                                text = { Text(supplier.name) },
                                                onClick = {
                                                    selectedSupplier = supplier
                                                    supplierExpanded = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("+ Add New Supplier", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                supplierExpanded = false
                                                showNewSupplierInput = true
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = invoiceNumber,
                                onValueChange = { invoiceNumber = it },
                                label = { Text("Invoice / Bill Number (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Products Received
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Items Received (${items.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                items.add(ReceiveLineState())
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Line", fontSize = 12.sp)
                        }
                    }
                }

                itemsIndexed(items) { index, line ->
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
                                    text = "Line #${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { items.removeAt(index) },
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
                                            text = { Text("${product.name} (${product.category})") },
                                            onClick = {
                                                line.productId = product.id
                                                line.productName = product.name
                                                line.packSize = product.packSize
                                                line.unitPriceStr = (product.movingAvgCostPaise / 100.0).toString()
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
                                    label = { Text("Qty (Base)") },
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

                            // Show allocated delivery share for this line if delivery > 0
                            val alloc = allocatedDelivery.firstOrNull { it.lineId == line.productId }
                            if (alloc != null && totalDeliveryPaise > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Allocated Delivery: ${CostCalculator.formatPaiseToRupees(alloc.allocatedDeliveryPaise)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Effective Landed: ${CostCalculator.formatPaiseToRupees(alloc.effectiveUnitCostPaise)}/pc",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Show profit after transport if selling price is set
                            val product = allProducts.firstOrNull { it.id == line.productId }
                            if (product != null && product.sellingPricePiecePaise > 0L) {
                                val landedCost = if (alloc != null && totalDeliveryPaise > 0L) alloc.effectiveUnitCostPaise else (((line.unitPriceStr.toDoubleOrNull() ?: 0.0) * 100).toLong())
                                val profitPaise = product.sellingPricePiecePaise - landedCost
                                val margin = if (product.sellingPricePiecePaise > 0) (profitPaise.toDouble() / product.sellingPricePiecePaise.toDouble()) * 100.0 else 0.0
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Selling: ${CostCalculator.formatPaiseToRupees(product.sellingPricePiecePaise)}/pc",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Profit after transport: ${CostCalculator.formatPaiseToRupees(profitPaise)} (${String.format(java.util.Locale.US, "%.1f", margin)}% margin)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (profitPaise >= 0) Color(0xFF75B890) else Color(0xFFE57373)
                                    )
                                }
                            }
                        }
                    }
                }

                // Delivery Charge Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Delivery & Freight Allocation",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = deliveryChargeStr,
                                onValueChange = { deliveryChargeStr = it },
                                label = { Text("Total Delivery Charge (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Distributed proportionally according to each item's purchase value.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Delivery owed to:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        deliveryPaidTo = if (deliveryPaidTo == "SUPPLIER") "CARRIER" else "SUPPLIER"
                                    }
                                ) {
                                    Text(if (deliveryPaidTo == "SUPPLIER") "This Supplier" else "Carrier / Settled")
                                }
                            }
                        }
                    }
                }

                // Payment & Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bill Summary & Payment",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Goods Value:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CostCalculator.formatPaiseToRupees(totalGoodsValuePaise), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery Charge:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CostCalculator.formatPaiseToRupees(totalDeliveryPaise))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Landed Stock Cost:", fontWeight = FontWeight.Bold)
                                Text(CostCalculator.formatPaiseToRupees(totalLandedCostPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = amountPaidStr,
                                onValueChange = { amountPaidStr = it },
                                label = { Text("Amount Paid Now (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Remaining Owed to Supplier:", fontWeight = FontWeight.Bold)
                                Text(
                                    CostCalculator.formatPaiseToRupees(remainingPayablePaise),
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingPayablePaise > 0) Color(0xFFE57373) else Color(0xFF75B890)
                                )
                            }
                        }
                    }
                }

                // Save Goods Received Button
                item {
                    Button(
                        onClick = {
                            val supplier = selectedSupplier ?: return@Button
                            val validItems = items.mapNotNull { line ->
                                val qty = line.quantityStr.toIntOrNull() ?: 0
                                val baseQty = if (line.isPack) qty * line.packSize else qty
                                val pricePaise = ((line.unitPriceStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                                if (line.productId.isNotBlank() && baseQty > 0) {
                                    PurchaseItemParam(
                                        productId = line.productId,
                                        quantityBase = baseQty,
                                        unitPurchasePricePaise = pricePaise
                                    )
                                } else null
                            }

                            if (validItems.isEmpty()) return@Button

                            viewModel.receiveGoods(
                                supplierId = supplier.id,
                                invoiceNumber = invoiceNumber.trim().ifBlank { null },
                                items = validItems,
                                totalDeliveryPaise = totalDeliveryPaise,
                                deliveryPaidTo = deliveryPaidTo,
                                amountPaidPaise = amountPaidPaise,
                                paymentMethod = paymentMethod,
                                notes = notes.trim().ifBlank { null },
                                onDone = onNavigateBack
                            )
                        },
                        enabled = selectedSupplier != null && lineInputs.isNotEmpty(),
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
                            text = "Save Goods Received",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}