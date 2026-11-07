package com.alieducational.app.ui.pigmy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alieducational.app.R
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.PigmyWithdrawalEntity
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
    val allSchemes by viewModel.allPigmySchemes.collectAsState()
    val allDeposits by viewModel.allPigmyDeposits.collectAsState()
    val allWithdrawals by viewModel.allPigmyWithdrawals.collectAsState()

    var showAddSchemeDialog by remember { mutableStateOf(false) }
    var schemeToEdit by remember { mutableStateOf<PigmySchemeEntity?>(null) }
    var schemeForDeposit by remember { mutableStateOf<PigmySchemeEntity?>(null) }
    var schemeForWithdrawal by remember { mutableStateOf<PigmySchemeEntity?>(null) }
    var schemeForOpeningBalance by remember { mutableStateOf<PigmySchemeEntity?>(null) }

    var depositToEdit by remember { mutableStateOf<PigmyDepositEntity?>(null) }
    var depositToVoid by remember { mutableStateOf<PigmyDepositEntity?>(null) }
    var withdrawalToEdit by remember { mutableStateOf<PigmyWithdrawalEntity?>(null) }
    var withdrawalToVoid by remember { mutableStateOf<PigmyWithdrawalEntity?>(null) }

    var voidReason by remember { mutableStateOf("") }
    var showArchivedSchemes by remember { mutableStateOf(false) }

    val displayedSchemes = remember(allSchemes, showArchivedSchemes) {
        if (showArchivedSchemes) allSchemes else allSchemes.filter { !it.isArchived }
    }

    // Combined summary calculations
    val combinedOpeningPaise = remember(allSchemes) {
        allSchemes.sumOf { it.openingBalancePaise }
    }
    val combinedRecordedDepositsPaise = remember(allDeposits) {
        allDeposits.filter { !it.isVoided }.sumOf { it.amountPaise }
    }
    val combinedWithdrawalsPaise = remember(allWithdrawals) {
        allWithdrawals.filter { !it.isVoided }.sumOf { it.amountPaise }
    }
    val combinedTrackedSavingsPaise = combinedOpeningPaise + combinedRecordedDepositsPaise - combinedWithdrawalsPaise

    val todayCombinedDepositsPaise = remember(allDeposits) {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        allDeposits.filter { !it.isVoided && it.paidTimestamp >= startOfDay }.sumOf { it.amountPaise }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pigmy_coin),
                            contentDescription = "Pigmy savings",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pigmy Savings", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddSchemeDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Scheme", fontWeight = FontWeight.Bold)
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
                // Combined Summary Card at Top
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pigmy_coin),
                                    contentDescription = "Pigmy savings",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Tracked Savings Balance",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Combined savings • Returned savings without interest",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = CostCalculator.formatPaiseToRupees(combinedTrackedSavingsPaise),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Deposits", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(combinedRecordedDepositsPaise),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Withdrawals", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                    Text(
                                        CostCalculator.formatPaiseToRupees(combinedWithdrawalsPaise),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                                if (combinedOpeningPaise > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Opening", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            CostCalculator.formatPaiseToRupees(combinedOpeningPaise),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Today's Deposits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            CostCalculator.formatPaiseToRupees(todayCombinedDepositsPaise),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Scheme Filter Toggle Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pigmy Schemes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showArchivedSchemes,
                                onClick = { showArchivedSchemes = false },
                                label = { Text("Active") }
                            )
                            FilterChip(
                                selected = showArchivedSchemes,
                                onClick = { showArchivedSchemes = true },
                                label = { Text("All (${allSchemes.size})") }
                            )
                        }
                    }
                }

                // Scheme Cards
                if (displayedSchemes.isEmpty()) {
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
                                    text = "No schemes found. Tap 'Add Scheme' above to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displayedSchemes, key = { it.id }) { scheme ->
                        val schemeDeposits = remember(allDeposits, scheme.id) {
                            allDeposits.filter { it.accountSchemeId == scheme.id }
                        }
                        val schemeWithdrawals = remember(allWithdrawals, scheme.id) {
                            allWithdrawals.filter { it.accountSchemeId == scheme.id }
                        }

                        PigmySchemeCard(
                            scheme = scheme,
                            deposits = schemeDeposits,
                            withdrawals = schemeWithdrawals,
                            onAddDeposit = { schemeForDeposit = scheme },
                            onAddWithdrawal = { schemeForWithdrawal = scheme },
                            onSetOpeningBalance = { schemeForOpeningBalance = scheme },
                            onEditScheme = { schemeToEdit = scheme },
                            onArchiveToggle = { isArchived ->
                                viewModel.setPigmySchemeArchived(scheme.id, isArchived)
                            },
                            onEditDeposit = { deposit -> depositToEdit = deposit },
                            onVoidDeposit = { deposit -> depositToVoid = deposit },
                            onEditWithdrawal = { withdrawal -> withdrawalToEdit = withdrawal },
                            onVoidWithdrawal = { withdrawal -> withdrawalToVoid = withdrawal }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialog: Add Scheme
    if (showAddSchemeDialog) {
        AddOrEditSchemeDialog(
            existingSchemes = allSchemes,
            initialScheme = null,
            onDismiss = { showAddSchemeDialog = false },
            onConfirm = { name, usualAmountPaise ->
                viewModel.createPigmyScheme(name, usualAmountPaise) {
                    showAddSchemeDialog = false
                }
            }
        )
    }

    // Dialog: Edit Scheme
    schemeToEdit?.let { scheme ->
        AddOrEditSchemeDialog(
            existingSchemes = allSchemes,
            initialScheme = scheme,
            onDismiss = { schemeToEdit = null },
            onConfirm = { name, usualAmountPaise ->
                viewModel.updatePigmyScheme(scheme.id, name, usualAmountPaise) {
                    schemeToEdit = null
                }
            }
        )
    }

    // Dialog: Set Opening Balance
    schemeForOpeningBalance?.let { scheme ->
        SetOpeningBalanceDialog(
            scheme = scheme,
            onDismiss = { schemeForOpeningBalance = null },
            onConfirm = { openingPaise, date ->
                viewModel.updatePigmySchemeOpeningBalance(scheme.id, openingPaise, date) {
                    schemeForOpeningBalance = null
                }
            }
        )
    }

    // Dialog: Record Deposit for Scheme
    schemeForDeposit?.let { scheme ->
        RecordPigmyDepositDialog(
            scheme = scheme,
            existingDeposits = allDeposits.filter { it.accountSchemeId == scheme.id },
            onDismiss = { schemeForDeposit = null },
            onConfirm = { amountPaise, source, notes, timestamp ->
                viewModel.recordPigmyDeposit(
                    schemeId = scheme.id,
                    amountPaise = amountPaise,
                    paymentSource = source,
                    notes = notes,
                    paidTimestamp = timestamp,
                    onDone = { schemeForDeposit = null }
                )
            }
        )
    }

    // Dialog: Record Withdrawal for Scheme
    schemeForWithdrawal?.let { scheme ->
        val schemeDeposits = allDeposits.filter { it.accountSchemeId == scheme.id && !it.isVoided }
        val schemeWithdrawals = allWithdrawals.filter { it.accountSchemeId == scheme.id && !it.isVoided }
        val currentTrackedBalance = scheme.openingBalancePaise + schemeDeposits.sumOf { it.amountPaise } - schemeWithdrawals.sumOf { it.amountPaise }

        RecordPigmyWithdrawalDialog(
            scheme = scheme,
            currentTrackedBalancePaise = currentTrackedBalance,
            onDismiss = { schemeForWithdrawal = null },
            onConfirm = { amountPaise, destination, notes, timestamp ->
                viewModel.recordPigmyWithdrawal(
                    schemeId = scheme.id,
                    amountPaise = amountPaise,
                    destination = destination,
                    notes = notes,
                    withdrawalTimestamp = timestamp,
                    onDone = { schemeForWithdrawal = null },
                    onError = { /* handled in dialog */ }
                )
            }
        )
    }

    // Dialog: Edit Existing Deposit
    depositToEdit?.let { deposit ->
        val currentScheme = allSchemes.find { it.id == deposit.accountSchemeId }
        EditPigmyDepositDialog(
            deposit = deposit,
            allSchemes = allSchemes,
            currentScheme = currentScheme,
            onDismiss = { depositToEdit = null },
            onConfirm = { schemeId, amountPaise, source, notes, timestamp ->
                viewModel.updatePigmyDeposit(
                    depositId = deposit.id,
                    schemeId = schemeId,
                    amountPaise = amountPaise,
                    paymentSource = source,
                    notes = notes,
                    paidTimestamp = timestamp,
                    onDone = { depositToEdit = null }
                )
            }
        )
    }

    // Dialog: Edit Existing Withdrawal
    withdrawalToEdit?.let { withdrawal ->
        val currentScheme = allSchemes.find { it.id == withdrawal.accountSchemeId }
        val schemeDeposits = allDeposits.filter { it.accountSchemeId == withdrawal.accountSchemeId && !it.isVoided }
        val otherWithdrawals = allWithdrawals.filter { it.accountSchemeId == withdrawal.accountSchemeId && !it.isVoided && it.id != withdrawal.id }
        val maxAvailable = (currentScheme?.openingBalancePaise ?: 0L) + schemeDeposits.sumOf { it.amountPaise } - otherWithdrawals.sumOf { it.amountPaise }

        EditPigmyWithdrawalDialog(
            withdrawal = withdrawal,
            currentScheme = currentScheme,
            maxAvailablePaise = maxAvailable,
            onDismiss = { withdrawalToEdit = null },
            onConfirm = { amountPaise, dest, notes, timestamp ->
                viewModel.updatePigmyWithdrawal(
                    withdrawalId = withdrawal.id,
                    schemeId = withdrawal.accountSchemeId,
                    amountPaise = amountPaise,
                    destination = dest,
                    notes = notes,
                    withdrawalTimestamp = timestamp,
                    onDone = { withdrawalToEdit = null }
                )
            }
        )
    }

    // Dialog: Void / Cancel Deposit
    depositToVoid?.let { deposit ->
        AlertDialog(
            onDismissRequest = { depositToVoid = null },
            title = { Text("Cancel Pigmy Deposit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to cancel this deposit of ${CostCalculator.formatPaiseToRupees(deposit.amountPaise)}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (deposit.paymentSource == "CASH" && deposit.linkedExpenseId != null) {
                        Text(
                            text = "• The linked Shop Cash Outflow (Savings Transfer) in Cash In/Out Tracker will be automatically reversed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for cancellation *") },
                        placeholder = { Text("e.g. Accidental entry, Agent returned money") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.voidPigmyDeposit(deposit.id, voidReason.trim()) {
                                depositToVoid = null
                                voidReason = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = voidReason.isNotBlank()
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositToVoid = null; voidReason = "" }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Dialog: Void / Cancel Withdrawal
    withdrawalToVoid?.let { withdrawal ->
        AlertDialog(
            onDismissRequest = { withdrawalToVoid = null },
            title = { Text("Cancel Pigmy Withdrawal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to cancel this withdrawal of ${CostCalculator.formatPaiseToRupees(withdrawal.amountPaise)}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (withdrawal.destination == "CASH" && withdrawal.linkedExpenseId != null) {
                        Text(
                            text = "• The linked Shop Cash Inflow (Savings Transfer) in Cash In/Out Tracker will be automatically reversed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for cancellation *") },
                        placeholder = { Text("e.g. Returned to scheme, Error correction") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.voidPigmyWithdrawal(withdrawal.id, voidReason.trim()) {
                                withdrawalToVoid = null
                                voidReason = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = voidReason.isNotBlank()
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { withdrawalToVoid = null; voidReason = "" }) {
                    Text("Go Back")
                }
            }
        )
    }
}

sealed class PigmyActivityItem {
    abstract val timestamp: Long
    abstract val isVoided: Boolean

    data class Deposit(val deposit: PigmyDepositEntity) : PigmyActivityItem() {
        override val timestamp: Long get() = deposit.paidTimestamp
        override val isVoided: Boolean get() = deposit.isVoided
    }

    data class Withdrawal(val withdrawal: PigmyWithdrawalEntity) : PigmyActivityItem() {
        override val timestamp: Long get() = withdrawal.withdrawalTimestamp
        override val isVoided: Boolean get() = withdrawal.isVoided
    }
}

@Composable
fun PigmySchemeCard(
    scheme: PigmySchemeEntity,
    deposits: List<PigmyDepositEntity>,
    withdrawals: List<PigmyWithdrawalEntity>,
    onAddDeposit: () -> Unit,
    onAddWithdrawal: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    onEditScheme: () -> Unit,
    onArchiveToggle: (Boolean) -> Unit,
    onEditDeposit: (PigmyDepositEntity) -> Unit,
    onVoidDeposit: (PigmyDepositEntity) -> Unit,
    onEditWithdrawal: (PigmyWithdrawalEntity) -> Unit,
    onVoidWithdrawal: (PigmyWithdrawalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var historyFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Deposits, 2: Withdrawals
    var showOverflowMenu by remember { mutableStateOf(false) }

    val activeDepositsTotal = remember(deposits) {
        deposits.filter { !it.isVoided }.sumOf { it.amountPaise }
    }
    val activeWithdrawalsTotal = remember(withdrawals) {
        withdrawals.filter { !it.isVoided }.sumOf { it.amountPaise }
    }
    val trackedBalancePaise = scheme.openingBalancePaise + activeDepositsTotal - activeWithdrawalsTotal

    val allActivities = remember(deposits, withdrawals) {
        val list = mutableListOf<PigmyActivityItem>()
        deposits.forEach { list.add(PigmyActivityItem.Deposit(it)) }
        withdrawals.forEach { list.add(PigmyActivityItem.Withdrawal(it)) }
        list.sortedByDescending { it.timestamp }
    }

    val filteredActivities = remember(allActivities, historyFilterTab) {
        when (historyFilterTab) {
            1 -> allActivities.filterIsInstance<PigmyActivityItem.Deposit>()
            2 -> allActivities.filterIsInstance<PigmyActivityItem.Withdrawal>()
            else -> allActivities
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (scheme.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Scheme Name + Status + Overflow Menu Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (scheme.isArchived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "Archived",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Scheme Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Opening Balance") },
                            leadingIcon = {
                                Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onSetOpeningBalance()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Scheme") },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showOverflowMenu = false
                                onEditScheme()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (scheme.isArchived) "Reactivate Scheme" else "Archive Scheme") },
                            leadingIcon = {
                                Icon(
                                    if (scheme.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onArchiveToggle(!scheme.isArchived)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tracked Balance & Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Tracked Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CostCalculator.formatPaiseToRupees(trackedBalancePaise),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (trackedBalancePaise >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    if (scheme.openingBalancePaise > 0) {
                        Text(
                            text = "Opening: ${CostCalculator.formatPaiseToRupees(scheme.openingBalancePaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Deposits: ${CostCalculator.formatPaiseToRupees(activeDepositsTotal)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Withdrawals: ${CostCalculator.formatPaiseToRupees(activeWithdrawalsTotal)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "Usual daily: ${CostCalculator.formatPaiseToRupees(scheme.usualDailyAmountPaise)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Action Buttons: Record Deposit & Record Withdrawal (equal weight, min height 48dp, single line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddDeposit,
                    enabled = !scheme.isArchived,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deposit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onAddWithdrawal,
                    enabled = !scheme.isArchived && trackedBalancePaise > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Withdraw",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated History Row
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide history (${allActivities.size} entries)" else "View history (${allActivities.size} entries)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Activity History
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // History Filter Tabs
                    TabRow(
                        selectedTabIndex = historyFilterTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = historyFilterTab == 0,
                            onClick = { historyFilterTab = 0 },
                            text = { Text("All (${allActivities.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = historyFilterTab == 1,
                            onClick = { historyFilterTab = 1 },
                            text = { Text("Deposits (${deposits.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = historyFilterTab == 2,
                            onClick = { historyFilterTab = 2 },
                            text = { Text("Withdrawals (${withdrawals.size})", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredActivities.isEmpty()) {
                        Text(
                            text = "No records for this scheme in the selected filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filteredActivities.forEach { item ->
                            when (item) {
                                is PigmyActivityItem.Deposit -> {
                                    DepositRow(
                                        deposit = item.deposit,
                                        onEdit = { onEditDeposit(item.deposit) },
                                        onVoid = { onVoidDeposit(item.deposit) }
                                    )
                                }
                                is PigmyActivityItem.Withdrawal -> {
                                    WithdrawalRow(
                                        withdrawal = item.withdrawal,
                                        onEdit = { onEditWithdrawal(item.withdrawal) },
                                        onVoid = { onVoidWithdrawal(item.withdrawal) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepositRow(
    deposit: PigmyDepositEntity,
    onEdit: () -> Unit,
    onVoid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(deposit.paidTimestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (deposit.isVoided) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+ ${CostCalculator.formatPaiseToRupees(deposit.amountPaise)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (deposit.isVoided) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF2E7D32),
                        textDecoration = if (deposit.isVoided) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (deposit.paymentSource == "CASH") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                        )
                    ) {
                        Text(
                            text = if (deposit.paymentSource == "CASH") "Deposit (Cash Outflow)" else "Deposit (Direct / Bank)",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (deposit.paymentSource == "CASH") Color(0xFF2E7D32) else Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!deposit.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${deposit.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deposit.isVoided) {
                    Text(
                        text = "CANCELLED: ${deposit.voidReason ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!deposit.isVoided) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onVoid, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Block, contentDescription = "Cancel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawalRow(
    withdrawal: PigmyWithdrawalEntity,
    onEdit: () -> Unit,
    onVoid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(withdrawal.withdrawalTimestamp))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (withdrawal.isVoided) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "- ${CostCalculator.formatPaiseToRupees(withdrawal.amountPaise)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (withdrawal.isVoided) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD97706),
                        textDecoration = if (withdrawal.isVoided) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (withdrawal.destination == "CASH") Color(0xFFFFF3E0) else Color(0xFFE3F2FD)
                        )
                    ) {
                        Text(
                            text = if (withdrawal.destination == "CASH") "Withdrawal (Shop Cash In)" else "Withdrawal (Bank Account)",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (withdrawal.destination == "CASH") Color(0xFFD97706) else Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!withdrawal.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${withdrawal.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (withdrawal.isVoided) {
                    Text(
                        text = "CANCELLED: ${withdrawal.voidReason ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!withdrawal.isVoided) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onVoid, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Block, contentDescription = "Cancel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditSchemeDialog(
    existingSchemes: List<PigmySchemeEntity>,
    initialScheme: PigmySchemeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, usualAmountPaise: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialScheme?.name ?: "") }
    var amountStr by remember { mutableStateOf(if (initialScheme != null) (initialScheme.usualDailyAmountPaise / 100).toString() else "100") }

    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
    val isDuplicateName = remember(name, initialScheme) {
        existingSchemes.any { it.name.equals(name.trim(), ignoreCase = true) && it.id != initialScheme?.id }
    }

    val isValid = name.isNotBlank() && amountPaise > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialScheme == null) "Add Pigmy Scheme" else "Edit Pigmy Scheme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scheme Name *") },
                    placeholder = { Text("e.g. Bank Scheme A, Daily Deposit 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isDuplicateName) {
                    Text(
                        text = "Warning: A scheme with this name already exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Usual Daily Deposit (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(name.trim(), amountPaise)
                    }
                },
                enabled = isValid
            ) {
                Text(if (initialScheme == null) "Create Scheme" else "Save Changes")
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
fun SetOpeningBalanceDialog(
    scheme: PigmySchemeEntity,
    onDismiss: () -> Unit,
    onConfirm: (openingPaise: Long, date: Long?) -> Unit
) {
    var amountStr by remember { mutableStateOf(if (scheme.openingBalancePaise > 0) (scheme.openingBalancePaise / 100).toString() else "0") }
    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Starting Savings: ${scheme.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "• Confirmed savings accumulated with bank/agent before tracking in this app.\n• This establishes starting savings balance only and will NOT create any cash transaction or count as new deposit income.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Confirmed Starting Savings (₹) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(maxOf(0L, amountPaise), System.currentTimeMillis())
                }
            ) {
                Text("Save Opening Balance")
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
fun RecordPigmyDepositDialog(
    scheme: PigmySchemeEntity,
    existingDeposits: List<PigmyDepositEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, source: String, notes: String?, timestamp: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf((scheme.usualDailyAmountPaise / 100).toString()) }
    var paymentSource by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    var entryTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entryTimestamp))

    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pigmy_coin),
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deposit: ${scheme.name}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Deposit Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date & Time:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = entryTimestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                TimePickerDialog(context, { _, h, min ->
                                    c.set(Calendar.HOUR_OF_DAY, h)
                                    c.set(Calendar.MINUTE, min)
                                    entryTimestamp = c.timeInMillis
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, fontSize = 11.5.sp)
                    }
                }

                Text("Payment Funding Source:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "• Shop cash outflow will be automatically linked in Cash In/Out Tracker as a Savings Transfer (does not reduce shop profit).",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "• Direct/Bank payment does not reduce physical shop cash.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Note (Optional)") },
                    placeholder = { Text("e.g. Receipt #, Agent name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0) {
                        onConfirm(amountPaise, paymentSource, notes.trim().ifBlank { null }, entryTimestamp)
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

@Composable
fun RecordPigmyWithdrawalDialog(
    scheme: PigmySchemeEntity,
    currentTrackedBalancePaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, destination: String, notes: String?, timestamp: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("CASH") } // CASH, BANK
    var notes by remember { mutableStateOf("") }
    var entryTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entryTimestamp))

    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
    val exceedsBalance = amountPaise > currentTrackedBalancePaise

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pigmy_coin),
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Withdraw: ${scheme.name}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Available Tracked Savings: ${CostCalculator.formatPaiseToRupees(currentTrackedBalancePaise)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Withdrawal Amount (₹) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = exceedsBalance,
                    modifier = Modifier.fillMaxWidth()
                )

                if (exceedsBalance) {
                    Text(
                        text = "⚠️ Withdrawal (${CostCalculator.formatPaiseToRupees(amountPaise)}) exceeds tracked savings (${CostCalculator.formatPaiseToRupees(currentTrackedBalancePaise)}). Please review history or set confirmed opening balance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date & Time:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = entryTimestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                TimePickerDialog(context, { _, h, min ->
                                    c.set(Calendar.HOUR_OF_DAY, h)
                                    c.set(Calendar.MINUTE, min)
                                    entryTimestamp = c.timeInMillis
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, fontSize = 11.5.sp)
                    }
                }

                Text("Receipt Destination:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = destination == "CASH",
                        onClick = { destination = "CASH" },
                        label = { Text("Shop Cash (Inflow)") }
                    )
                    FilterChip(
                        selected = destination == "BANK",
                        onClick = { destination = "BANK" },
                        label = { Text("Bank Account") }
                    )
                }

                if (destination == "CASH") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "• Received into shop cash: automatically creates exactly one linked Cash In entry as returned savings (NOT sales revenue, NOT profit).",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "• Received into bank account: records savings reduction, does NOT increase physical shop cash.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Note (Optional)") },
                    placeholder = { Text("e.g. Returned savings, Cheque #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0 && !exceedsBalance) {
                        onConfirm(amountPaise, destination, notes.trim().ifBlank { null }, entryTimestamp)
                    }
                },
                enabled = amountPaise > 0 && !exceedsBalance,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Confirm Withdrawal")
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
fun EditPigmyDepositDialog(
    deposit: PigmyDepositEntity,
    allSchemes: List<PigmySchemeEntity>,
    currentScheme: PigmySchemeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (schemeId: String, amountPaise: Long, source: String, notes: String?, timestamp: Long) -> Unit
) {
    var selectedSchemeId by remember { mutableStateOf(deposit.accountSchemeId) }
    var amountStr by remember { mutableStateOf(CostCalculator.formatPaiseToRupees(deposit.amountPaise).replace("₹", "").trim()) }
    var paymentSource by remember { mutableStateOf(deposit.paymentSource) }
    var notes by remember { mutableStateOf(deposit.notes ?: "") }
    var entryTimestamp by remember { mutableLongStateOf(deposit.paidTimestamp) }
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entryTimestamp))

    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pigmy Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Scheme:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allSchemes.forEach { sc ->
                        FilterChip(
                            selected = selectedSchemeId == sc.id,
                            onClick = { selectedSchemeId = sc.id },
                            label = { Text(sc.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Deposit Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date & Time:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = entryTimestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                TimePickerDialog(context, { _, h, min ->
                                    c.set(Calendar.HOUR_OF_DAY, h)
                                    c.set(Calendar.MINUTE, min)
                                    entryTimestamp = c.timeInMillis
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, fontSize = 11.5.sp)
                    }
                }

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

                Text(
                    text = "• Updating will synchronize the linked Cash Out savings transfer atomically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0) {
                        onConfirm(selectedSchemeId, amountPaise, paymentSource, notes.trim().ifBlank { null }, entryTimestamp)
                    }
                },
                enabled = amountPaise > 0
            ) {
                Text("Save Changes")
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
fun EditPigmyWithdrawalDialog(
    withdrawal: PigmyWithdrawalEntity,
    currentScheme: PigmySchemeEntity?,
    maxAvailablePaise: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPaise: Long, destination: String, notes: String?, timestamp: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf(CostCalculator.formatPaiseToRupees(withdrawal.amountPaise).replace("₹", "").trim()) }
    var destination by remember { mutableStateOf(withdrawal.destination) }
    var notes by remember { mutableStateOf(withdrawal.notes ?: "") }
    var entryTimestamp by remember { mutableLongStateOf(withdrawal.withdrawalTimestamp) }
    val context = LocalContext.current
    val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entryTimestamp))

    val amountPaise = CostCalculator.parseRupeesToPaise(amountStr) ?: 0L
    val exceedsBalance = amountPaise > maxAvailablePaise

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pigmy Withdrawal (${currentScheme?.name ?: "Scheme"})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Max Available Savings: ${CostCalculator.formatPaiseToRupees(maxAvailablePaise)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Withdrawal Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = exceedsBalance,
                    modifier = Modifier.fillMaxWidth()
                )

                if (exceedsBalance) {
                    Text(
                        text = "⚠️ Amount exceeds available tracked savings (${CostCalculator.formatPaiseToRupees(maxAvailablePaise)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date & Time:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = entryTimestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                TimePickerDialog(context, { _, h, min ->
                                    c.set(Calendar.HOUR_OF_DAY, h)
                                    c.set(Calendar.MINUTE, min)
                                    entryTimestamp = c.timeInMillis
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, fontSize = 11.5.sp)
                    }
                }

                Text("Destination:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = destination == "CASH",
                        onClick = { destination = "CASH" },
                        label = { Text("Shop Cash (Inflow)") }
                    )
                    FilterChip(
                        selected = destination == "BANK",
                        onClick = { destination = "BANK" },
                        label = { Text("Bank Account") }
                    )
                }

                Text(
                    text = "• Updating will synchronize the linked Cash In savings transfer atomically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amountPaise > 0 && !exceedsBalance) {
                        onConfirm(amountPaise, destination, notes.trim().ifBlank { null }, entryTimestamp)
                    }
                },
                enabled = amountPaise > 0 && !exceedsBalance
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
