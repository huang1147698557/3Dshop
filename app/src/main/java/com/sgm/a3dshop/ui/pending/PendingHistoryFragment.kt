package com.sgm.a3dshop.ui.pending

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentPendingHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PendingHistoryFragment : Fragment() {
    private var _binding: FragmentPendingHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingHistoryViewModel by viewModels {
        PendingHistoryViewModelFactory(requireActivity().application)
    }

    private val adapter = PendingHistoryAdapter()

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
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            inflateMenu(R.menu.menu_pending_history)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_clear -> {
                        viewModel.clearHistory()
                        true
                    }
                    else -> false
                }
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
            viewModel.historyItems.collectLatest { items ->
                adapter.submitList(items)
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 