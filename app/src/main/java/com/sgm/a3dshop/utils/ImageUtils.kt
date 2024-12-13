package com.sgm.a3dshop.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val COMPRESS_QUALITY = 80
    private const val MAX_IMAGE_DIMENSION = 1920

    // 定义不同模块的图片存储目录
    private const val DIR_PENDING = "pending_images"
    private const val DIR_IDEA = "idea_images"

    fun createImageFile(context: Context, isIdea: Boolean = false): File {
        val timestamp = System.currentTimeMillis()
        val filename = "IMG_$timestamp.jpg"
        val storageDir = if (isIdea) {
            File(context.getExternalFilesDir(null), DIR_IDEA).apply { mkdirs() }
        } else {
            File(context.getExternalFilesDir(null), DIR_PENDING).apply { mkdirs() }
        }
        return File(storageDir, filename)
    }

    fun compressImage(context: Context, uri: Uri, isIdea: Boolean = false): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // 计算缩放比例
            val scale = calculateScale(options.outWidth, options.outHeight)

            // 重新打开输入流并解码图片
            val newInputStream = context.contentResolver.openInputStream(uri)
            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            var bitmap = BitmapFactory.decodeStream(newInputStream, null, decodingOptions)
            newInputStream?.close()

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return null
            }

            // 获取图片的Exif信息并修正旋转
            val exifRotation = getExifRotation(context, uri)
            if (exifRotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(exifRotation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            // 保存压缩后的图片
            val outputFile = createCompressedImageFile(context, isIdea)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
            }
            bitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }

    private fun calculateScale(width: Int, height: Int): Int {
        var scale = 1
        while (width / scale > MAX_IMAGE_DIMENSION || height / scale > MAX_IMAGE_DIMENSION) {
            scale *= 2
        }
        return scale
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                return when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting EXIF rotation", e)
        }
        return 0
    }

    private fun createCompressedImageFile(context: Context, isIdea: Boolean = false): File {
        val timestamp = System.currentTimeMillis()
        val filename = "COMPRESSED_$timestamp.jpg"
        val storageDir = if (isIdea) {
            File(context.getExternalFilesDir(null), DIR_IDEA).apply { mkdirs() }
        } else {
            File(context.getExternalFilesDir(null), DIR_PENDING).apply { mkdirs() }
        }
        return File(storageDir, filename)
    }
} 