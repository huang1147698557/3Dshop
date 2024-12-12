package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()

    private val _saleRecords = MutableStateFlow<List<SaleRecord>>(emptyList())
    val saleRecords: StateFlow<List<SaleRecord>> = _saleRecords.asStateFlow()

    init {
        loadSaleRecords()
    }

    private fun loadSaleRecords() {
        viewModelScope.launch {
            saleRecordDao.getAllSaleRecords().collect { records ->
                _saleRecords.value = records
            }
        }
    }

    fun insertSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch {
            saleRecordDao.insertSaleRecord(saleRecord)
        }
    }

    fun updateSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch {
            saleRecordDao.updateSaleRecord(saleRecord)
        }
    }

    fun deleteSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch {
            saleRecordDao.deleteSaleRecord(saleRecord)
        }
    }

    fun deleteAllSaleRecords() {
        viewModelScope.launch {
            saleRecordDao.deleteAllSaleRecords()
        }
    }
} 