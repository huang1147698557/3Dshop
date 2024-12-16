package com.sgm.a3dshop.ui.products

import android.app.Application
import android.util.Log
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
    
    companion object {
        private const val TAG = "ProductDB_Debug"
    }

    private val productDao = AppDatabase.getDatabase(getApplication()).productDao()
    private val _product = MutableLiveData<Product>()
    val product: LiveData<Product> = _product

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            val loadedProduct = productDao.getProductById(productId)
            Log.d(TAG, "从数据库加载产品数据:")
            Log.d(TAG, "- ID: ${loadedProduct.id}")
            Log.d(TAG, "- 名称: ${loadedProduct.name}")
            Log.d(TAG, "- 重量: ${loadedProduct.weight}g")
            Log.d(TAG, "- 打印时间: ${loadedProduct.printTime}分钟")
            Log.d(TAG, "- 人工费: ${loadedProduct.laborCost}元")
            Log.d(TAG, "- 盘数: ${loadedProduct.plateCount}")
            Log.d(TAG, "- 耗材单价: ${loadedProduct.materialUnitPrice}元/kg")
            Log.d(TAG, "- 后处理费: ${loadedProduct.postProcessingCost}元")
            Log.d(TAG, "- 数量: ${loadedProduct.quantity}")
            Log.d(TAG, "- 描述: ${loadedProduct.description}")
            _product.value = loadedProduct
        }
    }
}