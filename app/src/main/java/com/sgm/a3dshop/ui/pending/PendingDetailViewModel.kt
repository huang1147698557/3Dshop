package com.sgm.a3dshop.ui.pending

import android.app.Application
import androidx.lifecycle.*
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.PendingProduct
import kotlinx.coroutines.launch

class PendingDetailViewModel(
    application: Application,
    private val pendingProductId: Long
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val pendingProductDao = database.pendingProductDao()

    private val _pendingProduct = MutableLiveData<PendingProduct>()
    val pendingProduct: LiveData<PendingProduct> = _pendingProduct

    init {
        loadPendingProduct()
    }

    private fun loadPendingProduct() {
        viewModelScope.launch {
            _pendingProduct.value = pendingProductDao.getById(pendingProductId.toInt())
        }
    }

    fun updatePendingProduct(pendingProduct: PendingProduct) {
        viewModelScope.launch {
            pendingProductDao.update(pendingProduct)
            _pendingProduct.value = pendingProduct
        }
    }
}

class PendingDetailViewModelFactory(
    private val application: Application,
    private val pendingProductId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PendingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PendingDetailViewModel(application, pendingProductId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 