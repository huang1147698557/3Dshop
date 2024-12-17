package com.sgm.a3dshop.ui.products

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sgm.a3dshop.BuildConfig
import com.sgm.a3dshop.R
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.databinding.FragmentProductDetailBinding
import com.sgm.a3dshop.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailFragment : Fragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private val viewModel: ProductDetailViewModel by viewModels {
        ProductDetailViewModelFactory(requireActivity().application, args.productId)
    }
    private var currentPhotoPath: String? = null

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
                val compressedPath = ImageUtils.compressImage(requireContext(), Uri.fromFile(File(path)), ImageUtils.DIR_PRODUCTS)
                if (compressedPath != null) {
                    viewModel.updateImage(compressedPath)
                } else {
                    Toast.makeText(requireContext(), "保存图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val compressedPath = ImageUtils.compressImage(requireContext(), uri, ImageUtils.DIR_PRODUCTS)
                if (compressedPath != null) {
                    viewModel.updateImage(compressedPath)
                } else {
                    Toast.makeText(requireContext(), "保存图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeProduct()
        setupToolbar()
        setupFragmentResultListener()
        setupImageClick()
    }

    private fun setupImageClick() {
        binding.productImage.setOnClickListener {
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
        pickImage.launch(intent)
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

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener("product_updated", viewLifecycleOwner) { _, bundle ->
            bundle.getParcelable<Product>("product")?.let { updatedProduct ->
                displayProduct(updatedProduct)
            }
        }
    }

    private fun parseDescription(description: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        description.lines().forEach { line ->
            when {
                line.startsWith("后处理成本:") -> result["postProcessingCost"] = line.substringAfter(":").trim().replace("¥", "")
                line.startsWith("数量:") -> result["quantity"] = line.substringAfter(":").trim()
                line.startsWith("单个耗时:") -> result["singleTime"] = line.substringAfter(":").trim().replace("小时", "")
                line.startsWith("利润:") -> result["profit"] = line.substringAfter(":").trim().replace("¥", "")
            }
        }
        return result
    }

    private fun observeProduct() {
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let { displayProduct(it) }
        }
    }

    private fun displayProduct(product: Product) {
        // 显示图片
        if (!product.imageUrl.isNullOrEmpty()) {
            val imageSource = if (product.imageUrl.startsWith("http")) {
                product.imageUrl  // 网络图片URL
            } else {
                File(product.imageUrl)  // 本地文件路径
            }
            
            Glide.with(this)
                .load(imageSource)
                .into(binding.productImage)
        }

        // 从描述中解析额外数据
        val extraData = parseDescription(product.description ?: "")

        // 显示基本信息
        binding.apply {
            tvName.text = product.name
            tvPrice.text = String.format("售价: ¥%.2f", product.price)
            tvWeight.text = String.format("重量: %.1fg", product.weight)
            val printTimeHours = product.printTime / 60.0
            tvPrintTime.text = String.format("打印时间: %.2f小时", printTimeHours)
            tvLaborCost.text = String.format("人工费: ¥%.2f", product.laborCost)
            tvPlateCount.text = String.format("盘数: %d", product.plateCount)
            tvMaterialUnitPrice.text = String.format("耗材单价: ¥%.2f/kg", product.materialUnitPrice)
            tvPostProcessingCost.text = String.format("后处理物料费: ¥%.2f", product.postProcessingCost)
            tvQuantity.text = String.format("数量: %d", product.quantity)
            tvSingleTime.text = String.format("单个耗时: %.2f小时", product.printTime / 60.0 / product.quantity)

            // 显示计算结果
            val unitCost = product.calculateUnitCost()
            val expectedPrice = product.calculateExpectedPrice()
            val profit = product.calculateProfit()
            tvUnitCost.text = String.format("单个成本: ¥%.2f", unitCost)
            tvExpectedPrice.text = String.format("预计售价: ¥%.2f", expectedPrice)
            tvProfit.text = String.format("利润: ¥%.2f", profit)

            // 显示备注
            tvNote.text = product.description?.substringAfter("备注: ") ?: "无"
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "商品详情"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            inflateMenu(R.menu.menu_product_detail)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        findNavController().navigate(
                            ProductDetailFragmentDirections.actionProductDetailFragmentToProductEditFragment(args.productId)
                        )
                        true
                    }
                    else -> false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 