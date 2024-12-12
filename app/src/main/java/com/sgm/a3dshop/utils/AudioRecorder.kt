package com.sgm.a3dshop.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    @Throws(IOException::class)
    fun startRecording(outputFile: String) {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile)
            prepare()
            start()
        }
    }

    fun stopRecording() {
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // Ignore
            }
            release()
        }
        recorder = null
    }
} 