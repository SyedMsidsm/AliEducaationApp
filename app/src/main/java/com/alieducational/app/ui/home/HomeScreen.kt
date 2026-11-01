package com.alieducational.app.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AliViewModel,
    onNavigateToNewBill: () -> Unit,
    onNavigateToAddStock: () -> Unit,
    onNavigateToRecordPayment: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToAccounts: (Int) -> Unit, // 0 for suppliers, 1 for customers
    onNavigateToProfit: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToCashTracker: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPigmy: () -> Unit = {},
    onNavigateToBillHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shopName by viewModel.shopName.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val totalStockValue by viewModel.totalStockValuePaise.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val weOweSuppliers by viewModel.totalWeOweSuppliersPaise.collectAsState()
    val customersOweUs by viewModel.totalCustomersOweUsPaise.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val recentMovements by viewModel.recentMovements.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Logo & Shop Name
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isDarkTheme) R.drawable.ali_logo_dark else R.drawable.ali_logo_light
                        ),
                        contentDescription = "Ali Educational Monogram",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(42.dp)
                            .width(80.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (appLanguage == "ur") stringResource(R.string.app_name) else shopName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.home_stationery_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // 3 Prominent Everyday Actions: New Bill, Add Stock, Record Payment
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryActionButton(
                        title = stringResource(R.string.action_new_bill),
                        icon = Icons.Filled.PointOfSale,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToNewBill
                    )
                    PrimaryActionButton(
                        title = stringResource(R.string.action_add_stock),
                        icon = Icons.Filled.Add,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAddStock
                    )
                    PrimaryActionButton(
                        title = stringResource(R.string.action_record_payment),
                        icon = Icons.Filled.Payment,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRecordPayment
                    )
                }
            }

            // 4 Metrics Cards (2x2 grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = stringResource(R.string.home_stock_value),
                            value = CostCalculator.formatPaiseToRupees(totalStockValue ?: 0L),
                            icon = Icons.Filled.Inventory2,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToStock
                        )
                        MetricCard(
                            title = stringResource(R.string.home_low_stock),
                            value = stringResource(R.string.home_items_count, lowStockCount),
                            icon = Icons.Filled.Warning,
                            iconTint = if (lowStockCount > 0) Color(0xFFE4B765) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToStock
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = stringResource(R.string.home_we_owe),
                            value = CostCalculator.formatPaiseToRupees(weOweSuppliers ?: 0L),
                            icon = Icons.Filled.ArrowUpward,
                            iconTint = Color(0xFFE57373),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToAccounts(0) }
                        )
                        MetricCard(
                            title = stringResource(R.string.home_customers_owe),
                            value = CostCalculator.formatPaiseToRupees(customersOweUs ?: 0L),
                            icon = Icons.Filled.ArrowDownward,
                            iconTint = Color(0xFF75B890),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToAccounts(1) }
                        )
                    }
                }
            }

            // Backup Status Card with "Back up now"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val backupText = if (lastBackupTime > 0L) {
                                    val format = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                    "Last backup: ${format.format(Date(lastBackupTime))}"
                                } else {
                                    stringResource(R.string.home_backup_status)
                                }
                                Text(
                                    text = backupText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.home_stored_safely),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.createBackup() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text(text = stringResource(R.string.home_backup_now), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Reports & Settings Section (Accessible directly from Home)
            item {
                Text(
                    text = stringResource(R.string.home_reports_and_tools),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolQuickCard(
                            title = stringResource(R.string.home_profit_analysis),
                            icon = Icons.Filled.TrendingUp,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToProfit
                        )
                        ToolQuickCard(
                            title = stringResource(R.string.home_expenses),
                            icon = Icons.Filled.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToExpenses
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolQuickCard(
                            title = stringResource(R.string.home_backup_restore),
                            icon = Icons.Filled.Backup,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToBackup
                        )
                        ToolQuickCard(
                            title = "Cash In / Out Tracker",
                            icon = Icons.Filled.PointOfSale,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCashTracker
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolQuickCard(
                            title = "Pigmy Daily Savings",
                            iconRes = R.drawable.ic_pigmy_coin,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPigmy
                        )
                        ToolQuickCard(
                            title = "Bill History",
                            icon = Icons.Filled.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToBillHistory
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
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
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ToolQuickCard(
    title: String,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(58.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Pigmy savings",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
