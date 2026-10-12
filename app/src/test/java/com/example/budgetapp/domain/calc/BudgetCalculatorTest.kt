package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.Bonus
import com.example.budgetapp.domain.model.BudgetInput
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.IncomeKind
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetCalculatorTest {

    private val today = LocalDate.of(2026, 10, 8)
    private val period = PeriodCalculator.periodFor(today, mainIncomeDay = 5)

    private val salary = Income(1, "Plata", 120_000, 5, IncomeKind.MAIN)
    private val sideBusiness = Income(2, "Side business", 20_000, 15, IncomeKind.RECURRING)
    private val electricity = Expense(10, "Struja", 10_000, ExpenseKind.FIXED)
    private val water = Expense(11, "Voda", 2_000, ExpenseKind.FIXED)
    private val food = Expense(12, "Hrana", 28_000, ExpenseKind.PROGRESSIVE)

    private fun input(
        incomes: List<Income> = listOf(salary),
        incomeOverrides: Map<Long, Long> = emptyMap(),
        bonuses: List<Bonus> = emptyList(),
        expenses: List<Expense> = listOf(electricity, water, food),
        expenseOverrides: Map<Long, Long> = emptyMap(),
        purchases: List<Purchase> = emptyList(),
        instant: List<InstantExpense> = emptyList(),
        percent: Int = 20,
        asOf: LocalDate = today
    ) = BudgetInput(
        period = period,
        today = asOf,
        savingsPercent = percent,
        incomes = incomes,
        incomeOverrides = incomeOverrides,
        bonuses = bonuses,
        expenses = expenses,
        expenseOverrides = expenseOverrides,
        purchases = purchases,
        instantExpenses = instant
    )

    @Test
    fun period_runsFromFifthToFifth() {
        assertEquals(LocalDate.of(2026, 10, 5), period.start)
        assertEquals(LocalDate.of(2026, 11, 5), period.endExclusive)
        assertEquals(LocalDate.of(2026, 11, 4), period.endInclusive)

        val before = PeriodCalculator.periodFor(LocalDate.of(2026, 10, 3), 5)
        assertEquals(LocalDate.of(2026, 9, 5), before.start)
        assertEquals(LocalDate.of(2026, 10, 5), before.endExclusive)
    }

    @Test
    fun startOfMonth_basicBudget() {
        val s = BudgetCalculator.calculate(input())
        assertEquals(120_000, s.incomeTotal)
        assertEquals(12_000, s.fixedTotal)
        assertEquals(28_000, s.progressiveReserved)
        assertEquals(80_000, s.base)
        assertEquals(16_000, s.savingsGoal)
        assertEquals(64_000, s.unallocatedLeft)
        assertEquals(16_000, s.savingsCurrent)
    }

    @Test
    fun instantExpense_reducesUnallocated() {
        val kafana = InstantExpense(1, "Kafana", 10_000, LocalDate.of(2026, 10, 7))
        val s = BudgetCalculator.calculate(input(instant = listOf(kafana)))
        assertEquals(54_000, s.unallocatedLeft)
        assertEquals(16_000, s.savingsCurrent)
    }

    @Test
    fun progressivePurchase_withinReserve_doesNotChangeBudget() {
        val p = Purchase(1, food.id, 1_000, LocalDate.of(2026, 10, 6), "Maxi")
        val s = BudgetCalculator.calculate(input(purchases = listOf(p)))
        assertEquals(64_000, s.unallocatedLeft)
        assertEquals(27_000, s.progressiveLines.single().remaining)
    }

    @Test
    fun progressivePurchase_overReserve_takesFromUnallocated() {
        val p = Purchase(1, food.id, 33_000, LocalDate.of(2026, 10, 6), "Pazar")
        val s = BudgetCalculator.calculate(input(purchases = listOf(p)))
        assertEquals(5_000, s.progressiveOverflow)
        assertEquals(59_000, s.unallocatedLeft)
    }

    @Test
    fun overspending_eatsSavings() {
        val big = InstantExpense(1, "Putovanje", 70_000, LocalDate.of(2026, 10, 7))
        val s = BudgetCalculator.calculate(input(instant = listOf(big)))
        assertEquals(0, s.unallocatedLeft)
        assertEquals(6_000, s.savingsEaten)
        assertEquals(10_000, s.savingsCurrent)
        assertFalse(s.isOverBudget)
    }

    @Test
    fun bonus_isSplitByPercentWithoutRecalculation() {
        val (toSavings, toFree) = SavingsRules.splitBonus(5_000, 20)
        assertEquals(1_000, toSavings)
        assertEquals(4_000, toFree)

        val bonus = Bonus(1, "Bonus", 5_000, LocalDate.of(2026, 10, 7), toSavings, toFree)
        val s = BudgetCalculator.calculate(input(bonuses = listOf(bonus)))
        assertEquals(16_000, s.savingsGoal)
        assertEquals(17_000, s.savingsCurrent)
        assertEquals(68_000, s.unallocatedLeft)
    }

    @Test
    fun recurringIncome_countsOnlyAfterItsDate() {
        val incomes = listOf(salary, sideBusiness)

        val before = BudgetCalculator.calculate(input(incomes = incomes, asOf = LocalDate.of(2026, 10, 8)))
        assertEquals(120_000, before.incomeTotal)
        assertFalse(before.incomeLines.last().arrived)

        val after = BudgetCalculator.calculate(input(incomes = incomes, asOf = LocalDate.of(2026, 10, 15)))
        assertEquals(140_000, after.incomeTotal)
        assertTrue(after.incomeLines.last().arrived)
    }

    @Test
    fun overrides_changeOnlyThatPeriod() {
        val s = BudgetCalculator.calculate(
            input(
                incomeOverrides = mapOf(salary.id to 130_000L),
                expenseOverrides = mapOf(electricity.id to 8_000L)
            )
        )
        assertEquals(130_000, s.incomeTotal)
        assertEquals(10_000, s.fixedTotal)
        assertEquals(92_000, s.base)
        assertEquals(18_400, s.savingsGoal)
        assertTrue(s.incomeLines.first().isOverridden)
    }

    @Test
    fun percent_isClampedTo0Through50() {
        assertEquals(50, SavingsRules.clampPercent(80))
        assertEquals(0, SavingsRules.clampPercent(-5))
        val s = BudgetCalculator.calculate(input(percent = 90))
        assertEquals(50, s.savingsPercent)
        assertEquals(40_000, s.savingsGoal)
    }
}