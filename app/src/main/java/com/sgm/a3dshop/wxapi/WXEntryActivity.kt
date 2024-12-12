package com.sgm.a3dshop.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

class WXEntryActivity : Activity(), IWXAPIEventHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.sgm.a3dshop.utils.WeChatUtils.api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            com.sgm.a3dshop.utils.WeChatUtils.api?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReq(req: BaseReq?) {
        finish()
    }

    override fun onResp(resp: BaseResp?) {
        val result = when (resp?.errCode) {
            BaseResp.ErrCode.ERR_OK -> "分享成功"
            BaseResp.ErrCode.ERR_USER_CANCEL -> "分享取消"
            BaseResp.ErrCode.ERR_AUTH_DENIED -> "分享被拒绝"
            else -> "分享失败"
        }
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        finish()
    }
} 