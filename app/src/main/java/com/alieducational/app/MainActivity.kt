package com.alieducational.app

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alieducational.app.ui.AliViewModel
import com.alieducational.app.ui.navigation.AppNavigation
import com.alieducational.app.ui.navigation.BottomNavBar
import com.alieducational.app.ui.navigation.Screen
import com.alieducational.app.ui.theme.AliEducationalTheme
import com.alieducational.app.util.BiometricAuthManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val viewModel: AliViewModel by viewModels()
    fun getAppViewModel(): AliViewModel = viewModel

    private var lastPausedTime: Long = 0L
    private var configuredLockTimeout = 120
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        enableEdgeToEdge()
        extractWidgetAction(intent)
        com.alieducational.app.widget.AliSlimBarWidgetProvider.updateAllWidgets(this)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val appTextSize by viewModel.appTextSize.collectAsState()
            val operationError by viewModel.operationError.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val startupSettings by viewModel.startupSettings.collectAsState()
            if (startupSettings == null) return@setContent
            val isFirstLaunch = startupSettings!!.first
            val isLockEnabled = startupSettings!!.second
            val timeout by viewModel.lockTimeoutSeconds.collectAsState()
            androidx.compose.runtime.SideEffect { configuredLockTimeout = timeout }

            val layoutDirection = LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AliEducationalTheme(darkTheme = isDarkTheme, appTextSize = appTextSize) {
                    operationError?.let { message ->
                        androidx.compose.material3.AlertDialog(onDismissRequest = { viewModel.clearOperationError() },
                            title = { androidx.compose.material3.Text("Could not complete action") },
                            text = { androidx.compose.material3.Text(message) },
                            confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.clearOperationError() }) { androidx.compose.material3.Text("OK") } })
                    }
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val startDestination = when {
                        isFirstLaunch -> Screen.Setup.route
                        isLockEnabled && viewModel.isAppLocked -> Screen.Unlock.route
                        else -> Screen.Home.route
                    }

                    val mainTabRoutes = listOf(
                        Screen.Home.route,
                        Screen.Stock.route,
                        Screen.Billing.route,
                        Screen.Accounts.route
                    )

                    val showBottomBar = currentRoute in mainTabRoutes &&
                            !isFirstLaunch &&
                            (!isLockEnabled || !viewModel.isAppLocked)

                    androidx.compose.runtime.LaunchedEffect(pendingWidgetAction, isLockEnabled, viewModel.isAppLocked, isFirstLaunch) {
                        val action = pendingWidgetAction
                        if (action != null && !isFirstLaunch && (!isLockEnabled || !viewModel.isAppLocked)) {
                            when (action) {
                                com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_IN -> {
                                    viewModel.triggerWidgetAction(com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_IN)
                                    navController.navigate(Screen.CashTracker.route) {
                                        launchSingleTop = true
                                    }
                                }
                                com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_OUT -> {
                                    viewModel.triggerWidgetAction(com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_CASH_OUT)
                                    navController.navigate(Screen.CashTracker.route) {
                                        launchSingleTop = true
                                    }
                                }
                                com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_NEW_BILL -> {
                                    navController.navigate(Screen.Billing.route) {
                                        launchSingleTop = true
                                    }
                                }
                                com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_ADD_STOCK -> {
                                    viewModel.triggerWidgetAction(com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_ADD_STOCK)
                                    navController.navigate(Screen.Stock.route) {
                                        launchSingleTop = true
                                    }
                                }
                                com.alieducational.app.widget.AliSlimBarWidgetProvider.ACTION_HOME -> {
                                    navController.navigate(Screen.Home.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                            pendingWidgetAction = null
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        if (isLockEnabled && viewModel.isAppLocked && !isFirstLaunch) {
                            com.alieducational.app.ui.auth.UnlockScreen(biometricAuthManager = biometricAuthManager, onUnlocked = { viewModel.isAppLocked = false })
                        } else AppNavigation(
                            navController = navController,
                            startDestination = startDestination,
                            viewModel = viewModel,
                            biometricAuthManager = biometricAuthManager,
                            onUnlocked = { viewModel.isAppLocked = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastPausedTime = SystemClock.elapsedRealtime()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetAction(intent)
    }

    private fun extractWidgetAction(intent: android.content.Intent?) {
        if (intent?.action == "com.alieducational.app.action.PIN_WIDGET" || intent?.getStringExtra("action") == "PIN_WIDGET") {
            requestPinWidget()
            return
        }
        val action = intent?.getStringExtra(com.alieducational.app.widget.AliSlimBarWidgetProvider.EXTRA_WIDGET_ACTION)
        if (action != null) {
            pendingWidgetAction = action
        }
    }

    fun requestPinWidget() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(android.appwidget.AppWidgetManager::class.java)
            val myProvider = android.content.ComponentName(this, com.alieducational.app.widget.AliSlimBarWidgetProvider::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            }
        }
        com.alieducational.app.widget.AliSlimBarWidgetProvider.updateAllWidgets(this)
    }

    override fun onResume() {
        super.onResume()
        com.alieducational.app.widget.AliSlimBarWidgetProvider.updateAllWidgets(this)
        if (lastPausedTime > 0L) {
            val elapsedSeconds = (SystemClock.elapsedRealtime() - lastPausedTime) / 1000
            // If background timeout exceeded (default 120s), relock
            if (elapsedSeconds >= configuredLockTimeout) {
                viewModel.isAppLocked = true
            }
        }
    }
}