package com.sgm.a3dshop.ui.products

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.ItemProductBinding

class ProductAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onQuantityChanged: (Product, Int) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    companion object {
        private val tempRect = android.graphics.Rect()
        private var lastTouchX = 0f
        private var lastTouchY = 0f
    }

    // 先声明变量
    private val itemTouchListener = View.OnTouchListener { _, event ->
        lastTouchX = event.x
        lastTouchY = event.y
        false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // 设置触摸监听器
        binding.root.setOnTouchListener(itemTouchListener)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 设置整个卡片的点击事件
            binding.root.setOnClickListener { view ->
                // 检查点击的位置是否在数量控制器区域内
                binding.quantityControl.getHitRect(tempRect)
                val x = lastTouchX.toInt()
                val y = lastTouchY.toInt()
                
                // 如果点击位置不在数量控制器区域内，才触发详情页跳转
                if (!tempRect.contains(x, y)) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(getItem(position))
                    }
                }
            }

            // 设置数量控制按钮的点击事件
            binding.btnDecrease.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = getItem(position)
                    val newCount = (product.remainingCount - 1).coerceAtLeast(0)
                    if (newCount != product.remainingCount) {
                        onQuantityChanged(product, newCount)
                        updateQuantityWithAnimation(binding.tvQuantity, product.remainingCount, newCount)
                        updateItemAppearance(product.copy(remainingCount = newCount))
                    }
                }
            }

            binding.btnIncrease.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = getItem(position)
                    val newCount = product.remainingCount + 1
                    onQuantityChanged(product, newCount)
                    updateQuantityWithAnimation(binding.tvQuantity, product.remainingCount, newCount)
                    updateItemAppearance(product.copy(remainingCount = newCount))
                }
            }

            // 防止数量显示文本被点击触发详情页
            binding.tvQuantity.setOnClickListener { }
        }

        fun bind(product: Product) {
            binding.apply {
                tvName.text = product.name
                tvPrice.text = String.format("¥%.2f", product.price)
                tvQuantity.text = product.remainingCount.toString()

                // 使用Glide加载图片
                Glide.with(ivProduct)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(ivProduct)

                // 更新商品外观
                updateItemAppearance(product)
            }
        }

        private fun updateItemAppearance(product: Product) {
            binding.apply {
                val context = root.context
                val isOutOfStock = product.remainingCount == 0
                
                // 设置透明度
                root.alpha = if (isOutOfStock) 0.5f else 1.0f
                ivProduct.alpha = if (isOutOfStock) 0.5f else 1.0f
                
                // 更新文字颜色
                tvName.setTextColor(ContextCompat.getColor(context, 
                    if (isOutOfStock) android.R.color.darker_gray else android.R.color.black))
                tvPrice.setTextColor(ContextCompat.getColor(context, 
                    if (isOutOfStock) android.R.color.darker_gray else R.color.price_color))
                
                // 更新按钮状态
                btnDecrease.isEnabled = !isOutOfStock
                btnDecrease.alpha = if (isOutOfStock) 0.5f else 1.0f
            }
        }

        private fun updateQuantityWithAnimation(textView: android.widget.TextView, oldValue: Int, newValue: Int) {
            ValueAnimator.ofInt(oldValue, newValue).apply {
                duration = 200
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    textView.text = animator.animatedValue.toString()
                }
                start()
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
} 