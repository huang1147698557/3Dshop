package com.sgm.a3dshop.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.VoiceNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val voiceNoteDao = database.voiceNoteDao()

    private val _voiceNotes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voiceNotes: StateFlow<List<VoiceNote>> = _voiceNotes.asStateFlow()

    init {
        loadVoiceNotes()
    }

    private fun loadVoiceNotes() {
        viewModelScope.launch {
            voiceNoteDao.getAllVoiceNotes().collect { notes ->
                _voiceNotes.value = notes
            }
        }
    }

    fun saveVoiceNote(filePath: String) {
        viewModelScope.launch {
            val voiceNote = VoiceNote(
                id = 0,
                filePath = filePath,
                createdAt = Date(),
                isLoopEnabled = false,
                intervalSeconds = 5
            )
            voiceNoteDao.insertVoiceNote(voiceNote)
        }
    }

    fun refreshData() {
        loadVoiceNotes()
    }

    fun updateVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            voiceNoteDao.updateVoiceNote(voiceNote)
        }
    }

    fun deleteVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            // 删除文件
            File(voiceNote.filePath).delete()
            // 删除数据库记录
            voiceNoteDao.deleteVoiceNote(voiceNote)
        }
    }
} 