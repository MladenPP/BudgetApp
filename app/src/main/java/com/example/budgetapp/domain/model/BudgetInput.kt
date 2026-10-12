package com.example.budgetapp.domain.model

import java.time.LocalDate

data class BudgetInput(
    val period: BudgetPeriod,
    val today: LocalDate,
    val savingsPercent: Int,
    val incomes: List<Income>,
    val incomeOverrides: Map<Long, Long>,
    val bonuses: List<Bonus>,
    val expenses: List<Expense>,
    val expenseOverrides: Map<Long, Long>,
    val purchases: List<Purchase>,
    val instantExpenses: List<InstantExpense>
)