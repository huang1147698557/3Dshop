package com.sgm.a3dshop.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.IOException

object AudioUtils {
    private const val TAG = "AudioUtils"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isLoopPlaying = false
    private var isPaused = false
    private var currentPlayingPath: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var loopRunnable: Runnable? = null

    fun createAudioFile(context: Context): File {
        val audioDir = File(context.getExternalFilesDir(null), "VoiceNotes").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        return File(audioDir, "AUDIO_${System.currentTimeMillis()}.mp3")
    }

    fun startRecording(context: Context, filePath: String) {
        try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)
                try {
                    prepare()
                    start()
                } catch (e: IOException) {
                    Log.e(TAG, "prepare() failed", e)
                    release()
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            mediaRecorder?.release()
            mediaRecorder = null
            throw e
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording failed", e)
        } finally {
            mediaRecorder = null
        }
    }

    fun startPlaying(filePath: String, onCompletion: () -> Unit) {
        stopLoopPlay()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                setOnCompletionListener {
                    currentPlayingPath = null
                    onCompletion()
                }
                prepare()
                start()
                currentPlayingPath = filePath
                isPaused = false
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
                release()
                currentPlayingPath = null
            }
        }
    }

    fun startLoopPlay(filePath: String, intervalSeconds: Int, onPlayStateChanged: ((Boolean) -> Unit)? = null) {
        stopLoopPlay()
        isLoopPlaying = true
        isPaused = false
        currentPlayingPath = filePath
        
        loopRunnable = object : Runnable {
            override fun run() {
                if (isLoopPlaying && !isPaused) {
                    playAudio(filePath)
                    onPlayStateChanged?.invoke(true)
                    handler.postDelayed(this, intervalSeconds * 1000L)
                }
            }
        }
        
        loopRunnable?.run()
    }

    private fun playAudio(filePath: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "playAudio failed", e)
                release()
            }
        }
    }

    fun stopLoopPlay() {
        isLoopPlaying = false
        loopRunnable?.let { handler.removeCallbacks(it) }
        loopRunnable = null
        stopPlaying()
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (!it.isPlaying && isPaused) {
                it.start()
                isPaused = false
            }
        }
    }

    fun stopPlaying() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentPlayingPath = null
        isPaused = false
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun isPaused(): Boolean = isPaused
    fun isLoopPlaying(): Boolean = isLoopPlaying
    fun getCurrentPlayingPath(): String? = currentPlayingPath
} 