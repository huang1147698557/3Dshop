package com.sgm.a3dshop.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "materials")
data class Material(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val color: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 0,
    val remainingPercentage: Int = 100,
    val imageUrl: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) : Parcelable 