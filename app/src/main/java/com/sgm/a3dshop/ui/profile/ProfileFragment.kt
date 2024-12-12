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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sgm.a3dshop.data.entity.VoiceNote
import com.sgm.a3dshop.databinding.FragmentProfileBinding
import com.sgm.a3dshop.ui.common.VoiceRecordDialog
import com.sgm.a3dshop.ui.common.PlaySettingsDialog
import com.sgm.a3dshop.utils.AudioUtils
import com.sgm.a3dshop.utils.DataTransferManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent
import android.app.Activity
import java.io.File

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

        // 添加滑动删除功能
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val voiceNote = voiceNoteAdapter.currentList[position]
                showDeleteConfirmDialog(voiceNote, position)
            }
        }).attachToRecyclerView(binding.recyclerVoiceNotes)
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
        lifecycleScope.launch {
            try {
                val (success, filePath) = dataTransferManager.exportToLocal()
                if (success) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("导出成功")
                        .setMessage("数据已导出到：\n$filePath")
                        .setPositiveButton("确定", null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "数据导出失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导出错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importData() {
        // 打开文件选择器
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        startActivityForResult(Intent.createChooser(intent, "选择备份文件"), REQUEST_IMPORT_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                lifecycleScope.launch {
                    try {
                        // 将 URI 转换为临时文件
                        val tempFile = File(requireContext().cacheDir, "temp_import.zip")
                        requireContext().contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        val success = dataTransferManager.importFromLocal(tempFile)
                        val message = if (success) "数据导入成功" else "数据导入失败"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        
                        // 删除临时文件
                        tempFile.delete()
                        
                        // 刷新数据
                        viewModel.refreshData()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "导入错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.voiceNotes.collectLatest { notes ->
                voiceNoteAdapter.submitList(notes)
            }
        }
    }

    private fun showDeleteConfirmDialog(voiceNote: VoiceNote, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要删除这条录音吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteVoiceNote(voiceNote)
            }
            .setNegativeButton("取消") { _, _ ->
                // 取消删除，恢复列表项
                voiceNoteAdapter.notifyItemChanged(position)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentDialog = null
        AudioUtils.stopLoopPlay()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMPORT_FILE = 1001
    }
} 