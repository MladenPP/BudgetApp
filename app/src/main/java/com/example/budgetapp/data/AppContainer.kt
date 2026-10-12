package com.example.budgetapp.data

import android.content.Context
import com.example.budgetapp.data.local.BudgetDatabase
import com.example.budgetapp.data.repository.BudgetRepository
import com.example.budgetapp.data.settings.SettingsRepository
import com.example.budgetapp.data.settings.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Ručni DI kontejner: pravi bazu i repozitorijume jednom za celu aplikaciju. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: BudgetDatabase by lazy { BudgetDatabase.getInstance(appContext) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(
            incomeDao = database.incomeDao(),
            bonusDao = database.bonusDao(),
            expenseDao = database.expenseDao(),
            purchaseDao = database.purchaseDao(),
            instantExpenseDao = database.instantExpenseDao(),
            reportDao = database.reportDao(),
            settings = settingsRepository
        )
    }

    /**
     * Briše sve: primanja, troškove, bonuse, kupovine, izmene za mesece, sačuvane izveštaje
     * i podešavanja. Posle toga je aplikacija kao tek instalirana.
     */
    suspend fun resetAll() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settingsRepository.clearAll()
        settingsRepository.ensureFirstUseDay(LocalDate.now())
    }
}