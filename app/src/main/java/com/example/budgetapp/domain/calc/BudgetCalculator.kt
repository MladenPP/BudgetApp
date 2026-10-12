package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.BudgetInput
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.FixedLine
import com.example.budgetapp.domain.model.IncomeKind
import com.example.budgetapp.domain.model.IncomeLine
import com.example.budgetapp.domain.model.ProgressiveLine

object BudgetCalculator {

    fun calculate(input: BudgetInput): BudgetSummary {
        val period = input.period
        val percent = SavingsRules.clampPercent(input.savingsPercent)

        val incomeLines = input.incomes.map { income ->
            val override = input.incomeOverrides[income.id]
            val arrived = income.kind == IncomeKind.MAIN ||
                    !PeriodCalculator.occurrenceDate(income.dayOfMonth, period).isAfter(input.today)
            IncomeLine(income, override ?: income.amount, override != null, arrived)
        }
        val incomeTotal = incomeLines.filter { it.arrived }.sumOf { it.amount }

        val fixedLines = input.expenses
            .filter { it.kind == ExpenseKind.FIXED }
            .map { expense ->
                val override = input.expenseOverrides[expense.id]
                FixedLine(expense, override ?: expense.amount, override != null)
            }
        val fixedTotal = fixedLines.sumOf { it.amount }

        val purchasesInPeriod = input.purchases.filter { period.contains(it.date) }
        val progressiveLines = input.expenses
            .filter { it.kind == ExpenseKind.PROGRESSIVE }
            .map { expense ->
                val override = input.expenseOverrides[expense.id]
                val spent = purchasesInPeriod
                    .filter { it.expenseId == expense.id }
                    .sumOf { it.amount }
                ProgressiveLine(expense, override ?: expense.amount, spent, override != null)
            }
        val progressiveReserved = progressiveLines.sumOf { it.reserved }
        val progressiveOverflow = progressiveLines.sumOf { it.overflow }

        val base = incomeTotal - fixedTotal - progressiveReserved
        val savingsGoal = if (base > 0) SavingsRules.savingsPart(base, percent) else 0L
        val unallocatedStart = base - savingsGoal

        val bonuses = input.bonuses.filter { period.contains(it.date) }
        val bonusSavings = bonuses.sumOf { it.savingsPart }
        val bonusUnallocated = bonuses.sumOf { it.unallocatedPart }

        val instantExpenses = input.instantExpenses.filter { period.contains(it.date) }
        val instantSpent = instantExpenses.sumOf { it.amount }

        val available = unallocatedStart + bonusUnallocated
        val spend = instantSpent + progressiveOverflow
        val unallocatedLeft = (available - spend).coerceAtLeast(0)
        val savingsEaten = (spend - available).coerceAtLeast(0)
        val savingsCurrent = savingsGoal + bonusSavings - savingsEaten

        return BudgetSummary(
            period = period,
            savingsPercent = percent,
            incomeLines = incomeLines,
            fixedLines = fixedLines,
            progressiveLines = progressiveLines,
            bonuses = bonuses,
            instantExpenses = instantExpenses,
            incomeTotal = incomeTotal,
            fixedTotal = fixedTotal,
            progressiveReserved = progressiveReserved,
            base = base,
            savingsGoal = savingsGoal,
            unallocatedStart = unallocatedStart,
            bonusSavings = bonusSavings,
            bonusUnallocated = bonusUnallocated,
            instantSpent = instantSpent,
            progressiveOverflow = progressiveOverflow,
            unallocatedLeft = unallocatedLeft,
            savingsEaten = savingsEaten,
            savingsCurrent = savingsCurrent
        )
    }
}