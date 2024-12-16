package com.sgm.a3dshop.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.PendingHistory
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PendingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val pendingProductDao = database.pendingProductDao()
    private val pendingHistoryDao = database.pendingHistoryDao()
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

    private var isAscending = false

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
            try {
                // 先删除待打印记录
                pendingProductDao.delete(pendingProduct)

                // 处理图片
                val newImagePath = pendingProduct.imageUrl?.let { originalImageUrl ->
                    if (!originalImageUrl.startsWith("http")) {
                        val originalFile = File(originalImageUrl)
                        if (originalFile.exists()) {
                            // 创建历史记录图片目录
                            val historyImageDir = File(getApplication<Application>().getExternalFilesDir(null), "history_images")
                            historyImageDir.mkdirs()
                            
                            // 复制图片到历史记录目录
                            val newFile = File(historyImageDir, originalFile.name)
                            originalFile.copyTo(newFile, overwrite = true)
                            newFile.absolutePath
                        } else {
                            null
                        }
                    } else {
                        originalImageUrl
                    }
                }

                // 保存到历史记录
                val pendingHistory = PendingHistory(
                    name = pendingProduct.name,
                    salePrice = pendingProduct.salePrice,
                    imageUrl = newImagePath,
                    note = pendingProduct.note,
                    createdAt = pendingProduct.createdAt
                )
                pendingHistoryDao.insert(pendingHistory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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