package com.sgm.a3dshop.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val productDao = database.productDao()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private var isAscending = false

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productDao.getAllProducts().collect { products ->
                _products.value = if (isAscending) {
                    products.sortedBy { it.createdAt }
                } else {
                    products.sortedByDescending { it.createdAt }
                }
            }
        }
    }

    fun toggleSort() {
        isAscending = !isAscending
        _products.value = if (isAscending) {
            _products.value.sortedBy { it.createdAt }
        } else {
            _products.value.sortedByDescending { it.createdAt }
        }
    }

    fun insertProduct(product: Product) {
        viewModelScope.launch {
            productDao.insertProduct(product)
        }
    }

    fun insertProducts(products: List<Product>) {
        viewModelScope.launch {
            productDao.insertProducts(products)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productDao.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }

    fun deleteAllProducts() {
        viewModelScope.launch {
            productDao.deleteAllProducts()
        }
    }
} 