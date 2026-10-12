package com.example.budgetapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.repository.BudgetRepository
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.model.BudgetSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    val hasMainIncome: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val summary: BudgetSummary? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeHasMainIncome(),
        repository.observeMainIncomeDay(),
        today
    ) { hasMainIncome, mainIncomeDay, date ->
        Triple(hasMainIncome, mainIncomeDay, date)
    }
        .flatMapLatest { (hasMainIncome, mainIncomeDay, date) ->
            if (!hasMainIncome) {
                flowOf(DashboardUiState(isLoading = false, today = date))
            } else {
                val period = PeriodCalculator.periodFor(date, mainIncomeDay)
                repository.observeSummary(period, date).map { summary ->
                    DashboardUiState(
                        isLoading = false,
                        hasMainIncome = true,
                        today = date,
                        summary = summary
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun saveMainIncome(name: String, amount: Long, dayOfMonth: Int) {
        viewModelScope.launch { repository.saveMainIncome(name, amount, dayOfMonth) }
    }

    fun addInstantExpense(name: String, amount: Long) {
        viewModelScope.launch { repository.addInstantExpense(name, amount, LocalDate.now()) }
    }

    fun addBonus(name: String, amount: Long) {
        viewModelScope.launch { repository.addBonus(name, amount, LocalDate.now()) }
    }

    fun addPurchase(expenseId: Long, amount: Long, note: String) {
        viewModelScope.launch {
            repository.addPurchase(expenseId, amount, LocalDate.now(), note)
        }
    }
}