package com.example.budgetapp.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


object ReportScheduler {

    private const val WORK_NAME = "report_check"
    private const val CHECK_HOUR = 18

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(CHECK_HOUR, 0)
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val delayMinutes = Duration.between(now, firstRun).toMinutes()

        val request = PeriodicWorkRequestBuilder<ReportWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun sendTest(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReportWorker>()
            .setInputData(workDataOf(ReportWorker.KEY_FORCE to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}