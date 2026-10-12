package com.example.budgetapp.domain.model

enum class ReportKind { LAST_DAY, FINISHED, PRELIMINARY }

data class ReportProgressiveLine(
    val name: String,
    val reserved: Long,
    val spent: Long
) {
    val overflow: Long get() = (spent - reserved).coerceAtLeast(0)
}

data class OverspentLine(
    val name: String,
    val planned: Long,
    val spent: Long
) {
    val over: Long get() = spent - planned
}

data class PeriodReport(
    val period: BudgetPeriod,
    val savingsPercent: Int,
    val incomeTotal: Long,
    val bonusTotal: Long,
    val fixedTotal: Long,
    val instantSpent: Long,
    val plannedSavings: Long,
    val savingsEaten: Long,
    val unallocatedLeft: Long,
    val remainingReserve: Long,
    val actualSaved: Long,
    val progressiveLines: List<ReportProgressiveLine>,
    val lastInstantExpense: InstantExpense?
) {
    val difference: Long get() = actualSaved - plannedSavings
    val metGoal: Boolean get() = difference >= 0

    val overspentProgressive: List<OverspentLine>
        get() = progressiveLines
            .filter { it.overflow > 0 }
            .map { OverspentLine(it.name, it.reserved, it.spent) }
}