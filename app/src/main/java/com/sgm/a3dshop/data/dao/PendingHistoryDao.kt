package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.PendingHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingHistoryDao {
    @Query("SELECT * FROM pending_history ORDER BY deletedAt DESC")
    fun getAllByDeletedAtDesc(): Flow<List<PendingHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingHistory: PendingHistory)

    @Delete
    suspend fun delete(pendingHistory: PendingHistory)

    @Query("DELETE FROM pending_history")
    suspend fun deleteAll()
} 