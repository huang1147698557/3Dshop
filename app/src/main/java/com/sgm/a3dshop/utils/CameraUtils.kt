package com.sgm.a3dshop.utils

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CameraUtils {
    suspend fun takePhoto(
        imageCapture: ImageCapture,
        outputFile: File,
        context: Context
    ): Uri = suspendCancellableCoroutine { continuation ->
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        continuation.resume(uri)
                    } ?: continuation.resumeWithException(Exception("Failed to get saved image URI"))
                }

                override fun onError(exc: ImageCaptureException) {
                    continuation.resumeWithException(exc)
                }
            }
        )
    }
} 