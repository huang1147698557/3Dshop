package com.sgm.a3dshop.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(
    tableName = "sale_records",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("productId")
    ]
)
data class SaleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int? = null,
    val name: String,
    val salePrice: Double,
    val imageUrl: String? = null,
    val note: String? = null,
    val createdAt: Date = Date()
) : Parcelable 