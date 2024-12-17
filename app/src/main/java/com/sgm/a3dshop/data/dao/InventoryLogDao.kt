package com.sgm.a3dshop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sgm.a3dshop.data.entity.InventoryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLogDao {
    @Insert
    suspend fun insert(log: InventoryLog)

    @Query("SELECT * FROM inventory_logs WHERE productId = :productId ORDER BY createdAt DESC")
    fun getLogsByProductId(productId: Int): Flow<List<InventoryLog>>

    @Query("SELECT * FROM inventory_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<InventoryLog>>
} 