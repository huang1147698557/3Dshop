package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val note: String? = null,
    val duration: Int = 0,
    val createTime: Long = System.currentTimeMillis(),
    var isLoopEnabled: Boolean = false,
    var intervalSeconds: Int = 5
) 