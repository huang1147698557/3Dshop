package com.sgm.a3dshop.ui.products

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.sgm.a3dshop.databinding.FragmentProductDetailBinding
import java.io.File

class ProductDetailFragment : Fragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private val viewModel: ProductDetailViewModel by viewModels {
        ProductDetailViewModelFactory(requireActivity().application, args.productId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeProduct()
    }

    private fun parseDescription(description: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        description.lines().forEach { line ->
            when {
                line.startsWith("后处理成本:") -> result["postProcessingCost"] = line.substringAfter(":").trim().replace("¥", "")
                line.startsWith("数量:") -> result["quantity"] = line.substringAfter(":").trim()
                line.startsWith("单个耗时:") -> result["singleTime"] = line.substringAfter(":").trim().replace("小时", "")
                line.startsWith("利润:") -> result["profit"] = line.substringAfter(":").trim().replace("¥", "")
            }
        }
        return result
    }

    private fun observeProduct() {
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let {
                // 显示图片
                if (!it.imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(File(it.imageUrl))
                        .into(binding.productImage)
                }

                // 从描述中解析额外数据
                val extraData = parseDescription(it.description ?: "")

                // 显示基本信息
                binding.tvName.text = it.name
                binding.tvPrice.text = String.format("售价: ¥%.2f", it.price)
                binding.tvWeight.text = String.format("重量: %.1fg", it.weight)
                val printTimeHours = it.printTime / 60.0
                Log.d("TimeConversion", "数据库中时间: ${it.printTime} 分钟")
                Log.d("TimeConversion", "转换后显示: $printTimeHours 小时")
                binding.tvPrintTime.text = String.format("打印时间: %.2f小时", printTimeHours)
                binding.tvLaborCost.text = String.format("人工费: ¥%.2f", it.laborCost)
                binding.tvPlateCount.text = String.format("盘数: %d", it.plateCount)
                binding.tvMaterialUnitPrice.text = String.format("耗材单价: ¥%.2f/kg", it.materialUnitPrice)
                binding.tvProfitRate.text = String.format("利润率: %.1f%%", it.profitRate * 100)

                // 显示额外信息
                binding.tvPostProcessingCost.text = String.format("后处理物料费: ¥%.2f", it.postProcessingCost)
                binding.tvQuantity.text = String.format("数量: %d", it.quantity)
                binding.tvSingleTime.text = String.format("单个耗时: %.2f小时", it.printTime / 60.0 / it.quantity)
                binding.tvProfit.text = String.format("利润: ¥%s", extraData["profit"] ?: "0.00")

                // 显示计算结果
                val unitCost = it.calculateUnitCost()
                val expectedPrice = it.calculateExpectedPrice()
                binding.tvUnitCost.text = String.format("单个成本: ¥%.2f", unitCost)
                binding.tvExpectedPrice.text = String.format("预计售价: ¥%.2f", expectedPrice)

                // 显示备注
                binding.tvNote.text = it.description?.substringAfter("备注: ") ?: "无"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 