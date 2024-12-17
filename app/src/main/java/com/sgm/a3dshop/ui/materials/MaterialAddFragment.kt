package com.sgm.a3dshop.ui.materials

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sgm.a3dshop.BuildConfig
import com.sgm.a3dshop.data.entity.Material
import com.sgm.a3dshop.databinding.FragmentMaterialAddBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MaterialAddFragment : Fragment() {
    private var _binding: FragmentMaterialAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MaterialsViewModel by viewModels()
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                selectedImageUri = Uri.fromFile(File(path))
                loadImage(selectedImageUri)
            }
        }
    }

    private val selectPictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImage(selectedImageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaterialAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.btnSave.setOnClickListener {
            saveMaterial()
        }

        binding.btnSelectImage.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择图片来源")
            .setItems(arrayOf("拍照", "从相册选择")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndStart()
                    1 -> selectImageFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val photoFile = createImageFile()
        photoFile.also { file ->
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.provider",
                file
            )
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectPictureLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun loadImage(uri: Uri?) {
        uri?.let {
            Glide.with(this)
                .load(it)
                .into(binding.imageView)
        }
    }

    private fun saveMaterial() {
        val name = binding.nameInput.text.toString()
        val color = binding.colorInput.text.toString()
        val priceText = binding.priceInput.text.toString()
        val quantityText = binding.quantityInput.text.toString()
        val remainingText = binding.remainingInput.text.toString()

        if (name.isBlank()) {
            binding.nameLayout.error = "请输入耗材名称"
            return
        }

        if (priceText.isBlank()) {
            binding.priceLayout.error = "请输入价格"
            return
        }

        if (quantityText.isBlank()) {
            binding.quantityLayout.error = "请输入数量"
            return
        }

        if (remainingText.isBlank()) {
            binding.remainingLayout.error = "请输入剩余百分比"
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.priceLayout.error = "请输入有效的价格"
            return
        }

        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity < 0) {
            binding.quantityLayout.error = "请输入有效的数量"
            return
        }

        val remaining = remainingText.toIntOrNull()
        if (remaining == null || remaining < 0 || remaining > 100) {
            binding.remainingLayout.error = "请输入0-100之间的百分比"
            return
        }

        val now = Date()
        val material = Material(
            id = 0, // Room会自动生成ID
            name = name,
            color = color.takeIf { it.isNotBlank() },
            price = price,
            quantity = quantity,
            remainingPercentage = remaining,
            imageUrl = selectedImageUri?.toString(),
            createdAt = now,
            updatedAt = now
        )

        viewModel.addMaterial(material)
        Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 