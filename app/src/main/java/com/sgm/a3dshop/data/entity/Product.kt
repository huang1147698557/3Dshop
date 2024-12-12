package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    val description: String?,
    val createTime: Long = System.currentTimeMillis()
) 