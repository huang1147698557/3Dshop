package com.sgm.a3dshop.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sgm.a3dshop.databinding.DialogVoiceRecordBinding
import com.sgm.a3dshop.utils.AudioRecorder
import java.io.File
import java.util.concurrent.TimeUnit

class VoiceRecordDialog(
    context: Context,
    private val onStartRecording: () -> Unit,
    private val onRecordFinished: (String) -> Unit
) : Dialog(context) {

    private var _binding: DialogVoiceRecordBinding? = null
    private val binding get() = _binding!!

    private var isRecording = false
    private var recordFile: File? = null
    private val audioRecorder = AudioRecorder(context)
    
    private var elapsedTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimer = object : Runnable {
        override fun run() {
            updateTimerText()
            if (isRecording) {
                elapsedTime += 1000
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DialogVoiceRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            btnRecord.setOnClickListener {
                if (!isRecording) {
                    onStartRecording()
                } else {
                    stopRecording()
                }
            }

            btnCancel.setOnClickListener {
                if (isRecording) {
                    stopRecording()
                }
                recordFile?.delete()
                dismiss()
            }

            btnSave.setOnClickListener {
                recordFile?.let { file ->
                    onRecordFinished(file.absolutePath)
                }
                dismiss()
            }
        }
    }

    fun startRecording() {
        recordFile = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.mp3")
        audioRecorder.startRecording(recordFile!!.absolutePath)
        isRecording = true
        elapsedTime = 0L
        handler.post(updateTimer)
        updateRecordingUI()
    }

    private fun stopRecording() {
        audioRecorder.stopRecording()
        isRecording = false
        handler.removeCallbacks(updateTimer)
        updateRecordingUI()
    }

    private fun updateTimerText() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
        binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateRecordingUI() {
        binding.apply {
            btnRecord.text = if (isRecording) "停止录音" else "开始录音"
            btnSave.isEnabled = !isRecording && recordFile != null
            btnPlay.isEnabled = !isRecording && recordFile != null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isRecording) {
            stopRecording()
        }
        handler.removeCallbacks(updateTimer)
        _binding = null
    }
} 