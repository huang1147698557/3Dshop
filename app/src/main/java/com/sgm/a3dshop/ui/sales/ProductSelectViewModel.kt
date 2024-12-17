package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.InventoryLog
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.repository.ProductRepository
import com.sgm.a3dshop.data.repository.SaleRecordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ProductSelectViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val productRepository: ProductRepository
    private val saleRecordRepository: SaleRecordRepository
    private val inventoryLogDao = database.inventoryLogDao()
    private val searchQuery = MutableStateFlow("")

    init {
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

    suspend fun getProductRemainingCount(productId: Int): Int {
        return withContext(Dispatchers.IO) {
            productRepository.getProductRemainingCount(productId)
        }
    }

    fun createSaleRecordWithInventoryUpdate(
        saleRecord: SaleRecord,
        beforeCount: Int,
        afterCount: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 创建销售记录
                saleRecordRepository.insertSaleRecord(saleRecord)

                // 2. 更新商品库存（只在有关联产品时更新）
                saleRecord.productId?.let { productId ->
                    productRepository.updateProductRemainingCount(
                        productId = productId,
                        newCount = afterCount
                    )

                    // 3. 记录库存变动日志
                    val inventoryLog = InventoryLog(
                        productId = productId,
                        operationType = InventoryLog.OPERATION_SALE,
                        beforeCount = beforeCount,
                        afterCount = afterCount
                    )
                    inventoryLogDao.insert(inventoryLog)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSaleRecordWithInventoryUpdate(
        saleRecord: SaleRecord
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 删除销售记录
                saleRecordRepository.deleteSaleRecord(saleRecord)

                // 2. 更新商品库存（只在有关联产品时更新）
                saleRecord.productId?.let { productId ->
                    // 获取当前库存
                    val currentCount = productRepository.getProductRemainingCount(productId)
                    // 增加库存（+1）
                    val newCount = currentCount + 1

                    // 更新库存
                    productRepository.updateProductRemainingCount(
                        productId = productId,
                        newCount = newCount
                    )

                    // 3. 记录库存变动日志
                    val inventoryLog = InventoryLog(
                        productId = productId,
                        operationType = InventoryLog.OPERATION_DELETE_SALE,
                        beforeCount = currentCount,
                        afterCount = newCount
                    )
                    inventoryLogDao.insert(inventoryLog)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 