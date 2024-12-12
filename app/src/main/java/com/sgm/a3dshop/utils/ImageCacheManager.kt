package com.sgm.a3dshop.utils

import android.content.Context
import android.os.Environment
import com.bumptech.glide.Glide
import java.io.File
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

object ImageCacheManager {
    const val MAX_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
    private const val CACHE_CLEAN_INTERVAL = 24 * 60 * 60 * 1000L // 24小时
    private const val MAX_FILE_AGE = 7 * 24 * 60 * 60 * 1000L // 7天

    private var timer: Timer? = null

    fun init(context: Context) {
        // 启动定时清理任务
        timer = Timer().apply {
            scheduleAtFixedRate(delay = 0, period = CACHE_CLEAN_INTERVAL) {
                cleanCache(context)
            }
        }
    }

    fun cleanCache(context: Context) {
        try {
            // 清理Glide缓存
            clearGlideCache(context)
            
            // 清理自定义图片缓存
            clearCustomCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearGlideCache(context: Context) {
        try {
            // 清理内存缓存
            Glide.get(context).clearMemory()
            
            // 在后台线程中清理磁盘缓存
            Thread {
                Glide.get(context).clearDiskCache()
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearCustomCache(context: Context) {
        val imageDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            CameraUtils.IMAGES_FOLDER
        )

        if (!imageDir.exists()) return

        val currentTime = System.currentTimeMillis()
        var totalSize = 0L

        // 获取所有图片文件并按最后修改时间排序
        val imageFiles = imageDir.listFiles()?.filter {
            it.isFile && it.name.endsWith(".jpg", ignoreCase = true)
        }?.sortedBy { it.lastModified() } ?: return

        // 计算总大小
        imageFiles.forEach { file ->
            totalSize += file.length()
        }

        // 如果总大小超过限制或文件过期，则删除文件
        imageFiles.forEach { file ->
            val fileAge = currentTime - file.lastModified()
            
            if (fileAge > MAX_FILE_AGE || totalSize > MAX_CACHE_SIZE) {
                if (file.delete()) {
                    totalSize -= file.length()
                }
            }
        }
    }

    fun getCacheSize(context: Context): Long {
        var totalSize = 0L

        // 获取Glide缓存大小
        try {
            val cacheDirs = arrayOf(
                Glide.getPhotoCacheDir(context),
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                    File(it, CameraUtils.IMAGES_FOLDER)
                }
            )

            cacheDirs.filterNotNull().forEach { dir ->
                if (dir.exists()) {
                    totalSize += calculateDirSize(dir)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return totalSize
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun formatSize(size: Long): String {
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L

        return when {
            size >= gb -> String.format("%.1f GB", size.toFloat() / gb)
            size >= mb -> String.format("%.1f MB", size.toFloat() / mb)
            size >= kb -> String.format("%.1f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }

    fun destroy() {
        timer?.cancel()
        timer = null
    }
} 