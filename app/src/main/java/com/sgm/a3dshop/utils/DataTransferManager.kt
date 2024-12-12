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

class DataTransferManager(private val context: Context) {
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

    // 通过Socket发送数据
    suspend fun sendDataOverNetwork(port: Int = 8888): Boolean = withContext(Dispatchers.IO) {
        try {
            ServerSocket(port).use { serverSocket ->
                val socket = serverSocket.accept()
                sendData(socket)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 通过Socket接收数据
    suspend fun receiveDataOverNetwork(host: String, port: Int = 8888): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket(host, port).use { socket ->
                receiveData(socket)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun sendData(socket: Socket) {
        val appData = AppData(
            products = database.productDao().getAllProductsSync(),
            saleRecords = database.saleRecordDao().getAllSaleRecordsSync(),
            voiceNotes = database.voiceNoteDao().getAllVoiceNotesSync()
        )

        ZipOutputStream(socket.getOutputStream().buffered()).use { zip ->
            // 写入数据JSON
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(gson.toJson(appData).toByteArray())
            zip.closeEntry()

            // 发送录音文件
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
    }

    private suspend fun receiveData(socket: Socket) {
        ZipInputStream(socket.getInputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            var dataJson: String? = null

            while (entry != null) {
                when {
                    entry.name == "data.json" -> {
                        dataJson = zip.bufferedReader().readText()
                    }
                    entry.name.startsWith("voices/") -> {
                        val voiceFile = File(context.cacheDir, entry.name.substringAfter("voices/"))
                        voiceFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                }
                entry = zip.nextEntry
            }

            // 导入数据
            dataJson?.let { json ->
                val appData = gson.fromJson(json, AppData::class.java)
                database.productDao().deleteAllProducts()
                database.saleRecordDao().deleteAllSaleRecords()
                database.voiceNoteDao().deleteAllVoiceNotes()

                database.productDao().insertProducts(appData.products)
                database.saleRecordDao().insertSaleRecords(appData.saleRecords)
                database.voiceNoteDao().insertVoiceNotes(appData.voiceNotes)
            }
        }
    }
} 