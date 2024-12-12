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
) : ListAdapter<VoiceNote, VoiceNoteAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVoiceNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemVoiceNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(voiceNote: VoiceNote) {
            binding.apply {
                tvTitle.text = voiceNote.title
                tvNote.text = voiceNote.note
                tvCreateTime.text = dateFormat.format(Date(voiceNote.createTime))
                tvDuration.text = formatDuration(voiceNote.duration)

                // 更新播放按钮状态
                val isPlaying = AudioUtils.isPlaying() || AudioUtils.isLoopPlaying()
                btnPlay.text = if (isPlaying) "暂停" else "播放"
                btnPlay.setOnClickListener {
                    if (isPlaying) {
                        onPauseClick(voiceNote)
                    } else {
                        onPlayClick(voiceNote)
                    }
                }

                // 停止按钮
                btnStop.setOnClickListener {
                    onStopClick(voiceNote)
                }

                // 循环播放开关
                switchLoop.isChecked = voiceNote.isLoopEnabled
                switchLoop.setOnCheckedChangeListener { _, isChecked ->
                    voiceNote.isLoopEnabled = isChecked
                    if (!isChecked && AudioUtils.isLoopPlaying()) {
                        onStopClick(voiceNote)
                    }
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d", minutes, remainingSeconds)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<VoiceNote>() {
        override fun areItemsTheSame(oldItem: VoiceNote, newItem: VoiceNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VoiceNote, newItem: VoiceNote): Boolean {
            return oldItem == newItem
        }
    }
} 