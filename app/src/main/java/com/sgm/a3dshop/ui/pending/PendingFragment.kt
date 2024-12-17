package com.sgm.a3dshop.ui.pending

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.databinding.DialogDirectPendingBinding
import com.sgm.a3dshop.databinding.FragmentPendingBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingFragment : Fragment() {
    private var _binding: FragmentPendingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingViewModel by viewModels {
        PendingViewModelFactory(requireActivity().application)
    }

    private val adapter = PendingAdapter { pendingProduct ->
        val bundle = Bundle().apply {
            putLong("pendingProductId", pendingProduct.id.toLong())
        }
        findNavController().navigate(R.id.pendingDetailFragment, bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupViews()
        setupSwipeToDelete()
        setupFragmentResultListener()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "待打印"
            inflateMenu(R.menu.menu_pending)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_history -> {
                        findNavController().navigate(R.id.pendingHistoryFragment)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerPending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PendingFragment.adapter
        }
    }

    private fun setupViews() {
        binding.apply {
            fabAdd.setOnClickListener {
                showAddOptionsDialog()
            }
        }
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
                val pendingProduct = adapter.currentList[position]
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条记录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.deletePendingProduct(pendingProduct)
                        Snackbar.make(
                            binding.root,
                            "已删除 ${pendingProduct.name}",
                            Snackbar.LENGTH_LONG
                        ).setAction("撤销") {
                            viewModel.insertPendingProduct(pendingProduct)
                        }.show()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        // 恢复列表项
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerPending)
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("从商品选择", "拍照记录", "直接记录")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加待打商品")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.productSelectForPendingFragment)
                    1 -> findNavController().navigate(R.id.pendingCameraFragment)
                    2 -> showDirectPendingDialog()
                }
            }
            .show()
    }

    private fun showDirectPendingDialog() {
        val dialogBinding = DialogDirectPendingBinding.inflate(layoutInflater)
        
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
                    0.0
                }

                val pendingProduct = PendingProduct(
                    name = name,
                    salePrice = price,
                    note = note
                )

                viewModel.insertPendingProduct(pendingProduct)
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener("pending_record_key") { _: String, bundle: Bundle ->
            bundle.getParcelable<PendingProduct>("pending_record")?.let { pendingProduct ->
                viewModel.insertPendingProduct(pendingProduct)
                Snackbar.make(
                    binding.root,
                    "已添加 ${pendingProduct.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingProducts.collectLatest { products ->
                adapter.submitList(products)
                binding.tvEmpty?.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                binding.toolbar.title = "待打印 (${products.size})"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 