package com.sgm.a3dshop.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.sgm.a3dshop.databinding.FragmentSaleRecordDetailBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SaleRecordDetailFragment : Fragment() {
    private var _binding: FragmentSaleRecordDetailBinding? = null
    private val binding get() = _binding!!

    private val args: SaleRecordDetailFragmentArgs by navArgs()
    private val viewModel: SaleRecordDetailViewModel by viewModels {
        SaleRecordDetailViewModelFactory(requireActivity().application, args.saleRecordId)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaleRecordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.apply {
            btnSave.setOnClickListener {
                saveSaleRecord()
            }
        }
    }

    private fun saveSaleRecord() {
        val name = binding.etName.text?.toString()
        val priceStr = binding.etPrice.text?.toString()?.replace("¥", "")
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etName.error = "请输入商品名称"
            return
        }

        if (priceStr.isNullOrBlank()) {
            binding.etPrice.error = "请输入售价"
            return
        }

        val price = try {
            priceStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etPrice.error = "请输入有效的价格"
            return
        }

        viewModel.updateSaleRecord(name, price, note)
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun observeData() {
        viewModel.saleRecord.observe(viewLifecycleOwner) { saleRecord ->
            saleRecord?.let {
                binding.apply {
                    etName.setText(it.name)
                    etPrice.setText(String.format("¥%.2f", it.salePrice))
                    tvSaleTime.text = dateFormat.format(it.createdAt)
                    etNote.setText(it.note ?: "")
                    
                    // 加载商品图片
                    it.imageUrl?.let { imageUrl ->
                        val imageSource = if (imageUrl.startsWith("/")) {
                            // 本地文件路径
                            File(imageUrl)
                        } else {
                            // 网络URL或资源URL
                            imageUrl
                        }
                        
                        Glide.with(ivProduct)
                            .load(imageSource)
                            .centerCrop()
                            .into(ivProduct)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 