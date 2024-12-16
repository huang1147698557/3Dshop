package com.sgm.a3dshop.ui.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.PendingHistory
import com.sgm.a3dshop.databinding.ItemPendingHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class PendingHistoryAdapter(
    private val onRestore: (PendingHistory) -> Unit,
    private val onDelete: (PendingHistory) -> Unit
) : ListAdapter<PendingHistory, PendingHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingHistoryBinding.inflate(
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
        private val binding: ItemPendingHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: PendingHistory) {
            binding.apply {
                tvName.text = history.name
                tvQuantity.text = "价格：${history.salePrice}元"
                tvPrintTime.visibility = ViewGroup.GONE
                tvDeletedAt.text = "删除时间：${formatDate(history.deletedAt)}"
                
                // 加载图片
                if (!history.imageUrl.isNullOrEmpty()) {
                    Glide.with(root.context)
                        .load(history.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .into(ivImage)
                } else {
                    ivImage.setImageResource(R.drawable.ic_image_placeholder)
                }

                // 恢复按钮点击事件
                btnRestore.setOnClickListener {
                    onRestore(history)
                }

                // 永久删除按钮点击事件
                btnDelete.setOnClickListener {
                    onDelete(history)
                }
            }
        }

        private fun formatDate(date: Date): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PendingHistory>() {
        override fun areItemsTheSame(oldItem: PendingHistory, newItem: PendingHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PendingHistory, newItem: PendingHistory): Boolean {
            return oldItem == newItem
        }
    }
} 