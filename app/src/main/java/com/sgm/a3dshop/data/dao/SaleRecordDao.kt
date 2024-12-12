package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleRecordDao {
    @Query("SELECT * FROM sale_records ORDER BY createdAt DESC")
    fun getAllSaleRecords(): Flow<List<SaleRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecord(saleRecord: SaleRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecords(saleRecords: List<SaleRecord>)

    @Update
    suspend fun updateSaleRecord(saleRecord: SaleRecord)

    @Delete
    suspend fun deleteSaleRecord(saleRecord: SaleRecord)

    @Query("DELETE FROM sale_records")
    suspend fun deleteAllSaleRecords()

    @Query("SELECT * FROM sale_records WHERE id = :saleRecordId")
    suspend fun getSaleRecordById(saleRecordId: Long): SaleRecord

    @Query("SELECT * FROM sale_records ORDER BY createdAt DESC")
    suspend fun getAllSaleRecordsSync(): List<SaleRecord>
} 