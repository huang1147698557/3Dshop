package com.sgm.a3dshop.ui.idea

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
import com.sgm.a3dshop.data.entity.IdeaRecord
import com.sgm.a3dshop.databinding.DialogDirectIdeaBinding
import com.sgm.a3dshop.databinding.FragmentIdeaBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class IdeaFragment : Fragment() {
    private var _binding: FragmentIdeaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IdeaViewModel by viewModels {
        IdeaViewModelFactory(requireActivity().application)
    }

    private val adapter = IdeaAdapter { ideaRecord ->
        val action = IdeaFragmentDirections.actionNavigationIdeaToIdeaDetail(ideaRecord.id.toLong())
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
        setupViews()
        setupSwipeToDelete()
        setupFragmentResultListener()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "创意"
            inflateMenu(R.menu.menu_idea)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_history -> {
                        findNavController().navigate(R.id.action_navigation_idea_to_idea_history)
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
                val ideaRecord = adapter.currentList[position]
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条创意记录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        viewModel.deleteIdeaRecord(ideaRecord)
                        Snackbar.make(
                            binding.root,
                            "已删除 ${ideaRecord.name}",
                            Snackbar.LENGTH_LONG
                        ).setAction("撤销") {
                            viewModel.insertIdeaRecord(ideaRecord)
                        }.show()
                    }
                    .setNegativeButton("取消") { _, _ ->
                        // 恢复列表项
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerIdea)
    }

    private fun setupViews() {
        binding.fabAdd.setOnClickListener {
            showAddOptionsDialog()
        }
    }

    private fun showAddOptionsDialog() {
        val options = arrayOf("拍照记录", "直接创建")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择创建方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(R.id.action_navigation_idea_to_idea_camera)
                    1 -> showDirectCreateDialog()
                }
            }
            .show()
    }

    private fun showDirectCreateDialog() {
        val dialogBinding = DialogDirectIdeaBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
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

    private fun setupFragmentResultListener() {
        setFragmentResultListener("idea_record_key") { _: String, bundle: Bundle ->
            bundle.getParcelable<IdeaRecord>("idea_record")?.let { ideaRecord ->
                viewModel.insertIdeaRecord(ideaRecord)
                Snackbar.make(
                    binding.root,
                    "已添加 ${ideaRecord.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ideaRecords.collectLatest { records ->
                adapter.submitList(records)
                binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                binding.toolbar.title = "创意记录 (${records.size})"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 