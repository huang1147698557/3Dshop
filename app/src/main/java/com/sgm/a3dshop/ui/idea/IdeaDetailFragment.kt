package com.sgm.a3dshop.ui.idea

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.sgm.a3dshop.databinding.FragmentIdeaDetailBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IdeaDetailFragment : Fragment() {
    private var _binding: FragmentIdeaDetailBinding? = null
    private val binding get() = _binding!!

    private val args: IdeaDetailFragmentArgs by navArgs()
    private val viewModel: IdeaDetailViewModel by viewModels {
        IdeaDetailViewModelFactory(requireActivity().application, args.ideaId)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdeaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.apply {
            btnSave.setOnClickListener {
                saveIdeaRecord()
            }
        }
    }

    private fun saveIdeaRecord() {
        val name = binding.etName.text?.toString()
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etName.error = "请输入创意名称"
            return
        }

        viewModel.ideaRecord.value?.let { currentRecord ->
            val updatedRecord = currentRecord.copy(
                name = name,
                note = note
            )
            viewModel.updateIdeaRecord(updatedRecord)
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun observeData() {
        viewModel.ideaRecord.observe(viewLifecycleOwner) { ideaRecord ->
            ideaRecord?.let {
                binding.apply {
                    etName.setText(it.name)
                    tvTime.text = dateFormat.format(it.createdAt)
                    etNote.setText(it.note ?: "")

                    it.imageUrl?.let { imageUrl ->
                        Glide.with(this@IdeaDetailFragment)
                            .load(File(imageUrl))
                            .into(ivPhoto)
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