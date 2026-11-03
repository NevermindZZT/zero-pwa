package com.letter.zeropwa.ui.main

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.letter.zeropwa.R
import com.letter.zeropwa.data.model.PwaItem
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPwaDialog(
    pwa: PwaItem,
    onConfirm: (PwaItem, String?) -> Unit,
    onRefetchIcon: suspend (String) -> String?,
    onValidateIcon: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

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

    // 图标重获与自定义状态
    var currentIconUrl by remember { mutableStateOf(pwa.iconUrl) }
    var showCustomUrlInput by remember { mutableStateOf(false) }
    var customUrlText by remember { mutableStateOf("") }
    var isRefetchingIcon by remember { mutableStateOf(false) }
    var isValidatingCustomUrl by remember { mutableStateOf(false) }

    val refetchSuccessMsg = stringResource(R.string.icon_refetch_success)
    val refetchFailedMsg = stringResource(R.string.icon_refetch_failed)
    val validSuccessMsg = stringResource(R.string.icon_valid_success)
    val validFailedMsg = stringResource(R.string.icon_valid_failed)

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
                        text = stringResource(R.string.dialog_edit_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dialog_edit_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            }

            // 1. 图标管理卡片 (支持实时预览、重新获取与自定义 URL 设置)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 图标展示 (优先展示新选中的 currentIconUrl，回退到已有本地文件)
                        val iconModel: Any? = when {
                            !currentIconUrl.isNullOrBlank() -> currentIconUrl
                            !pwa.iconPath.isNullOrBlank() && File(pwa.iconPath).exists() -> File(pwa.iconPath)
                            else -> null
                        }

                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            if (iconModel != null) {
                                Box(modifier = Modifier.fillMaxSize().padding(5.dp), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = iconModel,
                                        contentDescription = "App Icon",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(pwa.themeColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.icon_manage_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentIconUrl ?: pwa.iconPath?.substringAfterLast('/') ?: stringResource(R.string.dialog_status_bar_auto),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // 重新获取 & 手动设置按钮组
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 重新获取按钮
                        FilledTonalButton(
                            onClick = {
                                if (isRefetchingIcon) return@FilledTonalButton
                                isRefetchingIcon = true
                                coroutineScope.launch {
                                    val fetched = onRefetchIcon(startUrl.ifBlank { pwa.startUrl })
                                    isRefetchingIcon = false
                                    if (!fetched.isNullOrBlank()) {
                                        currentIconUrl = fetched
                                        Toast.makeText(context, refetchSuccessMsg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, refetchFailedMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRefetchingIcon) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.icon_refetching),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.icon_refetch),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        // 手动设置 URL 切换按钮
                        FilledTonalButton(
                            onClick = { showCustomUrlInput = !showCustomUrlInput },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.icon_custom_url),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }

                    // 展开的自定义图标 URL 输入区
                    if (showCustomUrlInput) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = customUrlText,
                                    onValueChange = { customUrlText = it },
                                    label = { Text(stringResource(R.string.icon_custom_url_label)) },
                                    placeholder = { Text(stringResource(R.string.icon_custom_url_placeholder)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (customUrlText.isNotBlank()) {
                                                IconButton(onClick = { customUrlText = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear))
                                                }
                                            }
                                            IconButton(onClick = {
                                                val text = clipboardManager.getText()?.text.orEmpty().trim()
                                                if (text.isNotBlank()) customUrlText = text
                                            }) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.common_paste))
                                            }
                                        }
                                    }
                                )

                                Button(
                                    onClick = {
                                        val trimmed = customUrlText.trim()
                                        if (trimmed.isBlank() || isValidatingCustomUrl) return@Button
                                        isValidatingCustomUrl = true
                                        coroutineScope.launch {
                                            val valid = onValidateIcon(trimmed)
                                            isValidatingCustomUrl = false
                                            if (valid) {
                                                currentIconUrl = trimmed
                                                showCustomUrlInput = false
                                                Toast.makeText(context, validSuccessMsg, Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, validFailedMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = customUrlText.isNotBlank() && !isValidatingCustomUrl,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isValidatingCustomUrl) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.icon_testing))
                                    } else {
                                        Text(stringResource(R.string.icon_apply))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. 名称编辑
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.dialog_name_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. 启动地址编辑
            OutlinedTextField(
                value = startUrl,
                onValueChange = { startUrl = it },
                label = { Text(stringResource(R.string.dialog_start_url_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. 主题颜色修改
            ThemeColorPickerSection(
                selectedColor = themeColor,
                extractedColor = null,
                onColorChanged = { themeColor = it }
            )

            // 5. 运行模式 Chip
            Column {
                Text(
                    text = stringResource(R.string.dialog_window_mode),
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
                        label = { Text(stringResource(R.string.home_chip_standalone)) },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_STANDALONE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = displayMode == PwaItem.DISPLAY_FULLSCREEN,
                        onClick = { displayMode = PwaItem.DISPLAY_FULLSCREEN },
                        label = { Text(stringResource(R.string.home_chip_fullscreen)) },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_FULLSCREEN) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = displayMode == PwaItem.DISPLAY_MINIMAL_UI,
                        onClick = { displayMode = PwaItem.DISPLAY_MINIMAL_UI },
                        label = { Text(stringResource(R.string.home_chip_minimal_ui)) },
                        leadingIcon = if (displayMode == PwaItem.DISPLAY_MINIMAL_UI) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // 6. 浏览器 User-Agent Chip
            Column {
                Text(
                    text = stringResource(R.string.dialog_ua_mode),
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
                        label = { Text(stringResource(R.string.dialog_ua_mobile)) },
                        leadingIcon = if (userAgentMode == PwaItem.UA_DEFAULT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = userAgentMode == PwaItem.UA_DESKTOP,
                        onClick = { userAgentMode = PwaItem.UA_DESKTOP },
                        label = { Text(stringResource(R.string.dialog_ua_desktop)) },
                        leadingIcon = if (userAgentMode == PwaItem.UA_DESKTOP) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // 7. 状态栏图标与文字风格
            Column {
                Text(
                    text = stringResource(R.string.dialog_status_bar_style),
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
                        label = { Text(stringResource(R.string.dialog_status_bar_auto)) },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_AUTO) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = iconMode == PwaItem.STATUS_BAR_ICON_DARK,
                        onClick = { iconMode = PwaItem.STATUS_BAR_ICON_DARK },
                        label = { Text(stringResource(R.string.dialog_status_bar_dark)) },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_DARK) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = iconMode == PwaItem.STATUS_BAR_ICON_LIGHT,
                        onClick = { iconMode = PwaItem.STATUS_BAR_ICON_LIGHT },
                        label = { Text(stringResource(R.string.dialog_status_bar_light)) },
                        leadingIcon = if (iconMode == PwaItem.STATUS_BAR_ICON_LIGHT) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // 8. 开关卡片
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
                            Text(stringResource(R.string.dialog_opt_floating_handle), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_floating_handle_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.dialog_opt_splash), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_splash_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.dialog_opt_immersive), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_immersive_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.dialog_opt_multi_window), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_multi_window_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.dialog_opt_long_press), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_long_press_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(stringResource(R.string.dialog_opt_open_external), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.dialog_opt_open_external_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = openExternal, onCheckedChange = { openExternal = it })
                    }
                }
            }

            // 保存按钮
            Button(
                onClick = {
                    val updatedPwa = pwa.copy(
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
                    val newIcon = if (!currentIconUrl.isNullOrBlank() && currentIconUrl != pwa.iconUrl) {
                        currentIconUrl
                    } else null
                    onConfirm(updatedPwa, newIcon)
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
                    text = stringResource(R.string.dialog_confirm_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
