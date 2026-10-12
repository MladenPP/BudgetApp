package com.example.budgetapp.domain.model

import java.time.LocalDate

enum class ExpenseKind { FIXED, PROGRESSIVE }

data class Expense(
    val id: Long,
    val name: String,
    val amount: Long,
    val kind: ExpenseKind
)

data class InstantExpense(
    val id: Long,
    val name: String,
    val amount: Long,
    val date: LocalDate
)

data class Purchase(
    val id: Long,
    val expenseId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String
)