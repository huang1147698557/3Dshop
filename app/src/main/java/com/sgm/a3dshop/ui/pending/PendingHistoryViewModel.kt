package com.sgm.a3dshop.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.PendingHistory
import com.sgm.a3dshop.data.entity.PendingProduct
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class PendingHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val pendingHistoryDao = database.pendingHistoryDao()
    private val pendingProductDao = database.pendingProductDao()

    val historyItems = pendingHistoryDao.getAllByDeletedAtDesc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun restoreItem(history: PendingHistory) = viewModelScope.launch {
        try {
            // 处理图片
            val newImagePath = history.imageUrl?.let { originalImageUrl ->
                if (!originalImageUrl.startsWith("http")) {
                    val originalFile = File(originalImageUrl)
                    if (originalFile.exists()) {
                        // 创建待打印图片目录
                        val pendingImageDir = File(getApplication<Application>().getExternalFilesDir(null), "pending_images")
                        pendingImageDir.mkdirs()
                        
                        // 复制图片到待打印目录
                        val newFile = File(pendingImageDir, originalFile.name)
                        originalFile.copyTo(newFile, overwrite = true)
                        
                        // 删除历史记录中的图片
                        originalFile.delete()
                        
                        newFile.absolutePath
                    } else {
                        null
                    }
                } else {
                    originalImageUrl
                }
            }

            // 创建待打印商品
            val pendingProduct = PendingProduct(
                name = history.name,
                salePrice = history.salePrice,
                imageUrl = newImagePath,
                note = history.note,
                createdAt = Date()
            )
            
            // 从历史记录中删除
            pendingHistoryDao.delete(history)
            
            // 添加到待打印列表
            pendingProductDao.insert(pendingProduct)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteItemPermanently(history: PendingHistory) = viewModelScope.launch {
        try {
            // 删除图片文件
            history.imageUrl?.let { imageUrl ->
                if (!imageUrl.startsWith("http")) {
                    val imageFile = File(imageUrl)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
            }
            
            // 从数据库中删除记录
            pendingHistoryDao.delete(history)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class PendingHistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PendingHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PendingHistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 