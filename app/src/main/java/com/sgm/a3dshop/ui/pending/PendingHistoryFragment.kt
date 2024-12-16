package com.sgm.a3dshop.ui.pending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.PendingHistory
import com.sgm.a3dshop.databinding.FragmentPendingHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingHistoryFragment : Fragment() {
    private var _binding: FragmentPendingHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingHistoryViewModel by viewModels {
        PendingHistoryViewModelFactory(requireActivity().application)
    }

    private val adapter = PendingHistoryAdapter(
        onRestore = { history ->
            showRestoreConfirmationDialog(history)
        },
        onDelete = { history ->
            showDeleteConfirmationDialog(history)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "历史记录"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PendingHistoryFragment.adapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.historyItems.collectLatest { histories ->
                adapter.submitList(histories)
                binding.tvEmpty.visibility = if (histories.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showRestoreConfirmationDialog(history: PendingHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle("恢复确认")
            .setMessage("确定要将\"${history.name}\"恢复到待打印列表吗？")
            .setPositiveButton("恢复") { _, _ ->
                viewModel.restoreItem(history)
                Snackbar.make(binding.root, "已恢复到待打印列表", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(history: PendingHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要永久删除\"${history.name}\"吗？此操作无法撤销。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteItemPermanently(history)
                Snackbar.make(binding.root, "已永久删除", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 