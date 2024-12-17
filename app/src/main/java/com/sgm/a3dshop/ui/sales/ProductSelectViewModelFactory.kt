package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ProductSelectViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductSelectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductSelectViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 