package com.sgm.a3dshop.ui.idea

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.IdeaRecord
import com.sgm.a3dshop.databinding.DialogDirectIdeaBinding
import com.sgm.a3dshop.databinding.FragmentIdeaBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class IdeaFragment : Fragment() {
    private var _binding: FragmentIdeaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IdeaViewModel by viewModels {
        IdeaViewModelFactory(requireActivity().application)
    }

    private val adapter = IdeaAdapter { ideaRecord ->
        val action = IdeaFragmentDirections
            .actionNavigationIdeaToIdeaDetail(ideaRecord.id.toLong())
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdeaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
        setupNavBackStackEntry()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_idea)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_history -> {
                        findNavController().navigate(
                            IdeaFragmentDirections.actionNavigationIdeaToIdeaHistory()
                        )
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerIdea.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@IdeaFragment.adapter
        }

        // 添加左右滑动删除功能
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val ideaRecord = adapter.currentList[position]
                
                // 显示确认对话框
                AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条创意记录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.deleteIdeaRecord(ideaRecord)
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        // 取消删除，恢复列表项
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        // 对话框被取消时也恢复列表项
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerIdea)
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("拍照记录", "直接创建")
        AlertDialog.Builder(requireContext())
            .setTitle("选择创建方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(
                        IdeaFragmentDirections.actionNavigationIdeaToIdeaCamera()
                    )
                    1 -> showDirectCreateDialog()
                }
            }
            .show()
    }

    private fun showDirectCreateDialog() {
        val dialogBinding = DialogDirectIdeaBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle("创建创意")
            .setView(dialogBinding.root)
            .setPositiveButton("确定") { _, _ ->
                val name = dialogBinding.etName.text.toString()
                val note = dialogBinding.etNote.text.toString()
                if (name.isNotBlank()) {
                    val ideaRecord = IdeaRecord(
                        name = name,
                        note = if (note.isBlank()) null else note,
                        createdAt = Date()
                    )
                    viewModel.insertIdeaRecord(ideaRecord)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ideaRecords.collectLatest { records ->
                    adapter.submitList(records)
                    binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupNavBackStackEntry() {
        val navController = findNavController()
        val navBackStackEntry = navController.getBackStackEntry(R.id.navigation_idea)
        
        navBackStackEntry.savedStateHandle.apply {
            getLiveData<String>("idea_name").observe(viewLifecycleOwner) { name ->
                if (name != null) {
                    val note = get<String>("idea_note")
                    val imagePath = get<String>("idea_image_path")
                    
                    if (imagePath != null) {
                        val ideaRecord = IdeaRecord(
                            name = name,
                            note = note,
                            imageUrl = imagePath,
                            createdAt = Date()
                        )
                        viewModel.insertIdeaRecord(ideaRecord)
                        
                        // 清除已处理的数据
                        remove<String>("idea_name")
                        remove<String>("idea_note")
                        remove<String>("idea_image_path")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 