package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.VoiceNote
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(voiceNote: VoiceNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNotes(voiceNotes: List<VoiceNote>)

    @Update
    suspend fun updateVoiceNote(voiceNote: VoiceNote)

    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNote)

    @Query("DELETE FROM voice_notes")
    suspend fun deleteAllVoiceNotes()

    @Query("SELECT * FROM voice_notes WHERE id = :voiceNoteId")
    suspend fun getVoiceNoteById(voiceNoteId: Int): VoiceNote?
} 