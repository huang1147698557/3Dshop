package com.sgm.a3dshop.ui.products

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.FragmentProductsBinding
import com.sgm.a3dshop.utils.CsvUtils
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
        setupSwipeToDelete()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = productAdapter.currentList[position]
                    
                    // 恢复item的显示
                    productAdapter.notifyItemChanged(position)
                    
                    // 显示确认对话框
                    AlertDialog.Builder(requireContext())
                        .setTitle("确认删除")
                        .setMessage("确定要删除商品\"${product.name}\"吗？")
                        .setPositiveButton("删除") { _, _ ->
                            // 删除商品
                            viewModel.deleteProduct(product)

                            // 显示撤销选项
                            Snackbar.make(
                                binding.root,
                                "已删除 ${product.name}",
                                Snackbar.LENGTH_LONG
                            ).setAction("撤销") {
                                // 恢复删除的商品
                                viewModel.insertProduct(product)
                            }.show()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                if (dX > 0) { // 右滑
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = iconLeft + deleteIcon.intrinsicWidth
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    val background = ColorDrawable(
                        ContextCompat.getColor(requireContext(), R.color.delete_background)
                    )
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    background.draw(c)
                } else if (dX < 0) { // 左滑
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - deleteIcon.intrinsicWidth
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                    val background = ColorDrawable(
                        ContextCompat.getColor(requireContext(), R.color.delete_background)
                    )
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)
                }

                deleteIcon.draw(c)

                super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY,
                    actionState, isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerProducts)
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