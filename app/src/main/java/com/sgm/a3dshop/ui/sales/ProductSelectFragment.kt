package com.sgm.a3dshop.ui.sales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val productAdapter = ProductAdapter { product ->
        // 创建销售记录并返回
        val saleRecord = SaleRecord(
            productId = product.id,
            name = product.name,
            salePrice = product.price,
            imageUrl = product.imageUrl
        )
        viewModel.insertSaleRecord(saleRecord)
        findNavController().navigateUp()
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