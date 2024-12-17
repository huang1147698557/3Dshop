package com.sgm.a3dshop.ui.materials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentMaterialsBinding
import kotlinx.coroutines.launch

class MaterialsFragment : Fragment() {
    private var _binding: FragmentMaterialsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MaterialsViewModel by viewModels()
    private lateinit var adapter: MaterialAdapter

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
        observeViewModel()
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 