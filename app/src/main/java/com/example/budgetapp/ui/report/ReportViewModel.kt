package com.example.budgetapp.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.repository.BudgetRepository
import com.example.budgetapp.data.repository.todayTicker
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.calc.ReportGenerator
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.ReportKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportUiState(
    val isLoading: Boolean = true,
    val report: PeriodReport? = null,
    val kind: ReportKind = ReportKind.PRELIMINARY,
    val history: List<PeriodReport> = emptyList(),
    val nextReportDate: LocalDate? = null
)

private data class ReportInputs(
    val hasMainIncome: Boolean,
    val mainIncomeDay: Int,
    val today: LocalDate,
    val selectedArchivedStart: Long?,
    val saved: List<PeriodReport>
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val selectedArchived = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            todayTicker().collect { today -> repository.archiveFinishedPeriods(today) }
        }
    }

    val uiState: StateFlow<ReportUiState> = combine(
        repository.observeHasMainIncome(),
        repository.observeMainIncomeDay(),
        todayTicker(),
        selectedArchived,
        repository.observeSavedReports()
    ) { hasMainIncome, mainIncomeDay, today, selected, saved ->
        ReportInputs(hasMainIncome, mainIncomeDay, today, selected, saved)
    }
        .flatMapLatest { input ->
            val archived = input.selectedArchivedStart?.let { key ->
                input.saved.firstOrNull { it.period.key == key }
            }
            val current = if (input.hasMainIncome) {
                PeriodCalculator.periodFor(input.today, input.mainIncomeDay)
            } else {
                null
            }

            when {
                archived != null -> flowOf(
                    ReportUiState(
                        isLoading = false,
                        report = archived,
                        kind = ReportKind.FINISHED,
                        history = input.saved,
                        nextReportDate = current?.endInclusive
                    )
                )

                current == null -> flowOf(
                    ReportUiState(isLoading = false, history = input.saved)
                )

                else -> repository.observeSummary(current, input.today).map { summary ->
                    ReportUiState(
                        isLoading = false,
                        report = ReportGenerator.generate(summary),
                        kind = if (input.today == current.endInclusive) {
                            ReportKind.LAST_DAY
                        } else {
                            ReportKind.PRELIMINARY
                        },
                        history = input.saved,
                        nextReportDate = current.endInclusive
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun showArchived(periodStart: Long) {
        selectedArchived.value = periodStart
    }

    fun showCurrent() {
        selectedArchived.value = null
    }
}