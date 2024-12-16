import android.content.Context
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.sgm.a3dshop.data.entity.Product
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val df = DecimalFormat("#.##")

    fun readProductsFromCsv(context: Context, uri: Uri): List<Product> {
        val products = mutableListOf<Product>()
        
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
                                模型克数: ${df.format(modelWeight)}g
                                打印时间: ${df.format(printTime)}小时
                                耗材价格: ¥${df.format(materialPrice)}/kg
                                后处理成本: ¥${df.format(postProcessingCost)}
                                人工费: ¥${df.format(laborCost)}
                                数量: $quantity
                                盘数: $batchSize
                                单个耗时: ${df.format(singleTime)}小时
                                单个成本: ¥${df.format(singleCost)}
                                预计售价: ¥${df.format(estimatedPrice)}
                                实际售价: ¥${df.format(actualPrice)}
                                利润: ¥${df.format(profit)}
                            """.trimIndent()
                            
                            val product = Product(
                                name = name,
                                price = actualPrice,
                                description = description,
                                imageUrl = imageUrl,
                                weight = modelWeight.toFloat(),
                                printTime = (printTime * 60).toInt(), // 将小时转换为分钟
                                laborCost = laborCost,
                                plateCount = batchSize,
                                materialUnitPrice = materialPrice,
                                quantity = quantity

                            )
                            products.add(product)
                            Log.d("CsvUtils", "Added product: ${product.name} with URL: ${product.quantity}")
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

    fun exportProductsToCsv(context: Context, uri: Uri, products: List<Product>) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = CSVWriter(OutputStreamWriter(outputStream))

                // 写入标题行
                writer.writeNext(arrayOf(
                    "商品名称",
                    "图片链接",
                    "模型克数(g)",
                    "打印时间(h)",
                    "耗材价格(¥/kg)",
                    "后处理成本(¥)",
                    "人工费(¥)",
                    "数量",
                    "盘数",
                    "单个耗时(h)",
                    "单个成本(¥)",
                    "预计售价(¥)",
                    "实际售价(¥)",
                    "利润(¥)"
                ))

                // 写入单位行（空行，保持格式一致）
                writer.writeNext(Array(14) { "" })

                // 写入数据行
                products.forEach { product ->
                    // 解析描述中的数据
                    val descriptionMap = parseDescription(product.description ?: "")
                    
                    writer.writeNext(arrayOf(
                        product.name,
                        product.imageUrl ?: "",
                        descriptionMap["modelWeight"] ?: "0",
                        descriptionMap["printTime"] ?: "0",
                        descriptionMap["materialPrice"] ?: "0",
                        descriptionMap["postProcessingCost"] ?: "0",
                        descriptionMap["laborCost"] ?: "0",
                        descriptionMap["quantity"] ?: "0",
                        descriptionMap["batchSize"] ?: "1",
                        descriptionMap["singleTime"] ?: "0",
                        descriptionMap["singleCost"] ?: "0",
                        descriptionMap["estimatedPrice"] ?: "0",
                        product.price.toString(),
                        descriptionMap["profit"] ?: "0"
                    ))
                }

                writer.close()
            }
        } catch (e: Exception) {
            Log.e("CsvUtils", "Error writing CSV file: ${e.message}")
            throw e
        }
    }

    private fun parseDescription(description: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        description.lines().forEach { line ->
            when {
                line.startsWith("模型克数:") -> result["modelWeight"] = extractNumber(line)
                line.startsWith("打印时间:") -> result["printTime"] = extractNumber(line)
                line.startsWith("耗材价格:") -> result["materialPrice"] = extractNumber(line)
                line.startsWith("后处理成本:") -> result["postProcessingCost"] = extractNumber(line)
                line.startsWith("人工费:") -> result["laborCost"] = extractNumber(line)
                line.startsWith("数量:") -> result["quantity"] = extractNumber(line)
                line.startsWith("盘数:") -> result["batchSize"] = extractNumber(line)
                line.startsWith("单个耗时:") -> result["singleTime"] = extractNumber(line)
                line.startsWith("单个成本:") -> result["singleCost"] = extractNumber(line)
                line.startsWith("预计售价:") -> result["estimatedPrice"] = extractNumber(line)
                line.startsWith("利润:") -> result["profit"] = extractNumber(line)
            }
        }
        return result
    }

    private fun extractNumber(line: String): String {
        return line.substringAfter(":")
            .replace("¥", "")
            .replace("g", "")
            .replace("h", "")
            .replace("/kg", "")
            .trim()
    }

    private fun processImageUrl(url: String?): String? {
        return url?.let {
            if (it.startsWith("http:/")) {
                "https://" + it.substring(6)
            } else {
                it
            }
        }
    }
}