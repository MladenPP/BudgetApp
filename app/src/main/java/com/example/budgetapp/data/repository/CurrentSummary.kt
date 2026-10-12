package com.example.budgetapp.data.repository

import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.model.BudgetSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

fun todayTicker(): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now())
        delay(60_000)
    }
}.distinctUntilChanged()

@OptIn(ExperimentalCoroutinesApi::class)
fun BudgetRepository.observeCurrentSummary(): Flow<BudgetSummary> =
    combine(observeMainIncomeDay(), todayTicker()) { day, date -> day to date }
        .flatMapLatest { (day, date) ->
            observeSummary(PeriodCalculator.periodFor(date, day), date)
        }