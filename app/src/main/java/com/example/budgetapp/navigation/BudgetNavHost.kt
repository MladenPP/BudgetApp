package com.example.budgetapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budgetapp.ui.dashboard.DashboardScreen
import com.example.budgetapp.ui.expenses.ExpensesScreen
import com.example.budgetapp.ui.incomes.IncomesScreen
import com.example.budgetapp.ui.report.ReportScreen
import com.example.budgetapp.ui.settings.SettingsScreen

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Pregled", Icons.Filled.Home),
    INCOMES("incomes", "Primanja", Icons.Filled.AccountBalanceWallet),
    EXPENSES("expenses", "Troškovi", Icons.Filled.Receipt),
    REPORT("report", "Izveštaj", Icons.Filled.Assessment),
    SETTINGS("settings", "Postavke", Icons.Filled.Settings)
}

@Composable
fun BudgetNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Destination.DASHBOARD.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Destination.DASHBOARD.route) { DashboardScreen() }
        composable(Destination.INCOMES.route) { IncomesScreen() }
        composable(Destination.EXPENSES.route) { ExpensesScreen() }
        composable(Destination.REPORT.route) { ReportScreen() }
        composable(Destination.SETTINGS.route) {
            SettingsScreen(
                onDataReset = {
                    navController.navigate(Destination.DASHBOARD.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}