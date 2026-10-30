package com.letter.zeropwa.ui.main

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.ui.settings.SettingsActivity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val pwaList by viewModel.pwaList.collectAsState()
    val inspectState by viewModel.inspectState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedActionPwa by remember { mutableStateOf<PwaItem?>(null) }
    var editingPwa by remember { mutableStateOf<PwaItem?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Zero PWA",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "任意网站 · 独立窗口 · 原生体验",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            // Material You 规范：完全去阴影，采用 Tonal Color 表达层次
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.resetInspectState()
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加 PWA", fontWeight = FontWeight.SemiBold) },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pwaList.isEmpty()) {
                EmptyStateView(onAddClick = {
                    viewModel.resetInspectState()
                    showAddDialog = true
                })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pwaList, key = { it.id }) { pwa ->
                        PwaItemCard(
                            pwa = pwa,
                            onClick = { viewModel.launchPwa(context, pwa.id, pwa.allowMultipleWindows) },
                            onMoreClick = { selectedActionPwa = pwa }
                        )
                    }
                }
            }
        }
    }

    // Material 3 原生操作底栏 (彻底取代旧式系统的 DropdownMenu)
    val activeActionPwa = selectedActionPwa?.let { selected ->
        pwaList.find { it.id == selected.id } ?: selected
    }
    activeActionPwa?.let { pwa ->
        PwaActionBottomSheet(
            pwa = pwa,
            onLaunch = { viewModel.launchPwa(context, pwa.id, pwa.allowMultipleWindows) },
            onPinShortcut = { viewModel.createPinnedShortcut(context, pwa) },
            onToggleShortcut = { viewModel.togglePinnedToShortcuts(pwa) },
            onEdit = { editingPwa = pwa },
            onDelete = { viewModel.deletePwa(pwa.id) },
            onDismiss = { selectedActionPwa = null }
        )
    }

    // 添加 PWA BottomSheet
    if (showAddDialog) {
        AddPwaDialog(
            inspectState = inspectState,
            onInspect = { url -> viewModel.inspectUrl(url) },
            onConfirmAdd = { candidate, name, customColor, displayMode, uaMode, iconMode, enableImmersive, allowMultiple, openExt, isPinned ->
                viewModel.savePwa(
                    candidate = candidate,
                    customName = name,
                    customThemeColor = customColor,
                    displayMode = displayMode,
                    userAgentMode = uaMode,
                    statusBarIconMode = iconMode,
                    enableImmersiveStatusBar = enableImmersive,
                    allowMultipleWindows = allowMultiple,
                    openExternalInBrowser = openExt,
                    isPinnedToShortcuts = isPinned
                )
                showAddDialog = false
            },
            onDismiss = {
                viewModel.resetInspectState()
                showAddDialog = false
            }
        )
    }

    // 编辑 PWA 参数 BottomSheet
    editingPwa?.let { pwa ->
        EditPwaDialog(
            pwa = pwa,
            onConfirm = { updated ->
                viewModel.updatePwa(updated)
                editingPwa = null
            },
            onDismiss = { editingPwa = null }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PwaItemCard(
    pwa: PwaItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    // 严格遵循 Material You 规范：无投影 (elevation=0)，通过 SurfaceContainer 纯色调区分
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMoreClick
            ),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标渲染 (SVG 与位图通用，大圆角更现代)
            val iconFile = pwa.iconPath?.let { File(it) }
            if (iconFile != null && iconFile.exists()) {
                AsyncImage(
                    model = iconFile,
                    contentDescription = pwa.displayName,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            pwa.themeColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pwa.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 信息展示
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pwa.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (pwa.isPinnedToShortcuts) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "长按置顶",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = pwa.startUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val modeLabel = when (pwa.displayMode) {
                        PwaItem.DISPLAY_FULLSCREEN -> "沉浸全屏"
                        PwaItem.DISPLAY_MINIMAL_UI -> "带操作栏"
                        else -> "独立应用"
                    }
                    SuggestionChip(
                        onClick = onClick,
                        label = { Text(modeLabel, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                        ),
                        border = null
                    )
                    if (pwa.userAgentMode == PwaItem.UA_DESKTOP) {
                        SuggestionChip(
                            onClick = onClick,
                            label = { Text("电脑版", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                            ),
                            border = null
                        )
                    }
                }
            }

            // 单一整洁的更多菜单入口
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多操作选项",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Web,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "尚未添加任何 PWA 网站",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "输入任意网站地址，自动提取 Manifest\n并以独立任务窗口畅享原生体验",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(16.dp),
            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("立即添加第一个 PWA", fontWeight = FontWeight.SemiBold)
        }
    }
}
