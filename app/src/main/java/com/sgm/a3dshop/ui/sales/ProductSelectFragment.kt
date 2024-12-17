package com.sgm.a3dshop.ui.sales

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
import com.sgm.a3dshop.data.entity.InventoryLog
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.databinding.FragmentProductSelectBinding
import com.sgm.a3dshop.ui.products.ProductAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductSelectFragment : Fragment() {
    private var _binding: FragmentProductSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductSelectViewModel by viewModels {
        ProductSelectViewModelFactory(requireActivity().application)
    }

    private val productAdapter = ProductAdapter(
        onItemClick = { product ->
            lifecycleScope.launch {
                try {
                    // 检查库存
                    val remainingCount = viewModel.getProductRemainingCount(product.id)
                    if (remainingCount <= 0) {
                        Toast.makeText(context, "商品库存不足", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // 创建销售记录
                    val saleRecord = SaleRecord(
                        productId = product.id,
                        name = product.name,
                        salePrice = product.price,
                        imageUrl = product.imageUrl
                    )

                    // 更新库存并记录日志
                    viewModel.createSaleRecordWithInventoryUpdate(
                        saleRecord = saleRecord,
                        beforeCount = remainingCount,
                        afterCount = remainingCount - 1
                    )

                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Toast.makeText(context, "创建销售记录失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onQuantityChanged = { _, _ ->
            // 在销售选择界面，我们不需要处理数量变化
        }
    )

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
                viewModel.setSearchQuery(newText.orEmpty())
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