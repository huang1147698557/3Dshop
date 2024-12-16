package com.sgm.a3dshop.ui.products

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
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

    fun getProductById(id: Long): LiveData<Product> = liveData {
        emit(productDao.getProductById(id))
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productDao.getAllProducts().collect { products ->
                _products.value = if (isAscending) {
                    products.sortedBy { it.id }
                } else {
                    products.sortedByDescending { it.id }
                }
            }
        }
    }

    fun toggleSort() {
        isAscending = !isAscending
        _products.value = if (isAscending) {
            _products.value.sortedBy { it.id }
        } else {
            _products.value.sortedByDescending { it.id }
        }
    }

    fun insertProduct(product: Product) {
        viewModelScope.launch {
            Log.d("ProductDB_Debug", "插入数据库时:")
            Log.d("ProductDB_Debug", "- 名称: ${product.name}")
            Log.d("ProductDB_Debug", "- 重量: ${product.weight}g")
            Log.d("ProductDB_Debug", "- 打印时间: ${product.printTime}分钟")
            Log.d("ProductDB_Debug", "- 人工费: ${product.laborCost}元")
            Log.d("ProductDB_Debug", "- 盘数: ${product.plateCount}")
            Log.d("ProductDB_Debug", "- 耗材单价: ${product.materialUnitPrice}元/kg")
            Log.d("ProductDB_Debug", "- 后处理费: ${product.postProcessingCost}��")
            Log.d("ProductDB_Debug", "- 数量: ${product.quantity}")
            Log.d("ProductDB_Debug", "- 描述: ${product.description}")
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

    suspend fun getAllProducts(): List<Product> {
        return productDao.getAllProductsSync()
    }
} 