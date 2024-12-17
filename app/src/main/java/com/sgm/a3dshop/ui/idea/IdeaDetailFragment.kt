package com.sgm.a3dshop.ui.idea

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentIdeaDetailBinding
import com.sgm.a3dshop.utils.ImageUtils
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
    private var currentPhotoPath: String? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val uri = Uri.fromFile(File(path))
                uri?.let {
                    val compressedPath = ImageUtils.compressImage(requireContext(), it, ImageUtils.DIR_IDEAS)
                    compressedPath?.let { finalPath ->
                        viewModel.updateImage(finalPath)
                    }
                }
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val compressedPath = ImageUtils.compressImage(requireContext(), uri, ImageUtils.DIR_IDEAS)
                compressedPath?.let { path ->
                    viewModel.updateImage(path)
                }
            }
        }
    }

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
        setupToolbar()
        setupViews()
        observeData()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.title = "创意详情"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViews() {
        binding.apply {
            fabChangeImage.setOnClickListener {
                showImagePickerDialog()
            }

            fabSave.setOnClickListener {
                saveIdeaRecord()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("拍照", "从相册选择")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择图片来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val photoFile = ImageUtils.createImageFile(requireContext(), ImageUtils.DIR_IDEAS)
        currentPhotoPath = photoFile.absolutePath

        try {
            val authority = "${requireContext().packageName}.provider"
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                authority,
                photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            takePicture.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "创建照片文件失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun saveIdeaRecord() {
        val name = binding.etName.text?.toString()
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.tilName.error = "请输入创意名称"
            return
        }

        viewModel.updateIdeaRecord(name, note)
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun observeData() {
        viewModel.ideaRecord.observe(viewLifecycleOwner) { ideaRecord ->
            ideaRecord?.let {
                binding.apply {
                    etName.setText(it.name)
                    tvTime.text = dateFormat.format(it.createdAt)
                    etNote.setText(it.note ?: "")
                    
                    // 加载创意图片
                    it.imageUrl?.let { imageUrl ->
                        val imageSource = if (imageUrl.startsWith("/")) {
                            File(imageUrl)
                        } else {
                            imageUrl
                        }
                        
                        Glide.with(ivPhoto)
                            .load(imageSource)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_error)
                            .centerCrop()
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