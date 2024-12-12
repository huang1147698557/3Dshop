package com.sgm.a3dshop.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
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
import com.sgm.a3dshop.utils.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent
import android.app.Activity
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.sgm.a3dshop.R
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
        val items = arrayOf("本地导出", "网络发送")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择导出方式")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> exportToLocal()
                    1 -> startSending()
                }
            }
            .show()
    }

    private fun exportToLocal() {
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
        val items = arrayOf("本地导入", "网络接收")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择导入方式")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> importFromLocal()
                    1 -> startReceiving()
                }
            }
            .show()
    }

    private fun importFromLocal() {
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
                        
                        // 刷新���据
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

    private fun startSending() {
        val progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("发送数据")
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            .setCancelable(true)  // 允许取消
            .create()

        lifecycleScope.launch {
            dataTransferManager.sendDataOverNetwork(
                progress = object : TransferProgress {
                    override fun onProgress(progress: Int, total: Int) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
                            progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = 
                                "${progress}%"
                        }
                    }

                    override fun onComplete(success: Boolean, message: String) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (message) {
                                "准备数据..." -> {
                                    progressDialog.show()
                                    progressDialog.setTitle("准备数据")
                                }
                                "正在寻找接收方..." -> {
                                    progressDialog.setTitle("正在寻找接收方")
                                }
                                "正在连接..." -> {
                                    progressDialog.setTitle("正在连接")
                                }
                                "连接成功，开始发送..." -> {
                                    progressDialog.setCancelable(false)  // 开始传输后不可取消
                                    progressDialog.setTitle("发送数据")
                                }
                                else -> {
                                    progressDialog.dismiss()
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun startReceiving() {
        // 创建等待连接对话框
        val waitDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("等待连接")
            .setMessage("正在建立连接...")
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // 创建接收进度对话框
        val progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("接收数据")
            .setView(layoutInflater.inflate(R.layout.dialog_progress, null))
            .setCancelable(false)
            .create()

        lifecycleScope.launch {
            waitDialog.show()
            dataTransferManager.receiveDataOverNetwork(
                progress = object : TransferProgress {
                    override fun onProgress(progress: Int, total: Int) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
                            progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = 
                                "${progress}%"
                        }
                    }

                    override fun onComplete(success: Boolean, message: String) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (message) {
                                "等待连接..." -> {
                                    // 已经显示等待对话框，不需要操作
                                }
                                "连接成功，开始接收数据..." -> {
                                    waitDialog.dismiss()
                                    progressDialog.show()
                                }
                                else -> {
                                    waitDialog.dismiss()
                                    progressDialog.dismiss()
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        viewModel.refreshData()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
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