package com.sgm.a3dshop.ui.idea

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.IdeaHistory
import com.sgm.a3dshop.data.entity.IdeaRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class IdeaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val ideaRecordDao = database.ideaRecordDao()
    private val ideaHistoryDao = database.ideaHistoryDao()

    private val _refreshTrigger = MutableStateFlow(0)
    val ideaRecords: StateFlow<List<IdeaRecord>> = _refreshTrigger
        .flatMapLatest {
            ideaRecordDao.getAllByCreatedAtDesc()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun insertIdeaRecord(ideaRecord: IdeaRecord) {
        viewModelScope.launch {
            try {
                val id = ideaRecordDao.insert(ideaRecord)
                refreshData()
            } catch (e: Exception) {
                Log.e(TAG, "插入创意记录失败", e)
            }
        }
    }

    fun deleteIdeaRecord(ideaRecord: IdeaRecord) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "开始删除创意记录: id=${ideaRecord.id}, name=${ideaRecord.name}")
                // 先保存到历史记录
                val ideaHistory = IdeaHistory(
                    name = ideaRecord.name,
                    imageUrl = ideaRecord.imageUrl,
                    note = ideaRecord.note,
                    createdAt = ideaRecord.createdAt,
                    deletedAt = Date()
                )
                Log.d(TAG, "创建历史记录对象: $ideaHistory")
                ideaHistoryDao.insert(ideaHistory)
                Log.d(TAG, "历史记录保存成功")
                
                // 然后删除记录
                ideaRecordDao.delete(ideaRecord)
                Log.d(TAG, "原记录删除成功")
                refreshData()
                Log.d(TAG, "数据刷新完成")
            } catch (e: Exception) {
                Log.e(TAG, "删除创意记录失败", e)
            }
        }
    }

    private fun refreshData() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }

    companion object {
        private const val TAG = "IdeaViewModel"
    }
}

class IdeaViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 