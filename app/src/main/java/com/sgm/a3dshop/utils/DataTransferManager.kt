package com.sgm.a3dshop.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.entity.VoiceNote
import kotlinx.coroutines.Dispatchers
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
                // 如果所有格式都失败了，记录错误并返回当前时间
                println("Failed to parse date: $dateStr")
                return Date()
            }
        })
        .create()

    // 数据模型类，用于JSON序列化
    data class AppData(
        val products: List<Product>,
        val saleRecords: List<SaleRecord>,
        val voiceNotes: List<VoiceNote>
    )

    // 本地导出
    suspend fun exportToLocal(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val appData = AppData(
                products = database.productDao().getAllProductsSync(),
                saleRecords = database.saleRecordDao().getAllSaleRecordsSync(),
                voiceNotes = database.voiceNoteDao().getAllVoiceNotesSync()
            )

            // 创建导出目录
            val exportDir = File(context.getExternalFilesDir(null), "export")
            exportDir.mkdirs()

            // 创建ZIP文件
            val zipFile = File(exportDir, "app_data_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                // 写入数据JSON
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(gson.toJson(appData).toByteArray())
                zip.closeEntry()

                // 复制录音文件
                appData.voiceNotes.forEach { voiceNote ->
                    val voiceFile = File(voiceNote.filePath)
                    if (voiceFile.exists()) {
                        zip.putNextEntry(ZipEntry("voices/${voiceFile.name}"))
                        voiceFile.inputStream().use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
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
                            val voiceFile = File(context.cacheDir, entry.name.substringAfter("voices/"))
                            voiceFile.parentFile?.mkdirs()  // 确保目录存在
                            voiceFile.outputStream().use { output ->
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
                        database.productDao().deleteAllProducts()
                        database.saleRecordDao().deleteAllSaleRecords()
                        database.voiceNoteDao().deleteAllVoiceNotes()

                        database.productDao().insertProducts(appData.products)
                        database.saleRecordDao().insertSaleRecords(appData.saleRecords)
                        database.voiceNoteDao().insertVoiceNotes(appData.voiceNotes)
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
                voiceNotes = database.voiceNoteDao().getAllVoiceNotesSync()
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
            }

            // 发送UDP广播寻找接收方
            progress.onComplete(false, "正在寻找接收方...")
            var receiverAddress: String? = null
            var retryCount = 0
            
            while (receiverAddress == null && retryCount < MAX_RETRY_COUNT) {
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

                        // 也发送到特定广播地址
                        val message = DISCOVERY_MESSAGE.toByteArray()
                        val addresses = arrayOf(
                            "255.255.255.255",
                            "192.168.43.255",  // 常见的热点广播地址
                            "192.168.1.255"    // 常见的路由器广播地址
                        )
                        
                        addresses.forEach { address ->
                            try {
                                val packet = DatagramPacket(
                                    message,
                                    message.size,
                                    InetAddress.getByName(address),
                                    DISCOVERY_PORT
                                )
                                socket.send(packet)
                            } catch (e: Exception) {
                                e.printStackTrace()
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
                            retryCount++
                            if (retryCount < MAX_RETRY_COUNT) {
                                progress.onComplete(false, "正在重试寻找接收方 (${retryCount}/${MAX_RETRY_COUNT})...")
                            }
                        }
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < MAX_RETRY_COUNT) {
                        progress.onComplete(false, "正在重试寻找接收方 (${retryCount}/${MAX_RETRY_COUNT})...")
                    }
                }
            }

            if (receiverAddress == null) {
                progress.onComplete(false, "未找到接收方，请确保接收方已准备就绪")
                return@withContext false
            }

            // 连接到接收方
            progress.onComplete(false, "正在连接...")
            Socket(receiverAddress, port).use { socket ->
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
                        while (bytes >= 0) {
                            out.write(buffer, 0, bytes)
                            bytes = input.read(buffer)
                        }
                    }
                    out.flush()
                }
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
        try {
            progress.onProgress(0, 100)
            progress.onComplete(false, "等待连接...")
            
            // 启动UDP监听，等待发送方发现
            var discoverySocket: DatagramSocket? = null
            try {
                discoverySocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }
                
                while (true) {
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket.receive(packet)
                    
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
                        // 多次发送响应以提高成功率
                        repeat(3) {
                            try {
                                discoverySocket.send(responsePacket)
                                Thread.sleep(100)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        break
                    }
                }
            } finally {
                discoverySocket?.close()
            }

            // 创建临时文件
            val tempFile = File(context.cacheDir, "temp_received.zip")
            
            // 等待TCP连接
            ServerSocket(port).use { serverSocket ->
                progress.onComplete(false, "发现发送方，等待连接...")
                val socket = serverSocket.accept()
                progress.onComplete(false, "连接成功，开始接收数据...")
                
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
        }
    }
} 