package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()

    private val _saleRecords = MutableStateFlow<List<SaleRecord>>(emptyList())
    val saleRecords: StateFlow<List<SaleRecord>> = _saleRecords

    private var isAscending = false

    init {
        loadSaleRecords()
    }

    private fun loadSaleRecords() {
        viewModelScope.launch {
            saleRecordDao.getAllSaleRecords().collect { records ->
                _saleRecords.value = if (isAscending) {
                    records.sortedBy { it.createdAt }
                } else {
                    records.sortedByDescending { it.createdAt }
                }
            }
        }
    }

    fun toggleSort() {
        isAscending = !isAscending
        _saleRecords.value = if (isAscending) {
            _saleRecords.value.sortedBy { it.createdAt }
        } else {
            _saleRecords.value.sortedByDescending { it.createdAt }
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