package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.ReportProgressiveLine

object ReportGenerator {

    fun generate(summary: BudgetSummary): PeriodReport {
        val remainingReserve = summary.progressiveLines.sumOf { it.remaining }
        val actualSaved = summary.savingsCurrent + summary.unallocatedLeft + remainingReserve
        val shortfall = (summary.plannedSavings - actualSaved).coerceAtLeast(0)

        val lines = summary.progressiveLines.map {
            ReportProgressiveLine(it.expense.name, it.reserved, it.spent)
        }
        val overflowTotal = lines.sumOf { it.overflow }

        val lastInstant = if (shortfall > 0 && overflowTotal < shortfall) {
            summary.instantExpenses.maxWithOrNull(
                compareBy<InstantExpense>({ it.date }, { it.id })
            )
        } else {
            null
        }

        return PeriodReport(
            period = summary.period,
            savingsPercent = summary.savingsPercent,
            incomeTotal = summary.incomeTotal,
            bonusTotal = summary.bonuses.sumOf { it.amount },
            fixedTotal = summary.fixedTotal,
            instantSpent = summary.instantSpent,
            plannedSavings = summary.plannedSavings,
            savingsEaten = summary.savingsEaten,
            unallocatedLeft = summary.unallocatedLeft,
            remainingReserve = remainingReserve,
            actualSaved = actualSaved,
            progressiveLines = lines,
            lastInstantExpense = lastInstant
        )
    }
}