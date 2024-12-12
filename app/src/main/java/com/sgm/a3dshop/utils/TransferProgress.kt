package com.sgm.a3dshop.utils

interface TransferProgress {
    fun onProgress(progress: Int, total: Int)
    fun onComplete(success: Boolean, message: String = "")
} 