package com.alieducational.app.ui.pigmy

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.ui.AliViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PigmySavingsScreen(
    viewModel: AliViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDeposits by viewModel.allPigmyDeposits.collectAsState()
    val totalRecordedPaise by viewModel.totalPigmyDepositsPaise.collectAsState()
    val usualAmountPaise by viewModel.pigmyUsualAmountPaise.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditUsualDialog by remember { mutableStateOf(false) }
    var depositToVoid by remember { mutableStateOf<PigmyDepositEntity?>(null) }
    var voidReason by remember { mutableStateOf("") }

    // Date filters: ALL, THIS_MONTH, TODAY
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredDeposits = remember(allDeposits, selectedFilter) {
        val now = Calendar.getInstance()
        when (selectedFilter) {
            "TODAY" -> {
                val startOfDay = now.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allDeposits.filter { it.paidTimestamp >= startOfDay }
            }
            "THIS_MONTH" -> {
                val startOfMonth = now.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                allDeposits.filter { it.paidTimestamp >= startOfMonth }
            }
            else -> allDeposits
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pigmy Daily Savings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // Total Recorded Deposits Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Recorded Deposits",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = CostCalculator.formatPaiseToRupees(totalRecordedPaise ?: 0L),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Recorded deposits total • Not a verified bank balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Configured Daily Amount Row
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Usual Daily Amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CostCalculator.formatPaiseToRupees(usualAmountPaise),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = { showEditUsualDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change")
                            }
                        }
                    }
                }

                // Primary Action Button: Add Deposit
                item {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Record Today's Deposit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${allDeposits.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "THIS_MONTH",
                            onClick = { selectedFilter = "THIS_MONTH" },
                            label = { Text("This Month") }
                        )
                        FilterChip(
                            selected = selectedFilter == "TODAY",
                            onClick = { selectedFilter = "TODAY" },
                            label = { Text("Today") }
                        )
                    }
                }

                // Deposit Records List
                if (filteredDeposits.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No deposits recorded in this period",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredDeposits, key = { it.id }) { deposit ->
                        DepositItemCard(
                            deposit = deposit,
                            onVoid = { depositToVoid = deposit }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Add Deposit Dialog
    if (showAddDialog) {
        AddPigmyDepositDialog(
            defaultAmountPaise = usualAmountPaise,
            existingDeposits = allDeposits,
            onDismiss = { showAddDialog = false },
            onConfirm = { amountPaise, source, notes ->
                viewModel.recordPigmyDeposit(
                    amountPaise = amountPaise,
                    paymentSource = source,
                    notes = notes,
                    onDone = { showAddDialog = false }
                )
            }
        )
    }

    // Edit Usual Amount Dialog
    if (showEditUsualDialog) {
        var newAmountStr by remember { mutableStateOf((usualAmountPaise / 100).toString()) }
        AlertDialog(
            onDismissRequest = { showEditUsualDialog = false },
            title = { Text("Configure Usual Daily Amount") },
            text = {
                Column {
                    Text(
                        text = "This amount will be prefilled each time you record a deposit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newAmountStr,
                        onValueChange = { newAmountStr = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rupees = newAmountStr.toDoubleOrNull() ?: 0.0
                        if (rupees > 0) {
                            viewModel.setPigmyUsualAmount((rupees * 100).toLong())
                            showEditUsualDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Void Deposit Dialog
    if (depositToVoid != null) {
        val target = depositToVoid!!
        AlertDialog(
            onDismissRequest = { depositToVoid = null },
            title = { Text("Void Pigmy Deposit?") },
            text = {
                Column {
                    Text(
                        text = "Voiding this deposit will reverse the ${CostCalculator.formatPaiseToRupees(target.amountPaise)} entry and any linked shop cash outflow.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for voiding *") },
                        placeholder = { Text("e.g. Entry mistake, cancelled deposit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.voidPigmyDeposit(target.id, voidReason.trim()) {
                                depositToVoid = null
                                voidReason = ""
                            }
                        }
                    },
                    enabled = voidReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Void Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositToVoid = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DepositItemCard(
    deposit: PigmyDepositEntity,
    onVoid: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(deposit.paidTimestamp) { dateFormat.format(Date(deposit.paidTimestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (deposit.isVoided) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CostCalculator.formatPaiseToRupees(deposit.amountPaise),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (deposit.isVoided) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (deposit.isVoided) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isCash = deposit.paymentSource == "CASH"
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (deposit.isVoided) MaterialTheme.colorScheme.errorContainer
                                else if (isCash) Color(0xFFE4B765).copy(alpha = 0.2f)
                                else Color(0xFF75B890).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (deposit.isVoided) "VOIDED"
                            else if (isCash) "Shop Cash Outflow"
                            else "Bank / Direct",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deposit.isVoided) MaterialTheme.colorScheme.error
                            else if (isCash) Color(0xFFE4B765)
                            else Color(0xFF75B890)
                        )
                    }

                    if (!deposit.isVoided) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onVoid,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = "Void",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!deposit.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${deposit.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (deposit.isVoided && !deposit.voidReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Void reason: ${deposit.voidReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddPigmyDepositDialog(
    defaultAmountPaise: Long,
    existingDeposits: List<PigmyDepositEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, source: String, notes: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf((defaultAmountPaise / 100).toString()) }
    var paymentSource by remember { mutableStateOf("CASH") } // CASH or BANK
    var notes by remember { mutableStateOf("") }

    // Check if there is already an active deposit today
    val hasDepositToday = remember(existingDeposits) {
        val now = Calendar.getInstance()
        val startOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        existingDeposits.any { !it.isVoided && it.paidTimestamp >= startOfDay }
    }

    val amountPaise = ((amountStr.toDoubleOrNull() ?: 0.0) * 100).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Pigmy Savings Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasDepositToday) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "A deposit was already recorded today. You can still proceed if this is an intentional second deposit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Deposit Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Source:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paymentSource == "CASH",
                        onClick = { paymentSource = "CASH" },
                        label = { Text("Shop Cash (Outflow)") }
                    )
                    FilterChip(
                        selected = paymentSource == "BANK",
                        onClick = { paymentSource = "BANK" },
                        label = { Text("Direct / Bank") }
                    )
                }

                if (paymentSource == "CASH") {
                    Text(
                        text = "• Shop cash outflow will be automatically recorded in Cash In/Out Tracker under Savings Transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Note (Optional)") },
                    placeholder = { Text("e.g. Receipt #123, Agent name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0) {
                        onConfirm(amountPaise, paymentSource, notes.trim().ifBlank { null })
                    }
                },
                enabled = amountPaise > 0
            ) {
                Text("Confirm Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
