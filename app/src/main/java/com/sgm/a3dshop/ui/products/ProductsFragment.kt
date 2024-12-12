package com.sgm.a3dshop.ui.products

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentProductsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductsFragment : Fragment(), MenuProvider {
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductsViewModel by viewModels {
        ProductsViewModelFactory(requireActivity().application)
    }

    private val productAdapter = ProductAdapter { product ->
        val action = ProductsFragmentDirections.actionProductsToDetail(product.id.toLong())
        findNavController().navigate(action)
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleCsvFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViews()
        observeData()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "商品简介"
    }

    private fun setupViews() {
        binding.apply {
            recyclerProducts.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = productAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0 && fabScrollTop.visibility == View.VISIBLE) {
                            fabScrollTop.hide()
                        } else if (dy < 0 && fabScrollTop.visibility != View.VISIBLE) {
                            fabScrollTop.show()
                        }
                    }
                })
            }

            fabScrollTop.setOnClickListener {
                recyclerProducts.smoothScrollToPosition(0)
            }

            btnImport.setOnClickListener {
                launchFilePicker()
            }
        }
    }

    private fun launchFilePicker() {
        getContent.launch("text/csv")
    }

    private fun handleCsvFile(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val products = CsvUtils.readProductsFromCsv(requireContext(), uri)
                if (products.isNotEmpty()) {
                    viewModel.deleteAllProducts()
                    viewModel.insertProducts(products)
                    Toast.makeText(context, "导入成功：${products.size}条数据", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "未找到有效数据", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                productAdapter.submitList(products)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_sort, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_sort -> {
                viewModel.toggleSort()
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 