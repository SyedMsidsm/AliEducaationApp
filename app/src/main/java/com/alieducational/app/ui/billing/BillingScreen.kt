package com.alieducational.app.ui.billing

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.alieducational.app.data.local.entity.ServiceDefinitionEntity
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel

@Composable
fun BillingScreen(
    viewModel: AliViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToBillHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val favoriteProducts by viewModel.favoriteProducts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allHeldBills by viewModel.allHeldBills.collectAsState()
    val activeServices by viewModel.activeServices.collectAsState()
    val deviceRole by viewModel.deviceRole.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val pairedOwnerHost by viewModel.pairedOwnerHost.collectAsState()

    var activeTab by remember { mutableStateOf("PRODUCTS") } // PRODUCTS or SERVICES
    var showEnterServiceDialog by remember { mutableStateOf(false) }
    var selectedServiceForJob by remember { mutableStateOf<ServiceDefinitionEntity?>(null) }
    var showAddServiceDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, FAVOURITES, CATEGORY, RECENT
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showHeldBillsDialog by remember { mutableStateOf(false) }

    // Filter products
    val displayedProducts = remember(allProducts, favoriteProducts, searchQuery, selectedFilter, selectedCategory) {
        var list = when (selectedFilter) {
            "FAVOURITES" -> favoriteProducts
            "CATEGORY" -> if (selectedCategory != null) allProducts.filter { it.category == selectedCategory } else allProducts
            else -> allProducts
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        (it.sku?.lowercase()?.contains(q) == true) ||
                        (it.brand?.lowercase()?.contains(q) == true) ||
                        (it.urduName?.contains(q) == true)
            }
        }
        list
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Shop Branding Bar
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val logoRes = if (isDarkTheme) R.drawable.ali_logo_dark else R.drawable.ali_logo_light
                            Image(
                                painter = painterResource(id = logoRes),
                                contentDescription = "Ali Educational Logo",
                                modifier = Modifier
                                    .height(42.dp)
                                    .width(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ali Educational",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = stringResource(R.string.billing_header_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // History & Held Bills Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateToBillHistory,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ReceiptLong,
                                    contentDescription = "Bill History",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            if (allHeldBills.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { showHeldBillsDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayCircleOutline,
                                            contentDescription = "Held Bills",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${allHeldBills.size} ${stringResource(R.string.billing_held_tag)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (billingState.items.isNotEmpty()) {
                                IconButton(onClick = { viewModel.holdCurrentBill() }) {
                                    Icon(
                                        Icons.Filled.PauseCircleOutline,
                                        contentDescription = "Hold bill",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Two-Phone Sync Status Card (for Billing Phone)
                if (deviceRole == "BILLING") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (pendingSyncCount > 0) Color(0xFF332A15) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Billing Device Mode",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (pendingSyncCount > 0) "$pendingSyncCount pending sync" else "All bills synced",
                                            fontSize = 11.sp,
                                            color = if (pendingSyncCount > 0) Color(0xFFE4B765) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (pairedOwnerHost.isNotBlank()) {
                                            viewModel.syncWithMainPhone(pairedOwnerHost)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Status Badge: Saved on this phone - Offline ready
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = Color(0xFF75B890),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.billing_offline_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section Title: New bill
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.billing_new_bill_title),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.billing_new_bill_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab Switcher: Products vs Services
                item {
                    TabRow(
                        selectedTabIndex = if (activeTab == "PRODUCTS") 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeTab == "PRODUCTS",
                            onClick = { activeTab = "PRODUCTS" },
                            text = { Text("Products", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == "SERVICES",
                            onClick = { activeTab = "SERVICES" },
                            text = { Text("Services", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (activeTab == "PRODUCTS") {
                    // Scan with camera button (Primary Gold Banner Button)
                    item {
                        Button(
                            onClick = onNavigateToScanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.billing_scan_with_camera),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Product Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.billing_search_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Filter Chips: Favourites, Categories, Recent
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == "FAVOURITES",
                                    onClick = {
                                        selectedFilter = if (selectedFilter == "FAVOURITES") "ALL" else "FAVOURITES"
                                    },
                                    label = { Text(stringResource(R.string.billing_filter_favourites)) },
                                    leadingIcon = {
                                        Icon(
                                            if (selectedFilter == "FAVOURITES") Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }

                            items(allCategories) { category ->
                                FilterChip(
                                    selected = selectedFilter == "CATEGORY" && selectedCategory == category,
                                    onClick = {
                                        if (selectedFilter == "CATEGORY" && selectedCategory == category) {
                                            selectedFilter = "ALL"
                                            selectedCategory = null
                                        } else {
                                            selectedFilter = "CATEGORY"
                                            selectedCategory = category
                                        }
                                    },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Quick Add Product Cards (2-column layout or list)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val firstTwo = displayedProducts.take(2)
                            firstTwo.forEach { prod ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.addItemToBill(prod, "PIECE", 1) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        val priceStr = CostCalculator.formatPaiseToRupees(prod.sellingPricePiecePaise)
                                        Text(
                                            text = "One piece · $priceStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.billing_add_btn),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // SERVICES TAB
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Services",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = { showAddServiceDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Service", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(activeServices) { service ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServiceForJob = service
                                    showEnterServiceDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when {
                                        service.name.contains("Print", ignoreCase = true) -> Icons.Filled.Print
                                        service.name.contains("Copy", ignoreCase = true) -> Icons.Filled.ContentCopy
                                        service.name.contains("Sticker", ignoreCase = true) -> Icons.Filled.Label
                                        else -> Icons.Filled.Print
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Unit: ${service.unitLabel} · Variable job rate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Filled.Add, contentDescription = "Add Job", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Section: In this bill
                if (billingState.items.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.billing_in_this_bill_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(billingState.items, key = { it.lineId }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.productName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val lineSubtitle = if (item.lineType == "SERVICE") {
                                            val opt = if (!item.serviceOption.isNullOrBlank()) " (${item.serviceOption})" else ""
                                            val unit = item.unitLabel ?: "unit"
                                            val unitPrice = CostCalculator.formatPaiseToRupees(item.unitPricePaise)
                                            "${item.quantity} $unit$opt · $unitPrice each"
                                        } else {
                                            val unitLabel = if (item.unitType == "PACK") "pack" else "piece"
                                            val unitPrice = CostCalculator.formatPaiseToRupees(item.unitPricePaise)
                                            "${item.quantity} $unitLabel(s) · $unitPrice each"
                                        }
                                        Text(
                                            text = lineSubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CostCalculator.formatPaiseToRupees(item.lineGrossPaise),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.removeItemFromBill(item.lineId) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Remove item",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateItemQuantity(item.lineId, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateItemQuantity(item.lineId, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Space for bottom sticky bar and tabs
                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }

            // Sticky Bottom Action Pill: Review X items · ₹... →
            if (billingState.items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    val count = billingState.totalItemCount
                    val totalStr = CostCalculator.formatPaiseToRupees(billingState.grossTotalPaise)
                    Button(
                        onClick = onNavigateToReview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "${stringResource(R.string.billing_review_action, count)} · $totalStr →",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    if (showHeldBillsDialog) {
        HeldBillsDialog(
            viewModel = viewModel,
            onDismiss = { showHeldBillsDialog = false }
        )
    }

    if (showEnterServiceDialog && selectedServiceForJob != null) {
        EnterServiceJobDialog(
            service = selectedServiceForJob!!,
            onDismiss = {
                showEnterServiceDialog = false
                selectedServiceForJob = null
            },
            onAddJob = { option, quantity, ratePaise ->
                viewModel.addServiceToBill(
                    serviceName = selectedServiceForJob!!.name,
                    option = option,
                    unitLabel = selectedServiceForJob!!.unitLabel,
                    quantity = quantity,
                    unitRatePaise = ratePaise,
                    serviceId = selectedServiceForJob!!.id
                )
                showEnterServiceDialog = false
                selectedServiceForJob = null
            }
        )
    }

    if (showAddServiceDialog) {
        AddReusableServiceDialog(
            onDismiss = { showAddServiceDialog = false },
            onSave = { name, unitLabel ->
                viewModel.addCustomService(name, unitLabel)
                showAddServiceDialog = false
            }
        )
    }
}

@Composable
fun EnterServiceJobDialog(
    service: ServiceDefinitionEntity,
    onDismiss: () -> Unit,
    onAddJob: (option: String?, quantity: Int, ratePaise: Long) -> Unit
) {
    val isPrinting = service.name.contains("Print", ignoreCase = true)
    var selectedOption by remember { mutableStateOf(if (isPrinting) "A4 B&W" else null) }
    var quantityText by remember { mutableStateOf("1") }
    var rateText by remember { mutableStateOf("") } // Starts BLANK per requirement

    val qty = quantityText.toIntOrNull() ?: 0
    val rateDouble = rateText.toDoubleOrNull() ?: 0.0
    val totalAmount = qty * rateDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${service.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isPrinting) {
                    Text("Print Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedOption == "A4 B&W",
                            onClick = {
                                if (selectedOption != "A4 B&W") {
                                    selectedOption = "A4 B&W"
                                    rateText = "" // Reset rate on toggle per requirement
                                }
                            },
                            label = { Text("A4 B&W") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedOption == "A4 Colour",
                            onClick = {
                                if (selectedOption != "A4 Colour") {
                                    selectedOption = "A4 Colour"
                                    rateText = "" // Reset rate on toggle per requirement
                                }
                            },
                            label = { Text("A4 Colour") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) quantityText = it },
                    label = { Text("Quantity (${service.unitLabel})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate per ${service.unitLabel} (₹) *") },
                    placeholder = { Text("Enter rate for this job") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (qty > 0 && rateDouble > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Job Total:", fontWeight = FontWeight.SemiBold)
                            Text("₹${String.format(java.util.Locale.ENGLISH, "%.2f", totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ratePaise = (rateDouble * 100).toLong()
                    if (qty > 0 && ratePaise > 0) {
                        onAddJob(selectedOption, qty, ratePaise)
                    }
                },
                enabled = qty > 0 && rateDouble > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add to Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddReusableServiceDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, unitLabel: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unitLabel by remember { mutableStateOf("pages") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reusable Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name * (e.g. Lamination, Spiral Binding)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it },
                    label = { Text("Unit Label * (e.g. sheets, books, items)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && unitLabel.isNotBlank()) {
                        onSave(name.trim(), unitLabel.trim())
                    }
                },
                enabled = name.isNotBlank() && unitLabel.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Service")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
