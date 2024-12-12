package com.sgm.a3dshop.ui.common

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sgm.a3dshop.databinding.DialogVoiceRecordBinding
import com.sgm.a3dshop.utils.AudioUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecordDialog(
    context: Context,
    private val onSave: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogVoiceRecordBinding
    private var isRecording = false
    private var outputFile: File? = null
    private var activity: AppCompatActivity? = null
    private val handler = Handler(Looper.getMainLooper())
    private var recordingTime = 0
    private val timeFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())

    private val requestPermissionLauncher = (context as? AppCompatActivity)?.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecordingWithPermission()
        } else {
            Toast.makeText(context, "需要录音权限才能录制语音", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogVoiceRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activity = context as? AppCompatActivity
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            btnRecord.setOnClickListener {
                if (!isRecording) {
                    checkAndRequestAudioPermission()
                } else {
                    stopRecording()
                }
            }

            btnPlay.setOnClickListener {
                outputFile?.let { file ->
                    if (AudioUtils.isPlaying()) {
                        AudioUtils.stopPlaying()
                        btnPlay.text = "播放"
                    } else {
                        AudioUtils.startPlaying(file.absolutePath) {
                            btnPlay.post {
                                btnPlay.text = "播放"
                            }
                        }
                        btnPlay.text = "暂停"
                    }
                }
            }

            btnSave.setOnClickListener {
                outputFile?.let { file ->
                    onSave(file.absolutePath)
                    dismiss()
                }
            }

            btnCancel.setOnClickListener {
                AudioUtils.stopPlaying()
                outputFile?.delete()
                dismiss()
            }
        }
    }

    private fun checkAndRequestAudioPermission() {
        activity?.let { act ->
            when {
                ContextCompat.checkSelfPermission(
                    act,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startRecordingWithPermission()
                }
                act.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("需要录音权限")
            .setMessage("录制语音需要使用录音权限，请在设置中授予权限。")
            .setPositiveButton("请求权限") { _, _ ->
                requestPermissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startRecordingWithPermission() {
        try {
            outputFile = AudioUtils.createAudioFile(context)
            outputFile?.let { file ->
                AudioUtils.startRecording(context, file.absolutePath)
                isRecording = true
                binding.btnRecord.text = "停止"
                binding.btnPlay.isEnabled = false
                binding.btnSave.isEnabled = false
                startTimer()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "录音失败: ${e.message}", Toast.LENGTH_SHORT).show()
            outputFile?.delete()
            outputFile = null
        }
    }

    private fun stopRecording() {
        AudioUtils.stopRecording()
        isRecording = false
        binding.btnRecord.text = "录制"
        binding.btnPlay.isEnabled = true
        binding.btnSave.isEnabled = true
        stopTimer()
    }

    private fun startTimer() {
        recordingTime = 0
        updateTimer()
        handler.post(object : Runnable {
            override fun run() {
                if (isRecording) {
                    recordingTime++
                    updateTimer()
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopTimer() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateTimer() {
        val date = Date(recordingTime * 1000L)
        binding.tvTimer.text = timeFormatter.format(date)
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
        AudioUtils.stopPlaying()
        handler.removeCallbacksAndMessages(null)
    }
} 