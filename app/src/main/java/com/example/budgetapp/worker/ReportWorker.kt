package com.example.budgetapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgetapp.BudgetApplication
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.calc.ReportGenerator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BudgetApplication).container
        val repository = container.budgetRepository
        val settings = container.settingsRepository
        val force = inputData.getBoolean(KEY_FORCE, false)

        if (!repository.observeHasMainIncome().first()) return Result.success()

        val today = LocalDate.now()
        val mainDay = repository.observeMainIncomeDay().first()
        val current = PeriodCalculator.periodFor(today, mainDay)

        val target = when {
            force -> current
            today == current.endInclusive -> current
            today == current.start -> PeriodCalculator.previousPeriod(current, mainDay)
            else -> return Result.success()
        }

        if (!force) {
            if (settings.lastReportNotifiedPeriod.first() == target.key) return Result.success()
            if (target.endExclusive.toEpochDay() <= settings.firstUseDay.first()) {
                return Result.success()
            }
        }

        val summary = repository.observeSummary(target, today).first()
        val shown = ReportNotifier.show(applicationContext, ReportGenerator.generate(summary))
        if (shown && !force) {
            settings.setLastReportNotifiedPeriod(target.key)
        }
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}