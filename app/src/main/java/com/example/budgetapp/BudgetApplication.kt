package com.example.budgetapp

import android.app.Application
import com.example.budgetapp.data.AppContainer
import com.example.budgetapp.worker.ReportScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class BudgetApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        applicationScope.launch {

            container.settingsRepository.ensureFirstUseDay(LocalDate.now())

            container.budgetRepository.archiveFinishedPeriods(LocalDate.now())
        }

        ReportScheduler.schedule(this)
    }
}