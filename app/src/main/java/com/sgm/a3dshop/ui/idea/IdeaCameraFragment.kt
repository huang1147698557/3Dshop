package com.sgm.a3dshop.ui.idea

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.sgm.a3dshop.data.entity.IdeaRecord
import com.sgm.a3dshop.databinding.FragmentIdeaCameraBinding
import com.sgm.a3dshop.utils.ImageUtils
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdeaCameraFragment : Fragment() {
    private var _binding: FragmentIdeaCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentPhotoPath: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val compressedPath = ImageUtils.compressImage(requireContext(), uri, ImageUtils.DIR_IDEAS)
                compressedPath?.let { path ->
                    currentPhotoPath = path
                    showPhotoAndForm(path)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdeaCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCamera()
        setupButtons()
        setupForm()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setupButtons() {
        binding.apply {
            fabCamera.setOnClickListener { takePhoto() }
            fabGallery.setOnClickListener { openGallery() }
            btnSave.setOnClickListener { saveRecord() }
        }
    }

    private fun setupForm() {
        // 直接显示表单
        binding.formContainer.visibility = View.VISIBLE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = ImageUtils.createImageFile(requireContext(), ImageUtils.DIR_IDEAS)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val compressedPath = ImageUtils.compressImage(requireContext(), savedUri, ImageUtils.DIR_IDEAS)
                    if (compressedPath != null) {
                        currentPhotoPath = compressedPath
                        showPhotoAndForm(compressedPath)
                    } else {
                        Toast.makeText(requireContext(), "保存图片失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "拍照失败", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showPhotoAndForm(photoPath: String) {
        binding.apply {
            // 显示照片
            viewFinder.visibility = View.GONE
            ivPhoto.visibility = View.VISIBLE
            Glide.with(this@IdeaCameraFragment)
                .load(File(photoPath))
                .into(ivPhoto)
        }
    }

    private fun saveRecord() {
        // 验证输入
        val name = binding.etName.text?.toString()
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.tilName.error = "请输入创意名称"
            return
        }

        if (currentPhotoPath == null) {
            Toast.makeText(requireContext(), "请先拍照或选择图片", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建创意记录
        val ideaRecord = IdeaRecord(
            name = name,
            imageUrl = currentPhotoPath,
            note = note,
            createdAt = Date()
        )

        // 使用 setFragmentResult 传递数据
        setFragmentResult("idea_record_key", bundleOf("idea_record" to ideaRecord))
        findNavController().navigateUp()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "需要相机权限才能使用此功能",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "IdeaCameraFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
} 