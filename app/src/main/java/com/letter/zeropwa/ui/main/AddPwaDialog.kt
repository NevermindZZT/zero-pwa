package com.letter.zeropwa.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.letter.zeropwa.data.AppPreferences
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.data.parser.PwaCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPwaDialog(
    inspectState: InspectState,
    onInspect: (String) -> Unit,
    onConfirmAdd: (
        candidate: PwaCandidate,
        name: String,
        customThemeColor: Int?,
        displayMode: String,
        userAgentMode: Int,
        statusBarIconMode: Int,
        enableFloatingHandle: Boolean,
        enableSplashScreen: Boolean,
        enableImmersiveStatusBar: Boolean,
        allowMultipleWindows: Boolean,
        openExternalInBrowser: Boolean,
        isPinnedToShortcuts: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val clipboardManager = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf("") }

    var editableName by remember { mutableStateOf("") }
    var selectedThemeColor by remember { mutableStateOf<Int?>(null) }
    var selectedDisplayMode by remember { mutableStateOf(PwaItem.DISPLAY_STANDALONE) }
    var selectedUaMode by remember { mutableIntStateOf(PwaItem.UA_DEFAULT) }
    var selectedIconMode by remember { mutableIntStateOf(PwaItem.STATUS_BAR_ICON_AUTO) }
    var enableFloatingHandle by remember { mutableStateOf(prefs.defaultFloatingToolHandle) }
    var enableSplashScreen by remember { mutableStateOf(prefs.defaultSplashScreen) }
    var enableImmersive by remember { mutableStateOf(true) }
    var allowMultipleWindows by remember { mutableStateOf(false) }
    var openExternal by remember { mutableStateOf(true) }
    var pinToShortcuts by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "添加网站为 PWA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "自动提取网站元数据并独立窗口运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            // 网址输入区
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("网站地址 (URL)") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (urlInput.isNotBlank()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清空")
                            }
                        }
                        IconButton(onClick = {
                            val text = clipboardManager.getText()?.text.orEmpty().trim()
                            if (text.isNotBlank()) {
                                urlInput = text
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "粘贴")
                        }
                    }
                }
            )

            // 探测与分析按钮
            FilledTonalButton(
                onClick = { onInspect(urlInput) },
                enabled = urlInput.isNotBlank() && inspectState !is InspectState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (inspectState is InspectState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("正在探测 Manifest 与提取图标...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分析网站配置", fontWeight = FontWeight.SemiBold)
                }
            }

            // 错误提示条
            if (inspectState is InspectState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = inspectState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 探测成功后展示的配置与预览卡片
            if (inspectState is InspectState.Success) {
                val candidate = inspectState.candidate
                if (editableName.isBlank()) {
                    editableName = candidate.name
                    selectedDisplayMode = candidate.displayMode
                    selectedThemeColor = candidate.themeColor
                }

                // 网站信息预览卡片 (遵循 Material You 无阴影色调规范)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 网站图标与主要标题行
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!candidate.iconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = candidate.iconUrl,
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = candidate.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = candidate.startUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                if (selectedThemeColor != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(selectedThemeColor!!))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (selectedThemeColor == candidate.themeColor) "提取主题色" else "自定义主题色",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 可编辑应用名称
                        OutlinedTextField(
                            value = editableName,
                            onValueChange = { editableName = it },
                            label = { Text("应用名称") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 主题色手动选择与微调
                        ThemeColorPickerSection(
                            selectedColor = selectedThemeColor,
                            extractedColor = candidate.themeColor,
                            onColorChanged = { selectedThemeColor = it }
                        )

                        // 显示模式选择（Chip 胶囊式）
                        Column {
                            Text(
                                text = "窗口运行模式",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_STANDALONE,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_STANDALONE },
                                    label = { Text("独立应用") },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_STANDALONE) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_FULLSCREEN,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_FULLSCREEN },
                                    label = { Text("沉浸全屏") },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_FULLSCREEN) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_MINIMAL_UI,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_MINIMAL_UI },
                                    label = { Text("操作栏") },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_MINIMAL_UI) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // 浏览器内核 (UA)
                        Column {
                            Text(
                                text = "设备仿真 (User-Agent)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedUaMode == PwaItem.UA_DEFAULT,
                                    onClick = { selectedUaMode = PwaItem.UA_DEFAULT },
                                    label = { Text("移动版 (默认)") },
                                    leadingIcon = if (selectedUaMode == PwaItem.UA_DEFAULT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedUaMode == PwaItem.UA_DESKTOP,
                                    onClick = { selectedUaMode = PwaItem.UA_DESKTOP },
                                    label = { Text("电脑桌面版") },
                                    leadingIcon = if (selectedUaMode == PwaItem.UA_DESKTOP) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // 状态栏图标与文字风格
                        Column {
                            Text(
                                text = "状态栏文字与图标风格",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_AUTO,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_AUTO },
                                    label = { Text("智能适配") },
                                    leadingIcon = if (selectedIconMode == PwaItem.STATUS_BAR_ICON_AUTO) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_DARK,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_DARK },
                                    label = { Text("深色字 (黑)") },
                                    leadingIcon = if (selectedIconMode == PwaItem.STATUS_BAR_ICON_DARK) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_LIGHT,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_LIGHT },
                                    label = { Text("浅色字 (白)") },
                                    leadingIcon = if (selectedIconMode == PwaItem.STATUS_BAR_ICON_LIGHT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // 高级功能开关
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("贴边悬浮工具手柄", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("平时贴边半隐形不遮挡，轻触随时刷新网页与主页导航", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = enableFloatingHandle, onCheckedChange = { enableFloatingHandle = it })
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("品牌启动过渡屏", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("首帧加载前展示专属主题色与图标过渡，消除白屏等待感", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = enableSplashScreen, onCheckedChange = { enableSplashScreen = it })
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("状态栏沉浸模式", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("开启后内容延伸穿透状态栏；关闭则保留安全区隔离", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = enableImmersive, onCheckedChange = { enableImmersive = it })
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("允许多任务窗口并发", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("默认关闭（单窗口：再次点击复用已打开窗口）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = allowMultipleWindows, onCheckedChange = { allowMultipleWindows = it })
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("长按应用图标直接打开", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("加入桌面图标长按弹出的快捷菜单", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = pinToShortcuts, onCheckedChange = { pinToShortcuts = it })
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("外部链接调用系统浏览器", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("离开本站范围的链接由系统默认浏览器接管", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = openExternal, onCheckedChange = { openExternal = it })
                                }
                            }
                        }
                    }
                }

                // 确认添加主操作按钮
                Button(
                    onClick = {
                        onConfirmAdd(
                            candidate,
                            editableName.ifBlank { candidate.name },
                            selectedThemeColor,
                            selectedDisplayMode,
                            selectedUaMode,
                            selectedIconMode,
                            enableFloatingHandle,
                            enableSplashScreen,
                            enableImmersive,
                            allowMultipleWindows,
                            openExternal,
                            pinToShortcuts
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "确认添加为此 PWA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
