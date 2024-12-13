package com.sgm.a3dshop.ui.pending

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.PendingProduct
import com.sgm.a3dshop.databinding.FragmentPendingCameraBinding
import com.sgm.a3dshop.utils.CameraUtils
import com.sgm.a3dshop.utils.ImageUtils
import com.sgm.a3dshop.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PendingCameraFragment : Fragment() {
    private var _binding: FragmentPendingCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PendingViewModel by viewModels {
        PendingViewModelFactory(requireActivity().application)
    }

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentPhotoPath: String? = null
    private var isPreviewMode = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPendingCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermission()
        setupViews()
    }

    private fun checkAndRequestPermission() {
        when {
            PermissionUtils.hasCameraPermission(requireContext()) -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        PermissionUtils.showCameraPermissionDialog(
            requireContext(),
            onPositive = {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onNegative = {
                findNavController().navigateUp()
            }
        )
    }

    private fun setupViews() {
        binding.apply {
            fabCamera.setOnClickListener {
                if (isPreviewMode) {
                    takePhoto()
                } else {
                    switchToPreviewMode()
                }
            }

            fabGallery.setOnClickListener {
                pickImage.launch("image/*")
            }

            btnSave.setOnClickListener {
                savePendingProduct()
            }

            ivPhoto.setOnClickListener {
                switchToPreviewMode()
            }
        }
        updateFabIcon()
    }

    private fun switchToPreviewMode() {
        isPreviewMode = true
        binding.apply {
            previewView.visibility = View.VISIBLE
            ivPhoto.visibility = View.GONE
            formContainer.visibility = View.GONE
            startCamera()
        }
        updateFabIcon()
    }

    private fun switchToPhotoMode() {
        isPreviewMode = false
        binding.apply {
            previewView.visibility = View.GONE
            ivPhoto.visibility = View.VISIBLE
            formContainer.visibility = View.VISIBLE
        }
        updateFabIcon()
    }

    private fun updateFabIcon() {
        binding.fabCamera.setImageResource(
            if (isPreviewMode) {
                android.R.drawable.ic_menu_camera
            } else {
                R.drawable.ic_refresh
            }
        )
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(binding.previewView.display.rotation)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Use case binding failed", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        lifecycleScope.launch {
            try {
                val outputFile = CameraUtils.createImageFile(requireContext(), isIdea = false)
                val savedUri = CameraUtils.takePhoto(imageCapture, outputFile, requireContext())
                val compressedImagePath = ImageUtils.compressImage(requireContext(), savedUri, isIdea = false)

                if (compressedImagePath != null) {
                    // 删除原始图片
                    outputFile.delete()
                    currentPhotoPath = compressedImagePath

                    // 显示压缩后的图片
                    switchToPhotoMode()
                    Glide.with(this@PendingCameraFragment)
                        .load(File(compressedImagePath))
                        .into(binding.ivPhoto)
                } else {
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Photo capture failed", e)
                Toast.makeText(context, "拍照失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val compressedImagePath = ImageUtils.compressImage(requireContext(), uri, isIdea = false)
                if (compressedImagePath != null) {
                    currentPhotoPath = compressedImagePath
                    switchToPhotoMode()
                    Glide.with(this@PendingCameraFragment)
                        .load(File(compressedImagePath))
                        .into(binding.ivPhoto)
                } else {
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image processing failed", e)
                Toast.makeText(context, "图片处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePendingProduct() {
        val name = binding.etName.text?.toString()
        val priceStr = binding.etPrice.text?.toString()
        val note = binding.etNote.text?.toString()

        if (name.isNullOrBlank()) {
            binding.etName.error = "请输入商品名称"
            return
        }

        if (priceStr.isNullOrBlank()) {
            binding.etPrice.error = "请输入价格"
            return
        }

        if (currentPhotoPath == null) {
            Toast.makeText(context, "请先拍照", Toast.LENGTH_SHORT).show()
            return
        }

        val price = try {
            priceStr.toDouble()
        } catch (e: NumberFormatException) {
            binding.etPrice.error = "请输入有效的价格"
            return
        }

        val pendingProduct = PendingProduct(
            name = name,
            salePrice = price,
            imageUrl = currentPhotoPath,
            note = note
        )

        viewModel.insertPendingProduct(pendingProduct)
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        _binding = null
    }

    companion object {
        private const val TAG = "PendingCameraFragment"
    }
} 