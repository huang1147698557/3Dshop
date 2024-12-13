package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "idea_records")
data class IdeaRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val imageUrl: String? = null,
    val note: String? = null,
    val createdAt: Date = Date()
) 