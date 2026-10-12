package com.example.budgetapp.data.repository

import com.example.budgetapp.data.local.dao.BonusDao
import com.example.budgetapp.data.local.dao.ExpenseDao
import com.example.budgetapp.data.local.dao.IncomeDao
import com.example.budgetapp.data.local.dao.InstantExpenseDao
import com.example.budgetapp.data.local.dao.PurchaseDao
import com.example.budgetapp.data.local.dao.ReportDao
import com.example.budgetapp.data.local.entity.ExpenseOverrideEntity
import com.example.budgetapp.data.local.entity.IncomeEntity
import com.example.budgetapp.data.local.entity.IncomeOverrideEntity
import com.example.budgetapp.data.mapper.toDomain
import com.example.budgetapp.data.mapper.toEntity
import com.example.budgetapp.data.settings.SettingsRepository
import com.example.budgetapp.domain.calc.BudgetCalculator
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.calc.ReportGenerator
import com.example.budgetapp.domain.calc.SavingsRules
import com.example.budgetapp.domain.model.Bonus
import com.example.budgetapp.domain.model.BudgetInput
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.IncomeKind
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BudgetRepository(
    private val incomeDao: IncomeDao,
    private val bonusDao: BonusDao,
    private val expenseDao: ExpenseDao,
    private val purchaseDao: PurchaseDao,
    private val instantExpenseDao: InstantExpenseDao,
    private val reportDao: ReportDao,
    private val settings: SettingsRepository
) {


    fun observeHasMainIncome(): Flow<Boolean> =
        incomeDao.observeAll()
            .map { list -> list.any { it.kind == IncomeKind.MAIN } }
            .distinctUntilChanged()

    fun observeMainIncomeDay(): Flow<Int> =
        incomeDao.observeAll()
            .map { list ->
                list.firstOrNull { it.kind == IncomeKind.MAIN }?.dayOfMonth ?: DEFAULT_PERIOD_DAY
            }
            .distinctUntilChanged()

    fun observeSummary(period: BudgetPeriod, today: LocalDate): Flow<BudgetSummary> {
        val fromDay = period.start.toEpochDay()
        val toDay = period.endExclusive.toEpochDay()

        val templates = combine(
            incomeDao.observeAll(),
            expenseDao.observeAll(),
            settings.savingsPercent
        ) { incomes, expenses, percent ->
            Templates(incomes.map { it.toDomain() }, expenses.map { it.toDomain() }, percent)
        }

        val overrides = combine(
            incomeDao.observeOverrides(period.key),
            expenseDao.observeOverrides(period.key)
        ) { incomeOverrides, expenseOverrides ->
            Overrides(
                income = incomeOverrides.associate { it.incomeId to it.amount },
                expense = expenseOverrides.associate { it.expenseId to it.amount }
            )
        }

        val entries = combine(
            bonusDao.observeBetween(fromDay, toDay),
            purchaseDao.observeBetween(fromDay, toDay),
            instantExpenseDao.observeBetween(fromDay, toDay)
        ) { bonuses, purchases, instant ->
            Entries(
                bonuses.map { it.toDomain() },
                purchases.map { it.toDomain() },
                instant.map { it.toDomain() }
            )
        }

        return combine(templates, overrides, entries) { t, o, e ->
            BudgetCalculator.calculate(
                BudgetInput(
                    period = period,
                    today = today,
                    savingsPercent = t.percent,
                    incomes = t.incomes,
                    incomeOverrides = o.income,
                    bonuses = e.bonuses,
                    expenses = t.expenses,
                    expenseOverrides = o.expense,
                    purchases = e.purchases,
                    instantExpenses = e.instant
                )
            )
        }
    }

    fun observePurchases(period: BudgetPeriod): Flow<List<Purchase>> =
        purchaseDao.observeBetween(period.start.toEpochDay(), period.endExclusive.toEpochDay())
            .map { list -> list.map { it.toDomain() } }


    fun observeSavedReports(): Flow<List<PeriodReport>> =
        reportDao.observeAll().map { list -> list.map { it.toDomain() } }


    suspend fun archiveFinishedPeriods(today: LocalDate) {
        if (!observeHasMainIncome().first()) return

        val mainDay = observeMainIncomeDay().first()
        val firstUseDay = settings.firstUseDay.first()
        val current = PeriodCalculator.periodFor(today, mainDay)

        var period = PeriodCalculator.previousPeriod(current, mainDay)
        var checked = 0
        while (checked < MAX_ARCHIVED_PERIODS_PER_RUN &&
            period.endExclusive.toEpochDay() > firstUseDay &&
            reportDao.countOverlapping(
                period.start.toEpochDay(),
                period.endExclusive.toEpochDay()
            ) == 0
        ) {
            val summary = observeSummary(period, today).first()
            reportDao.upsert(ReportGenerator.generate(summary).toEntity())
            period = PeriodCalculator.previousPeriod(period, mainDay)
            checked++
        }
    }

    suspend fun saveMainIncome(name: String, amount: Long, dayOfMonth: Int) {
        val day = dayOfMonth.coerceIn(1, 31)
        val existing = incomeDao.observeAll().first().firstOrNull { it.kind == IncomeKind.MAIN }
        if (existing == null) {
            incomeDao.insert(
                IncomeEntity(name = name, amount = amount, dayOfMonth = day, kind = IncomeKind.MAIN)
            )
        } else {
            incomeDao.update(existing.copy(name = name, amount = amount, dayOfMonth = day))
        }
    }

    suspend fun addRecurringIncome(name: String, amount: Long, dayOfMonth: Int) {
        incomeDao.insert(
            IncomeEntity(
                name = name,
                amount = amount,
                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                kind = IncomeKind.RECURRING
            )
        )
    }

    suspend fun updateIncome(income: Income) {
        incomeDao.update(income.copy(dayOfMonth = income.dayOfMonth.coerceIn(1, 31)).toEntity())
    }

    suspend fun deleteIncome(id: Long) = incomeDao.deleteById(id)

    suspend fun setIncomeOverride(incomeId: Long, period: BudgetPeriod, amount: Long) {
        incomeDao.upsertOverride(IncomeOverrideEntity(incomeId, period.key, amount))
    }

    suspend fun clearIncomeOverride(incomeId: Long, period: BudgetPeriod) {
        incomeDao.deleteOverride(incomeId, period.key)
    }

    suspend fun addBonus(name: String, amount: Long, date: LocalDate) {
        val percent = settings.savingsPercent.first()
        val (toSavings, toFree) = SavingsRules.splitBonus(amount, percent)
        bonusDao.insert(Bonus(0, name, amount, date, toSavings, toFree).toEntity())
    }

    suspend fun deleteBonus(id: Long) = bonusDao.deleteById(id)


    suspend fun addExpense(name: String, amount: Long, kind: ExpenseKind) {
        expenseDao.insert(Expense(0, name, amount, kind).toEntity())
    }

    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense.toEntity())

    suspend fun deleteExpense(id: Long) = expenseDao.deleteById(id)

    suspend fun setExpenseOverride(expenseId: Long, period: BudgetPeriod, amount: Long) {
        expenseDao.upsertOverride(ExpenseOverrideEntity(expenseId, period.key, amount))
    }

    suspend fun clearExpenseOverride(expenseId: Long, period: BudgetPeriod) {
        expenseDao.deleteOverride(expenseId, period.key)
    }

    suspend fun addPurchase(expenseId: Long, amount: Long, date: LocalDate, note: String) {
        purchaseDao.insert(Purchase(0, expenseId, amount, date, note).toEntity())
    }

    suspend fun deletePurchase(id: Long) = purchaseDao.deleteById(id)

    suspend fun addInstantExpense(name: String, amount: Long, date: LocalDate) {
        instantExpenseDao.insert(InstantExpense(0, name, amount, date).toEntity())
    }

    suspend fun deleteInstantExpense(id: Long) = instantExpenseDao.deleteById(id)


    private data class Templates(
        val incomes: List<Income>,
        val expenses: List<Expense>,
        val percent: Int
    )

    private data class Overrides(
        val income: Map<Long, Long>,
        val expense: Map<Long, Long>
    )

    private data class Entries(
        val bonuses: List<Bonus>,
        val purchases: List<Purchase>,
        val instant: List<InstantExpense>
    )

    companion object {
        const val DEFAULT_PERIOD_DAY = 1

        private const val MAX_ARCHIVED_PERIODS_PER_RUN = 36
    }
}