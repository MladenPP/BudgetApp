package com.example.budgetapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetapp.data.local.dao.BonusDao
import com.example.budgetapp.data.local.dao.ExpenseDao
import com.example.budgetapp.data.local.dao.IncomeDao
import com.example.budgetapp.data.local.dao.InstantExpenseDao
import com.example.budgetapp.data.local.dao.PurchaseDao
import com.example.budgetapp.data.local.dao.ReportDao
import com.example.budgetapp.data.local.entity.BonusEntity
import com.example.budgetapp.data.local.entity.ExpenseEntity
import com.example.budgetapp.data.local.entity.ExpenseOverrideEntity
import com.example.budgetapp.data.local.entity.IncomeEntity
import com.example.budgetapp.data.local.entity.IncomeOverrideEntity
import com.example.budgetapp.data.local.entity.InstantExpenseEntity
import com.example.budgetapp.data.local.entity.PurchaseEntity
import com.example.budgetapp.data.local.entity.ReportSnapshotEntity

@Database(
    entities = [
        IncomeEntity::class,
        BonusEntity::class,
        ExpenseEntity::class,
        InstantExpenseEntity::class,
        PurchaseEntity::class,
        IncomeOverrideEntity::class,
        ExpenseOverrideEntity::class,
        ReportSnapshotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun incomeDao(): IncomeDao
    abstract fun bonusDao(): BonusDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun instantExpenseDao(): InstantExpenseDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var instance: BudgetDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `period_reports` (" +
                            "`periodStart` INTEGER NOT NULL, " +
                            "`periodEndExclusive` INTEGER NOT NULL, " +
                            "`savingsPercent` INTEGER NOT NULL, " +
                            "`incomeTotal` INTEGER NOT NULL, " +
                            "`bonusTotal` INTEGER NOT NULL, " +
                            "`fixedTotal` INTEGER NOT NULL, " +
                            "`instantSpent` INTEGER NOT NULL, " +
                            "`plannedSavings` INTEGER NOT NULL, " +
                            "`savingsEaten` INTEGER NOT NULL, " +
                            "`unallocatedLeft` INTEGER NOT NULL, " +
                            "`remainingReserve` INTEGER NOT NULL, " +
                            "`actualSaved` INTEGER NOT NULL, " +
                            "`progressiveEncoded` TEXT NOT NULL, " +
                            "`lastInstantName` TEXT, " +
                            "`lastInstantAmount` INTEGER, " +
                            "`lastInstantDate` INTEGER, " +
                            "PRIMARY KEY(`periodStart`))"
                )
            }
        }

        fun getInstance(context: Context): BudgetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}