package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.model.DailySales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()

    private val _dailySales = MutableStateFlow<List<DailySales>>(emptyList())
    val dailySales: StateFlow<List<DailySales>> = _dailySales

    init {
        loadSaleRecords()
    }

    private fun loadSaleRecords() {
        viewModelScope.launch {
            saleRecordDao.getAllSaleRecords().collect { records ->
                // 按日期分组
                val groupedRecords = records.groupBy { record ->
                    // 将时间戳转换为当天的开始时间（00:00:00）
                    Calendar.getInstance().apply {
                        time = record.createdAt
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                }.map { (date, dailyRecords) ->
                    // 计算每天的总金额
                    val totalAmount = dailyRecords.sumOf { it.salePrice }
                    // 按时间倒序排序每天内的记录
                    val sortedRecords = dailyRecords.sortedByDescending { it.createdAt }
                    DailySales(date, sortedRecords, totalAmount)
                }.sortedByDescending { it.date } // 按日期倒序排序

                _dailySales.value = groupedRecords
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