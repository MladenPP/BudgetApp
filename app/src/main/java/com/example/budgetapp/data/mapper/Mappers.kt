package com.example.budgetapp.data.mapper

import com.example.budgetapp.data.local.entity.BonusEntity
import com.example.budgetapp.data.local.entity.ExpenseEntity
import com.example.budgetapp.data.local.entity.IncomeEntity
import com.example.budgetapp.data.local.entity.InstantExpenseEntity
import com.example.budgetapp.data.local.entity.PurchaseEntity
import com.example.budgetapp.domain.model.Bonus
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.Purchase
import java.time.LocalDate

fun IncomeEntity.toDomain() = Income(id, name, amount, dayOfMonth, kind)
fun Income.toEntity() = IncomeEntity(id, name, amount, dayOfMonth, kind)

fun BonusEntity.toDomain() = Bonus(
    id, name, amount, LocalDate.ofEpochDay(dateEpochDay), savingsPart, unallocatedPart
)

fun Bonus.toEntity() = BonusEntity(
    id, name, amount, date.toEpochDay(), savingsPart, unallocatedPart
)

fun ExpenseEntity.toDomain() = Expense(id, name, amount, kind)
fun Expense.toEntity() = ExpenseEntity(id, name, amount, kind)

fun InstantExpenseEntity.toDomain() = InstantExpense(
    id, name, amount, LocalDate.ofEpochDay(dateEpochDay)
)

fun InstantExpense.toEntity() = InstantExpenseEntity(id, name, amount, date.toEpochDay())

fun PurchaseEntity.toDomain() = Purchase(
    id, expenseId, amount, LocalDate.ofEpochDay(dateEpochDay), note
)

fun Purchase.toEntity() = PurchaseEntity(id, expenseId, amount, date.toEpochDay(), note)