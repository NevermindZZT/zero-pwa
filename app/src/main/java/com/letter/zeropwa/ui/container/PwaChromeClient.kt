package com.letter.zeropwa.ui.container

import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class PwaChromeClient(
    private val onProgressChangedListener: (Int) -> Unit,
    private val onTitleReceivedListener: (String) -> Unit,
    private val onFileChooserListener: (ValueCallback<Array<Uri>>?, FileChooserParams?) -> Boolean
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChangedListener(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            onTitleReceivedListener(title)
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return onFileChooserListener(filePathCallback, fileChooserParams)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // 允许地理位置权限（在原生应用已获权限的前提下）
        callback?.invoke(origin, true, false)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // 支持 WebRTC 摄像头与麦克风等网页授权请求
        request?.grant(request.resources)
    }
}
