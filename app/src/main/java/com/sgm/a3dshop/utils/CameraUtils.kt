package com.sgm.a3dshop.utils

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor

object CameraUtils {
    suspend fun createImageFile(context: Context, isIdea: Boolean = false): File = withContext(Dispatchers.IO) {
        ImageUtils.createImageFile(context, isIdea)
    }

    suspend fun takePhoto(
        imageCapture: ImageCapture,
        photoFile: File,
        context: Context
    ): Uri = withContext(Dispatchers.IO) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        return@withContext suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: photoFile.toUri()
                        continuation.resumeWith(Result.success(savedUri))
                    }

                    override fun onError(exc: ImageCaptureException) {
                        continuation.resumeWith(Result.failure(exc))
                    }
                }
            )
        }
    }
} 