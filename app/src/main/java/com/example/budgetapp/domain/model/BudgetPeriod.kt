package com.example.budgetapp.domain.model

import java.time.LocalDate

data class BudgetPeriod(
    val start: LocalDate,
    val endExclusive: LocalDate
) {
    val endInclusive: LocalDate get() = endExclusive.minusDays(1)

    val key: Long get() = start.toEpochDay()

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && date.isBefore(endExclusive)
}