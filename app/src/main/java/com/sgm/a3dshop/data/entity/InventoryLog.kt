package com.sgm.a3dshop.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "inventory_logs")
data class InventoryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,          // 商品ID
    val operationType: String,   // 操作类型：SALE/DELETE_SALE
    val beforeCount: Int,        // 变动前数量
    val afterCount: Int,         // 变动后数量
    val createdAt: Date = Date() // 操作时间
) {
    companion object {
        const val OPERATION_SALE = "SALE"           // 销售操作
        const val OPERATION_DELETE_SALE = "DELETE_SALE"  // 删除销售记录
    }
} 