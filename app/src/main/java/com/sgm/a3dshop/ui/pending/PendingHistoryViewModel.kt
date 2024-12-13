package com.sgm.a3dshop.ui.pending

import android.app.Application
import androidx.lifecycle.*
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.PendingHistory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val pendingHistoryDao = database.pendingHistoryDao()

    val historyItems: StateFlow<List<PendingHistory>> = pendingHistoryDao
        .getAllByDeletedAtDesc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            pendingHistoryDao.deleteAll()
        }
    }
}

class PendingHistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PendingHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PendingHistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 