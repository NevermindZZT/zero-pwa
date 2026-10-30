package com.letter.zeropwa.ui.container

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.letter.zeropwa.data.model.PwaItem
import java.net.URI

class PwaWebViewClient(
    private val context: Context,
    private val pwa: PwaItem,
    private val onPageStartedListener: (String) -> Unit,
    private val onPageFinishedListener: (String) -> Unit,
    private val onErrorReceivedListener: (String) -> Unit
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            onPageStartedListener(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            onPageFinishedListener(url)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onErrorReceivedListener(error?.description?.toString() ?: "加载失败")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val urlString = uri.toString()
        val scheme = uri.scheme?.lowercase() ?: ""

        // 非 http/https 协议（如 mailto:, tel:, intent: 等）交由系统处理
        if (scheme != "http" && scheme != "https") {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                true
            }
        }

        // 外链处理
        if (pwa.openExternalLinksInBrowser && isExternalUrl(urlString)) {
            return try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }

        return false
    }

    private fun isExternalUrl(targetUrl: String): Boolean {
        // 如果配置了具体的 scope，以 scope 为基准
        val scope = pwa.scope
        if (!scope.isNullOrBlank()) {
            return !targetUrl.startsWith(scope, ignoreCase = true)
        }

        // 默认按 hostname 判断
        return try {
            val targetHost = URI(targetUrl).host ?: return false
            val startHost = URI(pwa.startUrl.ifBlank { pwa.url }).host ?: return false
            !targetHost.equals(startHost, ignoreCase = true) && !targetHost.endsWith(".$startHost", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
