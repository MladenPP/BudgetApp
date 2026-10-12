package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetapp.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Query(
        "SELECT * FROM purchases WHERE dateEpochDay >= :fromDay AND dateEpochDay < :toDay " +
                "ORDER BY dateEpochDay DESC, id DESC"
    )
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<PurchaseEntity>>

    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deleteById(id: Long)
}