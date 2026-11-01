package com.alieducational.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.accounts.AccountsScreen
import com.alieducational.app.ui.auth.UnlockScreen
import com.alieducational.app.ui.billing.BarcodeScannerScreen
import com.alieducational.app.ui.billing.BillReceiptScreen
import com.alieducational.app.ui.billing.BillingScreen
import com.alieducational.app.ui.billing.PaymentScreen
import com.alieducational.app.ui.billing.ReviewBillScreen
import com.alieducational.app.ui.home.HomeScreen
import com.alieducational.app.ui.more.BackupRestoreScreen
import com.alieducational.app.ui.more.ExpensesScreen
import com.alieducational.app.ui.reports.CashTrackerScreen
import com.alieducational.app.ui.more.MoreScreen
import com.alieducational.app.ui.reports.ProfitScreen
import com.alieducational.app.ui.pigmy.PigmySavingsScreen
import com.alieducational.app.ui.sales.RecordDailyTotalScreen
import com.alieducational.app.ui.sales.RecordSaleScreen
import com.alieducational.app.ui.settings.SettingsScreen
import com.alieducational.app.ui.setup.SetupScreen
import com.alieducational.app.ui.stock.ImportCatalogScreen
import com.alieducational.app.ui.stock.ReceiveStockScreen
import com.alieducational.app.ui.stock.RemoveStockScreen
import com.alieducational.app.ui.stock.StockScreen
import com.alieducational.app.util.BiometricAuthManager

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    viewModel: AliViewModel,
    biometricAuthManager: BiometricAuthManager,
    onUnlocked: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // 4 Primary Bottom Bar Screens
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNewBill = {
                    navController.navigate(Screen.Billing.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAddStock = {
                    navController.navigate(Screen.Stock.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRecordPayment = {
                    navController.navigate(Screen.Accounts.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToStock = {
                    navController.navigate(Screen.Stock.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAccounts = { tab ->
                    navController.navigate(Screen.Accounts.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfit = { navController.navigate(Screen.ProfitAnalysis.route) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToCashTracker = { navController.navigate(Screen.CashTracker.route) },
                onNavigateToPigmy = { navController.navigate(Screen.PigmySavings.route) },
                onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Stock.route) {
            StockScreen(
                viewModel = viewModel,
                onNavigateToReceiveStock = { navController.navigate(Screen.ReceiveStock.route) },
                onNavigateToImport = { navController.navigate(Screen.ImportCatalog.route) },
                onNavigateToRemoveStock = { navController.navigate(Screen.RemoveStock.route) },
                onNavigateBack = {
                    if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                        navController.navigate(Screen.Home.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Billing.route) {
            BillingScreen(
                viewModel = viewModel,
                onNavigateToScanner = { navController.navigate(Screen.BarcodeScanner.route) },
                onNavigateToReview = { navController.navigate(Screen.ReviewBill.route) },
                onNavigateBack = {
                    if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                        navController.navigate(Screen.Home.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                        navController.navigate(Screen.Home.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Full Counter Billing Workflow Screens
        composable(Screen.BarcodeScanner.route) {
            BarcodeScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { navController.navigate(Screen.ReviewBill.route) }
            )
        }

        composable(Screen.ReviewBill.route) {
            ReviewBillScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = { navController.navigate(Screen.BarcodeScanner.route) },
                onNavigateToPayment = { navController.navigate(Screen.Payment.route) }
            )
        }

        composable(Screen.Payment.route) {
            PaymentScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onBillSaved = { receiptNum ->
                    navController.navigate(Screen.BillReceipt.createRoute(receiptNum)) {
                        popUpTo(Screen.Billing.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.BillReceipt.route,
            arguments = listOf(navArgument("receiptNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val receiptNum = backStackEntry.arguments?.getString("receiptNumber") ?: "AE-RECEIPT"
            BillReceiptScreen(
                receiptNumber = receiptNum,
                viewModel = viewModel,
                onNewBill = {
                    navController.navigate(Screen.Billing.route) {
                        popUpTo(Screen.Billing.route) { inclusive = true }
                    }
                },
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Legacy / Secondary Management & Reports
        composable(Screen.More.route) {
            MoreScreen(
                viewModel = viewModel,
                onNavigateToProfit = { navController.navigate(Screen.ProfitAnalysis.route) },
                onNavigateToDailySales = { navController.navigate(Screen.RecordDailyTotal.route) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onNavigateToImport = { navController.navigate(Screen.ImportCatalog.route) },
                onNavigateToBackup = { navController.navigate(Screen.BackupRestore.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Auth & Setup
        composable(Screen.Unlock.route) {
            UnlockScreen(
                biometricAuthManager = biometricAuthManager,
                onUnlocked = {
                    onUnlocked()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Unlock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        // Workflows
        composable(Screen.ReceiveStock.route) {
            ReceiveStockScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RemoveStock.route) {
            RemoveStockScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecordSale = {
                    navController.navigate(Screen.RecordSale.route) {
                        popUpTo(Screen.RemoveStock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RecordSale.route) {
            RecordSaleScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RecordDailyTotal.route) {
            RecordDailyTotalScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImportCatalog.route) {
            ImportCatalogScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfitAnalysis.route) {
            ProfitScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CashTracker.route) {
            CashTrackerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BackupRestore.route) {
            BackupRestoreScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PigmySavings.route) {
            PigmySavingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
