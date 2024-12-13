package com.sgm.a3dshop.ui.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.data.entity.PendingHistory
import com.sgm.a3dshop.databinding.ItemPendingProductBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PendingHistoryAdapter : ListAdapter<PendingHistory, PendingHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingProductBinding.inflate(
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
        private val binding: ItemPendingProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(pendingHistory: PendingHistory) {
            binding.apply {
                tvName.text = pendingHistory.name
                tvPrice.text = String.format("¥%.2f", pendingHistory.salePrice)
                tvTime.text = dateFormat.format(pendingHistory.deletedAt)
                tvNote.text = pendingHistory.note ?: ""

                pendingHistory.imageUrl?.let { imageUrl ->
                    Glide.with(ivPhoto)
                        .load(File(imageUrl))
                        .into(ivPhoto)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PendingHistory>() {
            override fun areItemsTheSame(oldItem: PendingHistory, newItem: PendingHistory): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PendingHistory, newItem: PendingHistory): Boolean {
                return oldItem == newItem
            }
        }
    }
} 