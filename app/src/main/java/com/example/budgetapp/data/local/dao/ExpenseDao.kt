package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.budgetapp.data.local.entity.ExpenseEntity
import com.example.budgetapp.data.local.entity.ExpenseOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY kind, name")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expense_overrides WHERE periodStart = :periodStart")
    fun observeOverrides(periodStart: Long): Flow<List<ExpenseOverrideEntity>>

    @Upsert
    suspend fun upsertOverride(entity: ExpenseOverrideEntity)

    @Query("DELETE FROM expense_overrides WHERE expenseId = :expenseId AND periodStart = :periodStart")
    suspend fun deleteOverride(expenseId: Long, periodStart: Long)
}