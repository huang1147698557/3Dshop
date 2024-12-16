package com.sgm.a3dshop.ui.products

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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.FragmentProductEditBinding
import com.sgm.a3dshop.utils.ImageUtils
import java.io.File

class ProductEditFragment : Fragment() {
    private var _binding: FragmentProductEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductsViewModel by viewModels {
        ProductsViewModelFactory(requireActivity().application)
    }
    private val args: ProductEditFragmentArgs by navArgs()
    private var currentPhotoPath: String? = null
    private var imageCapture: ImageCapture? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val compressedPath = ImageUtils.compressImage(requireContext(), uri, ImageUtils.DIR_SALES)
                compressedPath?.let { path ->
                    currentPhotoPath = path
                    showPhoto(path)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupImageButtons()
        loadProductData()
        setupSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "编辑商品"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupImageButtons() {
        binding.apply {
            fabCamera.setOnClickListener { takePhoto() }
            fabGallery.setOnClickListener { openGallery() }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun takePhoto() {
        val photoFile = ImageUtils.createImageFile(requireContext(), ImageUtils.DIR_SALES)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val compressedPath = ImageUtils.compressImage(requireContext(), savedUri, ImageUtils.DIR_SALES)
                    if (compressedPath != null) {
                        currentPhotoPath = compressedPath
                        showPhoto(compressedPath)
                    } else {
                        Toast.makeText(requireContext(), "保存图片失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "拍照失败", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showPhoto(photoPath: String) {
        binding.productImage.let { imageView ->
            Glide.with(this)
                .load(File(photoPath))
                .into(imageView)
        }
    }

    private fun loadProductData() {
        viewModel.getProductById(args.productId).observe(viewLifecycleOwner) { product ->
            product?.let {
                binding.apply {
                    // 加载图片
                    if (!it.imageUrl.isNullOrEmpty()) {
                        currentPhotoPath = it.imageUrl
                        showPhoto(it.imageUrl)
                    }
                    
                    etName.setText(it.name)
                    etPrice.setText(String.format("%.2f", it.price))
                    etWeight.setText(String.format("%.1f", it.weight))
                    etPrintTime.setText(String.format("%.2f", it.printTime / 60.0)) // 转换为小时
                    etLaborCost.setText(String.format("%.2f", it.laborCost))
                    etPlateCount.setText(it.plateCount.toString())
                    etMaterialUnitPrice.setText(String.format("%.2f", it.materialUnitPrice))
                    etDescription.setText(it.description)
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val name = binding.etName.text.toString().trim()
            val price = binding.etPrice.text.toString().toDouble()
            val weight = binding.etWeight.text.toString().toFloat()
            val printTimeHours = binding.etPrintTime.text.toString().toDouble()
            val laborCost = binding.etLaborCost.text.toString().toDouble()
            val plateCount = binding.etPlateCount.text.toString().toInt()
            val materialUnitPrice = binding.etMaterialUnitPrice.text.toString().toDouble()
            val description = binding.etDescription.text.toString().trim()

            viewModel.getProductById(args.productId).observe(viewLifecycleOwner) { originalProduct ->
                originalProduct?.let {
                    val updatedProduct = it.copy(
                        name = name,
                        price = price,
                        weight = weight,
                        printTime = (printTimeHours * 60).toInt(), // 转换为分钟
                        laborCost = laborCost,
                        plateCount = plateCount,
                        materialUnitPrice = materialUnitPrice,
                        description = description,
                        imageUrl = currentPhotoPath
                    )
                    viewModel.updateProduct(updatedProduct)
                    
                    // 通知详情页更新数据
                    parentFragmentManager.setFragmentResult(
                        "product_updated",
                        Bundle().apply {
                            putParcelable("product", updatedProduct)
                        }
                    )
                    
                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            if (etName.text.toString().trim().isEmpty()) {
                tilName.error = "请输入商品名称"
                return false
            }

            val fields = listOf(
                Triple(etPrice, tilPrice, "请输入有效的价格"),
                Triple(etWeight, tilWeight, "请输入有效的重量"),
                Triple(etPrintTime, tilPrintTime, "请输入有效的打印时间"),
                Triple(etLaborCost, tilLaborCost, "请输入有效的人工费"),
                Triple(etPlateCount, tilPlateCount, "请输入有效的盘数"),
                Triple(etMaterialUnitPrice, tilMaterialUnitPrice, "请输入有效的耗材单价")
            )

            for ((editText, inputLayout, errorMessage) in fields) {
                val value = editText.text.toString().trim()
                if (value.isEmpty()) {
                    inputLayout.error = errorMessage
                    return false
                }
                try {
                    if (editText == etPlateCount) {
                        value.toInt()
                    } else {
                        value.toDouble()
                    }
                } catch (e: NumberFormatException) {
                    inputLayout.error = errorMessage
                    return false
                }
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 