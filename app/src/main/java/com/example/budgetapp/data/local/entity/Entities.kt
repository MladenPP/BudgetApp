package com.example.budgetapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.IncomeKind


@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dayOfMonth: Int,
    val kind: IncomeKind
)

@Entity(tableName = "bonuses")
data class BonusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dateEpochDay: Long,
    val savingsPart: Long,
    val unallocatedPart: Long
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val kind: ExpenseKind
)

@Entity(tableName = "instant_expenses")
data class InstantExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dateEpochDay: Long
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val amount: Long,
    val dateEpochDay: Long,
    val note: String
)

@Entity(
    tableName = "income_overrides",
    primaryKeys = ["incomeId", "periodStart"],
    foreignKeys = [
        ForeignKey(
            entity = IncomeEntity::class,
            parentColumns = ["id"],
            childColumns = ["incomeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("incomeId")]
)
data class IncomeOverrideEntity(
    val incomeId: Long,
    val periodStart: Long,
    val amount: Long
)

@Entity(
    tableName = "expense_overrides",
    primaryKeys = ["expenseId", "periodStart"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId")]
)
data class ExpenseOverrideEntity(
    val expenseId: Long,
    val periodStart: Long,
    val amount: Long
)


@Entity(tableName = "period_reports")
data class ReportSnapshotEntity(
    @PrimaryKey val periodStart: Long,
    val periodEndExclusive: Long,
    val savingsPercent: Int,
    val incomeTotal: Long,
    val bonusTotal: Long,
    val fixedTotal: Long,
    val instantSpent: Long,
    val plannedSavings: Long,
    val savingsEaten: Long,
    val unallocatedLeft: Long,
    val remainingReserve: Long,
    val actualSaved: Long,
    val progressiveEncoded: String,
    val lastInstantName: String?,
    val lastInstantAmount: Long?,
    val lastInstantDate: Long?
)