package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.repository.ProductRepository
import com.sgm.a3dshop.data.repository.SaleRecordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductSelectViewModel(application: Application) : AndroidViewModel(application) {
    private val productRepository: ProductRepository
    private val saleRecordRepository: SaleRecordRepository
    private val searchQuery = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        productRepository = ProductRepository(database.productDao())
        saleRecordRepository = SaleRecordRepository(database.saleRecordDao())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredProducts: StateFlow<List<Product>> = searchQuery
        .flatMapLatest { query ->
            productRepository.allProducts.map { products ->
                if (query.isEmpty()) {
                    products
                } else {
                    products.filter { product ->
                        product.name.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun insertSaleRecord(saleRecord: SaleRecord) = viewModelScope.launch {
        saleRecordRepository.insertSaleRecord(saleRecord)
    }
} 