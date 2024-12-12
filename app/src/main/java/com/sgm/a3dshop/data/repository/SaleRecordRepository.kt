package com.sgm.a3dshop.data.repository

import com.sgm.a3dshop.data.dao.SaleRecordDao
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.Flow

class SaleRecordRepository(private val saleRecordDao: SaleRecordDao) {
    val allSaleRecords: Flow<List<SaleRecord>> = saleRecordDao.getAllSaleRecords()

    suspend fun insertSaleRecord(saleRecord: SaleRecord) {
        saleRecordDao.insertSaleRecord(saleRecord)
    }

    suspend fun updateSaleRecord(saleRecord: SaleRecord) {
        saleRecordDao.updateSaleRecord(saleRecord)
    }

    suspend fun deleteSaleRecord(saleRecord: SaleRecord) {
        saleRecordDao.deleteSaleRecord(saleRecord)
    }

    suspend fun deleteAllSaleRecords() {
        saleRecordDao.deleteAllSaleRecords()
    }

    suspend fun getSaleRecordById(saleRecordId: Long): SaleRecord {
        return saleRecordDao.getSaleRecordById(saleRecordId)
    }
} 