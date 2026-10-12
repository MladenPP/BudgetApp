package com.example.budgetapp.domain.model

data class IncomeLine(
    val income: Income,
    val amount: Long,
    val isOverridden: Boolean,
    val arrived: Boolean
)

data class FixedLine(
    val expense: Expense,
    val amount: Long,
    val isOverridden: Boolean
)

data class ProgressiveLine(
    val expense: Expense,
    val reserved: Long,
    val spent: Long,
    val isOverridden: Boolean
) {
    val overflow: Long get() = (spent - reserved).coerceAtLeast(0)

    val remaining: Long get() = (reserved - spent).coerceAtLeast(0)
}

data class BudgetSummary(
    val period: BudgetPeriod,
    val savingsPercent: Int,
    val incomeLines: List<IncomeLine>,
    val fixedLines: List<FixedLine>,
    val progressiveLines: List<ProgressiveLine>,
    val bonuses: List<Bonus>,
    val instantExpenses: List<InstantExpense>,
    val incomeTotal: Long,
    val fixedTotal: Long,
    val progressiveReserved: Long,
    val base: Long,
    val savingsGoal: Long,
    val unallocatedStart: Long,
    val bonusSavings: Long,
    val bonusUnallocated: Long,
    val instantSpent: Long,
    val progressiveOverflow: Long,
    val unallocatedLeft: Long,
    val savingsEaten: Long,
    val savingsCurrent: Long
) {
    val plannedSavings: Long get() = savingsGoal + bonusSavings
    val isOverBudget: Boolean get() = savingsCurrent < 0
}