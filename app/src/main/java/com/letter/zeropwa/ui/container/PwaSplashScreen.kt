package com.letter.zeropwa.ui.container

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.letter.zeropwa.R
import com.letter.zeropwa.data.model.PwaItem

@Composable
fun PwaSplashScreen(
    pwa: PwaItem,
    iconBitmap: Bitmap?,
    isTimeout: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 背景色：优先采用 PWA 专属主题色，确保第 0 毫秒即完成视觉沉浸，彻底消除白屏
    val splashBgColor = pwa.themeColor?.let { Color(it) }
        ?: pwa.backgroundColor?.let { Color(it) }
        ?: MaterialTheme.colorScheme.surface

    val isBgLight = ColorUtils.calculateLuminance(splashBgColor.toArgb()) > 0.5
    val contentColor = if (isBgLight) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.95f)
    val subContentColor = if (isBgLight) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)

    // 图标轻微呼吸微动效
    val infiniteTransition = rememberInfiniteTransition(label = "splash_breathe")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(splashBgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 图标展示 (大圆角 18dp，带细腻呼吸微动效与防裁切留白)
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (!isTimeout) scaleAnim else 1f),
                shape = RoundedCornerShape(18.dp),
                color = contentColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
            ) {
                if (iconBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = pwa.displayName,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = pwa.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. 应用名称
            Text(
                text = pwa.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 网站地址
            Text(
                text = pwa.effectiveStartUrl,
                style = MaterialTheme.typography.bodySmall,
                color = subContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isTimeout) {
                // 4. 加载中环形指示条
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = contentColor
                )
            } else {
                // 5. 超时诊断与重试卡片 (10 秒仍未有首帧上屏时呈现)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = contentColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.splash_timeout_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.splash_retry))
                            }
                            FilledTonalButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.splash_skip))
                            }
                        }
                    }
                }
            }
        }
    }
}
