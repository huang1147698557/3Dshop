package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "pending_history")
data class PendingHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val salePrice: Double,
    val imageUrl: String? = null,
    val note: String? = null,
    val createdAt: Date = Date(),
    val deletedAt: Date = Date()
) 