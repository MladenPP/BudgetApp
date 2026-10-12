package com.example.budgetapp.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PeriodEdgeCasesTest {

    private val firstDate = LocalDate.of(2027, 1, 1)
    private val lastDate = LocalDate.of(2028, 12, 31)

    private fun allDates() = generateSequence(firstDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(lastDate) }

    @Test
    fun everyDateBelongsToExactlyItsPeriod_forEveryMainIncomeDay() {
        for (day in 1..31) {
            for (date in allDates()) {
                val period = PeriodCalculator.periodFor(date, day)
                assertTrue("day=$day date=$date", period.contains(date))
                // Računanje je stabilno: ista perioda za svaki datum unutar nje
                assertEquals("day=$day start", period, PeriodCalculator.periodFor(period.start, day))
                assertEquals("day=$day end", period, PeriodCalculator.periodFor(period.endInclusive, day))
            }
        }
    }

    @Test
    fun periodsAreContiguous_noGapsAndNoOverlap() {
        for (day in 1..31) {
            var period = PeriodCalculator.periodFor(firstDate, day)
            repeat(30) {
                val next = PeriodCalculator.nextPeriod(period, day)
                assertEquals("day=$day", period.endExclusive, next.start)
                assertEquals("day=$day", period, PeriodCalculator.previousPeriod(next, day))
                val length = ChronoUnit.DAYS.between(period.start, period.endExclusive)
                assertTrue("day=$day length=$length", length in 28..31)
                period = next
            }
        }
    }

    @Test
    fun mainIncomeOn31st_followsShortMonths() {
        // 31. u mesecu: u februaru period počinje 28. (poslednji dan meseca)
        val feb = PeriodCalculator.periodFor(LocalDate.of(2027, 2, 15), 31)
        assertEquals(LocalDate.of(2027, 1, 31), feb.start)
        assertEquals(LocalDate.of(2027, 2, 28), feb.endExclusive)

        val march = PeriodCalculator.periodFor(LocalDate.of(2027, 3, 1), 31)
        assertEquals(LocalDate.of(2027, 2, 28), march.start)
        assertEquals(LocalDate.of(2027, 3, 31), march.endExclusive)
    }

    @Test
    fun leapYear_februaryHas29Days() {
        val period = PeriodCalculator.periodFor(LocalDate.of(2028, 2, 29), 30)
        assertEquals(LocalDate.of(2028, 2, 29), period.start)
        assertEquals(LocalDate.of(2028, 3, 30), period.endExclusive)
    }

    @Test
    fun periodAcrossNewYear() {
        val period = PeriodCalculator.periodFor(LocalDate.of(2027, 1, 3), 5)
        assertEquals(LocalDate.of(2026, 12, 5), period.start)
        assertEquals(LocalDate.of(2027, 1, 5), period.endExclusive)
    }

    @Test
    fun recurringIncomeDate_alwaysInsideThePeriod_forEveryCombination() {
        for (mainDay in 1..31) {
            for (date in allDates().filter { it.dayOfMonth % 3 == 0 }) {
                val period = PeriodCalculator.periodFor(date, mainDay)
                for (recurringDay in 1..31) {
                    val occurrence = PeriodCalculator.occurrenceDate(recurringDay, period)
                    assertTrue(
                        "main=$mainDay recurring=$recurringDay period=$period occurrence=$occurrence",
                        period.contains(occurrence)
                    )
                }
            }
        }
    }

    @Test
    fun recurringIncome_dayThatWouldFallOnNextPeriodStart_arrivesOnLastDayOfThisPeriod() {
        // glavno primanje 29., period 29. jan - 27. feb; stalno primanje 28. bi palo na 28. feb
        val period = PeriodCalculator.periodFor(LocalDate.of(2027, 2, 10), 29)
        assertEquals(LocalDate.of(2027, 1, 29), period.start)
        assertEquals(LocalDate.of(2027, 2, 28), period.endExclusive)
        assertEquals(LocalDate.of(2027, 2, 27), PeriodCalculator.occurrenceDate(28, period))
    }

    @Test
    fun recurringIncome_dayBeforePeriodStart_arrivesInNextMonth() {
        val period = PeriodCalculator.periodFor(LocalDate.of(2026, 10, 10), 5)
        assertEquals(LocalDate.of(2026, 11, 3), PeriodCalculator.occurrenceDate(3, period))
        assertEquals(LocalDate.of(2026, 10, 5), PeriodCalculator.occurrenceDate(5, period))
        assertEquals(LocalDate.of(2026, 10, 31), PeriodCalculator.occurrenceDate(31, period))
    }
}