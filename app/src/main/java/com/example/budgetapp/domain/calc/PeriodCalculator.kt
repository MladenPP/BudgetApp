package com.example.budgetapp.domain.calc

import com.example.budgetapp.domain.model.BudgetPeriod
import java.time.LocalDate
import java.time.YearMonth

object PeriodCalculator {

    fun periodFor(date: LocalDate, mainIncomeDay: Int): BudgetPeriod {
        val month = YearMonth.from(date)
        val thisMonthStart = dateInMonth(month, mainIncomeDay)
        val start = if (!date.isBefore(thisMonthStart)) {
            thisMonthStart
        } else {
            dateInMonth(month.minusMonths(1), mainIncomeDay)
        }
        val end = dateInMonth(YearMonth.from(start).plusMonths(1), mainIncomeDay)
        return BudgetPeriod(start, end)
    }

    fun nextPeriod(period: BudgetPeriod, mainIncomeDay: Int): BudgetPeriod =
        periodFor(period.endExclusive, mainIncomeDay)

    fun previousPeriod(period: BudgetPeriod, mainIncomeDay: Int): BudgetPeriod =
        periodFor(period.start.minusDays(1), mainIncomeDay)

    fun occurrenceDate(dayOfMonth: Int, period: BudgetPeriod): LocalDate {
        val startMonth = YearMonth.from(period.start)
        val inStartMonth = dateInMonth(startMonth, dayOfMonth)
        val candidate = if (!inStartMonth.isBefore(period.start)) {
            inStartMonth
        } else {
            dateInMonth(startMonth.plusMonths(1), dayOfMonth)
        }
        return if (candidate.isBefore(period.endExclusive)) candidate else period.endInclusive
    }

    private fun dateInMonth(month: YearMonth, dayOfMonth: Int): LocalDate =
        month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))
}