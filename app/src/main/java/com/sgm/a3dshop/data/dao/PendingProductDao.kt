package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.PendingProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingProductDao {
    @Query("SELECT * FROM pending_products ORDER BY createdAt ASC")
    fun getAllByCreatedAtAsc(): Flow<List<PendingProduct>>

    @Query("SELECT * FROM pending_products ORDER BY createdAt DESC")
    fun getAllByCreatedAtDesc(): Flow<List<PendingProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingProduct: PendingProduct)

    @Update
    suspend fun update(pendingProduct: PendingProduct)

    @Delete
    suspend fun delete(pendingProduct: PendingProduct)

    @Query("SELECT * FROM pending_products WHERE id = :id")
    suspend fun getById(id: Int): PendingProduct?

    @Query("DELETE FROM pending_products")
    suspend fun deleteAll()
} 