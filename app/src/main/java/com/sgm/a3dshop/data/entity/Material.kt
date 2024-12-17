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
    val material: String = MATERIAL_PLA,
    val color: String? = null,
    val price: Double = DEFAULT_PRICE,
    val quantity: Int = DEFAULT_QUANTITY,
    val remainingPercentage: Int = DEFAULT_REMAINING_PERCENTAGE,
    val imageUrl: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) : Parcelable {
    companion object {
        const val MATERIAL_PLA = "PLA"
        const val MATERIAL_PETG = "PETG"
        const val DEFAULT_PRICE = 39.0
        const val DEFAULT_QUANTITY = 1
        const val DEFAULT_REMAINING_PERCENTAGE = 100
    }
} 