package com.sgm.a3dshop.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sgm.a3dshop.databinding.FragmentProfileBinding
import com.sgm.a3dshop.ui.common.VoiceRecordDialog
import com.sgm.a3dshop.utils.AudioUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireActivity().application)
    }

    private lateinit var voiceNoteAdapter: VoiceNoteAdapter

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
        setupViews()
        observeData()
    }

    private fun setupViews() {
        voiceNoteAdapter = VoiceNoteAdapter(
            onPlayClick = { voiceNote ->
                if (voiceNote.isLoopEnabled) {
                    AudioUtils.startLoopPlay(voiceNote.filePath, voiceNote.intervalSeconds) { isPlaying ->
                        voiceNoteAdapter.notifyItemChanged(voiceNoteAdapter.currentList.indexOf(voiceNote))
                    }
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

            view?.findViewById<FloatingActionButton>(com.sgm.a3dshop.R.id.fab_add)?.setOnClickListener {
                showVoiceRecordDialog()
            }
        }
    }

    private fun showVoiceRecordDialog() {
        VoiceRecordDialog(requireContext()) { filePath ->
            viewModel.saveVoiceNote(filePath)
        }.show()
    }

    private fun exportData() {
        // Implement data export functionality here
    }

    private fun importData() {
        // Implement data import functionality here
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
        AudioUtils.stopLoopPlay()
        _binding = null
    }
} 