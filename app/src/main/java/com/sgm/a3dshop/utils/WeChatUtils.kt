package com.sgm.a3dshop.utils

import android.content.Context
import android.graphics.Bitmap
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXImageObject
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXTextObject
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.io.ByteArrayOutputStream

object WeChatUtils {
    private const val APP_ID = "your_app_id" // 替换为你的微信AppID
    private const val THUMB_SIZE = 150 // 缩略图大小
    var api: IWXAPI? = null

    fun init(context: Context) {
        api = WXAPIFactory.createWXAPI(context, APP_ID, true)
        api?.registerApp(APP_ID)
    }

    fun isWeChatInstalled(): Boolean {
        return api?.isWXAppInstalled ?: false
    }

    fun shareText(text: String, scene: Int = SendMessageToWX.Req.WXSceneSession): Boolean {
        if (api == null || !isWeChatInstalled()) return false

        // 创建文本对象
        val textObj = WXTextObject()
        textObj.text = text

        // 创建多媒体消息
        val msg = WXMediaMessage()
        msg.mediaObject = textObj
        msg.description = text

        // 创建发送请求
        val req = SendMessageToWX.Req()
        req.transaction = buildTransaction("text")
        req.message = msg
        req.scene = scene

        // 发送请求
        return api?.sendReq(req) ?: false
    }

    fun shareImage(bitmap: Bitmap, title: String, description: String, scene: Int = SendMessageToWX.Req.WXSceneSession): Boolean {
        if (api == null || !isWeChatInstalled()) return false

        try {
            // 压缩图片
            val compressedBitmap = compressBitmap(bitmap)
            
            // 创建图片对象
            val imgObj = WXImageObject(compressedBitmap)

            // 创建多媒体消息
            val msg = WXMediaMessage()
            msg.mediaObject = imgObj
            msg.title = title
            msg.description = description

            // 设置缩略图
            val thumbBitmap = createThumbBitmap(bitmap)
            msg.thumbData = bitmapToByteArray(thumbBitmap)
            thumbBitmap.recycle()

            // 创建发送请求
            val req = SendMessageToWX.Req()
            req.transaction = buildTransaction("img")
            req.message = msg
            req.scene = scene

            // 发送请求
            return api?.sendReq(req) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 1920 // 微信图片最大尺寸
        var width = bitmap.width
        var height = bitmap.height

        if (width > maxSize || height > maxSize) {
            val scale = if (width > height) {
                maxSize.toFloat() / width
            } else {
                maxSize.toFloat() / height
            }
            width = (width * scale).toInt()
            height = (height * scale).toInt()
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        return bitmap
    }

    private fun createThumbBitmap(bitmap: Bitmap): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        val scale = THUMB_SIZE.toFloat() / if (width > height) width else height
        width = (width * scale).toInt()
        height = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        return output.toByteArray()
    }

    private fun buildTransaction(type: String): String {
        return "${type}${System.currentTimeMillis()}"
    }

    fun destroy() {
        api?.unregisterApp()
        api = null
    }
} 