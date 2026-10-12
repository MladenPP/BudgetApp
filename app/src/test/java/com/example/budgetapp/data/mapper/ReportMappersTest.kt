package com.example.budgetapp.data.mapper

import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.ReportProgressiveLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportMappersTest {

    private val period = BudgetPeriod(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 5))

    private fun report(lastInstant: InstantExpense?) = PeriodReport(
        period = period,
        savingsPercent = 20,
        incomeTotal = 120_000,
        bonusTotal = 5_000,
        fixedTotal = 12_000,
        instantSpent = 70_000,
        plannedSavings = 17_000,
        savingsEaten = 6_000,
        unallocatedLeft = 0,
        remainingReserve = 0,
        actualSaved = 11_000,
        progressiveLines = listOf(
            ReportProgressiveLine("Hrana", 28_000, 33_000),
            ReportProgressiveLine("Gorivo", 10_000, 4_000)
        ),
        lastInstantExpense = lastInstant
    )

    @Test
    fun report_survivesRoundTripThroughEntity_withLastInstantExpense() {
        val original = report(InstantExpense(0, "Putovanje", 65_000, LocalDate.of(2026, 3, 25)))
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun report_survivesRoundTripThroughEntity_withoutLastInstantExpense() {
        val original = report(null)
        val restored = original.toEntity().toDomain()
        assertEquals(original, restored)
        assertNull(restored.lastInstantExpense)
    }

    @Test
    fun progressiveLines_roundTrip_andEmptyList() {
        val lines = listOf(ReportProgressiveLine("Hrana", 28_000, 33_000))
        assertEquals(lines, ProgressiveLinesCodec.decode(ProgressiveLinesCodec.encode(lines)))
        assertEquals(
            emptyList<ReportProgressiveLine>(),
            ProgressiveLinesCodec.decode(ProgressiveLinesCodec.encode(emptyList()))
        )
    }

    @Test
    fun progressiveLines_nameWithSeparators_isCleaned() {
        val encoded = ProgressiveLinesCodec.encode(
            listOf(ReportProgressiveLine("Ha\tna\nhrana", 1_000, 2_000))
        )
        val decoded = ProgressiveLinesCodec.decode(encoded).single()
        assertEquals("Ha na hrana", decoded.name)
        assertEquals(1_000, decoded.reserved)
        assertEquals(2_000, decoded.spent)
    }
}