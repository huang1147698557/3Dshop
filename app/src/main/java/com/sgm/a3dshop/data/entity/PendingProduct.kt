package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "pending_products",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PendingProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int? = null,
    val name: String,
    val salePrice: Double,
    val imageUrl: String? = null,
    val note: String? = null,
    val createdAt: Date = Date()
) 