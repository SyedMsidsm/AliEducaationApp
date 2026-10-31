package com.alieducational.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.hilt.navigation.compose.hiltViewModel
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

    private var lastPausedTime: Long = 0L
    private var isAppLocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: AliViewModel = hiltViewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
            val isLockEnabled by viewModel.isLockEnabled.collectAsState()

            val layoutDirection = LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AliEducationalTheme(darkTheme = isDarkTheme) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val startDestination = when {
                        isFirstLaunch -> Screen.Setup.route
                        isLockEnabled && isAppLocked -> Screen.Unlock.route
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
                            (!isLockEnabled || !isAppLocked)

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        AppNavigation(
                            navController = navController,
                            startDestination = startDestination,
                            viewModel = viewModel,
                            biometricAuthManager = biometricAuthManager,
                            onUnlocked = { isAppLocked = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastPausedTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (lastPausedTime > 0L) {
            val elapsedSeconds = (System.currentTimeMillis() - lastPausedTime) / 1000
            // If background timeout exceeded (default 120s), relock
            if (elapsedSeconds >= 120) {
                isAppLocked = true
            }
        }
    }
}