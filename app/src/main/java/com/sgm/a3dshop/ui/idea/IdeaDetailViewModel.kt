package com.sgm.a3dshop.ui.idea

import android.app.Application
import androidx.lifecycle.*
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.IdeaRecord
import kotlinx.coroutines.launch

class IdeaDetailViewModel(
    application: Application,
    private val ideaId: Long
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val ideaRecordDao = database.ideaRecordDao()

    private val _ideaRecord = MutableLiveData<IdeaRecord>()
    val ideaRecord: LiveData<IdeaRecord> = _ideaRecord

    init {
        loadIdeaRecord()
    }

    private fun loadIdeaRecord() {
        viewModelScope.launch {
            _ideaRecord.value = ideaRecordDao.getById(ideaId.toInt())
        }
    }

    fun updateIdeaRecord(name: String, note: String?) {
        viewModelScope.launch {
            _ideaRecord.value?.let { currentRecord ->
                val updatedRecord = currentRecord.copy(
                    name = name,
                    note = note
                )
                ideaRecordDao.update(updatedRecord)
                _ideaRecord.value = updatedRecord
            }
        }
    }

    fun updateImage(imagePath: String) {
        viewModelScope.launch {
            _ideaRecord.value?.let { currentRecord ->
                val updatedRecord = currentRecord.copy(
                    imageUrl = imagePath
                )
                ideaRecordDao.update(updatedRecord)
                _ideaRecord.value = updatedRecord
            }
        }
    }
}

class IdeaDetailViewModelFactory(
    private val application: Application,
    private val ideaId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeaDetailViewModel(application, ideaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 