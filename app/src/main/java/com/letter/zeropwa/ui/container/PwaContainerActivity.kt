package com.letter.zeropwa.ui.container

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.letter.zeropwa.data.db.PwaRepository
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.data.parser.PwaManifestParser
import com.letter.zeropwa.shortcut.PwaShortcutManager
import com.letter.zeropwa.ui.theme.ZeroPwaTheme
import kotlinx.coroutines.launch

class PwaContainerActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback == null) return@registerForActivityResult
        val clipData = result.data?.clipData
        val dataString = result.data?.dataString
        val results: Array<Uri>? = when {
            clipData != null -> {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }
            dataString != null -> {
                arrayOf(Uri.parse(dataString))
            }
            else -> null
        }
        fileUploadCallback?.onReceiveValue(results)
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pwaId = extractPwaId(intent)
        if (pwaId <= 0) {
            Toast.makeText(this, "无效的 PWA 标识", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ZeroPwaTheme {
                PwaContainerScreen(
                    pwaId = pwaId,
                    onSetupWebView = { wv -> this.webView = wv },
                    onOpenFileChooser = { callback, params ->
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = callback
                        val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                        }
                        try {
                            fileChooserLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            fileUploadCallback = null
                            false
                        }
                    },
                    onDownloadRequested = { url, userAgent, contentDisposition, mimeType ->
                        handleDownload(url, userAgent, contentDisposition, mimeType)
                    },
                    onApplyTheme = { pwa, iconBitmap ->
                        applyTaskDescriptionAndOrientation(pwa, iconBitmap)
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pwaId = extractPwaId(intent)
        if (pwaId > 0) {
            val repo = PwaRepository.getInstance(applicationContext)
            lifecycleScope.launch {
                repo.markOpened(pwaId)
            }
        }
    }

    private fun extractPwaId(intent: Intent?): Long {
        if (intent == null) return -1L
        val extraId = intent.getLongExtra(PwaShortcutManager.EXTRA_PWA_ID, -1L)
        if (extraId > 0) return extraId

        val uri = intent.data ?: return -1L
        if (uri.scheme == PwaShortcutManager.URI_SCHEME && uri.host == PwaShortcutManager.URI_HOST) {
            val pathSegments = uri.pathSegments
            if (!pathSegments.isNullOrEmpty()) {
                return pathSegments[0].toLongOrNull() ?: -1L
            }
        }
        return -1L
    }

    private fun applyTaskDescriptionAndOrientation(pwa: PwaItem, iconBitmap: Bitmap?) {
        // 设置独立窗口任务卡片的标题、图标与主题色 (Document-centric Tasks)
        val primaryColor = pwa.themeColor ?: AndroidColor.parseColor("#1C1B1F")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val td = ActivityManager.TaskDescription.Builder()
                    .setLabel(pwa.displayName)
                    .setPrimaryColor(primaryColor)
                    .build()
                setTaskDescription(td)
            } else {
                @Suppress("DEPRECATION")
                val td = ActivityManager.TaskDescription(pwa.displayName, iconBitmap, primaryColor)
                @Suppress("DEPRECATION")
                setTaskDescription(td)
            }
        } catch (_: Exception) {}

        // 设置屏幕方向
        when (pwa.orientation) {
            PwaItem.ORIENTATION_PORTRAIT -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            PwaItem.ORIENTATION_LANDSCAPE -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        // 处理全屏模式
        if (pwa.displayMode == PwaItem.DISPLAY_FULLSCREEN) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        try {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("正在下载文件...")
                setTitle(filename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "已开始下载: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "创建下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        webView?.let {
            it.stopLoading()
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PwaContainerScreen(
    pwaId: Long,
    onSetupWebView: (WebView) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit,
    onApplyTheme: (PwaItem, Bitmap?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val repo = remember { PwaRepository.getInstance(context) }

    var pwaItem by remember { mutableStateOf<PwaItem?>(null) }
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var dynamicThemeColor by remember { mutableStateOf<Int?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTitle by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(pwaId) {
        val item = repo.getById(pwaId)
        pwaItem = item
        if (item != null) {
            repo.markOpened(item.id)
            val bmp = repo.loadIconBitmap(item.iconPath)
            iconBitmap = bmp
            dynamicThemeColor = item.themeColor
            onApplyTheme(item, bmp)
        }
    }

    // 网页返回键导航监听
    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onClose()
        }
    }

    val currentPwa = pwaItem
    if (currentPwa == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 计算状态栏颜色：用户手动配置的主题色具备最高优先级，其次使用网页动态提取色，最后回退系统色
    val effectiveColorInt = currentPwa.themeColor ?: dynamicThemeColor ?: MaterialTheme.colorScheme.surface.toArgb()
    val statusBarComposeColor = Color(effectiveColorInt)

    DisposableEffect(effectiveColorInt) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val isLight = ColorUtils.calculateLuminance(effectiveColorInt) > 0.5
            controller.isAppearanceLightStatusBars = isLight
        }
        onDispose {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = currentPwa.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 状态栏沉浸与安全区配置处理：
            // 1. 若关闭了沉浸式（开启安全区保护隔离）且非全屏：插入状态栏高度的保护条，将安全区隔离在 WebView 外
            // 2. 若开启了沉浸式（默认）：网页内容直接穿透状态栏，享受纯正沉浸式视觉
            if (!currentPwa.enableImmersiveStatusBar && currentPwa.displayMode != PwaItem.DISPLAY_FULLSCREEN) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(statusBarComposeColor)
                )
            }

            // 如果是 minimal-ui 模式显示原生轻量操作栏
            if (currentPwa.displayMode == PwaItem.DISPLAY_MINIMAL_UI) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (currentPwa.enableImmersiveStatusBar) Modifier.statusBarsPadding() else Modifier
                        )
                        .background(statusBarComposeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                    Text(
                        text = currentTitle.ifBlank { currentPwa.displayName },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("返回主页") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    webViewRef?.loadUrl(currentPwa.effectiveStartUrl)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("在浏览器中打开") },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { currentPwa.effectiveStartUrl }))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("复制当前链接") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString(currentUrl.ifBlank { currentPwa.effectiveStartUrl }))
                                    Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // 网页加载细线进度指示器
            AnimatedVisibility(
                visible = isLoading && progress < 1f,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = currentPwa.themeColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // 基础 WebSettings 配置
                            settings.apply {
                                javaScriptEnabled = currentPwa.enableJavaScript
                                domStorageEnabled = currentPwa.enableDomStorage
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mediaPlaybackRequiresUserGesture = false
                                setSupportMultipleWindows(false)
                                cacheMode = WebSettings.LOAD_DEFAULT
                                allowFileAccess = false
                                allowContentAccess = true

                                // User-Agent 配置
                                when (currentPwa.userAgentMode) {
                                    PwaItem.UA_DESKTOP -> {
                                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                                    }
                                    PwaItem.UA_MOBILE -> {
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                                    }
                                    else -> {}
                                }
                            }

                            // Cookie 管理，支持第三方 Cookie
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // 下载监听器
                            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                onDownloadRequested(url, userAgent, contentDisposition, mimeType)
                            }

                            // ChromeClient
                            webChromeClient = PwaChromeClient(
                                onProgressChangedListener = { p ->
                                    progress = p / 100f
                                    isLoading = p < 100
                                },
                                onTitleReceivedListener = { title ->
                                    currentTitle = title
                                },
                                onFileChooserListener = onOpenFileChooser
                            )

                            // WebViewClient
                            webViewClient = PwaWebViewClient(
                                context = ctx,
                                pwa = currentPwa,
                                onPageStartedListener = { url ->
                                    currentUrl = url
                                    isLoading = true
                                    errorMessage = null
                                },
                                onPageFinishedListener = { url ->
                                    currentUrl = url
                                    isLoading = false
                                    // 仅当用户未显式设置主题色时，才尝试提取页面动态更新的 theme-color
                                    if (currentPwa.themeColor == null) {
                                        evaluateJavascript(
                                            "(function() { var meta = document.querySelector('meta[name=theme-color]'); return meta ? meta.getAttribute('content') : ''; })()"
                                        ) { colorValue ->
                                            val cleanColor = colorValue?.replace("\"", "")?.trim()
                                            if (!cleanColor.isNullOrBlank()) {
                                                val parsed = PwaManifestParser.parseColorSafely(cleanColor)
                                                if (parsed != null) {
                                                    dynamicThemeColor = parsed
                                                }
                                            }
                                        }
                                    }
                                },
                                onErrorReceivedListener = { err ->
                                    errorMessage = err
                                    isLoading = false
                                }
                            )

                            onSetupWebView(this)
                            webViewRef = this
                            loadUrl(currentPwa.effectiveStartUrl)
                        }
                    },
                    update = {}
                )

                // 错误提示蒙层
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "网页加载失败",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(onClick = {
                                errorMessage = null
                                webViewRef?.reload()
                            }) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }
}
