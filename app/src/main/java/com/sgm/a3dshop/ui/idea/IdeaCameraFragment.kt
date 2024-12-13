package com.sgm.a3dshop.ui.idea

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.sgm.a3dshop.R
import com.sgm.a3dshop.databinding.FragmentIdeaCameraBinding
import com.sgm.a3dshop.utils.CameraUtils
import com.sgm.a3dshop.utils.ImageUtils
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdeaCameraFragment : Fragment() {
    private var _binding: FragmentIdeaCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var photoUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "需要相机权限才能使用此功能", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageResult(uri)
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
        
        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupClickListeners() {
        binding.apply {
            btnTakePhoto.setOnClickListener {
                takePhoto()
            }

            fabGallery.setOnClickListener {
                openGallery()
            }

            btnSave.setOnClickListener {
                saveIdeaRecord()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun handleImageResult(uri: Uri) {
        photoUri = uri
        binding.apply {
            viewFinder.visibility = View.GONE
            ivPhoto.visibility = View.VISIBLE
            Glide.with(this@IdeaCameraFragment)
                .load(uri)
                .into(ivPhoto)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "相机启动失败", e)
                Toast.makeText(context, "相机启动失败", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val photoFile = CameraUtils.createImageFile(requireContext(), isIdea = true)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(requireContext()),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri ?: photoFile.toUri()
                            photoUri = savedUri
                            Log.d(TAG, "照片已保存到: $savedUri")
                            
                            binding.apply {
                                viewFinder.visibility = View.GONE
                                ivPhoto.visibility = View.VISIBLE
                                Glide.with(this@IdeaCameraFragment)
                                    .load(savedUri)
                                    .into(ivPhoto)
                            }
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "拍照失败", exc)
                            Toast.makeText(context, "拍照失败: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "创建图片文件失败", e)
                Toast.makeText(context, "创建图片文件失败", Toast.LENGTH_SHORT).show()
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

        if (photoUri == null) {
            Toast.makeText(context, "请先拍照或选择图片", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            photoUri?.let { uri ->
                val compressedImagePath = ImageUtils.compressImage(requireContext(), uri, isIdea = true)
                if (compressedImagePath != null) {
                    val navController = findNavController()
                    val navBackStackEntry = navController.getBackStackEntry(R.id.navigation_idea)
                    
                    navBackStackEntry.savedStateHandle.apply {
                        set("idea_name", name)
                        set("idea_note", note)
                        set("idea_image_path", compressedImagePath)
                    }
                    
                    navController.navigateUp()
                } else {
                    Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show()
                }
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
    }
} 