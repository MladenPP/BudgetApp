package com.example.budgetapp.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.ui.dashboard.DashboardViewModel
import com.example.budgetapp.ui.expenses.ExpensesViewModel
import com.example.budgetapp.ui.incomes.IncomesViewModel
import com.example.budgetapp.ui.report.ReportViewModel
import com.example.budgetapp.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(budgetApplication().container.budgetRepository)
        }
        initializer {
            IncomesViewModel(budgetApplication().container.budgetRepository)
        }
        initializer {
            ExpensesViewModel(budgetApplication().container.budgetRepository)
        }
        initializer {
            ReportViewModel(budgetApplication().container.budgetRepository)
        }
        initializer {
            val container = budgetApplication().container
            SettingsViewModel(container.settingsRepository, container::resetAll)
        }
    }
}

fun CreationExtras.budgetApplication(): BudgetApplication =
    this[APPLICATION_KEY] as BudgetApplication