package com.sgm.a3dshop.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProductRepository
    private val products: Flow<List<Product>>
    private val sortOrderFlow = MutableStateFlow(true) // true for descending, false for ascending

    val sortedProducts: StateFlow<List<Product>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProductRepository(database.productDao())
        products = repository.allProducts

        sortedProducts = combine(products, sortOrderFlow) { items, isDescending ->
            if (isDescending) {
                items.sortedByDescending { it.createTime }
            } else {
                items.sortedBy { it.createTime }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun toggleSortOrder() {
        sortOrderFlow.value = !sortOrderFlow.value
    }

    fun isDescendingOrder() = sortOrderFlow.value

    fun insertProduct(product: Product) = viewModelScope.launch {
        repository.insertProduct(product)
    }

    fun insertProducts(products: List<Product>) = viewModelScope.launch {
        repository.insertProducts(products)
    }

    fun updateProduct(product: Product) = viewModelScope.launch {
        repository.updateProduct(product)
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        repository.deleteProduct(product)
    }

    fun deleteAllProducts() = viewModelScope.launch {
        repository.deleteAllProducts()
    }
} 