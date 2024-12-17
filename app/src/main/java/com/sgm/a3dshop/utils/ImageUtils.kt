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
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val COMPRESS_QUALITY = 80
    private const val MAX_IMAGE_DIMENSION = 1920

    // 定义不同模块的图片存储目录
    const val DIR_PENDING = "pending_images"
    const val DIR_IDEAS = "idea_images"
    const val DIR_SALES = "sales_images"
    const val DIR_PRODUCTS = "product_images"

    internal fun getUserSpecificDir(context: Context, type: String): File {
        val userId = context.packageName + "_" + context.getSystemService(Context.USER_SERVICE).hashCode()
        return File(context.getExternalFilesDir(null), "${userId}_$type").apply { mkdirs() }
    }

    fun createImageFile(context: Context, type: String = DIR_SALES): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "IMG_${timestamp}.jpg"
        val storageDir = getUserSpecificDir(context, type)
        return File(storageDir, filename)
    }

    fun compressImage(context: Context, uri: Uri, type: String = DIR_SALES): String? {
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
            val outputFile = createImageFile(context, type)
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

    fun saveImageFromUri(context: Context, uri: Uri, type: String = DIR_SALES): String? {
        return compressImage(context, uri, type)
    }

    fun fixImageOrientation(imagePath: String) {
        try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                // 读取原始图片
                val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                
                // 根据EXIF信息旋转图片
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                }

                // 创建新的图片
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    matrix, true
                )

                // 保存旋转后的图片
                FileOutputStream(imagePath).use { out ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                // 清理
                bitmap.recycle()
                rotatedBitmap.recycle()

                // 更新EXIF信息
                val newExif = ExifInterface(imagePath)
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                newExif.saveAttributes()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 