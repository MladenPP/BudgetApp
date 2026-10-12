package com.example.budgetapp.ui.incomes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.repository.BudgetRepository
import com.example.budgetapp.data.repository.observeCurrentSummary
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.Income
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IncomesUiState(
    val isLoading: Boolean = true,
    val summary: BudgetSummary? = null
)

class IncomesViewModel(private val repository: BudgetRepository) : ViewModel() {

    val uiState: StateFlow<IncomesUiState> = repository.observeCurrentSummary()
        .map { IncomesUiState(isLoading = false, summary = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncomesUiState())

    fun saveMainIncome(name: String, amount: Long, dayOfMonth: Int) {
        viewModelScope.launch { repository.saveMainIncome(name, amount, dayOfMonth) }
    }

    fun addRecurringIncome(name: String, amount: Long, dayOfMonth: Int) {
        viewModelScope.launch { repository.addRecurringIncome(name, amount, dayOfMonth) }
    }

    fun updateIncome(income: Income) {
        viewModelScope.launch { repository.updateIncome(income) }
    }

    fun deleteIncome(id: Long) {
        viewModelScope.launch { repository.deleteIncome(id) }
    }

    fun setIncomeOverride(incomeId: Long, period: BudgetPeriod, amount: Long) {
        viewModelScope.launch { repository.setIncomeOverride(incomeId, period, amount) }
    }

    fun clearIncomeOverride(incomeId: Long, period: BudgetPeriod) {
        viewModelScope.launch { repository.clearIncomeOverride(incomeId, period) }
    }

    fun deleteBonus(id: Long) {
        viewModelScope.launch { repository.deleteBonus(id) }
    }
}