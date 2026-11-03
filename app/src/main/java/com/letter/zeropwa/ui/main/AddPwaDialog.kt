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
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.letter.zeropwa.data.parser.PwaManifestParser
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

    val coroutineScope = rememberCoroutineScope()
    var editableName by remember { mutableStateOf("") }
    var selectedThemeColor by remember { mutableStateOf<Int?>(null) }
    var selectedIconUrl by remember { mutableStateOf<String?>(null) }
    var showCustomIconInput by remember { mutableStateOf(false) }
    var customIconText by remember { mutableStateOf("") }
    var isValidatingIcon by remember { mutableStateOf(false) }

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
                        text = stringResource(R.string.dialog_add_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.dialog_add_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            }

            // 网址输入区
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text(stringResource(R.string.dialog_url_label)) },
                placeholder = { Text(stringResource(R.string.dialog_url_placeholder)) },
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
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear))
                            }
                        }
                        IconButton(onClick = {
                            val text = clipboardManager.getText()?.text.orEmpty().trim()
                            if (text.isNotBlank()) {
                                urlInput = text
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.common_paste))
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
                    Text(stringResource(R.string.dialog_inspecting))
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_inspect_btn), fontWeight = FontWeight.SemiBold)
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
                    selectedIconUrl = candidate.iconUrl
                }

                val validSuccessMsg = stringResource(R.string.icon_valid_success)
                val validFailedMsg = stringResource(R.string.icon_valid_failed)

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
                            if (!selectedIconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = selectedIconUrl,
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
                                                text = if (selectedThemeColor == candidate.themeColor) stringResource(R.string.theme_color_extracted_tag) else stringResource(R.string.theme_color_custom_tag),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 自定义图标按钮
                        FilledTonalButton(
                            onClick = { showCustomIconInput = !showCustomIconInput },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.icon_custom_url),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }

                        // 自定义图标展开输入
                        if (showCustomIconInput) {
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
                                        value = customIconText,
                                        onValueChange = { customIconText = it },
                                        label = { Text(stringResource(R.string.icon_custom_url_label)) },
                                        placeholder = { Text(stringResource(R.string.icon_custom_url_placeholder)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (customIconText.isNotBlank()) {
                                                    IconButton(onClick = { customIconText = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_clear))
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    val text = clipboardManager.getText()?.text.orEmpty().trim()
                                                    if (text.isNotBlank()) customIconText = text
                                                }) {
                                                    Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.common_paste))
                                                }
                                            }
                                        }
                                    )

                                    Button(
                                        onClick = {
                                            val trimmed = customIconText.trim()
                                            if (trimmed.isBlank() || isValidatingIcon) return@Button
                                            isValidatingIcon = true
                                            coroutineScope.launch {
                                                val bitmap = PwaManifestParser.downloadBitmap(context, trimmed)
                                                isValidatingIcon = false
                                                if (bitmap != null) {
                                                    selectedIconUrl = trimmed
                                                    showCustomIconInput = false
                                                    Toast.makeText(context, validSuccessMsg, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, validFailedMsg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = customIconText.isNotBlank() && !isValidatingIcon,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isValidatingIcon) {
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 可编辑应用名称
                        OutlinedTextField(
                            value = editableName,
                            onValueChange = { editableName = it },
                            label = { Text(stringResource(R.string.dialog_name_label)) },
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
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_STANDALONE,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_STANDALONE },
                                    label = { Text(stringResource(R.string.home_chip_standalone)) },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_STANDALONE) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_FULLSCREEN,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_FULLSCREEN },
                                    label = { Text(stringResource(R.string.home_chip_fullscreen)) },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_FULLSCREEN) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedDisplayMode == PwaItem.DISPLAY_MINIMAL_UI,
                                    onClick = { selectedDisplayMode = PwaItem.DISPLAY_MINIMAL_UI },
                                    label = { Text(stringResource(R.string.home_chip_minimal_ui)) },
                                    leadingIcon = if (selectedDisplayMode == PwaItem.DISPLAY_MINIMAL_UI) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // 浏览器内核 (UA)
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
                                    selected = selectedUaMode == PwaItem.UA_DEFAULT,
                                    onClick = { selectedUaMode = PwaItem.UA_DEFAULT },
                                    label = { Text(stringResource(R.string.dialog_ua_mobile)) },
                                    leadingIcon = if (selectedUaMode == PwaItem.UA_DEFAULT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedUaMode == PwaItem.UA_DESKTOP,
                                    onClick = { selectedUaMode = PwaItem.UA_DESKTOP },
                                    label = { Text(stringResource(R.string.dialog_ua_desktop)) },
                                    leadingIcon = if (selectedUaMode == PwaItem.UA_DESKTOP) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        // 状态栏图标与文字风格
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
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_AUTO,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_AUTO },
                                    label = { Text(stringResource(R.string.dialog_status_bar_auto)) },
                                    leadingIcon = if (selectedIconMode == PwaItem.STATUS_BAR_ICON_AUTO) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_DARK,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_DARK },
                                    label = { Text(stringResource(R.string.dialog_status_bar_dark)) },
                                    leadingIcon = if (selectedIconMode == PwaItem.STATUS_BAR_ICON_DARK) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                FilterChip(
                                    selected = selectedIconMode == PwaItem.STATUS_BAR_ICON_LIGHT,
                                    onClick = { selectedIconMode = PwaItem.STATUS_BAR_ICON_LIGHT },
                                    label = { Text(stringResource(R.string.dialog_status_bar_light)) },
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
                                    Switch(checked = enableSplashScreen, onCheckedChange = { enableSplashScreen = it })
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
                                    Switch(checked = pinToShortcuts, onCheckedChange = { pinToShortcuts = it })
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
                    }
                }

                // 确认添加主操作按钮
                Button(
                    onClick = {
                        onConfirmAdd(
                            candidate.copy(iconUrl = selectedIconUrl),
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
                        text = stringResource(R.string.dialog_confirm_add),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
