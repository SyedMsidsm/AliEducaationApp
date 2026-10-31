package com.alieducational.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.ui.graphics.vector.ImageVector
import com.alieducational.app.R

sealed class Screen(val route: String) {
    // 4 Primary Bottom Bar Tabs
    data object Home : Screen("home")
    data object Stock : Screen("stock")
    data object Billing : Screen("billing")
    data object Accounts : Screen("accounts")

    // Full Counter Billing Workflow
    data object BarcodeScanner : Screen("barcode_scanner")
    data object ReviewBill : Screen("review_bill")
    data object Payment : Screen("payment")
    data object BillReceipt : Screen("bill_receipt/{receiptNumber}") {
        fun createRoute(num: String) = "bill_receipt/$num"
    }

    // Secondary Management & Reports (Accessible from Home)
    data object More : Screen("more")

    // Auth & Setup
    data object Unlock : Screen("unlock")
    data object Setup : Screen("setup")

    // Workflows
    data object ReceiveStock : Screen("receive_stock")
    data object RemoveStock : Screen("remove_stock")
    data object RecordSale : Screen("record_sale")
    data object RecordDailyTotal : Screen("record_daily_total")
    data object ImportCatalog : Screen("import_catalog")
    data object ProfitAnalysis : Screen("profit_analysis")
    data object Expenses : Screen("expenses")
    data object CashTracker : Screen("cash_tracker")
    data object BackupRestore : Screen("backup_restore")
    data object PigmySavings : Screen("pigmy_savings")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    BottomNavItem(
        screen = Screen.Stock,
        labelResId = R.string.nav_stock,
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
    ),
    BottomNavItem(
        screen = Screen.Billing,
        labelResId = R.string.nav_billing,
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
    ),
    BottomNavItem(
        screen = Screen.Accounts,
        labelResId = R.string.nav_accounts,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    ),
)