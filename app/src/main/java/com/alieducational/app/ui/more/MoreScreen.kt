package com.alieducational.app.ui.more

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.ui.AliViewModel

@Composable
fun MoreScreen(
    viewModel: AliViewModel,
    onNavigateToProfit: () -> Unit,
    onNavigateToDailySales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reports, Tools & Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Secondary tasks kept off the home screen to preserve everyday simplicity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reports & Analysis Group
            Text(
                text = "REPORTS & FINANCIALS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            MoreMenuItem(
                title = "Gross Profit Analysis",
                subtitle = "Expected vs. actual gross profit after weighted costs",
                icon = Icons.Filled.Assessment,
                onClick = onNavigateToProfit
            )

            MoreMenuItem(
                title = "Daily Sales & Records",
                subtitle = "Daily net sales, wholesale discounts, and money totals",
                icon = Icons.Filled.PointOfSale,
                onClick = onNavigateToDailySales
            )

            MoreMenuItem(
                title = "Other Money & Expenses",
                subtitle = "Shop rent, electricity, salaries, and non-sale deposits",
                icon = Icons.Filled.MoneyOff,
                onClick = onNavigateToExpenses
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Management & Tools Group
            Text(
                text = "DATA & BACKUP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            MoreMenuItem(
                title = "Import Product Catalog",
                subtitle = "Import Excel (.xlsx / .xls) or CSV spreadsheet",
                icon = Icons.Filled.FileDownload,
                onClick = onNavigateToImport
            )

            MoreMenuItem(
                title = "Backup & Restore",
                subtitle = "Save encrypted local backup ZIP and restore data safely",
                icon = Icons.Filled.Backup,
                onClick = onNavigateToBackup
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App Settings
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            MoreMenuItem(
                title = "Settings",
                subtitle = "Theme, language (English / Urdu), receipts, and biometric lock",
                icon = Icons.Filled.Settings,
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}