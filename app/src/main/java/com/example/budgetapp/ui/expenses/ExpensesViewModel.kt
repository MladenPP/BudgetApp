package com.example.budgetapp.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.repository.BudgetRepository
import com.example.budgetapp.data.repository.observeCurrentSummary
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.Purchase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val summary: BudgetSummary? = null,
    val purchases: List<Purchase> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(private val repository: BudgetRepository) : ViewModel() {

    val uiState: StateFlow<ExpensesUiState> = repository.observeCurrentSummary()
        .flatMapLatest { summary ->
            repository.observePurchases(summary.period).map { purchases ->
                ExpensesUiState(isLoading = false, summary = summary, purchases = purchases)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

    fun addExpense(name: String, amount: Long, kind: ExpenseKind) {
        viewModelScope.launch { repository.addExpense(name, amount, kind) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { repository.updateExpense(expense) }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { repository.deleteExpense(id) }
    }

    fun setExpenseOverride(expenseId: Long, period: BudgetPeriod, amount: Long) {
        viewModelScope.launch { repository.setExpenseOverride(expenseId, period, amount) }
    }

    fun clearExpenseOverride(expenseId: Long, period: BudgetPeriod) {
        viewModelScope.launch { repository.clearExpenseOverride(expenseId, period) }
    }

    fun deletePurchase(id: Long) {
        viewModelScope.launch { repository.deletePurchase(id) }
    }

    fun deleteInstantExpense(id: Long) {
        viewModelScope.launch { repository.deleteInstantExpense(id) }
    }
}