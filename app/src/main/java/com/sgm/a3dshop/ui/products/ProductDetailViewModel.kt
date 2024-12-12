package com.sgm.a3dshop.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    application: Application,
    private val productId: Long
) : AndroidViewModel(application) {
    
    private val productDao = AppDatabase.getDatabase(getApplication()).productDao()
    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _product.value = productDao.getProductById(productId)
        }
    }
}