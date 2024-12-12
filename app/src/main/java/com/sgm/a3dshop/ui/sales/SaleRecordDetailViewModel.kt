package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.*
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.repository.SaleRecordRepository
import kotlinx.coroutines.launch

class SaleRecordDetailViewModel(
    application: Application,
    private val saleRecordId: Long
) : AndroidViewModel(application) {

    private val repository: SaleRecordRepository
    private val _saleRecord = MutableLiveData<SaleRecord>()
    val saleRecord: LiveData<SaleRecord> = _saleRecord

    init {
        val database = AppDatabase.getDatabase(getApplication())
        repository = SaleRecordRepository(database.saleRecordDao())
        loadSaleRecord()
    }

    private fun loadSaleRecord() {
        viewModelScope.launch {
            val record = repository.getSaleRecordById(saleRecordId)
            _saleRecord.postValue(record)
        }
    }

    fun updateSaleRecord(name: String, salePrice: Double, note: String?) {
        val currentRecord = _saleRecord.value ?: return
        val updatedRecord = currentRecord.copy(
            name = name,
            salePrice = salePrice,
            note = note
        )
        viewModelScope.launch {
            repository.updateSaleRecord(updatedRecord)
            _saleRecord.postValue(updatedRecord)
        }
    }
} 