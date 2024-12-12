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
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.databinding.FragmentSaleRecordDetailBinding
import com.sgm.a3dshop.ui.common.ImagePreviewDialog
import java.text.SimpleDateFormat
import java.util.*

class SaleRecordDetailFragment : Fragment() {
    private var _binding: FragmentSaleRecordDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<SaleRecordDetailFragmentArgs>()
    private val viewModel: SaleRecordDetailViewModel by viewModels {
        SaleRecordDetailViewModelFactory(requireActivity().application, args.saleRecordId)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

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
        observeSaleRecord()
    }

    private fun setupViews() {
        binding.btnSave.setOnClickListener {
            saveSaleRecord()
        }

        binding.ivProduct.setOnClickListener {
            viewModel.saleRecord.value?.imageUrl?.let { imagePath ->
                ImagePreviewDialog(requireContext(), imagePath).show()
            }
        }
    }

    private fun observeSaleRecord() {
        viewModel.saleRecord.observe(viewLifecycleOwner) { saleRecord ->
            saleRecord?.let {
                updateUI(it)
            }
        }
    }

    private fun updateUI(saleRecord: SaleRecord) {
        binding.apply {
            etName.setText(saleRecord.name)
            etPrice.setText(String.format("%.2f", saleRecord.salePrice))
            etNote.setText(saleRecord.note)
            tvSaleTime.text = dateFormat.format(Date(saleRecord.createTime))

            Glide.with(this@SaleRecordDetailFragment)
                .load(saleRecord.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(ivProduct)
        }
    }

    private fun saveSaleRecord() {
        val name = binding.etName.text?.toString()
        val priceStr = binding.etPrice.text?.toString()
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

        viewModel.updateSaleRecord(
            name = name,
            salePrice = price,
            note = note
        )

        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 