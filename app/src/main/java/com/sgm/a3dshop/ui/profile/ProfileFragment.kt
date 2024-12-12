package com.sgm.a3dshop.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sgm.a3dshop.databinding.FragmentProfileBinding
import com.sgm.a3dshop.ui.common.VoiceRecordDialog
import com.sgm.a3dshop.ui.common.PlaySettingsDialog
import com.sgm.a3dshop.utils.AudioUtils
import com.sgm.a3dshop.utils.DataTransferManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireActivity().application)
    }

    private lateinit var voiceNoteAdapter: VoiceNoteAdapter
    private lateinit var dataTransferManager: DataTransferManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentDialog?.startRecording()
        } else {
            Toast.makeText(context, "需要录音权限才能继续", Toast.LENGTH_SHORT).show()
        }
    }

    private var currentDialog: VoiceRecordDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataTransferManager = DataTransferManager(requireContext())
        setupViews()
        observeData()
    }

    private fun setupViews() {
        voiceNoteAdapter = VoiceNoteAdapter(
            onPlayClick = { voiceNote ->
                if (voiceNote.isLoopEnabled) {
                    showPlaySettingsDialog(voiceNote.filePath)
                } else {
                    AudioUtils.startPlaying(voiceNote.filePath) {
                        voiceNoteAdapter.notifyItemChanged(voiceNoteAdapter.currentList.indexOf(voiceNote))
                    }
                }
            },
            onPauseClick = { voiceNote ->
                if (AudioUtils.isPaused()) {
                    AudioUtils.resumeAudio()
                } else {
                    AudioUtils.pauseAudio()
                }
                voiceNoteAdapter.notifyItemChanged(voiceNoteAdapter.currentList.indexOf(voiceNote))
            },
            onStopClick = { voiceNote ->
                AudioUtils.stopLoopPlay()
                voiceNoteAdapter.notifyItemChanged(voiceNoteAdapter.currentList.indexOf(voiceNote))
            }
        )

        binding.apply {
            recyclerVoiceNotes.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = voiceNoteAdapter
            }

            btnRecord.setOnClickListener {
                showVoiceRecordDialog()
            }

            btnExport.setOnClickListener {
                exportData()
            }

            btnImport.setOnClickListener {
                importData()
            }
        }
    }

    private fun showVoiceRecordDialog() {
        currentDialog = VoiceRecordDialog(
            requireContext(),
            onStartRecording = {
                checkPermissionAndRecord()
            },
            onRecordFinished = { filePath ->
                viewModel.saveVoiceNote(filePath)
            }
        ).also { it.show() }
    }

    private fun checkPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                currentDialog?.startRecording()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showPlaySettingsDialog(filePath: String) {
        PlaySettingsDialog(requireContext()) { intervalSeconds ->
            AudioUtils.startLoopPlay(filePath, intervalSeconds) { isPlaying ->
                // 更新UI状态
                voiceNoteAdapter.notifyDataSetChanged()
            }
        }.show()
    }

    private fun exportData() {
        try {
            val success = dataTransferManager.exportData()
            if (success) {
                Toast.makeText(context, "数据导出成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "数据导出失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        try {
            val success = dataTransferManager.importData()
            if (success) {
                Toast.makeText(context, "数据导入成功", Toast.LENGTH_SHORT).show()
                // 刷新数据
                viewModel.refreshData()
            } else {
                Toast.makeText(context, "数据导入失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导入错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.voiceNotes.collectLatest { notes ->
                voiceNoteAdapter.submitList(notes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentDialog = null
        AudioUtils.stopLoopPlay()
        _binding = null
    }
} 