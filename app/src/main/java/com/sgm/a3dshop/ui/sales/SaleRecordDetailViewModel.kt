package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import kotlinx.coroutines.launch

class SaleRecordDetailViewModel(
    application: Application,
    private val recordId: Long
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()

    private val _saleRecord = MutableLiveData<SaleRecord?>()
    val saleRecord: LiveData<SaleRecord?> = _saleRecord

    init {
        loadSaleRecord()
    }

    private fun loadSaleRecord() {
        viewModelScope.launch {
            _saleRecord.value = saleRecordDao.getSaleRecordById(recordId)
        }
    }

    fun updateSaleRecord(name: String, price: Double, note: String?) {
        viewModelScope.launch {
            _saleRecord.value?.let { record ->
                val updatedRecord = record.copy(
                    name = name,
                    salePrice = price,
                    note = note
                )
                saleRecordDao.updateSaleRecord(updatedRecord)
                _saleRecord.value = updatedRecord
            }
        }
    }

    fun updateImage(imagePath: String) {
        viewModelScope.launch {
            _saleRecord.value?.let { record ->
                val updatedRecord = record.copy(imageUrl = imagePath)
                saleRecordDao.updateSaleRecord(updatedRecord)
                _saleRecord.value = updatedRecord
            }
        }
    }
} 