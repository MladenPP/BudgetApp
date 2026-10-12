package com.example.budgetapp.domain.model

import java.time.LocalDate

enum class IncomeKind { MAIN, RECURRING }

data class Income(
    val id: Long,
    val name: String,
    val amount: Long,
    val dayOfMonth: Int,
    val kind: IncomeKind
)

data class Bonus(
    val id: Long,
    val name: String,
    val amount: Long,
    val date: LocalDate,
    val savingsPart: Long,
    val unallocatedPart: Long
)