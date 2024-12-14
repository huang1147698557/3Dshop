package com.sgm.a3dshop.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.model.DailySales
import com.sgm.a3dshop.databinding.ItemDailySalesBinding
import com.sgm.a3dshop.databinding.ItemSaleRecordBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DailySalesAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<DailySales, DailySalesAdapter.DailySalesViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailySalesViewHolder {
        val binding = ItemDailySalesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DailySalesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailySalesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailySalesViewHolder(
        private val binding: ItemDailySalesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dailySales: DailySales) {
            binding.apply {
                // 设置日期和总金额
                tvDate.text = dateFormat.format(dailySales.date)
                tvTotalAmount.text = String.format("总计：¥%.2f", dailySales.totalAmount)

                // 清除之前的记录视图
                recordsContainer.removeAllViews()

                // 添加每条销售记录
                dailySales.records.forEach { record ->
                    val recordBinding = ItemSaleRecordBinding.inflate(
                        LayoutInflater.from(recordsContainer.context),
                        recordsContainer,
                        true
                    )

                    recordBinding.apply {
                        tvName.text = record.name
                        tvPrice.text = String.format("¥%.2f", record.salePrice)
                        tvTime.text = timeFormat.format(record.createdAt)

                        // 加载商品图片
                        record.imageUrl?.let { imageUrl ->
                            val imageSource = if (imageUrl.startsWith("/")) {
                                File(imageUrl)
                            } else {
                                imageUrl
                            }
                            
                            Glide.with(ivProduct)
                                .load(imageSource)
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image)
                                .centerCrop()
                                .into(ivProduct)
                        }

                        // 设置点击事件
                        root.setOnClickListener {
                            onItemClick(record.id.toLong())
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DailySales>() {
            override fun areItemsTheSame(oldItem: DailySales, newItem: DailySales): Boolean {
                return oldItem.date == newItem.date
            }

            override fun areContentsTheSame(oldItem: DailySales, newItem: DailySales): Boolean {
                return oldItem == newItem
            }
        }
    }
} 