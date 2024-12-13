package com.sgm.a3dshop.ui.idea

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.IdeaHistory
import com.sgm.a3dshop.databinding.ItemIdeaRecordBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IdeaHistoryAdapter : ListAdapter<IdeaHistory, IdeaHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val binding = ItemIdeaRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: position=$position")
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemIdeaRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(ideaHistory: IdeaHistory) {
            Log.d(TAG, "绑定数据: id=${ideaHistory.id}, name=${ideaHistory.name}")
            binding.apply {
                tvName.text = ideaHistory.name
                tvTime.text = "创建时间: ${dateFormat.format(ideaHistory.createdAt)}\n删除时间: ${dateFormat.format(ideaHistory.deletedAt)}"
                tvNote.text = ideaHistory.note ?: ""

                ideaHistory.imageUrl?.let { imageUrl ->
                    val imageFile = File(imageUrl)
                    if (imageFile.exists()) {
                        Log.d(TAG, "加载图片: $imageUrl")
                        Glide.with(ivPhoto)
                            .load(imageFile)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .centerCrop()
                            .into(ivPhoto)
                    } else {
                        Log.d(TAG, "图片文件不存在: $imageUrl")
                        Glide.with(ivPhoto)
                            .load(R.drawable.error_image)
                            .centerCrop()
                            .into(ivPhoto)
                    }
                } ?: run {
                    Log.d(TAG, "无图片，使用占位图")
                    Glide.with(ivPhoto)
                        .load(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(ivPhoto)
                }
            }
        }
    }

    companion object {
        private const val TAG = "IdeaHistoryAdapter"
        
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<IdeaHistory>() {
            override fun areItemsTheSame(oldItem: IdeaHistory, newItem: IdeaHistory): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: IdeaHistory, newItem: IdeaHistory): Boolean {
                return oldItem == newItem
            }
        }
    }
} 