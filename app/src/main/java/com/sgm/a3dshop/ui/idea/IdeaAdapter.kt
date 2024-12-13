package com.sgm.a3dshop.ui.idea

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.IdeaRecord
import com.sgm.a3dshop.databinding.ItemIdeaRecordBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IdeaAdapter(
    private val onItemClick: (IdeaRecord) -> Unit
) : ListAdapter<IdeaRecord, IdeaAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIdeaRecordBinding.inflate(
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
        private val binding: ItemIdeaRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(ideaRecord: IdeaRecord) {
            binding.apply {
                tvName.text = ideaRecord.name
                tvTime.text = dateFormat.format(ideaRecord.createdAt)
                tvNote.text = ideaRecord.note ?: ""

                ideaRecord.imageUrl?.let { imageUrl ->
                    Glide.with(ivPhoto)
                        .load(File(imageUrl))
                        .placeholder(R.drawable.placeholder_image)
                        .fitCenter()
                        .into(ivPhoto)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IdeaRecord>() {
            override fun areItemsTheSame(oldItem: IdeaRecord, newItem: IdeaRecord): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: IdeaRecord, newItem: IdeaRecord): Boolean {
                return oldItem == newItem
            }
        }
    }
} 