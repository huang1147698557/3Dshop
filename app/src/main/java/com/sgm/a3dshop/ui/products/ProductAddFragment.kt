package com.sgm.a3dshop.ui.products

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.FragmentProductAddBinding

class ProductAddFragment : Fragment() {
    private var _binding: FragmentProductAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by viewModels {
        ProductsViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTakePhoto.setOnClickListener {
            findNavController().navigate(R.id.action_add_to_camera)
        }

        binding.btnSave.setOnClickListener {
            saveProduct()
        }

        // 监听相机返回的数据
        setFragmentResultListener("product_key") { _, bundle ->
            val product = bundle.getParcelable<Product>("product")
            product?.let {
                Log.d("ProductDB_Debug", "ProductAddFragment接收到产品数据:")
                Log.d("ProductDB_Debug", "- 名称: ${it.name}")
                Log.d("ProductDB_Debug", "- 重量: ${it.weight}g")
                Log.d("ProductDB_Debug", "- 打印时间: ${it.printTime}分钟")
                Log.d("ProductDB_Debug", "- 人工费: ${it.laborCost}元")
                Log.d("ProductDB_Debug", "- 盘数: ${it.plateCount}")
                Log.d("ProductDB_Debug", "- 耗材单价: ${it.materialUnitPrice}元/kg")
                Log.d("ProductDB_Debug", "- 后处理费: ${it.postProcessingCost}元")
                Log.d("ProductDB_Debug", "- 数量: ${it.quantity}")
                Log.d("ProductDB_Debug", "- 描述: ${it.description}")
                viewModel.insertProduct(it)
                findNavController().navigateUp()
            }
        }
    }

    private fun saveProduct() {
        val name = binding.nameInput.text.toString()
        val weight = binding.weightInput.text.toString()
        val printTime = binding.printTimeInput.text.toString()
        val price = binding.priceInput.text.toString()
        val note = binding.noteInput.text.toString()
        val quantity = binding.quantityInput.text.toString()

        if (name.isBlank()) {
            Toast.makeText(context, "请输入商品名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (weight.isBlank()) {
            Toast.makeText(context, "请输入重量", Toast.LENGTH_SHORT).show()
            return
        }

        if (printTime.isBlank()) {
            Toast.makeText(context, "请输入打印时间", Toast.LENGTH_SHORT).show()
            return
        }

        if (price.isBlank()) {
            Toast.makeText(context, "请输入价格", Toast.LENGTH_SHORT).show()
            return
        }

        val weightValue = try {
            weight.toFloat()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的重量", Toast.LENGTH_SHORT).show()
            return
        }

        val printTimeValue = try {
            val hours = printTime.toDouble()
            val minutes = (hours * 60).toInt()
            Log.d("TimeConversion", "输入时间: $hours 小时")
            Log.d("TimeConversion", "转换后时间: $minutes 分钟")
            minutes
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的打印时间", Toast.LENGTH_SHORT).show()
            return
        }

        val priceValue = try {
            price.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的价格", Toast.LENGTH_SHORT).show()
            return
        }

        val materialUnitPrice = binding.etMaterialUnitPrice.text.toString()
        if (materialUnitPrice.isBlank()) {
            Toast.makeText(context, "请输入耗材单价", Toast.LENGTH_SHORT).show()
            return
        }
        val materialUnitPriceValue = try {
            materialUnitPrice.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的耗材单价", Toast.LENGTH_SHORT).show()
            return
        }

        val postProcessingCost = binding.etPostProcessingCost.text.toString()
        if (postProcessingCost.isBlank()) {
            Toast.makeText(context, "请输入后处理物料费", Toast.LENGTH_SHORT).show()
            return
        }
        val postProcessingCostValue = try {
            postProcessingCost.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的后处理物料费", Toast.LENGTH_SHORT).show()
            return
        }

        val laborCost = binding.etLaborCost.text.toString()
        if (laborCost.isBlank()) {
            Toast.makeText(context, "请输入人工费", Toast.LENGTH_SHORT).show()
            return
        }
        val laborCostValue = try {
            laborCost.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的人工费", Toast.LENGTH_SHORT).show()
            return
        }

        val plateCount = binding.etPlateCount.text.toString()
        if (plateCount.isBlank()) {
            Toast.makeText(context, "请输入盘数", Toast.LENGTH_SHORT).show()
            return
        }
        val plateCountValue = try {
            plateCount.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "请输入有效的盘数", Toast.LENGTH_SHORT).show()
            return
        }

        val quantityValue = try {
            quantity.toIntOrNull() ?: 1
        } catch (e: NumberFormatException) {
            1
        }

        val description = buildString {
            append("重量: ${weightValue}g")
            append("\n打印时间: ${printTime}小时")
            append("\n数量: $quantityValue")
            append("\n盘数: $plateCountValue")
            append("\n耗材单价: ¥${materialUnitPriceValue}/kg")
            append("\n后处理物料费: ¥${postProcessingCostValue}")
            append("\n人工费: ¥${laborCostValue}")
            if (note.isNotBlank()) {
                append("\n备注: $note")
            }
        }

        val product = Product(
            name = name,
            description = description,
            price = priceValue,
            weight = weightValue,
            printTime = printTimeValue,
            quantity = quantityValue,
            materialUnitPrice = materialUnitPriceValue,
            postProcessingCost = postProcessingCostValue,
            laborCost = laborCostValue,
            plateCount = plateCountValue
        )

        viewModel.insertProduct(product)
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 