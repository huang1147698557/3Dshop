package com.sgm.a3dshop.ui.pending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.databinding.ItemPendingProductBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PendingAdapter(
    private val onItemClick: (PendingProduct) -> Unit
) : ListAdapter<PendingProduct, PendingAdapter.ViewHolder>(DIFF_CALLBACK) {

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

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(pendingProduct: PendingProduct) {
            binding.apply {
                tvName.text = pendingProduct.name
                tvTime.text = dateFormat.format(pendingProduct.createdAt)
                tvNote.text = pendingProduct.note ?: ""

                // 清除之前的图片
                Glide.with(ivPhoto).clear(ivPhoto)
                
                if (pendingProduct.imageUrl != null && pendingProduct.imageUrl.isNotEmpty()) {
                    val imageFile = File(pendingProduct.imageUrl)
                    if (imageFile.exists()) {
                        Glide.with(ivPhoto)
                            .load(imageFile)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_error)
                            .into(ivPhoto)
                    } else {
                        ivPhoto.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_image_placeholder)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PendingProduct>() {
            override fun areItemsTheSame(oldItem: PendingProduct, newItem: PendingProduct): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PendingProduct, newItem: PendingProduct): Boolean {
                return oldItem == newItem
            }
        }
    }
} 