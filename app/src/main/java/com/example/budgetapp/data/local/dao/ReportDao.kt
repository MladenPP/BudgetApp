package com.example.budgetapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.budgetapp.data.local.entity.ReportSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query("SELECT * FROM period_reports ORDER BY periodStart DESC")
    fun observeAll(): Flow<List<ReportSnapshotEntity>>

    @Query("SELECT * FROM period_reports WHERE periodStart = :periodStart")
    suspend fun getByPeriodStart(periodStart: Long): ReportSnapshotEntity?

    @Query(
        "SELECT COUNT(*) FROM period_reports " +
                "WHERE periodStart < :endExclusive AND periodEndExclusive > :start"
    )
    suspend fun countOverlapping(start: Long, endExclusive: Long): Int

    @Upsert
    suspend fun upsert(entity: ReportSnapshotEntity)
}