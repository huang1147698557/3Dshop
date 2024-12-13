package com.sgm.a3dshop.ui.pending

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
import com.sgm.a3dshop.databinding.FragmentPendingDetailBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PendingDetailFragment : Fragment() {
    private var _binding: FragmentPendingDetailBinding? = null
    private val binding get() = _binding!!

    private val args: PendingDetailFragmentArgs by navArgs()
    private val viewModel: PendingDetailViewModel by viewModels {
        PendingDetailViewModelFactory(requireActivity().application, args.pendingProductId)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingDetailBinding.inflate(inflater, container, false)
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
                savePendingProduct()
            }
        }
    }

    private fun savePendingProduct() {
        val name = binding.etName.text?.toString()
        val priceStr = binding.etPrice.text?.toString()?.replace("¥", "")
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etName.error = "请输入商品名称"
            return
        }

        if (priceStr.isNullOrBlank()) {
            binding.etPrice.error = "请输入价格"
            return
        }

        val price = try {
            priceStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etPrice.error = "请输入有效的价格"
            return
        }

        viewModel.pendingProduct.value?.let { currentProduct ->
            val updatedProduct = currentProduct.copy(
                name = name,
                salePrice = price,
                note = note
            )
            viewModel.updatePendingProduct(updatedProduct)
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun observeData() {
        viewModel.pendingProduct.observe(viewLifecycleOwner) { pendingProduct ->
            pendingProduct?.let {
                binding.apply {
                    etName.setText(it.name)
                    etPrice.setText(String.format("¥%.2f", it.salePrice))
                    tvTime.text = dateFormat.format(it.createdAt)
                    etNote.setText(it.note ?: "")

                    it.imageUrl?.let { imageUrl ->
                        Glide.with(this@PendingDetailFragment)
                            .load(File(imageUrl))
                            .into(ivPhoto)
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