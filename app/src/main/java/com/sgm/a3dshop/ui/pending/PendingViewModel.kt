package com.sgm.a3dshop.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PendingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val pendingProductDao = database.pendingProductDao()
    private val productDao = database.productDao()

    private val _pendingProducts = MutableStateFlow<List<PendingProduct>>(emptyList())
    val pendingProducts: StateFlow<List<PendingProduct>> = _pendingProducts

    private val searchQuery = MutableStateFlow("")
    val filteredProducts: StateFlow<List<Product>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                productDao.getAllProducts()
            } else {
                productDao.searchProducts("%$query%")
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var isAscending = true

    init {
        loadPendingProducts()
    }

    private fun loadPendingProducts() {
        viewModelScope.launch {
            if (isAscending) {
                pendingProductDao.getAllByCreatedAtAsc().collect { products ->
                    _pendingProducts.value = products
                }
            } else {
                pendingProductDao.getAllByCreatedAtDesc().collect { products ->
                    _pendingProducts.value = products
                }
            }
        }
    }

    fun toggleSort() {
        isAscending = !isAscending
        loadPendingProducts()
    }

    fun insertPendingProduct(pendingProduct: PendingProduct) {
        viewModelScope.launch {
            pendingProductDao.insert(pendingProduct)
        }
    }

    fun deletePendingProduct(pendingProduct: PendingProduct) {
        viewModelScope.launch {
            pendingProductDao.delete(pendingProduct)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
}

class PendingViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PendingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PendingViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 