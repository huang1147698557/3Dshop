package com.sgm.a3dshop.data.entity

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sgm.a3dshop.data.DateParcelConverter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.util.*

@Parcelize
@Entity(tableName = "products")
@TypeParceler<Date, DateParceler>
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null,
    val price: Double,
    val imageUrl: String? = null,
    val createdAt: Date = Date(),
    val weight: Float,  // 重量(g)
    val printTime: Int, // 打印时间(分钟)
    val laborCost: Double = 0.0, // 人工费
    val plateCount: Int = 1,     // 盘数
    val materialUnitPrice: Double = 0.0, // 耗材单价(元/kg)
    val profitRate: Double = 0.3, // 默认利润率30%
    val postProcessingCost: Double = 0.0, // 后处理物料费
    val quantity: Int = 1 // 数量
) : Parcelable {
    companion object {
        private const val TAG = "Product_Calculation"
    }

    init {
        Log.d(TAG, "创建Product对象:")
        Log.d(TAG, "- ID: $id")
        Log.d(TAG, "- 名称: $name")
        Log.d(TAG, "- 数量: $quantity")
        Log.d(TAG, "- 描述: $description")
    }

    // 计算单个成本
    fun calculateUnitCost(): Double {
        val materialCost = weight * materialUnitPrice / 1000
        val totalCost = materialCost + laborCost + postProcessingCost
        val unitCost = totalCost / quantity
        Log.d(TAG, "计算单个成本:")
        Log.d(TAG, "- 重量: ${weight}g")
        Log.d(TAG, "- 耗材单价: ${materialUnitPrice}元/kg")
        Log.d(TAG, "- 耗材成本: ${materialCost}元")
        Log.d(TAG, "- 人工费: ${laborCost}元")
        Log.d(TAG, "- 后处理费: ${postProcessingCost}元")
        Log.d(TAG, "- 数量: ${quantity}")
        Log.d(TAG, "- 单个成本: ${unitCost}元")
        return unitCost
    }

    // 计算预计售价
    fun calculateExpectedPrice(): Double {
        val totalTime = printTime / 60.0  // 将分钟转换为小时
        val costPart = calculateUnitCost() * 7.5
        val timePart = totalTime/quantity * plateCount * 0.5
        Log.d(TAG, "计算预计售价:")
        Log.d(TAG, "- 打印时间: ${totalTime}小时")
        Log.d(TAG, "- 盘数: ${plateCount}")
        Log.d(TAG, "- 数量: ${quantity}")
        Log.d(TAG, "- 成本部分: ${costPart}元")
        Log.d(TAG, "- 时间部分: ${timePart}元")
        Log.d(TAG, "- 总预计售价: ${costPart + timePart}元")
        return costPart + timePart
    }

    // 计算利润
    fun calculateProfit(): Double {
        val unitCost = calculateUnitCost()
        val price = price  // 使用实际售价
        val profit = price - unitCost
        
        Log.d(TAG, "计算利润:")
        Log.d(TAG, "- 实际售价: ${price}元")
        Log.d(TAG, "- 单个成本: ${unitCost}元")
        Log.d(TAG, "- 利润: ${profit}元")
        
        return profit
    }
}

object DateParceler : Parceler<Date> {
    override fun create(parcel: Parcel): Date = DateParcelConverter.readDate(parcel)
    override fun Date.write(parcel: Parcel, flags: Int) = DateParcelConverter.writeDate(parcel, this)
} 