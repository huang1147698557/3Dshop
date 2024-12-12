package com.sgm.a3dshop.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReader
import com.sgm.a3dshop.data.entity.Product
import java.io.InputStreamReader
import java.text.DecimalFormat

object CsvUtils {
    fun readProductsFromCsv(context: Context, uri: Uri): List<Product> {
        val products = mutableListOf<Product>()
        val df = DecimalFormat("#.##")
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = CSVReader(InputStreamReader(inputStream))
                
                // 跳过标题行
                reader.skip(2)
                
                var line: Array<String>?
                while (reader.readNext().also { line = it } != null) {
                    try {
                        if (line?.size ?: 0 >= 13) {
                            val rawName = line?.get(0)?.trim()?.takeIf { it.isNotBlank() } ?: continue
                            
                            // 从名称中提取URL
                            val name: String
                            val imageUrl: String?
                            
                            if (rawName.contains("https://")) {
                                val parts = rawName.split(" ")
                                name = parts.takeWhile { !it.startsWith("https://") }.joinToString(" ").trim()
                                imageUrl = parts.find { it.startsWith("https://") }
                                Log.d("CsvUtils", "Found URL: $imageUrl for product: $name")
                            } else {
                                name = rawName
                                imageUrl = null
                                Log.d("CsvUtils", "No URL found for product: $name")
                            }
                            
                            val modelWeight = line?.get(1)?.toDoubleOrNull() ?: 0.0
                            val printTime = line?.get(2)?.toDoubleOrNull() ?: 0.0
                            val materialPrice = line?.get(3)?.toDoubleOrNull() ?: 0.0
                            val postProcessingCost = line?.get(4)?.toDoubleOrNull() ?: 0.0
                            val laborCost = line?.get(5)?.toDoubleOrNull() ?: 0.0
                            val quantity = line?.get(6)?.toDoubleOrNull()?.toInt() ?: 0
                            val batchSize = line?.get(7)?.toDoubleOrNull()?.toInt() ?: 1
                            val singleTime = line?.get(8)?.toDoubleOrNull() ?: 0.0
                            val singleCost = line?.get(9)?.toDoubleOrNull() ?: 0.0
                            val estimatedPrice = line?.get(10)?.toDoubleOrNull() ?: 0.0
                            val actualPrice = line?.get(11)?.toDoubleOrNull() ?: 0.0
                            val profit = line?.get(12)?.toDoubleOrNull() ?: 0.0
                            
                            val description = """
                                模型完数: ${df.format(modelWeight)}g
                                打印时间: ${df.format(printTime)}h
                                耗材价格: ¥${df.format(materialPrice)}/kg
                                后处理成本: ¥${df.format(postProcessingCost)}
                                人工费: ¥${df.format(laborCost)}
                                数量: $quantity
                                盘数: $batchSize
                                单个耗时: ${df.format(singleTime)}h
                                单个成本: ¥${df.format(singleCost)}
                                预计售价: ¥${df.format(estimatedPrice)}
                                实际售价: ¥${df.format(actualPrice)}
                                利润: ¥${df.format(profit)}
                            """.trimIndent()
                            
                            val product = Product(
                                name = name,
                                price = actualPrice,
                                description = description,
                                imageUrl = imageUrl
                            )
                            products.add(product)
                            Log.d("CsvUtils", "Added product: ${product.name} with URL: ${product.imageUrl}")
                        }
                    } catch (e: Exception) {
                        Log.e("CsvUtils", "Error parsing line: ${e.message}")
                        e.printStackTrace()
                        // 跳过错误的行
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CsvUtils", "Error reading CSV: ${e.message}")
            e.printStackTrace()
            throw Exception("CSV文件读取失败: ${e.message}")
        }
        
        Log.d("CsvUtils", "Total products parsed: ${products.size}")
        return products
    }
} 