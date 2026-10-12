package com.example.budgetapp.data.mapper

import com.example.budgetapp.data.local.entity.ReportSnapshotEntity
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.ReportProgressiveLine
import java.time.LocalDate


object ProgressiveLinesCodec {

    private const val FIELD_SEPARATOR = '\t'
    private const val LINE_SEPARATOR = '\n'

    fun encode(lines: List<ReportProgressiveLine>): String =
        lines.joinToString(LINE_SEPARATOR.toString()) { line ->
            "${clean(line.name)}$FIELD_SEPARATOR${line.reserved}$FIELD_SEPARATOR${line.spent}"
        }

    fun decode(text: String): List<ReportProgressiveLine> {
        if (text.isEmpty()) return emptyList()
        return text.split(LINE_SEPARATOR).mapNotNull { row ->
            val parts = row.split(FIELD_SEPARATOR)
            if (parts.size != 3) return@mapNotNull null
            val reserved = parts[1].toLongOrNull() ?: return@mapNotNull null
            val spent = parts[2].toLongOrNull() ?: return@mapNotNull null
            ReportProgressiveLine(parts[0], reserved, spent)
        }
    }

    private fun clean(name: String): String =
        name.replace(FIELD_SEPARATOR, ' ').replace(LINE_SEPARATOR, ' ').replace('\r', ' ')
}

fun PeriodReport.toEntity() = ReportSnapshotEntity(
    periodStart = period.start.toEpochDay(),
    periodEndExclusive = period.endExclusive.toEpochDay(),
    savingsPercent = savingsPercent,
    incomeTotal = incomeTotal,
    bonusTotal = bonusTotal,
    fixedTotal = fixedTotal,
    instantSpent = instantSpent,
    plannedSavings = plannedSavings,
    savingsEaten = savingsEaten,
    unallocatedLeft = unallocatedLeft,
    remainingReserve = remainingReserve,
    actualSaved = actualSaved,
    progressiveEncoded = ProgressiveLinesCodec.encode(progressiveLines),
    lastInstantName = lastInstantExpense?.name,
    lastInstantAmount = lastInstantExpense?.amount,
    lastInstantDate = lastInstantExpense?.date?.toEpochDay()
)

fun ReportSnapshotEntity.toDomain(): PeriodReport {
    val name = lastInstantName
    val amount = lastInstantAmount
    val date = lastInstantDate
    val lastInstant = if (name != null && amount != null && date != null) {
        InstantExpense(0, name, amount, LocalDate.ofEpochDay(date))
    } else {
        null
    }
    return PeriodReport(
        period = BudgetPeriod(
            LocalDate.ofEpochDay(periodStart),
            LocalDate.ofEpochDay(periodEndExclusive)
        ),
        savingsPercent = savingsPercent,
        incomeTotal = incomeTotal,
        bonusTotal = bonusTotal,
        fixedTotal = fixedTotal,
        instantSpent = instantSpent,
        plannedSavings = plannedSavings,
        savingsEaten = savingsEaten,
        unallocatedLeft = unallocatedLeft,
        remainingReserve = remainingReserve,
        actualSaved = actualSaved,
        progressiveLines = ProgressiveLinesCodec.decode(progressiveEncoded),
        lastInstantExpense = lastInstant
    )
}