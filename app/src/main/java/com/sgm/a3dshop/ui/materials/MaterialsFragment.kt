package com.sgm.a3dshop.ui.materials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentMaterialsBinding
import kotlinx.coroutines.launch

class MaterialsFragment : Fragment() {
    private var _binding: FragmentMaterialsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MaterialsViewModel by viewModels()
    private lateinit var adapter: MaterialAdapter
    private var sortMenuItem: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaterialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupToolbar()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_materials)
            sortMenuItem = menu.findItem(R.id.action_sort)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_sort -> {
                        viewModel.toggleSortByRemaining()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MaterialAdapter(
            onItemClick = { material ->
                findNavController().navigate(
                    R.id.action_materials_to_detail,
                    Bundle().apply {
                        putLong("materialId", material.id)
                    }
                )
            },
            onQuantityChange = { material, newQuantity ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.updateMaterialQuantity(material, newQuantity)
                }
            },
            onRemainingChange = { material, newPercentage ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.updateMaterialRemaining(material, newPercentage)
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MaterialsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_materials_to_add)
        }
    }

    private fun observeViewModel() {
        viewModel.materials.observe(viewLifecycleOwner) { materials ->
            adapter.submitList(materials)
            binding.emptyView.visibility = if (materials.isEmpty()) View.VISIBLE else View.GONE
            
            // 计算耗材总数量（所有耗材数量之和）
            val totalQuantity = materials.sumOf { it.quantity }
            // 耗材种类数量就是 materials 的大小
            val materialTypes = materials.size
            
            // 更新标题栏显示耗材种类数和总数量
            binding.toolbar.title = "耗材管理 ($materialTypes/$totalQuantity)"
        }

        viewModel.sortByRemaining.observe(viewLifecycleOwner) { isSortingByRemaining ->
            sortMenuItem?.title = if (isSortingByRemaining) {
                "按剩余量排序 ↓"
            } else {
                "按更新时间排序 ↓"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        sortMenuItem = null
    }
} 