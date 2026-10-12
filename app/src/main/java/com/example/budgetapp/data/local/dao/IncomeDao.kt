package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.budgetapp.data.local.entity.IncomeEntity
import com.example.budgetapp.data.local.entity.IncomeOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes ORDER BY kind, dayOfMonth")
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getById(id: Long): IncomeEntity?

    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM income_overrides WHERE periodStart = :periodStart")
    fun observeOverrides(periodStart: Long): Flow<List<IncomeOverrideEntity>>

    @Upsert
    suspend fun upsertOverride(entity: IncomeOverrideEntity)

    @Query("DELETE FROM income_overrides WHERE incomeId = :incomeId AND periodStart = :periodStart")
    suspend fun deleteOverride(incomeId: Long, periodStart: Long)
}