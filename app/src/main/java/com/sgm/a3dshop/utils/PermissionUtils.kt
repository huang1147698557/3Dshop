package com.sgm.a3dshop.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {
    const val CAMERA_PERMISSION_REQUEST = 100
    const val STORAGE_PERMISSION_REQUEST = 101
    const val AUDIO_PERMISSION_REQUEST = 102

    private val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    private val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val AUDIO_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowCameraPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )
    }

    fun showCameraPermissionDialog(
        context: Context,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("需要相机权限")
            .setMessage("为了拍摄产品照片，应用需要访问相机。请在设置中授予相机权限。")
            .setPositiveButton("去设置") { _, _ ->
                onPositive()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                onNegative()
            }
            .setCancelable(false)
            .show()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    fun requestCameraPermission(
        fragment: Fragment,
        permissionLauncher: ActivityResultLauncher<String>,
        onShowRationale: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        when {
            hasCameraPermission(fragment.requireContext()) -> {
                // 已经有权限，可以直接使用相机
                fragment.requireActivity().runOnUiThread {
                    onPermissionDenied()
                }
            }
            shouldShowCameraPermissionRationale(fragment.requireActivity()) -> {
                // 需要显示权限解释
                onShowRationale()
            }
            else -> {
                // 直接请求权限
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return AUDIO_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission(fragment: Fragment) {
        fragment.requestPermissions(STORAGE_PERMISSIONS, STORAGE_PERMISSION_REQUEST)
    }

    fun requestAudioPermission(fragment: Fragment) {
        fragment.requestPermissions(AUDIO_PERMISSIONS, AUDIO_PERMISSION_REQUEST)
    }
} 