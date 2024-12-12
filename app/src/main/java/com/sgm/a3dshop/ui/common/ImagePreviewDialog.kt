package com.sgm.a3dshop.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.DialogImagePreviewBinding
import java.io.File

class ImagePreviewDialog(
    context: Context,
    private val imagePath: String
) : Dialog(context) {

    private lateinit var binding: DialogImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置对话框宽度为屏幕宽度的90%
        window?.apply {
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // 加载图片
        Glide.with(context)
            .load(File(imagePath))
            .placeholder(R.drawable.ic_product_placeholder)
            .error(R.drawable.ic_product_placeholder)
            .into(binding.photoView)

        // 点击图片外部区域关闭对话框
        binding.root.setOnClickListener {
            dismiss()
        }
    }
} 