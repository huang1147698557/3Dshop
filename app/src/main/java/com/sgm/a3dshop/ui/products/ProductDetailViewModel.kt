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
            try {
                val loadedProduct = productDao.getProductById(productId.toInt())
                _product.value = loadedProduct
            } catch (e: Exception) {
                Log.e(TAG, "Error loading product: ${e.message}")
            }
        }
    }

    fun updateImage(imagePath: String) {
        viewModelScope.launch {
            try {
                _product.value?.let { currentProduct ->
                    val updatedProduct = currentProduct.copy(
                        imageUrl = imagePath
                    )
                    productDao.updateProduct(updatedProduct)
                    _product.value = updatedProduct
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating product image: ${e.message}")
            }
        }
    }
}