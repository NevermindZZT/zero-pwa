package com.letter.zeropwa.ui.container

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Clamp on every layout change, including keyboard, landscape and split screen.
internal fun handleVerticalOffset(heightPx: Float, controlHeightPx: Float, fraction: Float): Float {
    val travel = (heightPx - controlHeightPx).coerceAtLeast(0f)
    return travel * fraction.coerceIn(0f, 1f)
}

@Composable
fun FloatingEdgeToolHandle(
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onOpenInBrowser: () -> Unit,
    currentUrl: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var highlighted by remember { mutableStateOf(true) }
    var interactionVersion by remember { mutableIntStateOf(0) }
    var positionFraction by rememberSaveable { mutableFloatStateOf(0.6f) }

    fun reveal() {
        highlighted = true
        interactionVersion++
    }
    fun closeMenu() {
        expanded = false
        reveal()
    }

    LaunchedEffect(interactionVersion, expanded, dragging) {
        if (!expanded && !dragging) {
            delay(3000)
            highlighted = false
        }
    }
    val handleAlpha by animateFloatAsState(
        targetValue = if (highlighted || expanded || dragging) 1f else 0.72f,
        animationSpec = tween(400), label = "handle_alpha"
    )
    BackHandler(enabled = expanded) { closeMenu() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val heightPx = with(density) { maxHeight.toPx() }
        val controlHeightPx = with(density) { 56.dp.toPx() }
        val travelPx = (heightPx - controlHeightPx).coerceAtLeast(0f)

        if (expanded) {
            Box(
                Modifier.fillMaxSize().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { closeMenu() }
                )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Stay away from the Android edge-back gesture and OEM sidebars.
                .padding(end = 8.dp)
                .offset { IntOffset(0, handleVerticalOffset(heightPx, controlHeightPx, positionFraction).roundToInt()) }
                .pointerInput(travelPx) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true; reveal() },
                        onDragEnd = { dragging = false; reveal() },
                        onDragCancel = { dragging = false; reveal() },
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            if (travelPx > 0f) {
                                positionFraction = (positionFraction + delta / travelPx).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        ) {
            if (!expanded) {
                // Visible 14dp handle within a 48dp touch target. It intentionally
                // covers a small area: a completely invisible handle cannot be found.
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 56.dp)
                        .semantics { contentDescription = "打开网页工具，拖动调整位置" }
                        .clickable { expanded = true; reveal() },
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        modifier = Modifier.width(14.dp).height(48.dp).alpha(handleAlpha),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.width(3.dp).height(22.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.inverseOnSurface)
                            )
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        QuickActionButton(Icons.Default.Refresh, "刷新网页") { closeMenu(); onReload() }
                        QuickActionButton(Icons.Default.Home, "返回主页") { closeMenu(); onGoHome() }
                        QuickActionButton(Icons.Default.ContentCopy, "复制链接") {
                            closeMenu()
                            val url = currentUrl()
                            if (url.isNotBlank()) {
                                clipboard.setText(AnnotatedString(url))
                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                            }
                        }
                        QuickActionButton(Icons.Default.OpenInBrowser, "在浏览器中打开") { closeMenu(); onOpenInBrowser() }
                        QuickActionButton(Icons.Default.Close, "收起工具") { closeMenu() }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}
