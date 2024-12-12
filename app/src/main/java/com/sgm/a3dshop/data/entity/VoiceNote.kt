package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filePath: String,
    val createdAt: Date,
    val isLoopEnabled: Boolean = false,
    val intervalSeconds: Int = 5
) 