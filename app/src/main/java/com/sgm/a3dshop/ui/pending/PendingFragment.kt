package com.sgm.a3dshop.ui.pending

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
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.databinding.DialogDirectPendingBinding
import com.sgm.a3dshop.databinding.FragmentPendingBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingFragment : Fragment(), MenuProvider {
    private var _binding: FragmentPendingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingViewModel by viewModels {
        PendingViewModelFactory(requireActivity().application)
    }

    private val pendingAdapter = PendingAdapter { pendingProduct ->
        val action = PendingFragmentDirections.actionPendingToDetail(pendingProduct.id.toLong())
        findNavController().navigate(action)
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
        setupViews()
        setupSwipeToDelete()
        observeData()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "待打商品"
    }

    private fun setupViews() {
        binding.apply {
            recyclerPending.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = pendingAdapter
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
                recyclerPending.smoothScrollToPosition(0)
            }

            fabAdd.setOnClickListener {
                showAddOptionsDialog()
            }
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("从商品选择", "拍照记录", "直接记录")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加待打商品")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.action_pending_to_product_select)
                    1 -> findNavController().navigate(R.id.action_pending_to_camera)
                    2 -> showDirectPendingDialog()
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
                val pendingProduct = pendingAdapter.currentList[position]
                
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
                        pendingAdapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerPending)
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

                if (priceStr.isNullOrBlank()) {
                    Toast.makeText(context, "请输入价格", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val price = try {
                    priceStr.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "请输入有效的价格", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
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

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingProducts.collectLatest { products ->
                pendingAdapter.submitList(products)
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