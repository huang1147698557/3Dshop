package com.sgm.a3dshop.ui.sales

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.databinding.FragmentSalesBinding
import com.sgm.a3dshop.databinding.DialogDirectSaleBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class SalesFragment : Fragment(), MenuProvider {
    private var _binding: FragmentSalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesViewModel by viewModels {
        SalesViewModelFactory(requireActivity().application)
    }

    private lateinit var adapter: DailySalesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesBinding.inflate(inflater, container, false)
        setupAdapter()
        return binding.root
    }

    private fun setupAdapter() {
        adapter = DailySalesAdapter(
            onItemClick = { recordId ->
                val action = SalesFragmentDirections.actionSalesToSaleDetail(recordId)
                findNavController().navigate(action)
            },
            onDeleteClick = { saleRecord ->
                viewModel.deleteSaleRecordWithInventoryUpdate(saleRecord)
                Snackbar.make(
                    binding.root,
                    "已删除 ${saleRecord.name}",
                    Snackbar.LENGTH_LONG
                ).setAction("撤销") {
                    viewModel.insertSaleRecord(saleRecord)
                }.show()
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViews()
        observeData()
        setupFragmentResultListener()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener("sale_record_key") { _, bundle ->
            bundle.getParcelable("sale_record", SaleRecord::class.java)?.let { saleRecord ->
                viewModel.insertSaleRecord(saleRecord)
            }
        }
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "销售记录"
    }

    private fun setupViews() {
        binding.apply {
            recyclerSales.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@SalesFragment.adapter
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

    private fun showDirectSaleRecordDialog() {
        val dialogBinding = DialogDirectSaleBinding.inflate(layoutInflater)
        
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

                val price = if (!priceStr.isNullOrBlank()) {
                    try {
                        priceStr.toDouble()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "价格格式无效", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                } else {
                    Toast.makeText(context, "请输入售价", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val saleRecord = SaleRecord(
                    name = name,
                    salePrice = price,
                    note = note,
                    createdAt = Date()
                )

                viewModel.insertSaleRecord(saleRecord)
                Snackbar.make(
                    binding.root,
                    "已添加 ${saleRecord.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailySales.collectLatest { dailySales ->
                adapter.submitList(dailySales)
                val totalRecords = dailySales.sumOf { it.records.size }
                binding.toolbar.title = "销售记录 ($totalRecords)"
            }
        }

        // 观察总销售额
        viewModel.totalSales.observe(viewLifecycleOwner) { total ->
            val formattedTotal = NumberFormat.getCurrencyInstance(Locale.CHINA).format(total)
            binding.tvTotalSales.text = formattedTotal
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        // 不再需要排序菜单，因为已经按日期固定排序
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showEditDialog(saleRecord: SaleRecord) {
        val editText = EditText(requireContext()).apply {
            setText(saleRecord.note)
            hint = "添加备注"
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            maxLines = 5
        }

        AlertDialog.Builder(requireContext())
            .setTitle("编辑备注")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val updatedSaleRecord = saleRecord.copy(
                    note = editText.text.toString().trim()
                )
                viewModel.updateSaleRecord(updatedSaleRecord)
            }
            .setNegativeButton("取消", null)
            .show()
    }
} 
