package com.sgm.a3dshop.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.databinding.ItemSaleRecordBinding
import java.text.SimpleDateFormat
import java.util.*

class SaleRecordAdapter(
    private val onClick: (SaleRecord) -> Unit
) : ListAdapter<SaleRecord, SaleRecordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleRecordBinding.inflate(
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
        private val binding: ItemSaleRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(getItem(position))
                }
            }
        }

        fun bind(saleRecord: SaleRecord) {
            binding.apply {
                tvName.text = saleRecord.name
                tvPrice.text = String.format("¥%.2f", saleRecord.salePrice)
                tvTime.text = dateFormat.format(Date(saleRecord.createTime))

                Glide.with(ivProduct)
                    .load(saleRecord.imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(ivProduct)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SaleRecord>() {
        override fun areItemsTheSame(oldItem: SaleRecord, newItem: SaleRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SaleRecord, newItem: SaleRecord): Boolean {
            return oldItem == newItem
        }
    }
} 