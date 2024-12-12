package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()

    private val _sortOrder = MutableStateFlow(true) // true for descending
    private val sortOrder = _sortOrder.asStateFlow()

    val sortedSaleRecords: StateFlow<List<SaleRecord>> = sortOrder.flatMapLatest { isDescending ->
        if (isDescending) {
            saleRecordDao.getAllSaleRecordsDesc()
        } else {
            saleRecordDao.getAllSaleRecordsAsc()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch {
            saleRecordDao.insertSaleRecord(saleRecord)
        }
    }

    fun deleteSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch {
            saleRecordDao.deleteSaleRecord(saleRecord)
        }
    }

    fun toggleSortOrder() {
        _sortOrder.value = !_sortOrder.value
    }

    fun isDescendingOrder() = _sortOrder.value
} 