package com.sgm.a3dshop.data.repository

import com.sgm.a3dshop.data.dao.SaleRecordDao
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.Flow

class SaleRecordRepository(private val saleRecordDao: SaleRecordDao) {
    val allSaleRecords: Flow<List<SaleRecord>> = saleRecordDao.getAllSaleRecordsDesc()

    suspend fun insertSaleRecord(saleRecord: SaleRecord): Long {
        return saleRecordDao.insertSaleRecord(saleRecord)
    }

    suspend fun insertSaleRecords(saleRecords: List<SaleRecord>) {
        saleRecordDao.insertSaleRecords(saleRecords)
    }

    suspend fun updateSaleRecord(saleRecord: SaleRecord) {
        saleRecordDao.updateSaleRecord(saleRecord)
    }

    suspend fun deleteSaleRecord(saleRecord: SaleRecord) {
        saleRecordDao.deleteSaleRecord(saleRecord)
    }

    fun getSaleRecordById(id: Long): Flow<SaleRecord> {
        return saleRecordDao.getSaleRecordById(id)
    }

    suspend fun getAllSaleRecordsSync(): List<SaleRecord> {
        return saleRecordDao.getAllSaleRecordsSync()
    }
} 