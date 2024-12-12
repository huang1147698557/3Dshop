package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleRecordDao {
    @Query("SELECT * FROM sale_records ORDER BY createTime DESC")
    fun getAllSaleRecordsDesc(): Flow<List<SaleRecord>>

    @Query("SELECT * FROM sale_records ORDER BY createTime ASC")
    fun getAllSaleRecordsAsc(): Flow<List<SaleRecord>>

    @Query("SELECT * FROM sale_records ORDER BY createTime DESC")
    suspend fun getAllSaleRecordsSync(): List<SaleRecord>

    @Query("SELECT * FROM sale_records WHERE id = :id")
    fun getSaleRecordById(id: Long): Flow<SaleRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecord(saleRecord: SaleRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleRecords(saleRecords: List<SaleRecord>)

    @Update
    suspend fun updateSaleRecord(saleRecord: SaleRecord)

    @Delete
    suspend fun deleteSaleRecord(saleRecord: SaleRecord)

    @Query("DELETE FROM sale_records")
    suspend fun deleteAllSaleRecords()
} 