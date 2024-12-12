package com.sgm.a3dshop.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.entity.VoiceNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DataManager {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private const val EXPORT_FILENAME = "3dshop_backup"
    private const val PRODUCTS_JSON = "products.json"
    private const val SALES_JSON = "sales.json"
    private const val VOICE_NOTES_JSON = "voice_notes.json"
    private const val IMAGES_DIR = "images"
    private const val VOICE_DIR = "voice_notes"

    suspend fun exportData(context: Context): File = withContext(Dispatchers.IO) {
        val timeStamp = dateFormat.format(Date())
        val exportFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "${EXPORT_FILENAME}_${timeStamp}.zip"
        )

        val db = AppDatabase.getDatabase(context)
        val gson = Gson()

        // 获取所有数据
        val products = db.productDao().getAllProductsSync()
        val sales = db.saleRecordDao().getAllSaleRecordsSync()
        val voiceNotes = db.voiceNoteDao().getAllVoiceNotesSync()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(exportFile))).use { zip ->
            // 导出商品数据
            zip.putNextEntry(ZipEntry(PRODUCTS_JSON))
            zip.write(gson.toJson(products).toByteArray())
            zip.closeEntry()

            // 导出销售记录
            zip.putNextEntry(ZipEntry(SALES_JSON))
            zip.write(gson.toJson(sales).toByteArray())
            zip.closeEntry()

            // 导出录音记录
            zip.putNextEntry(ZipEntry(VOICE_NOTES_JSON))
            zip.write(gson.toJson(voiceNotes).toByteArray())
            zip.closeEntry()

            // 导出图片文件
            for (product in products) {
                val imageUrl = product.imageUrl
                if (imageUrl != null) {
                    val imageFile = File(imageUrl)
                    if (imageFile.exists()) {
                        zip.putNextEntry(ZipEntry("$IMAGES_DIR/${imageFile.name}"))
                        imageFile.inputStream().use { input ->
                            input.copyTo(zip)
                        }
                        zip.closeEntry()
                    }
                }
            }

            // 导出录音文件
            for (voiceNote in voiceNotes) {
                val voiceFile = File(voiceNote.filePath)
                if (voiceFile.exists()) {
                    zip.putNextEntry(ZipEntry("$VOICE_DIR/${voiceFile.name}"))
                    voiceFile.inputStream().use { input ->
                        input.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }
        }

        exportFile
    }

    suspend fun importData(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val gson = Gson()
        val tempDir = File(context.cacheDir, "import_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        // 解压文件
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry: ZipEntry?
                while (zip.nextEntry.also { entry = it } != null) {
                    val file = File(tempDir, entry!!.name)
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        zip.copyTo(output)
                    }
                }
            }
        }

        // 读取并导入数据
        try {
            // 导入商品数据
            File(tempDir, PRODUCTS_JSON).let { file ->
                if (file.exists()) {
                    val products: List<Product> = gson.fromJson(
                        file.readText(),
                        object : TypeToken<List<Product>>() {}.type
                    )
                    db.productDao().insertProducts(products)
                }
            }

            // 导入销售记录
            File(tempDir, SALES_JSON).let { file ->
                if (file.exists()) {
                    val sales: List<SaleRecord> = gson.fromJson(
                        file.readText(),
                        object : TypeToken<List<SaleRecord>>() {}.type
                    )
                    db.saleRecordDao().insertSaleRecords(sales)
                }
            }

            // 导入录音记录
            File(tempDir, VOICE_NOTES_JSON).let { file ->
                if (file.exists()) {
                    val voiceNotes: List<VoiceNote> = gson.fromJson(
                        file.readText(),
                        object : TypeToken<List<VoiceNote>>() {}.type
                    )
                    db.voiceNoteDao().insertVoiceNotes(voiceNotes)
                }
            }

            // 复制图片文件
            val imagesDir = File(tempDir, IMAGES_DIR)
            if (imagesDir.exists()) {
                val targetImagesDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    CameraUtils.IMAGES_FOLDER
                ).apply { mkdirs() }
                imagesDir.listFiles()?.forEach { file ->
                    file.copyTo(File(targetImagesDir, file.name), overwrite = true)
                }
            }

            // 复制录音文件
            val voiceDir = File(tempDir, VOICE_DIR)
            if (voiceDir.exists()) {
                val targetVoiceDir = File(
                    context.getExternalFilesDir(null),
                    "VoiceNotes"
                ).apply { mkdirs() }
                voiceDir.listFiles()?.forEach { file ->
                    file.copyTo(File(targetVoiceDir, file.name), overwrite = true)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            // 清理临时文件
            tempDir.deleteRecursively()
        }
    }
} 