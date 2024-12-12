package com.sgm.a3dshop.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import com.sgm.a3dshop.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DataTransferManager {
    private const val SERVICE_NAME = "3DShopDataTransfer"
    private const val SERVICE_TYPE = "_3dshop._tcp."
    private const val TAG = "DataTransferManager"
    private const val PORT = 0 // 系统自动分配端口

    private var nsdManager: NsdManager? = null
    private var serverSocket: ServerSocket? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var currentServiceInfo: NsdServiceInfo? = null
    private var isResolving = AtomicBoolean(false)

    private var onDeviceFoundListener: ((String, Int) -> Unit)? = null
    private var onDataReceivedListener: (() -> Unit)? = null
    private var onTransferProgressListener: ((TransferProgress) -> Unit)? = null

    data class TransferProgress(
        val status: TransferStatus,
        val progress: Int = 0,
        val message: String = ""
    )

    enum class TransferStatus {
        CONNECTING,
        CONNECTED,
        TRANSFERRING,
        COMPLETED,
        FAILED
    }

    // 发送端相关方法
    fun startDeviceDiscovery(context: Context) {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains(SERVICE_NAME)) {
                    resolveService(context, serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
            }
        }

        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    fun setOnDeviceFoundListener(listener: (String, Int) -> Unit) {
        onDeviceFoundListener = listener
    }

    fun setOnTransferProgressListener(listener: (TransferProgress) -> Unit) {
        onTransferProgressListener = listener
    }

    suspend fun sendDataToDevice(context: Context, host: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            onTransferProgressListener?.invoke(TransferProgress(TransferStatus.CONNECTING))
            Socket(host, port).use { socket ->
                onTransferProgressListener?.invoke(TransferProgress(TransferStatus.CONNECTED))
                val outputStream = socket.getOutputStream()
                
                // 计算总数据量（用于进度计算）
                val db = AppDatabase.getDatabase(context)
                val products = db.productDao().getAllProductsSync()
                val sales = db.saleRecordDao().getAllSaleRecordsSync()
                val voiceNotes = db.voiceNoteDao().getAllVoiceNotesSync()
                
                var totalBytes = 0L
                var transferredBytes = 0L

                // 计算所有文件的总大小
                products.forEach { product ->
                    product.imageUrl?.let { imagePath ->
                        val file = File(imagePath)
                        if (file.exists()) totalBytes += file.length()
                    }
                }
                voiceNotes.forEach { note ->
                    val file = File(note.filePath)
                    if (file.exists()) totalBytes += file.length()
                }

                onTransferProgressListener?.invoke(
                    TransferProgress(
                        TransferStatus.TRANSFERRING,
                        0,
                        "开始传输数据..."
                    )
                )

                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    val gson = Gson()

                    // 导出数据库数据
                    zip.putNextEntry(ZipEntry("products.json"))
                    zip.write(gson.toJson(products).toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("sales.json"))
                    zip.write(gson.toJson(sales).toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("voice_notes.json"))
                    zip.write(gson.toJson(voiceNotes).toByteArray())
                    zip.closeEntry()

                    // 导出图片文件
                    products.forEach { product ->
                        product.imageUrl?.let { imagePath ->
                            val imageFile = File(imagePath)
                            if (imageFile.exists()) {
                                zip.putNextEntry(ZipEntry("images/${imageFile.name}"))
                                imageFile.inputStream().use { input ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        zip.write(buffer, 0, bytesRead)
                                        transferredBytes += bytesRead
                                        val progress = ((transferredBytes.toFloat() / totalBytes) * 100).toInt()
                                        onTransferProgressListener?.invoke(
                                            TransferProgress(
                                                TransferStatus.TRANSFERRING,
                                                progress,
                                                "传输中..."
                                            )
                                        )
                                    }
                                }
                                zip.closeEntry()
                            }
                        }
                    }

                    // 导出录音文件
                    voiceNotes.forEach { note ->
                        val voiceFile = File(note.filePath)
                        if (voiceFile.exists()) {
                            zip.putNextEntry(ZipEntry("voice_notes/${voiceFile.name}"))
                            voiceFile.inputStream().use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    zip.write(buffer, 0, bytesRead)
                                    transferredBytes += bytesRead
                                    val progress = ((transferredBytes.toFloat() / totalBytes) * 100).toInt()
                                    onTransferProgressListener?.invoke(
                                        TransferProgress(
                                            TransferStatus.TRANSFERRING,
                                            progress,
                                            "传输中..."
                                        )
                                    )
                                }
                            }
                            zip.closeEntry()
                        }
                    }
                }
            }
            onTransferProgressListener?.invoke(TransferProgress(TransferStatus.COMPLETED, 100, "传输完成"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onTransferProgressListener?.invoke(TransferProgress(TransferStatus.FAILED, 0, e.message ?: "传输失败"))
            false
        }
    }

    // 接收端相关方法
    fun startReceiveServer(context: Context) {
        // 创建服务器Socket
        serverSocket = ServerSocket(PORT)
        val localPort = serverSocket?.localPort ?: return

        // 注册服务
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = localPort
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${serviceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: $errorCode")
            }
        }

        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }

        // 启动监听线程
        Thread {
            while (true) {
                try {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingConnection(context, socket)
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }.start()
    }

    fun setOnDataReceivedListener(listener: () -> Unit) {
        onDataReceivedListener = listener
    }

    private fun resolveService(context: Context, serviceInfo: NsdServiceInfo) {
        if (isResolving.get()) {
            Log.d(TAG, "Already resolving a service")
            return
        }

        isResolving.set(true)
        
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
                isResolving.set(false)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: ${serviceInfo.port}")
                currentServiceInfo = serviceInfo
                
                // 获取主机地址
                @Suppress("DEPRECATION")
                val hostAddress = try {
                    serviceInfo.host.hostAddress
                } catch (e: Exception) {
                    null
                }
                
                if (hostAddress != null) {
                    onDeviceFoundListener?.invoke(hostAddress, serviceInfo.port)
                } else {
                    Log.e(TAG, "Failed to get host address")
                }
                isResolving.set(false)
            }
        }

        try {
            @Suppress("DEPRECATION")
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service", e)
            isResolving.set(false)
        }
    }

    private fun handleIncomingConnection(context: Context, socket: Socket) {
        kotlinx.coroutines.runBlocking {
            try {
                onTransferProgressListener?.invoke(TransferProgress(TransferStatus.CONNECTED))
                val inputStream = socket.getInputStream()
                val tempDir = File(context.cacheDir, "transfer_temp").apply {
                    deleteRecursively()
                    mkdirs()
                }

                onTransferProgressListener?.invoke(
                    TransferProgress(
                        TransferStatus.TRANSFERRING,
                        0,
                        "开始接收数据..."
                    )
                )

                // 解压接收到的数据
                var totalBytesRead = 0L
                ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                    var entry: ZipEntry?
                    while (zip.nextEntry.also { entry = it } != null) {
                        val file = File(tempDir, entry!!.name)
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (zip.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                // 这里我们无法知道总大小，所以只显示已接收的数据量
                                onTransferProgressListener?.invoke(
                                    TransferProgress(
                                        TransferStatus.TRANSFERRING,
                                        50, // 使用模糊进度
                                        "已接收 ${formatFileSize(totalBytesRead)}"
                                    )
                                )
                            }
                        }
                    }
                }

                // 导入数据
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    val gson = Gson()

                    // 导入商品数据
                    File(tempDir, "products.json").let { file ->
                        if (file.exists()) {
                            val products = gson.fromJson(
                                file.readText(),
                                Array<com.sgm.a3dshop.data.entity.Product>::class.java
                            ).toList()
                            db.productDao().insertProducts(products)
                        }
                    }

                    // 导入销售记录
                    File(tempDir, "sales.json").let { file ->
                        if (file.exists()) {
                            val sales = gson.fromJson(
                                file.readText(),
                                Array<com.sgm.a3dshop.data.entity.SaleRecord>::class.java
                            ).toList()
                            db.saleRecordDao().insertSaleRecords(sales)
                        }
                    }

                    // 导入录音记录
                    File(tempDir, "voice_notes.json").let { file ->
                        if (file.exists()) {
                            val voiceNotes = gson.fromJson(
                                file.readText(),
                                Array<com.sgm.a3dshop.data.entity.VoiceNote>::class.java
                            ).toList()
                            db.voiceNoteDao().insertVoiceNotes(voiceNotes)
                        }
                    }
                }

                // 复制图片文件
                val imagesDir = File(tempDir, "images")
                if (imagesDir.exists()) {
                    val targetImagesDir = File(
                        context.getExternalFilesDir(null),
                        "images"
                    ).apply { mkdirs() }
                    imagesDir.listFiles()?.forEach { file ->
                        file.copyTo(File(targetImagesDir, file.name), overwrite = true)
                    }
                }

                // 复制录音文件
                val voiceDir = File(tempDir, "voice_notes")
                if (voiceDir.exists()) {
                    val targetVoiceDir = File(
                        context.getExternalFilesDir(null),
                        "voice_notes"
                    ).apply { mkdirs() }
                    voiceDir.listFiles()?.forEach { file ->
                        file.copyTo(File(targetVoiceDir, file.name), overwrite = true)
                    }
                }

                // 清理临时文件
                tempDir.deleteRecursively()

                onTransferProgressListener?.invoke(TransferProgress(TransferStatus.COMPLETED, 100, "接收完成"))
                onDataReceivedListener?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                onTransferProgressListener?.invoke(TransferProgress(TransferStatus.FAILED, 0, e.message ?: "接收失败"))
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val kb = 1024L
        val mb = kb * 1024L
        val gb = mb * 1024L
        return when {
            size >= gb -> String.format("%.1f GB", size.toFloat() / gb)
            size >= mb -> String.format("%.1f MB", size.toFloat() / mb)
            size >= kb -> String.format("%.1f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }

    fun stopService() {
        try {
            serverSocket?.close()
            registrationListener?.let { listener ->
                nsdManager?.unregisterService(listener)
            }
            discoveryListener?.let { listener ->
                nsdManager?.stopServiceDiscovery(listener)
            }
            isResolving.set(false)
            nsdManager = null
            currentServiceInfo = null
            onDeviceFoundListener = null
            onDataReceivedListener = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 