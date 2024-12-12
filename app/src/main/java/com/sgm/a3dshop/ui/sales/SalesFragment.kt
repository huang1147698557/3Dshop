package com.sgm.a3dshop.ui.sales

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.databinding.DialogDirectSaleRecordBinding
import com.sgm.a3dshop.databinding.FragmentSalesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SalesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesViewModel by viewModels {
        SalesViewModelFactory(requireActivity().application)
    }

    private val saleRecordAdapter = SaleRecordAdapter { saleRecord ->
        val action = SalesFragmentDirections.actionSalesToSaleDetail(saleRecord.id.toLong())
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViews()
        setupSwipeToDelete()
        observeData()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "售出商品"
    }

    private fun setupViews() {
        binding.apply {
            recyclerSales.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = saleRecordAdapter
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
                recyclerSales.smoothScrollToPosition(0)
            }

            fabAdd.setOnClickListener {
                showAddOptionsDialog()
            }
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("从商品选择", "拍照记录", "直接记录")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加销售记录")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.action_sales_to_product_select)
                    1 -> findNavController().navigate(R.id.action_sales_to_camera)
                    2 -> showDirectSaleRecordDialog()
                }
            }
            .show()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val saleRecord = saleRecordAdapter.currentList[position]
                
                // 恢复item的显示
                saleRecordAdapter.notifyItemChanged(position)
                
                // 显示确认对话框
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条销售记录吗？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteSaleRecord(saleRecord)
                        Snackbar.make(
                            binding.root,
                            "已删除销售记录",
                            Snackbar.LENGTH_LONG
                        ).setAction("撤销") {
                            viewModel.insertSaleRecord(saleRecord)
                        }.show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerSales)
    }

    private fun showDirectSaleRecordDialog() {
        val dialogBinding = DialogDirectSaleRecordBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("直接记录")
            .setView(dialogBinding.root)
            .setPositiveButton("保存") { _, _ ->
                val name = dialogBinding.etName.text?.toString()
                val priceStr = dialogBinding.etPrice.text?.toString()
                val note = dialogBinding.etNote.text?.toString()

                if (name.isNullOrBlank()) {
                    Toast.makeText(context, "请输入商品名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (priceStr.isNullOrBlank()) {
                    Toast.makeText(context, "请输入售价", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val price = try {
                    priceStr.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "请输入有效的价格", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val saleRecord = SaleRecord(
                    name = name,
                    salePrice = price,
                    note = note
                )

                viewModel.insertSaleRecord(saleRecord)
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saleRecords.collectLatest { records ->
                saleRecordAdapter.submitList(records)
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
