package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.Bonus
import com.example.budgetapp.domain.model.BudgetInput
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.IncomeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetEdgeCasesTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val period = PeriodCalculator.periodFor(today, mainIncomeDay = 5)

    private fun input(
        incomes: List<Income> = emptyList(),
        expenses: List<Expense> = emptyList(),
        bonuses: List<Bonus> = emptyList(),
        percent: Int = 20
    ) = BudgetInput(
        period = period,
        today = today,
        savingsPercent = percent,
        incomes = incomes,
        incomeOverrides = emptyMap(),
        bonuses = bonuses,
        expenses = expenses,
        expenseOverrides = emptyMap(),
        purchases = emptyList(),
        instantExpenses = emptyList()
    )

    @Test
    fun emptyBudget_isAllZero() {
        val s = BudgetCalculator.calculate(input())
        assertEquals(0, s.incomeTotal)
        assertEquals(0, s.base)
        assertEquals(0, s.savingsGoal)
        assertEquals(0, s.unallocatedLeft)
        assertEquals(0, s.savingsCurrent)
        assertFalse(s.isOverBudget)
    }

    @Test
    fun fixedCostsLargerThanIncome_baseIsNegative_noSavingsGoal() {
        val s = BudgetCalculator.calculate(
            input(
                incomes = listOf(Income(1, "Plata", 10_000, 5, IncomeKind.MAIN)),
                expenses = listOf(Expense(2, "Kirija", 12_000, ExpenseKind.FIXED))
            )
        )
        assertEquals(-2_000, s.base)
        assertEquals(0, s.savingsGoal)
        assertEquals(0, s.unallocatedLeft)
        // nedostajućih 2.000 se vodi kao pojedeno iz štednje, pa je budžet u minusu
        assertEquals(2_000, s.savingsEaten)
        assertEquals(-2_000, s.savingsCurrent)
        assertTrue(s.isOverBudget)
    }

    @Test
    fun bonusOnNegativeBase_coversTheGapFirst() {
        val bonus = Bonus(1, "Bonus", 5_000, LocalDate.of(2026, 10, 7), 1_000, 4_000)
        val s = BudgetCalculator.calculate(
            input(
                incomes = listOf(Income(1, "Plata", 10_000, 5, IncomeKind.MAIN)),
                expenses = listOf(Expense(2, "Kirija", 12_000, ExpenseKind.FIXED)),
                bonuses = listOf(bonus)
            )
        )
        // slobodno: -2.000 + 4.000 = 2.000; štednja: samo 1.000 iz bonusa
        assertEquals(2_000, s.unallocatedLeft)
        assertEquals(0, s.savingsEaten)
        assertEquals(1_000, s.savingsCurrent)
        assertFalse(s.isOverBudget)
    }

    @Test
    fun zeroPercent_meansNoSavingsGoal_andBonusGoesEntirelyToFreeMoney() {
        val s = BudgetCalculator.calculate(
            input(incomes = listOf(Income(1, "Plata", 100_000, 5, IncomeKind.MAIN)), percent = 0)
        )
        assertEquals(0, s.savingsGoal)
        assertEquals(100_000, s.unallocatedLeft)

        val (toSavings, toFree) = SavingsRules.splitBonus(5_000, 0)
        assertEquals(0, toSavings)
        assertEquals(5_000, toFree)
    }

    @Test
    fun maxPercent_halvesTheBase() {
        val s = BudgetCalculator.calculate(
            input(incomes = listOf(Income(1, "Plata", 100_000, 5, IncomeKind.MAIN)), percent = 50)
        )
        assertEquals(50_000, s.savingsGoal)
        assertEquals(50_000, s.unallocatedLeft)
    }

    @Test
    fun veryLargeAmounts_doNotOverflow() {
        val s = BudgetCalculator.calculate(
            input(
                incomes = listOf(Income(1, "Plata", 999_999_999_999, 5, IncomeKind.MAIN)),
                expenses = listOf(Expense(2, "Kirija", 1, ExpenseKind.FIXED)),
                percent = 50
            )
        )
        assertEquals(999_999_999_998, s.base)
        assertEquals(499_999_999_999, s.savingsGoal)
    }

    @Test
    fun savingsRoundingNeverCreatesOrLosesMoney() {
        // 333 * 20% = 66 (zaokruženo naniže); ostatak ide u slobodno, zbir je uvek 333
        val s = BudgetCalculator.calculate(
            input(incomes = listOf(Income(1, "Plata", 333, 5, IncomeKind.MAIN)))
        )
        assertEquals(66, s.savingsGoal)
        assertEquals(267, s.unallocatedLeft)
        assertEquals(333, s.savingsGoal + s.unallocatedLeft)
    }
}