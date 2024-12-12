package com.sgm.a3dshop.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.VoiceNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val voiceNoteDao = database.voiceNoteDao()

    val voiceNotes: Flow<List<VoiceNote>> = voiceNoteDao.getAllVoiceNotes()

    fun saveVoiceNote(filePath: String) {
        viewModelScope.launch {
            val voiceNote = VoiceNote(
                title = "语音备忘 ${System.currentTimeMillis()}",
                filePath = filePath
            )
            voiceNoteDao.insertVoiceNote(voiceNote)
        }
    }
} 