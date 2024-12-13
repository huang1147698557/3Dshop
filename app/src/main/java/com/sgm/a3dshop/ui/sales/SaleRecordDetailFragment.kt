package com.sgm.a3dshop.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
            // 设置视图的初始状态
        }
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