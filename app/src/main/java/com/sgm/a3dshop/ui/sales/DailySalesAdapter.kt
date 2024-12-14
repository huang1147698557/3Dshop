package com.sgm.a3dshop.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.model.DailySales
import com.sgm.a3dshop.databinding.ItemDailySalesBinding
import java.text.SimpleDateFormat
import java.util.*

class DailySalesAdapter(
    private val onItemClick: (Long) -> Unit,
    private val onDeleteClick: (SaleRecord) -> Unit
) : ListAdapter<DailySales, DailySalesAdapter.DailySalesViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

        private lateinit var saleRecordAdapter: SaleRecordAdapter

        init {
            setupRecyclerView()
        }

        private fun setupRecyclerView() {
            saleRecordAdapter = SaleRecordAdapter(onItemClick)

            binding.recordsContainer.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = saleRecordAdapter

                // 设置滑动删除
                val swipeHandler = object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.RIGHT
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean = false

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val position = viewHolder.adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val saleRecord = saleRecordAdapter.currentList[position]
                            MaterialAlertDialogBuilder(context)
                                .setTitle("确认删除")
                                .setMessage("确定要删除这条销售记录吗？")
                                .setPositiveButton("确定") { _, _ ->
                                    onDeleteClick(saleRecord)
                                }
                                .setNegativeButton("取消") { _, _ ->
                                    // 恢复列表项
                                    saleRecordAdapter.notifyItemChanged(position)
                                }
                                .setOnCancelListener {
                                    // 对话框被取消时也恢复列表项
                                    saleRecordAdapter.notifyItemChanged(position)
                                }
                                .show()
                        }
                    }
                }

                ItemTouchHelper(swipeHandler).attachToRecyclerView(this)
            }
        }

        fun bind(dailySales: DailySales) {
            binding.apply {
                // 设置日期和总金额
                tvDate.text = dateFormat.format(dailySales.date)
                tvTotalAmount.text = String.format("总计：¥%.2f", dailySales.totalAmount)

                // 更新销售记录列表
                saleRecordAdapter.submitList(dailySales.records)
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