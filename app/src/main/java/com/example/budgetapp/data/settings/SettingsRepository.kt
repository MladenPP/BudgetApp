package com.example.budgetapp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.budgetapp.domain.calc.SavingsRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val savingsPercent: Flow<Int> = dataStore.data.map { prefs ->
        prefs[SAVINGS_PERCENT]?.let { SavingsRules.clampPercent(it) }
            ?: SavingsRules.DEFAULT_PERCENT
    }

    suspend fun setSavingsPercent(percent: Int) {
        dataStore.edit { prefs ->
            prefs[SAVINGS_PERCENT] = SavingsRules.clampPercent(percent)
        }
    }

    val firstUseDay: Flow<Long> = dataStore.data.map { prefs ->
        prefs[FIRST_USE_DAY] ?: LocalDate.now().toEpochDay()
    }

    suspend fun ensureFirstUseDay(today: LocalDate) {
        dataStore.edit { prefs ->
            if (prefs[FIRST_USE_DAY] == null) {
                prefs[FIRST_USE_DAY] = today.toEpochDay()
            }
        }
    }

    val lastReportNotifiedPeriod: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_REPORT_NOTIFIED] ?: -1L
    }

    suspend fun setLastReportNotifiedPeriod(periodKey: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_REPORT_NOTIFIED] = periodKey
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private companion object {
        val SAVINGS_PERCENT = intPreferencesKey("savings_percent")
        val FIRST_USE_DAY = longPreferencesKey("first_use_day")
        val LAST_REPORT_NOTIFIED = longPreferencesKey("last_report_notified_period")
    }
}