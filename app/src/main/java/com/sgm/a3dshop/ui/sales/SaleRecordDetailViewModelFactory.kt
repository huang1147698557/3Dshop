package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SaleRecordDetailViewModelFactory(
    private val application: Application,
    private val saleRecordId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleRecordDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaleRecordDetailViewModel(application, saleRecordId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 