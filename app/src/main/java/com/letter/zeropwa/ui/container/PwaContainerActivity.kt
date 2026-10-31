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
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.ComposeView
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
    private var lastBackPressTime = 0L

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

        // 统一注册系统返回手势调度器
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleAppBackPressed()
            }
        })

        // Configure before composition: Compose 1.6 consumes all native insets as soon
        // as imePadding reads them, otherwise starving the embedded WebView of safe-area.
        val composeView = createPwaComposeView(this)
        setContentView(composeView)
        composeView.setContent {
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
                    onBackPressed = { handleAppBackPressed() },
                    onClose = { handleFinishWithDoubleBack() }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleAppBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 多级智能返回导航管道：
     * 1. 优先触发 WebView 原生历史回退 (canGoBack)
     * 2. 针对 SPA 单页应用，探测页面内置返回按钮、关闭抽屉底栏与派发 Escape / backbutton
     * 3. 根视图防误触：2 秒内连续按两次返回键才真正退出 PWA
     */
    fun handleAppBackPressed() {
        val wv = webView
        if (wv == null) {
            handleFinishWithDoubleBack()
            return
        }

        // 1. 历史栈回退
        if (wv.canGoBack()) {
            wv.goBack()
            return
        }

        // 2. 针对单页应用进行智能返回与关闭弹层探测
        val smartBackJs = """
            (function() {
                try {
                    // 检查页面左上角或顶部的返回/后退按钮并优先点击 (如 "< 行程", "返回" 等)
                    var backSelectors = [
                        'button[aria-label*="返回"]', 'button[aria-label*="Back"]',
                        'a[aria-label*="返回"]', 'a[aria-label*="Back"]',
                        '.nav-back', '.btn-back', '.back-button', '.back',
                        '.atlas-topbar button', '.workspace-topbar button',
                        '[data-action="back"]', '.navbar-back'
                    ];
                    for (var i = 0; i < backSelectors.length; i++) {
                        var btn = document.querySelector(backSelectors[i]);
                        if (btn && btn.offsetParent !== null && typeof btn.click === 'function') {
                            btn.click();
                            return 'clicked_button';
                        }
                    }

                    // 派发 Escape 键盘事件以尝试关闭模态框、抽屉与菜单
                    var escEvent = new KeyboardEvent('keydown', {
                        key: 'Escape',
                        code: 'Escape',
                        keyCode: 27,
                        which: 27,
                        bubbles: true,
                        cancelable: true
                    });
                    document.dispatchEvent(escEvent);
                    window.dispatchEvent(escEvent);

                    // 派发 Hybrid 标准 backbutton 事件
                    var backEvent = new Event('backbutton', { bubbles: true, cancelable: true });
                    document.dispatchEvent(backEvent);
                    window.dispatchEvent(backEvent);
                } catch(e) {}
                return 'none';
            })();
        """.trimIndent()

        wv.evaluateJavascript(smartBackJs) { result ->
            val clean = result?.replace("\"", "")?.trim()
            if (clean == "clicked_button") {
                // 成功触发网页内部返回按钮，无需退出应用
                return@evaluateJavascript
            }
            // 页面已无内部回退项，进入防误触双击退出
            handleFinishWithDoubleBack()
        }
    }

    private fun handleFinishWithDoubleBack() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            finish()
        } else {
            lastBackPressTime = now
            Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        dispatchForegroundReconnectionPulse()
    }

    override fun onPause() {
        super.onPause()
        try {
            CookieManager.getInstance().flush()
        } catch (_: Exception) {}
    }

    /**
     * 前台长连接自愈脉冲：
     * 当应用从 Android 后台返回前台时，主动派发网络恢复与可见性事件，
     * 并探测已处于 CLOSED 或假死状态的 WebSocket 连接，促发前端框架自愈重连。
     */
    private fun dispatchForegroundReconnectionPulse() {
        val wv = webView ?: return
        wv.postDelayed({
            val pulseJs = """
                (function() {
                    try {
                        // 1. 强制派发网络重连与可见性聚焦事件
                        window.dispatchEvent(new Event('online'));
                        document.dispatchEvent(new Event('visibilitychange'));
                        window.dispatchEvent(new Event('focus'));

                        // 2. 扫描并唤醒断开或假死的 WebSocket 实例
                        if (window.__zeropwa_active_websockets && Array.isArray(window.__zeropwa_active_websockets)) {
                            window.__zeropwa_active_websockets.forEach(function(item) {
                                if (item && item.ws) {
                                    var ws = item.ws;
                                    if (ws.readyState === 2 || ws.readyState === 3) {
                                        try {
                                            if (typeof ws.onerror === 'function') ws.onerror(new Event('error'));
                                            if (typeof ws.onclose === 'function') {
                                                ws.onclose(new CloseEvent('close', { wasClean: false, code: 1006, reason: 'ZeroPwa Wakeup' }));
                                            }
                                        } catch(e) {}
                                    } else if (ws.readyState === 1) {
                                        try { ws.send(''); } catch(e) {}
                                    }
                                }
                            });
                        }
                    } catch(err) {}
                })();
            """.trimIndent()
            wv.evaluateJavascript(pulseJs, null)
        }, 200)
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

// Keep native status-bar/cutout insets available to WebView. Compose still uses IME
// insets to resize its layout; this flag only disables whole-packet consumption.
internal fun createPwaComposeView(context: Context): ComposeView = ComposeView(context).apply {
    consumeWindowInsets = false
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PwaContainerScreen(
    pwaId: Long,
    onSetupWebView: (WebView) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
    onDownloadRequested: (url: String, userAgent: String, contentDisposition: String, mimeType: String) -> Unit,
    onApplyTheme: (PwaItem, Bitmap?) -> Unit,
    onBackPressed: () -> Unit,
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

    // 网页返回键与侧滑手势导航监听：统一走多级智能调度器
    BackHandler(enabled = true) {
        onBackPressed()
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

    var sampledTopIsLight by remember { mutableStateOf<Boolean?>(null) }

    // 状态栏图标与文字深浅色智能自适应裁决：
    val isLightStatusBars = when (currentPwa.statusBarIconMode) {
        PwaItem.STATUS_BAR_ICON_DARK -> true  // 强制深色文字 (黑字，适合浅色页面/浅色地图)
        PwaItem.STATUS_BAR_ICON_LIGHT -> false // 强制浅色文字 (白字，适合暗黑夜间页面)
        else -> {
            // AUTO 自动智能模式：
            if (currentPwa.enableImmersiveStatusBar) {
                // 沉浸穿透模式：网页内容直接处于状态栏正下方
                // 优先使用页面顶部像素动态采样结果；未采样完成前优先默认深色字 (绝大多数现代网页/地图均为白底浅色 UI)
                sampledTopIsLight ?: currentPwa.backgroundColor?.let { ColorUtils.calculateLuminance(it) > 0.5 } ?: true
            } else {
                // 安全区隔离模式：根据状态栏着色条自身色彩亮度自适应反转
                ColorUtils.calculateLuminance(effectiveColorInt) > 0.5
            }
        }
    }

    DisposableEffect(isLightStatusBars) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLightStatusBars
        }
        onDispose {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = currentPwa.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

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
                            configureViewportInsets()
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

                                    // 注入 WebSocket 透明长连接生命周期追踪
                                    evaluateJavascript(
                                        """
                                        (function() {
                                            if (window.__zeropwa_ws_hooked) return;
                                            window.__zeropwa_ws_hooked = true;
                                            window.__zeropwa_active_websockets = [];
                                            var OrigWS = window.WebSocket;
                                            if (!OrigWS) return;
                                            window.WebSocket = function(url, protocols) {
                                                var ws = protocols !== undefined ? new OrigWS(url, protocols) : new OrigWS(url);
                                                var ref = { ws: ws, url: url, time: Date.now() };
                                                window.__zeropwa_active_websockets.push(ref);
                                                ws.addEventListener('close', function() {
                                                    var i = window.__zeropwa_active_websockets.indexOf(ref);
                                                    if (i > -1) window.__zeropwa_active_websockets.splice(i, 1);
                                                });
                                                return ws;
                                            };
                                            window.WebSocket.prototype = OrigWS.prototype;
                                            window.WebSocket.CONNECTING = OrigWS.CONNECTING;
                                            window.WebSocket.OPEN = OrigWS.OPEN;
                                            window.WebSocket.CLOSING = OrigWS.CLOSING;
                                            window.WebSocket.CLOSED = OrigWS.CLOSED;
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                },
                                onPageFinishedListener = { url ->
                                    currentUrl = url
                                    isLoading = false

                                    // 确保 WebSocket 透明长连接追踪在单页应用就绪
                                    evaluateJavascript(
                                        """
                                        (function() {
                                            if (window.__zeropwa_ws_hooked) return;
                                            window.__zeropwa_ws_hooked = true;
                                            window.__zeropwa_active_websockets = [];
                                            var OrigWS = window.WebSocket;
                                            if (!OrigWS) return;
                                            window.WebSocket = function(url, protocols) {
                                                var ws = protocols !== undefined ? new OrigWS(url, protocols) : new OrigWS(url);
                                                var ref = { ws: ws, url: url, time: Date.now() };
                                                window.__zeropwa_active_websockets.push(ref);
                                                ws.addEventListener('close', function() {
                                                    var i = window.__zeropwa_active_websockets.indexOf(ref);
                                                    if (i > -1) window.__zeropwa_active_websockets.splice(i, 1);
                                                });
                                                return ws;
                                            };
                                            window.WebSocket.prototype = OrigWS.prototype;
                                            window.WebSocket.CONNECTING = OrigWS.CONNECTING;
                                            window.WebSocket.OPEN = OrigWS.OPEN;
                                            window.WebSocket.CLOSING = OrigWS.CLOSING;
                                            window.WebSocket.CLOSED = OrigWS.CLOSED;
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
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

                                    // 沉浸穿透模式下：智能采样网页顶部视口真实背景色与亮度，自动适配状态栏文字黑白
                                    if (currentPwa.enableImmersiveStatusBar && currentPwa.statusBarIconMode == PwaItem.STATUS_BAR_ICON_AUTO) {
                                        val sampleJs = """
                                            (function() {
                                                try {
                                                    var el = document.elementFromPoint(window.innerWidth / 2, 20);
                                                    while (el && el !== document.documentElement) {
                                                        var s = window.getComputedStyle(el);
                                                        var bg = s.backgroundColor;
                                                        if (bg && bg !== 'transparent' && bg !== 'rgba(0, 0, 0, 0)') return bg;
                                                        el = el.parentElement;
                                                    }
                                                    var bodyBg = window.getComputedStyle(document.body).backgroundColor;
                                                    if (bodyBg && bodyBg !== 'transparent' && bodyBg !== 'rgba(0, 0, 0, 0)') return bodyBg;
                                                    return '';
                                                } catch(e) { return ''; }
                                            })()
                                        """.trimIndent()
                                        evaluateJavascript(sampleJs) { sampledVal ->
                                            val clean = sampledVal?.replace("\"", "")?.trim()
                                            if (!clean.isNullOrBlank()) {
                                                val parsed = PwaManifestParser.parseColorSafely(clean)
                                                if (parsed != null) {
                                                    sampledTopIsLight = ColorUtils.calculateLuminance(parsed) > 0.5
                                                }
                                            }
                                        }
                                    }
                                    // 确保页面聚焦输入框时，自动平滑滚动到视口舒适可见区域，防止软键盘遮挡
                                    val focusAdjustJs = """
                                        (function() {
                                            if (window.__zeropwa_focus_listener_attached) return;
                                            window.__zeropwa_focus_listener_attached = true;
                                            window.addEventListener('focusin', function(e) {
                                                var t = e.target;
                                                if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA' || t.isContentEditable)) {
                                                    setTimeout(function() {
                                                        try {
                                                            t.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                                        } catch(err) {
                                                            try { t.scrollIntoView(false); } catch(e2) {}
                                                        }
                                                    }, 300);
                                                }
                                            }, true);
                                        })()
                                    """.trimIndent()
                                    evaluateJavascript(focusAdjustJs, null)
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
