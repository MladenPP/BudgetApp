package com.example.budgetapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetapp.data.settings.SettingsRepository
import com.example.budgetapp.domain.calc.SavingsRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val resetAll: suspend () -> Unit
) : ViewModel() {

    val savingsPercent: StateFlow<Int> = settings.savingsPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsRules.DEFAULT_PERCENT)

    fun setSavingsPercent(percent: Int) {
        viewModelScope.launch { settings.setSavingsPercent(percent) }
    }

    fun resetApp(onDone: () -> Unit) {
        viewModelScope.launch {
            resetAll()
            onDone()
        }
    }
}