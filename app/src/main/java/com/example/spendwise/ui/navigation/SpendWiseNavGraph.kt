package com.example.spendwise.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.ui.components.SpendWiseBottomBar
import com.example.spendwise.ui.screens.addtransaction.AddTransactionScreen
import com.example.spendwise.ui.screens.analytics.AnalyticsScreen
import com.example.spendwise.ui.screens.budget.BudgetScreen
import com.example.spendwise.ui.screens.home.HomeScreen
import com.example.spendwise.ui.screens.settings.SettingsScreen
import com.example.spendwise.ui.screens.transactions.TransactionsScreen
import com.example.spendwise.viewmodel.SpendWiseViewModel

@Composable
fun SpendWiseNavGraph(
    viewModel: SpendWiseViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    val isLoggedIn = !currentUserId.isNullOrBlank()
    val isAppLockEnabled = userSettings?.isAppLockEnabled ?: false

    // 1. Not logged in -> Show Authentication (Login / Sign Up)
    if (!isLoggedIn) {
        com.example.spendwise.ui.screens.auth.AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // Auth state updates currentUserId which triggers recomposition directly to main Scaffold
            }
        )
        return
    }

    // 2. Logged in and App Lock is active -> Show PIN / Biometric Lock Screen
    if (isAppLockEnabled && isAppLocked) {
        com.example.spendwise.ui.screens.auth.AppLockScreen(
            viewModel = viewModel,
            onUnlocked = {
                // PIN or Biometrics verified, main dashboard unlocks smoothly
            },
            onLogout = {
                viewModel.logout()
            }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        bottomBar = {
            // Hide bottom bar on Add Transaction and Screenshot Review screens
            if (currentRoute != Screen.AddTransaction.route && currentRoute != Screen.ScreenshotReview.route) {
                SpendWiseBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { targetRoute ->
                        if (targetRoute == Screen.AddTransaction.route) {
                            editingTransaction = null
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onSeeAllClicked = { navController.navigate(Screen.Transactions.route) },
                    onTransactionClicked = { tx ->
                        editingTransaction = tx
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onBudgetCardClicked = { navController.navigate(Screen.Budget.route) },
                    onNavigateToScreenshotReview = {
                        navController.navigate(Screen.ScreenshotReview.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    viewModel = viewModel,
                    onEditTransaction = { tx ->
                        editingTransaction = tx
                        navController.navigate(Screen.AddTransaction.route)
                    }
                )
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    editingTransaction = editingTransaction,
                    onNavigateBack = {
                        editingTransaction = null
                        navController.popBackStack()
                    },
                    onNavigateToScreenshotReview = {
                        navController.navigate(Screen.ScreenshotReview.route)
                    }
                )
            }

            composable(Screen.ScreenshotReview.route) {
                com.example.spendwise.ui.screens.ocr.ScreenshotReviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onImportSuccess = { count ->
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }

            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
