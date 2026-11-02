package com.letter.zeropwa.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letter.zeropwa.data.model.PwaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPwaDialog(
    pwa: PwaItem,
    onConfirm: (PwaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(pwa.name) }
    var startUrl by remember { mutableStateOf(pwa.startUrl) }
    var themeColor by remember { mutableStateOf<Int?>(pwa.themeColor) }
    var displayMode by remember { mutableStateOf(pwa.displayMode) }
    var userAgentMode by remember { mutableIntStateOf(pwa.userAgentMode) }
    var iconMode by remember { mutableIntStateOf(pwa.statusBarIconMode) }
    var enableFloatingHandle by remember { mutableStateOf(pwa.enableFloatingToolHandle) }
    var enableSplash by remember { mutableStateOf(pwa.enableSplashScreen) }
    var enableImmersive by remember { mutableStateOf(pwa.enableImmersiveStatusBar) }
    var allowMultipleWindows by remember { mutableStateOf(pwa.allowMultipleWindows) }
    var openExternal by remember { mutableStateOf(pwa.openExternalLinksInBrowser) }
    var isPinned by remember { mutableStateOf(pwa.isPinnedToShortcuts) }

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
            // 头部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "配置 PWA 参数",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "调整运行模式、浏览器标识及系统快捷入口",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            // 名称编辑
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("应用名称") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 启动地址编辑
            OutlinedTextField(
                value = startUrl,
                onValueChange = { startUrl = it },
                label = { Text("启动网址 (Start URL)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 主题颜色修改
            ThemeColorPickerSection(
                selectedColor = themeColor,
                extractedColor = null,
                onColorChanged = { themeColor = it }
            )

            // 运行模式 Chip
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
                        selected = displayMode == PwaItem.DISPLAY_STANDALONE,
                        onClick = { displayMode = PwaItem.DISPLAY_STANDALONE },
                        label = { Text("独立应用") },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_STANDALONE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = displayMode == PwaItem.DISPLAY_FULLSCREEN,
                        onClick = { displayMode = PwaItem.DISPLAY_FULLSCREEN },
                        label = { Text("沉浸全屏") },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_FULLSCREEN) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = displayMode == PwaItem.DISPLAY_MINIMAL_UI,
                        onClick = { displayMode = PwaItem.DISPLAY_MINIMAL_UI },
                        label = { Text("操作栏") },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_MINIMAL_UI) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // 浏览器 User-Agent Chip
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
                        selected = userAgentMode == PwaItem.UA_DEFAULT,
                        onClick = { userAgentMode = PwaItem.UA_DEFAULT },
                        label = { Text("移动端 (默认)") },
                        leadingIcon = if (userAgentMode == PwaItem.UA_DEFAULT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = userAgentMode == PwaItem.UA_DESKTOP,
                        onClick = { userAgentMode = PwaItem.UA_DESKTOP },
                        label = { Text("电脑桌面版") },
                        leadingIcon = if (userAgentMode == PwaItem.UA_DESKTOP) {
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
                        selected = iconMode == PwaItem.STATUS_BAR_ICON_AUTO,
                        onClick = { iconMode = PwaItem.STATUS_BAR_ICON_AUTO },
                        label = { Text("智能适配") },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_AUTO) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = iconMode == PwaItem.STATUS_BAR_ICON_DARK,
                        onClick = { iconMode = PwaItem.STATUS_BAR_ICON_DARK },
                        label = { Text("深色字 (黑)") },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_DARK) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = iconMode == PwaItem.STATUS_BAR_ICON_LIGHT,
                        onClick = { iconMode = PwaItem.STATUS_BAR_ICON_LIGHT },
                        label = { Text("浅色字 (白)") },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_LIGHT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // 开关卡片
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
                        Switch(checked = enableSplash, onCheckedChange = { enableSplash = it })
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
                        Switch(checked = isPinned, onCheckedChange = { isPinned = it })
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

            // 保存按钮
            Button(
                onClick = {
                    onConfirm(
                        pwa.copy(
                            name = name.ifBlank { pwa.name },
                            startUrl = startUrl.ifBlank { pwa.startUrl },
                            themeColor = themeColor,
                            displayMode = displayMode,
                            userAgentMode = userAgentMode,
                            statusBarIconMode = iconMode,
                            enableFloatingToolHandle = enableFloatingHandle,
                            enableSplashScreen = enableSplash,
                            enableImmersiveStatusBar = enableImmersive,
                            allowMultipleWindows = allowMultipleWindows,
                            openExternalLinksInBrowser = openExternal,
                            isPinnedToShortcuts = isPinned
                        )
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
                    text = "保存配置修改",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
