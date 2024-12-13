package com.sgm.a3dshop.ui.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.databinding.FragmentProductSelectBinding
import com.sgm.a3dshop.ui.products.ProductAdapter
import com.sgm.a3dshop.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProductSelectForPendingFragment : Fragment() {
    private var _binding: FragmentProductSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingViewModel by viewModels {
        PendingViewModelFactory(requireActivity().application)
    }

    private val productAdapter = ProductAdapter { product ->
        lifecycleScope.launch {
            try {
                // 复制商品图片到待打商品的存储位置
                val newImagePath = if (product.imageUrl != null) {
                    if (product.imageUrl.startsWith("http")) {
                        // 处理网络图片
                        withContext(Dispatchers.IO) {
                            try {
                                val bitmap = Glide.with(requireContext())
                                    .asBitmap()
                                    .load(product.imageUrl)
                                    .submit()
                                    .get()

                                val outputFile = ImageUtils.createImageFile(requireContext())
                                FileOutputStream(outputFile).use { out ->
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                                }
                                outputFile.absolutePath
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } else {
                        // 处理本地图片
                        val sourceFile = File(product.imageUrl)
                        if (sourceFile.exists()) {
                            val destFile = ImageUtils.createImageFile(requireContext())
                            sourceFile.copyTo(destFile, overwrite = true)
                            destFile.absolutePath
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }

                // 创建待打商品记录并返回
                val pendingProduct = PendingProduct(
                    productId = product.id,
                    name = product.name,
                    salePrice = product.price,
                    imageUrl = newImagePath
                )
                viewModel.insertPendingProduct(pendingProduct)
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(context, "添加商品失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.recyclerProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredProducts.collectLatest { products ->
                productAdapter.submitList(products)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 