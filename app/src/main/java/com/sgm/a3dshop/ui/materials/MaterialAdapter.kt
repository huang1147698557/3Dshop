package com.sgm.a3dshop.ui.materials

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Material
import com.sgm.a3dshop.databinding.ItemMaterialBinding
import java.text.NumberFormat
import java.util.*

class MaterialAdapter(
    private val onItemClick: (Material) -> Unit,
    private val onQuantityChange: (Material, Int) -> Unit,
    private val onRemainingChange: (Material, Int) -> Unit
) : ListAdapter<Material, MaterialAdapter.ViewHolder>(MaterialDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMaterialBinding.inflate(
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
        private val binding: ItemMaterialBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.quantityController.btnMinus.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val material = getItem(position)
                    if (material.quantity > 0) {
                        onQuantityChange(material, material.quantity - 1)
                    }
                }
            }

            binding.quantityController.btnPlus.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val material = getItem(position)
                    onQuantityChange(material, material.quantity + 1)
                }
            }

            binding.seekBarRemaining.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            binding.tvRemainingPercentage.text = "$progress%"
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    seekBar?.let {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            onRemainingChange(getItem(position), it.progress)
                        }
                    }
                }
            })
        }

        fun bind(material: Material) {
            binding.apply {
                tvName.text = material.name
                tvColor.text = material.color?.let { "颜色：$it" } ?: "颜色：未设置"
                tvPrice.text = "价格：${NumberFormat.getCurrencyInstance(Locale.CHINA).format(material.price)}"
                
                quantityController.etQuantity.setText(material.quantity.toString())
                quantityController.btnMinus.isEnabled = material.quantity > 0
                quantityController.btnPlus.isEnabled = true

                seekBarRemaining.progress = material.remainingPercentage
                tvRemainingPercentage.text = "${material.remainingPercentage}%"

                Glide.with(ivMaterial)
                    .load(material.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .centerCrop()
                    .into(ivMaterial)
            }
        }
    }
}

class MaterialDiffCallback : DiffUtil.ItemCallback<Material>() {
    override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem == newItem
    }
} 