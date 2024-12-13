package com.sgm.a3dshop.ui.idea

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgm.a3dshop.databinding.FragmentIdeaHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IdeaHistoryFragment : Fragment() {
    private var _binding: FragmentIdeaHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IdeaHistoryViewModel by viewModels {
        IdeaHistoryViewModelFactory(requireActivity().application)
    }

    private val adapter = IdeaHistoryAdapter().apply {
        registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                Log.d(TAG, "适配器数据发生变化")
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                Log.d(TAG, "适配器数据局部更新: start=$positionStart, count=$itemCount")
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                Log.d(TAG, "适配器数据插入: start=$positionStart, count=$itemCount")
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentIdeaHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupToolbar()
        setupRecyclerView()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setTitle("创意历史")
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener {
                activity?.onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "设置RecyclerView")
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@IdeaHistoryFragment.adapter
            Log.d(TAG, "RecyclerView设置完成: adapter=${this.adapter}, layoutManager=${this.layoutManager}")
        }
    }

    private fun observeData() {
        Log.d(TAG, "开始观察历史记录数据")
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.historyItems.collectLatest { histories ->
                Log.d(TAG, "收到历史记录数据: ${histories.size} 条记录")
                histories.forEachIndexed { index, history ->
                    Log.d(TAG, "第${index + 1}条历史记录: id=${history.id}, name=${history.name}")
                }
                adapter.submitList(histories) {
                    Log.d(TAG, "适配器提交数据完成，当前列表大小: ${adapter.currentList.size}")
                }
                binding.tvEmpty.visibility = if (histories.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    companion object {
        private const val TAG = "IdeaHistoryFragment"
    }
} 