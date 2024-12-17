package com.sgm.a3dshop.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.model.DailySales
import com.sgm.a3dshop.data.repository.ProductRepository
import com.sgm.a3dshop.data.entity.InventoryLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val saleRecordDao = database.saleRecordDao()
    private val productRepository = ProductRepository(database.productDao())
    private val inventoryLogDao = database.inventoryLogDao()

    private val _dailySales = MutableStateFlow<List<DailySales>>(emptyList())
    val dailySales: StateFlow<List<DailySales>> = _dailySales.asStateFlow()

    private val _totalSales = MutableLiveData<Double>()
    val totalSales: LiveData<Double> = _totalSales

    init {
        loadSales()
    }

    fun deleteSaleRecordWithInventoryUpdate(saleRecord: SaleRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 删除销售记录
                saleRecordDao.deleteSaleRecord(saleRecord)

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

                // 4. 重新加载销售数据
                loadSales()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                saleRecordDao.insertSaleRecord(saleRecord)
                loadSales()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSaleRecord(saleRecord: SaleRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                saleRecordDao.updateSaleRecord(saleRecord)
                loadSales()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSales() {
        viewModelScope.launch {
            saleRecordDao.getAllSaleRecords().collect { records ->
                // 按日期分组
                val groupedSales = records.groupBy { record ->
                    record.createdAt.let { date ->
                        Calendar.getInstance().apply {
                            time = date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                    }
                }.map { (date, records) ->
                    DailySales(
                        date = date,
                        records = records,
                        totalAmount = records.sumOf { it.salePrice }
                    )
                }.sortedByDescending { it.date }

                _dailySales.value = groupedSales

                // 计算总销售额
                val total = groupedSales.sumOf { it.totalAmount }
                _totalSales.postValue(total)
            }
        }
    }
} 