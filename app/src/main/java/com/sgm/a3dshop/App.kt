package com.sgm.a3dshop

import android.app.Application
import com.sgm.a3dshop.utils.ImageCacheManager
import com.sgm.a3dshop.utils.WeChatUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化图片缓存管理
        ImageCacheManager.init(this)
        
        // 初始化微信SDK
        WeChatUtils.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        
        // 清理资源
        ImageCacheManager.destroy()
        WeChatUtils.destroy()
    }
} 