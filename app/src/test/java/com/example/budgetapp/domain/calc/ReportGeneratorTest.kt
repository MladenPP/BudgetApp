package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.BudgetInput
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.IncomeKind
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportGeneratorTest {

    private val today = LocalDate.of(2026, 11, 4)
    private val period = PeriodCalculator.periodFor(LocalDate.of(2026, 10, 10), mainIncomeDay = 5)

    private val salary = Income(1, "Plata", 120_000, 5, IncomeKind.MAIN)
    private val electricity = Expense(10, "Struja", 10_000, ExpenseKind.FIXED)
    private val water = Expense(11, "Voda", 2_000, ExpenseKind.FIXED)
    private val food = Expense(12, "Hrana", 28_000, ExpenseKind.PROGRESSIVE)

    private fun report(
        purchases: List<Purchase> = emptyList(),
        instant: List<InstantExpense> = emptyList()
    ): PeriodReport {
        val summary = BudgetCalculator.calculate(
            BudgetInput(
                period = period,
                today = today,
                savingsPercent = 20,
                incomes = listOf(salary),
                incomeOverrides = emptyMap(),
                bonuses = emptyList(),
                expenses = listOf(electricity, water, food),
                expenseOverrides = emptyMap(),
                purchases = purchases,
                instantExpenses = instant
            )
        )
        return ReportGenerator.generate(summary)
    }

    private fun foodPurchase(amount: Long) =
        Purchase(1, food.id, amount, LocalDate.of(2026, 10, 12), "")

    private fun instant(id: Long, name: String, amount: Long, day: Int) =
        InstantExpense(id, name, amount, LocalDate.of(2026, 10, day))

    @Test
    fun savedMoreThanPlanned_whenNotEverythingIsSpent() {
        // hrana potrošena 20k od 28k rezerve, ništa drugo
        val r = report(purchases = listOf(foodPurchase(20_000)))
        assertEquals(16_000, r.plannedSavings)
        // 16k štednja + 64k slobodno + 8k neiskorišćena rezerva
        assertEquals(88_000, r.actualSaved)
        assertEquals(8_000, r.remainingReserve)
        assertTrue(r.metGoal)
        assertEquals(72_000, r.difference)
        assertNull(r.lastInstantExpense)
    }

    @Test
    fun exactlyOnPlan_whenAllFreeMoneyIsSpent() {
        val r = report(
            purchases = listOf(foodPurchase(28_000)),
            instant = listOf(instant(1, "Kafana", 64_000, 20))
        )
        assertEquals(16_000, r.actualSaved)
        assertEquals(0, r.difference)
        assertTrue(r.metGoal)
    }

    @Test
    fun belowPlan_progressiveOverspent_isReportedWithAmount() {
        // hrana 33k od 28k (preko 5k) + kafana 60k: slobodnog nema dovoljno
        val r = report(
            purchases = listOf(foodPurchase(33_000)),
            instant = listOf(instant(1, "Kafana", 60_000, 20))
        )
        assertFalse(r.metGoal)
        assertEquals(15_000, r.actualSaved)
        assertEquals(-1_000, r.difference)
        val line = r.overspentProgressive.single()
        assertEquals("Hrana", line.name)
        assertEquals(5_000, line.over)
        // smanjenje hrane za 5k bi bilo dovoljno (fali samo 1k), pa nema poslednjeg troška
        assertNull(r.lastInstantExpense)
    }

    @Test
    fun belowPlan_reducingOverspendWouldJustReachGoal_noLastInstant() {
        // hrana preko rezerve za 700, kafana 64k: fali tačno 700, što je baš prekoračenje
        val r = report(
            purchases = listOf(foodPurchase(28_700)),
            instant = listOf(instant(1, "Kafana", 64_000, 20))
        )
        assertEquals(15_300, r.actualSaved)
        assertEquals(700, r.overspentProgressive.single().over)
        assertNull(r.lastInstantExpense)
    }

    @Test
    fun belowPlan_reducingOverspendIsNotEnough_addsLastInstant() {
        // hrana preko rezerve za 700, ali štednja je daleko ispod plana zbog velikog troška
        val r = report(
            purchases = listOf(foodPurchase(28_700)),
            instant = listOf(
                instant(1, "Kafana", 5_000, 8),
                instant(2, "Putovanje", 65_000, 25)
            )
        )
        assertFalse(r.metGoal)
        // potrošnja 70.000 + 700 preko rezerve = 70.700, slobodno 64.000 -> pojedeno 6.700
        assertEquals(9_300, r.actualSaved)
        assertEquals(700, r.overspentProgressive.single().over)
        assertEquals("Putovanje", r.lastInstantExpense?.name)
    }

    @Test
    fun belowPlan_progressiveOk_showsLastInstantExpense() {
        val r = report(
            purchases = listOf(foodPurchase(28_000)),
            instant = listOf(
                instant(1, "Kafana", 40_000, 8),
                instant(2, "Putovanje", 30_000, 25),
                instant(3, "Bioskop", 500, 14)
            )
        )
        assertFalse(r.metGoal)
        // 70.500 potrošeno na trenutne troškove, 64.000 slobodno -> 6.500 pojedeno iz štednje
        assertEquals(9_500, r.actualSaved)
        assertTrue(r.overspentProgressive.isEmpty())
        assertEquals("Putovanje", r.lastInstantExpense?.name)
    }

    @Test
    fun lastInstantExpense_sameDay_usesHighestId() {
        val r = report(
            purchases = listOf(foodPurchase(28_000)),
            instant = listOf(
                instant(5, "Prvo", 40_000, 20),
                instant(9, "Drugo", 40_000, 20)
            )
        )
        assertEquals("Drugo", r.lastInstantExpense?.name)
    }
}