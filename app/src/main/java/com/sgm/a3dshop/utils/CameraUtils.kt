package com.sgm.a3dshop.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object CameraUtils {
    private const val TAG = "CameraUtils"
    const val IMAGES_FOLDER = "3DShop_Images"
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun createImageFile(context: Context): File {
        val timeStamp = dateFormat.format(Date())
        val imageFileName = "ORIGINAL_${timeStamp}"
        val storageDir = File(
            context.getExternalFilesDir(null),
            IMAGES_FOLDER
        ).apply {
            if (!exists()) {
                mkdirs()
            }
        }

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    suspend fun takePhoto(
        imageCapture: ImageCapture,
        outputFile: File,
        context: Context
    ): Uri = suspendCoroutine { continuation ->
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            context.mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        Log.d(TAG, "Photo capture succeeded: ${uri.path}")
                        continuation.resume(uri)
                    } ?: run {
                        continuation.resumeWithException(Exception("Failed to get saved image URI"))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
} 