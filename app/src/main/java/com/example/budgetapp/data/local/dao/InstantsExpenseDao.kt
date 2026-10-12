package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetapp.data.local.entity.InstantExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstantExpenseDao {

    @Query(
        "SELECT * FROM instant_expenses WHERE dateEpochDay >= :fromDay AND dateEpochDay < :toDay " +
                "ORDER BY dateEpochDay DESC, id DESC"
    )
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<InstantExpenseEntity>>

    @Insert
    suspend fun insert(expense: InstantExpenseEntity): Long

    @Update
    suspend fun update(expense: InstantExpenseEntity)

    @Query("DELETE FROM instant_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}