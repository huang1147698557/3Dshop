package com.sgm.a3dshop.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sgm.a3dshop.data.entity.VoiceNote
import com.sgm.a3dshop.databinding.ItemVoiceNoteBinding
import com.sgm.a3dshop.utils.AudioUtils
import java.text.SimpleDateFormat
import java.util.*

class VoiceNoteAdapter(
    private val onPlayClick: (VoiceNote) -> Unit,
    private val onPauseClick: (VoiceNote) -> Unit,
    private val onStopClick: (VoiceNote) -> Unit
) : ListAdapter<VoiceNote, VoiceNoteAdapter.VoiceNoteViewHolder>(VoiceNoteDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceNoteViewHolder {
        val binding = ItemVoiceNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VoiceNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoiceNoteViewHolder, position: Int) {
        val voiceNote = getItem(position)
        holder.bind(voiceNote)
    }

    inner class VoiceNoteViewHolder(
        private val binding: ItemVoiceNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(voiceNote: VoiceNote) {
            binding.apply {
                tvTitle.text = "语音备忘 ${dateFormat.format(voiceNote.createdAt)}"
                
                val isCurrentPlaying = AudioUtils.isPlaying() && 
                    AudioUtils.getCurrentPlayingPath() == voiceNote.filePath

                btnPlay.text = when {
                    isCurrentPlaying && !AudioUtils.isPaused() -> "暂停"
                    isCurrentPlaying && AudioUtils.isPaused() -> "继续"
                    else -> "播放"
                }

                btnLoop.text = if (voiceNote.isLoopEnabled) "停止循环" else "循环播放"
                btnLoop.isEnabled = !isCurrentPlaying || voiceNote.isLoopEnabled

                btnPlay.setOnClickListener {
                    if (isCurrentPlaying) {
                        onPauseClick(voiceNote)
                    } else {
                        onPlayClick(voiceNote)
                    }
                }

                btnLoop.setOnClickListener {
                    if (voiceNote.isLoopEnabled) {
                        onStopClick(voiceNote)
                    } else {
                        onPlayClick(voiceNote.copy(isLoopEnabled = true))
                    }
                }
            }
        }
    }

    class VoiceNoteDiffCallback : DiffUtil.ItemCallback<VoiceNote>() {
        override fun areItemsTheSame(oldItem: VoiceNote, newItem: VoiceNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VoiceNote, newItem: VoiceNote): Boolean {
            return oldItem == newItem
        }
    }
} 