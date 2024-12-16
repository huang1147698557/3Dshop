package com.sgm.a3dshop.ui.products

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.FragmentProductsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { exportCsv(it) }
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
        setupFragmentResultListener()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
    }

    private fun setupViews() {
        binding.apply {
            recyclerProducts.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = productAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy > 0) {
                            fabAdd.hide()
                            if ((recyclerView.layoutManager as LinearLayoutManager)
                                    .findFirstVisibleItemPosition() > 5) {
                                fabScrollTop.show()
                            }
                        } else {
                            fabAdd.show()
                            if ((recyclerView.layoutManager as LinearLayoutManager)
                                    .findFirstVisibleItemPosition() <= 5) {
                                fabScrollTop.hide()
                            }
                        }
                    }
                })
            }

            fabAdd.setOnClickListener {
                findNavController().navigate(R.id.action_products_to_add)
            }

            fabScrollTop.setOnClickListener {
                recyclerProducts.smoothScrollToPosition(0)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                productAdapter.submitList(products)
                binding.emptyView.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener("product_key") { _, bundle ->
            bundle.getParcelable<Product>("product")?.let { product ->
                viewModel.insertProduct(product)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_products, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_sort -> {
                viewModel.toggleSort()
                true
            }
            R.id.menu_import -> {
                launchFilePicker()
                true
            }
            R.id.menu_export -> {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                createDocument.launch("products_$timestamp.csv")
                true
            }
            else -> false
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

    private fun exportCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val products = viewModel.getAllProducts()
                CsvUtils.exportProductsToCsv(requireContext(), uri, products)
                Toast.makeText(context, "导出成功：${products.size}条数据", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 