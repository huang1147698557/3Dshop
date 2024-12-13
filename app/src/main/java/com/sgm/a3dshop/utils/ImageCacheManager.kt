package com.sgm.a3dshop.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

object ImageCacheManager {
    private const val TAG = "ImageCacheManager"
    private const val IMAGES_FOLDER = "3DShop_Images"
    private const val PENDING_IMAGES = "pending_images"
    private const val IDEA_IMAGES = "idea_images"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        createImageDirectories()
    }

    private fun createImageDirectories() {
        val baseDir = appContext.getExternalFilesDir(null)
        File(baseDir, IMAGES_FOLDER).mkdirs()
        File(baseDir, PENDING_IMAGES).mkdirs()
        File(baseDir, IDEA_IMAGES).mkdirs()
    }

    fun copyImageToCache(sourceFile: File): String? {
        return try {
            val timestamp = System.currentTimeMillis()
            val destFile = File(
                appContext.getExternalFilesDir(null),
                "$IMAGES_FOLDER/IMG_$timestamp.jpg"
            )

            copyFile(sourceFile, destFile)
            destFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy image to cache", e)
            null
        }
    }

    fun deleteImage(imagePath: String) {
        try {
            File(imagePath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image: $imagePath", e)
        }
    }

    fun clearImageCache() {
        try {
            val baseDir = appContext.getExternalFilesDir(null)
            File(baseDir, IMAGES_FOLDER).deleteRecursively()
            File(baseDir, PENDING_IMAGES).deleteRecursively()
            File(baseDir, IDEA_IMAGES).deleteRecursively()
            createImageDirectories()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear image cache", e)
        }
    }

    private fun copyFile(sourceFile: File, destFile: File) {
        if (!destFile.exists()) {
            destFile.createNewFile()
        }

        var source: FileChannel? = null
        var destination: FileChannel? = null

        try {
            source = FileInputStream(sourceFile).channel
            destination = FileOutputStream(destFile).channel
            destination.transferFrom(source, 0, source.size())
        } finally {
            source?.close()
            destination?.close()
        }
    }

    fun destroy() {
        // 清理资源
    }
} 