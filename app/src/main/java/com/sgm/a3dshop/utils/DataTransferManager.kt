package com.sgm.a3dshop.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

class DataTransferManager(private val context: Context) {
    companion object {
        const val DEFAULT_PORT = 8888
        const val DISCOVERY_PORT = 8889
        const val BUFFER_SIZE = 8192
        const val DISCOVERY_MESSAGE = "3DSHOP_DISCOVERY"
        const val DISCOVERY_TIMEOUT = 3000 // 3秒超时
        const val MAX_RETRY_COUNT = 5 // 最大重试次数
        const val TCP_CONNECT_DELAY = 1000L // TCP连接前等待1秒
    }

    private val database = AppDatabase.getDatabase(context)
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    )
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, object : TypeAdapter<Date>() {
            override fun write(out: JsonWriter, value: Date?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    // 使用标准格式导出
                    out.value(dateFormats[0].format(value))
                }
            }

            override fun read(input: JsonReader): Date? {
                val dateStr = input.nextString()
                for (format in dateFormats) {
                    try {
                        return format.parse(dateStr)
                    } catch (e: Exception) {
                        // 尝试下一个格式
                        continue
                    }
                }
                // 如果所有格式都失败了记录错误并返回当前时间
                println("Failed to parse date: $dateStr")
                return Date()
            }
        })
        .create()

    // 数据模型类，用于JSON序列化
    data class AppData(
        val products: List<Product>,
        val saleRecords: List<SaleRecord>,
        val voiceNotes: List<VoiceNote>,
        val pendingProducts: List<PendingProduct>,
        val pendingHistory: List<PendingHistory>,
        val ideaRecords: List<IdeaRecord>,
        val ideaHistory: List<IdeaHistory>
    )

    // 本地导出
    suspend fun exportToLocal(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val appData = AppData(
                products = database.productDao().getAllProductsSync(),
                saleRecords = database.saleRecordDao().getAllSaleRecordsSync(),
                voiceNotes = database.voiceNoteDao().getAllVoiceNotesSync(),
                pendingProducts = database.pendingProductDao().getAllByCreatedAtDesc().first(),
                pendingHistory = database.pendingHistoryDao().getAllByDeletedAtDesc().first(),
                ideaRecords = database.ideaRecordDao().getAllByCreatedAtDesc().first(),
                ideaHistory = database.ideaHistoryDao().getAllByDeletedAtDesc().first()
            )

            // 创建导出目录
            val exportDir = File(context.getExternalFilesDir(null), "export")
            exportDir.mkdirs()

            // 创建ZIP文件
            val zipFile = File(exportDir, "app_data_${System.currentTimeMillis()}.zip")
            var totalSize = 0L
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                // 写入数据JSON
                zip.putNextEntry(ZipEntry("data.json"))
                val jsonData = gson.toJson(appData).toByteArray()
                zip.write(jsonData)
                totalSize += jsonData.size
                zip.closeEntry()

                // 复制录音文件
                appData.voiceNotes.forEach { voiceNote ->
                    val voiceFile = File(voiceNote.filePath)
                    if (voiceFile.exists()) {
                        zip.putNextEntry(ZipEntry("voices/${voiceFile.name}"))
                        voiceFile.inputStream().use { input ->
                            input.copyTo(zip)
                            totalSize += voiceFile.length()
                        }
                        zip.closeEntry()
                    }
                }

                // 复制待打印图片
                appData.pendingProducts.forEach { pendingProduct ->
                    pendingProduct.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("pending_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 复制待打印历史记录图片
                appData.pendingHistory.forEach { pendingHistory ->
                    pendingHistory.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("pending_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入创意图片
                appData.ideaRecords.forEach { ideaRecord ->
                    ideaRecord.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("idea_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入创意历史记录图片
                appData.ideaHistory.forEach { ideaHistory ->
                    ideaHistory.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("idea_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }
            Pair(true, zipFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "")
        }
    }

    // 本地导入
    suspend fun importFromLocal(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                var dataJson: String? = null

                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            dataJson = zip.bufferedReader().readText()
                        }
                        entry.name.startsWith("voices/") -> {
                            val voiceDir = File(context.getExternalFilesDir(null), "VoiceNotes")
                            voiceDir.mkdirs()
                            val voiceFile = File(voiceDir, entry.name.substringAfter("voices/"))
                            voiceFile.outputStream().use { output ->
                                zip.copyTo(output)
                            }
                        }
                        entry.name.startsWith("sales_images/") -> {
                            val imageDir = File(context.getExternalFilesDir(null), "3DShop_Images")
                            imageDir.mkdirs()
                            val imageFile = File(imageDir, entry.name.substringAfter("sales_images/"))
                            imageFile.outputStream().use { output ->
                                zip.copyTo(output)
                            }
                        }
                        entry.name.startsWith("pending_images/") -> {
                            val imageDir = File(context.getExternalFilesDir(null), "pending_images")
                            imageDir.mkdirs()
                            val imageFile = File(imageDir, entry.name.substringAfter("pending_images/"))
                            imageFile.outputStream().use { output ->
                                zip.copyTo(output)
                            }
                        }
                        entry.name.startsWith("idea_images/") -> {
                            val imageDir = File(context.getExternalFilesDir(null), "idea_images")
                            imageDir.mkdirs()
                            val imageFile = File(imageDir, entry.name.substringAfter("idea_images/"))
                            imageFile.outputStream().use { output ->
                                zip.copyTo(output)
                            }
                        }
                    }
                    entry = zip.nextEntry
                }

                // 导入数据
                dataJson?.let { json ->
                    try {
                        val appData = gson.fromJson(json, AppData::class.java)
                        
                        // 清空所有现有数据
                        database.productDao().deleteAllProducts()
                        database.saleRecordDao().deleteAllSaleRecords()
                        database.voiceNoteDao().deleteAllVoiceNotes()
                        database.pendingProductDao().deleteAll()
                        database.pendingHistoryDao().deleteAll()
                        database.ideaRecordDao().deleteAll()
                        database.ideaHistoryDao().deleteAll()

                        // 更新图片路径
                        val updatedProducts = appData.products.map { product ->
                            if (product.imageUrl != null && !product.imageUrl.startsWith("http")) {
                                val fileName = File(product.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "sales_images/$fileName").absolutePath
                                product.copy(imageUrl = newPath)
                            } else {
                                product
                            }
                        }

                        val updatedSaleRecords = appData.saleRecords.map { saleRecord ->
                            if (saleRecord.imageUrl != null && !saleRecord.imageUrl.startsWith("http")) {
                                val fileName = File(saleRecord.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "sales_images/$fileName").absolutePath
                                saleRecord.copy(imageUrl = newPath)
                            } else {
                                saleRecord
                            }
                        }

                        val updatedPendingProducts = appData.pendingProducts.map { pendingProduct ->
                            if (pendingProduct.imageUrl != null && !pendingProduct.imageUrl.startsWith("http")) {
                                val fileName = File(pendingProduct.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "pending_images/$fileName").absolutePath
                                pendingProduct.copy(imageUrl = newPath)
                            } else {
                                pendingProduct
                            }
                        }

                        val updatedPendingHistory = appData.pendingHistory.map { pendingHistory ->
                            if (pendingHistory.imageUrl != null && !pendingHistory.imageUrl.startsWith("http")) {
                                val fileName = File(pendingHistory.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "pending_images/$fileName").absolutePath
                                pendingHistory.copy(imageUrl = newPath)
                            } else {
                                pendingHistory
                            }
                        }

                        val updatedIdeaRecords = appData.ideaRecords.map { ideaRecord ->
                            if (ideaRecord.imageUrl != null && !ideaRecord.imageUrl.startsWith("http")) {
                                val fileName = File(ideaRecord.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "idea_images/$fileName").absolutePath
                                ideaRecord.copy(imageUrl = newPath)
                            } else {
                                ideaRecord
                            }
                        }

                        val updatedIdeaHistory = appData.ideaHistory.map { ideaHistory ->
                            if (ideaHistory.imageUrl != null && !ideaHistory.imageUrl.startsWith("http")) {
                                val fileName = File(ideaHistory.imageUrl).name
                                val newPath = File(context.getExternalFilesDir(null), "idea_images/$fileName").absolutePath
                                ideaHistory.copy(imageUrl = newPath)
                            } else {
                                ideaHistory
                            }
                        }

                        // 导入更新后的数据
                        database.productDao().insertProducts(updatedProducts)
                        database.saleRecordDao().insertSaleRecords(updatedSaleRecords)
                        database.voiceNoteDao().insertVoiceNotes(appData.voiceNotes)
                        updatedPendingProducts.forEach { database.pendingProductDao().insert(it) }
                        updatedPendingHistory.forEach { database.pendingHistoryDao().insert(it) }
                        updatedIdeaRecords.forEach { database.ideaRecordDao().insert(it) }
                        updatedIdeaHistory.forEach { database.ideaHistoryDao().insert(it) }
                        
                        return@withContext true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@withContext false
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 发送数据
    suspend fun sendDataOverNetwork(
        port: Int = DEFAULT_PORT,
        progress: TransferProgress
    ) = withContext(Dispatchers.IO) {
        try {
            progress.onProgress(0, 100)
            progress.onComplete(false, "准备数据...")
            
            // 准备数据
            val appData = AppData(
                products = database.productDao().getAllProductsSync(),
                saleRecords = database.saleRecordDao().getAllSaleRecordsSync(),
                voiceNotes = database.voiceNoteDao().getAllVoiceNotesSync(),
                pendingProducts = database.pendingProductDao().getAllByCreatedAtDesc().first(),
                pendingHistory = database.pendingHistoryDao().getAllByDeletedAtDesc().first(),
                ideaRecords = database.ideaRecordDao().getAllByCreatedAtDesc().first(),
                ideaHistory = database.ideaHistoryDao().getAllByDeletedAtDesc().first()
            )

            // 创建临时ZIP文件
            val tempFile = File(context.cacheDir, "temp_transfer.zip")
            var totalSize = 0L
            
            // 写入ZIP文件并计算总大小
            ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
                // 写入JSON数据
                zip.putNextEntry(ZipEntry("data.json"))
                val jsonData = gson.toJson(appData).toByteArray()
                zip.write(jsonData)
                totalSize += jsonData.size
                zip.closeEntry()

                // 写入商品图片
                appData.products.forEach { product ->
                    product.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {  // 只处理本地图片
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("sales_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入售出商品图片
                appData.saleRecords.forEach { saleRecord ->
                    saleRecord.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {  // 只处理本地图片
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("sales_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入录音文件
                appData.voiceNotes.forEach { voiceNote ->
                    val voiceFile = File(voiceNote.filePath)
                    if (voiceFile.exists()) {
                        zip.putNextEntry(ZipEntry("voices/${voiceFile.name}"))
                        voiceFile.inputStream().use { it.copyTo(zip) }
                        totalSize += voiceFile.length()
                        zip.closeEntry()
                    }
                }

                // 写入待打印图片
                appData.pendingProducts.forEach { pendingProduct ->
                    pendingProduct.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("pending_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入待打印历史记录图片
                appData.pendingHistory.forEach { pendingHistory ->
                    pendingHistory.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("pending_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入创意图片
                appData.ideaRecords.forEach { ideaRecord ->
                    ideaRecord.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("idea_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 写入创意历史记录图片
                appData.ideaHistory.forEach { ideaHistory ->
                    ideaHistory.imageUrl?.let { imageUrl ->
                        if (!imageUrl.startsWith("http")) {
                            val imageFile = File(imageUrl)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("idea_images/${imageFile.name}"))
                                imageFile.inputStream().use { it.copyTo(zip) }
                                totalSize += imageFile.length()
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }

            // 发送UDP广播寻找接收方
            progress.onComplete(false, "正在寻找接收方...")
            var receiverAddress: String? = null
            var discoveryRetryCount = 0
            
            while (receiverAddress == null && discoveryRetryCount < MAX_RETRY_COUNT) {
                try {
                    DatagramSocket().use { socket ->
                        socket.broadcast = true
                        socket.soTimeout = DISCOVERY_TIMEOUT

                        // 在所有网络接口上发送广播
                        NetworkInterface.getNetworkInterfaces().toList().forEach { networkInterface ->
                            if (networkInterface.isUp && !networkInterface.isLoopback) {
                                networkInterface.interfaceAddresses.forEach { interfaceAddress ->
                                    val broadcast = interfaceAddress.broadcast
                                    if (broadcast != null) {
                                        // 发送广播
                                        val message = DISCOVERY_MESSAGE.toByteArray()
                                        val packet = DatagramPacket(
                                            message,
                                            message.size,
                                            broadcast,
                                            DISCOVERY_PORT
                                        )
                                        try {
                                            socket.send(packet)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }

                        // 等待响应
                        val buffer = ByteArray(1024)
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(receivePacket)
                            val response = String(receivePacket.data, 0, receivePacket.length)
                            if (response == "OK") {
                                receiverAddress = receivePacket.address.hostAddress
                            }
                        } catch (e: Exception) {
                            // 超时或其他错误，继续重试
                            discoveryRetryCount++
                            if (discoveryRetryCount < MAX_RETRY_COUNT) {
                                progress.onComplete(false, "正在重试寻找接收方 (${discoveryRetryCount}/${MAX_RETRY_COUNT})...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    discoveryRetryCount++
                    if (discoveryRetryCount < MAX_RETRY_COUNT) {
                        progress.onComplete(false, "正在重试寻找接收方 (${discoveryRetryCount}/${MAX_RETRY_COUNT})...")
                    }
                }
            }

            if (receiverAddress == null) {
                progress.onComplete(false, "未找到接收方，请确保接收方已准备就绪")
                return@withContext false
            }

            // 发现接收方后，等待一段时间再建立TCP连接
            progress.onComplete(false, "正在连接...")
            delay(TCP_CONNECT_DELAY)  // 给接收方一些时间准备ServerSocket

            var connected = false
            var connectRetryCount = 0
            var lastError: Exception? = null

            while (!connected && connectRetryCount < MAX_RETRY_COUNT) {
                try {
                    Socket(receiverAddress, port).use { socket ->
                        connected = true
                        progress.onComplete(false, "连接成功，开始发送...")
                        
                        socket.use { s ->
                            val out = s.getOutputStream().buffered()
                            // 首先发送文件大小
                            val sizeBytes = tempFile.length().toString().toByteArray()
                            out.write(sizeBytes.size)
                            out.write(sizeBytes)
                            
                            // 发送文件内容
                            tempFile.inputStream().use { input ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var bytes = input.read(buffer)
                                var sent = 0L
                                while (bytes >= 0) {
                                    out.write(buffer, 0, bytes)
                                    sent += bytes
                                    val progressPercent = ((sent.toFloat() / totalSize) * 90).toInt()
                                    progress.onProgress(progressPercent, 100)
                                    bytes = input.read(buffer)
                                }
                            }
                            out.flush()
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    connectRetryCount++
                    if (connectRetryCount < MAX_RETRY_COUNT) {
                        progress.onComplete(false, "连接失败，正在重试 (${connectRetryCount}/${MAX_RETRY_COUNT})...")
                        delay(1000) // 等待1秒后重试
                    }
                }
            }

            if (!connected) {
                progress.onComplete(false, "连接失败: ${lastError?.message}")
                return@withContext false
            }

            // 清理临时文件
            tempFile.delete()
            progress.onComplete(true, "发送完成")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            progress.onComplete(false, "发送失败: ${e.message}")
            false
        }
    }

    // 接收数据
    suspend fun receiveDataOverNetwork(
        port: Int = DEFAULT_PORT,
        progress: TransferProgress
    ) = withContext(Dispatchers.IO) {
        var serverSocket: ServerSocket? = null
        try {
            progress.onProgress(0, 100)
            progress.onComplete(false, "等待连接...")
            
            // 先创建ServerSocket
            serverSocket = ServerSocket(port)

            // 创建UDP socket监听发现请求
            DatagramSocket(DISCOVERY_PORT).use { udpSocket ->
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                
                try {
                    // 等待发现请求
                    udpSocket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    
                    if (message == DISCOVERY_MESSAGE) {
                        // 发送响应
                        val response = "OK".toByteArray()
                        val responsePacket = DatagramPacket(
                            response,
                            response.size,
                            packet.address,
                            packet.port
                        )
                        udpSocket.send(responsePacket)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 等待TCP连接
            progress.onComplete(false, "发现发送方，等待连接...")
            val socket = serverSocket.accept()
            progress.onComplete(false, "连接成功，开始接收数据...")
            
            // 创建临时文件
            val tempFile = File(context.cacheDir, "temp_received.zip")
            
            socket.use { s ->
                val input = s.getInputStream().buffered()
                
                // 首先读取文件大小
                val sizeLength = input.read()
                val sizeBytes = ByteArray(sizeLength)
                input.read(sizeBytes)
                val totalSize = String(sizeBytes).toLong()
                
                var received = 0L
                
                // 接收文件内容
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        received += bytes
                        val progressPercent = ((received.toFloat() / totalSize) * 90).toInt()
                        progress.onProgress(progressPercent, 100)
                        bytes = input.read(buffer)
                    }
                }
            }

            // 导入接收到的数据
            progress.onProgress(90, 100)
            val success = importFromLocal(tempFile)
            tempFile.delete()
            
            progress.onComplete(success, if (success) "接收完成" else "导入失败")
            success
        } catch (e: Exception) {
            e.printStackTrace()
            progress.onComplete(false, "接收失败: ${e.message}")
            false
        } finally {
            serverSocket?.close()
        }
    }
} 