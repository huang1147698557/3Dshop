package com.sgm.a3dshop.ui.idea

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.IdeaHistory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IdeaHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val ideaHistoryDao = database.ideaHistoryDao()

    val historyItems: StateFlow<List<IdeaHistory>> = ideaHistoryDao
        .getAllByDeletedAtDesc()
        .onEach { items -> 
            Log.d(TAG, "历史记录数据更新: ${items.size} 条记录")
            items.forEach { item ->
                Log.d(TAG, "历史记录: id=${item.id}, name=${item.name}, createdAt=${item.createdAt}, deletedAt=${item.deletedAt}")
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            Log.d(TAG, "开始清空历史记录")
            ideaHistoryDao.deleteAll()
            Log.d(TAG, "历史记录已清空")
        }
    }

    companion object {
        private const val TAG = "IdeaHistoryViewModel"
    }
}

class IdeaHistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeaHistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 