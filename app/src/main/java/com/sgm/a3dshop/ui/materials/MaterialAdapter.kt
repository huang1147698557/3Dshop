package com.sgm.a3dshop.ui.materials

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
                        val newQuantity = material.quantity - 1
                        onQuantityChange(material, newQuantity)
                        updateItemAppearance(material.copy(quantity = newQuantity))
                    }
                }
            }

            binding.quantityController.btnPlus.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val material = getItem(position)
                    val newQuantity = material.quantity + 1
                    onQuantityChange(material, newQuantity)
                    updateItemAppearance(material.copy(quantity = newQuantity))
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
                            val material = getItem(position)
                            onRemainingChange(material, it.progress)
                            updateItemAppearance(material.copy(remainingPercentage = it.progress))
                        }
                    }
                }
            })
        }

        fun bind(material: Material) {
            binding.apply {
                tvName.text = material.name
                tvMaterial.text = "材质：${material.material}"
                tvColor.text = material.color?.let { "颜色：$it" } ?: "颜色：未设置"
                tvPrice.text = "价格：${NumberFormat.getCurrencyInstance(Locale.CHINA).format(material.price)}"
                quantityController.etQuantity.setText(material.quantity.toString())
                seekBarRemaining.progress = material.remainingPercentage
                tvRemainingPercentage.text = "${material.remainingPercentage}%"

                Glide.with(ivMaterial)
                    .load(material.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .centerCrop()
                    .into(ivMaterial)

                updateItemAppearance(material)
            }
        }

        private fun updateItemAppearance(material: Material) {
            binding.apply {
                val isQuantityZero = material.quantity == 0
                
                // 设置整体透明度
                root.alpha = if (isQuantityZero) 0.5f else 1.0f
                ivMaterial.alpha = if (isQuantityZero) 0.5f else 1.0f
                
                // 设置文字颜色
                val context = root.context
                tvName.setTextColor(ContextCompat.getColor(context, 
                    if (isQuantityZero) android.R.color.darker_gray else android.R.color.black))
                tvMaterial.setTextColor(ContextCompat.getColor(context,
                    if (isQuantityZero) android.R.color.darker_gray else android.R.color.black))
                tvPrice.setTextColor(ContextCompat.getColor(context, 
                    if (isQuantityZero) android.R.color.darker_gray else android.R.color.holo_red_light))
                tvColor.setTextColor(ContextCompat.getColor(context,
                    if (isQuantityZero) android.R.color.darker_gray else android.R.color.black))
                
                // 更新数量控制器状态
                quantityController.btnMinus.isEnabled = !isQuantityZero
                quantityController.btnMinus.alpha = if (isQuantityZero) 0.5f else 1.0f
                quantityController.etQuantity.isEnabled = !isQuantityZero
                quantityController.etQuantity.alpha = if (isQuantityZero) 0.5f else 1.0f
                quantityController.btnPlus.isEnabled = true
                quantityController.btnPlus.alpha = 1.0f

                // 更新滑动条状态
                seekBarRemaining.isEnabled = !isQuantityZero
                seekBarRemaining.alpha = if (isQuantityZero) 0.5f else 1.0f
                tvRemaining.alpha = if (isQuantityZero) 0.5f else 1.0f
                tvRemainingPercentage.alpha = if (isQuantityZero) 0.5f else 1.0f
            }
        }
    }
}

class MaterialDiffCallback : DiffUtil.ItemCallback<Material>() {
    override fun areItemsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Material, newItem: Material): Boolean {
        return oldItem.name == newItem.name &&
               oldItem.material == newItem.material &&
               oldItem.color == newItem.color &&
               oldItem.price == newItem.price &&
               oldItem.quantity == newItem.quantity &&
               oldItem.remainingPercentage == newItem.remainingPercentage &&
               oldItem.imageUrl == newItem.imageUrl
    }
} 