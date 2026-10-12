package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetapp.data.local.entity.BonusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BonusDao {

    @Query(
        "SELECT * FROM bonuses WHERE dateEpochDay >= :fromDay AND dateEpochDay < :toDay " +
                "ORDER BY dateEpochDay DESC, id DESC"
    )
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<BonusEntity>>

    @Insert
    suspend fun insert(bonus: BonusEntity): Long

    @Update
    suspend fun update(bonus: BonusEntity)

    @Query("DELETE FROM bonuses WHERE id = :id")
    suspend fun deleteById(id: Long)
}