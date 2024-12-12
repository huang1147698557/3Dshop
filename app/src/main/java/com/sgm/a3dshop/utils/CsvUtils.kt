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
                
                // 读取标题行
                val headers = reader.readNext()
                Log.d("CsvUtils", "Headers: ${headers?.joinToString(", ")}")
                
                // URL总是在第二列（索引1）
                val urlColumnIndex = 1
                Log.d("CsvUtils", "Using fixed URL column index: $urlColumnIndex")
                
                // 跳过第二行（单位行）
                val units = reader.readNext()
                Log.d("CsvUtils", "Units row: ${units?.joinToString(", ")}")
                
                var line: Array<String>?
                while (reader.readNext().also { line = it } != null) {
                    try {
                        if (line?.size ?: 0 >= 13) {
                            val name = line?.get(0)?.trim()?.takeIf { it.isNotBlank() } ?: continue
                            
                            // 从第二列获取URL
                            val imageUrl = if (urlColumnIndex < (line?.size ?: 0)) {
                                line?.get(urlColumnIndex)?.trim()?.takeIf { 
                                    it.isNotBlank() && it.startsWith("http")
                                }
                            } else null
                            
                            Log.d("CsvUtils", "Processing row: ${line?.joinToString(", ")}")
                            Log.d("CsvUtils", "Found product: $name with URL: $imageUrl")
                            
                            // 因为URL在第二列，所以其他数据列的索引需要偏移1位
                            val modelWeight = line?.get(2)?.toDoubleOrNull() ?: 0.0
                            val printTime = line?.get(3)?.toDoubleOrNull() ?: 0.0
                            val materialPrice = line?.get(4)?.toDoubleOrNull() ?: 0.0
                            val postProcessingCost = line?.get(5)?.toDoubleOrNull() ?: 0.0
                            val laborCost = line?.get(6)?.toDoubleOrNull() ?: 0.0
                            val quantity = line?.get(7)?.toDoubleOrNull()?.toInt() ?: 0
                            val batchSize = line?.get(8)?.toDoubleOrNull()?.toInt() ?: 1
                            val singleTime = line?.get(9)?.toDoubleOrNull() ?: 0.0
                            val singleCost = line?.get(10)?.toDoubleOrNull() ?: 0.0
                            val estimatedPrice = line?.get(11)?.toDoubleOrNull() ?: 0.0
                            val actualPrice = line?.get(12)?.toDoubleOrNull() ?: 0.0
                            val profit = line?.get(13)?.toDoubleOrNull() ?: 0.0
                            
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
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CsvUtils", "Error reading CSV file: ${e.message}")
            e.printStackTrace()
        }
        
        Log.d("CsvUtils", "Total products parsed: ${products.size}")
        return products
    }
} 