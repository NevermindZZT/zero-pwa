package com.letter.zeropwa.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.letter.zeropwa.R
import com.letter.zeropwa.data.parser.PwaManifestParser

@Composable
fun ThemeColorPickerSection(
    selectedColor: Int?,
    extractedColor: Int?,
    onColorChanged: (Int?) -> Unit
) {
    // 经典 Material 预设色盘
    val presetColors = remember {
        listOf(
            0xFF24695C.toInt(), // 墨绿 (JourneyIn)
            0xFF1A73E8.toInt(), // 谷歌蓝
            0xFF1C1B1F.toInt(), // 暗夜黑
            0xFF6750A4.toInt(), // M3 经典紫
            0xFF0D47A1.toInt(), // 深海蓝
            0xFF00796B.toInt(), // 薄荷青
            0xFF2E7D32.toInt(), // 森林绿
            0xFFE65100.toInt(), // 暖阳橙
            0xFFB3261E.toInt(), // 绯红
            0xFF4E342E.toInt(), // 摩卡棕
            0xFF37474F.toInt()  // 蓝灰
        )
    }

    var hexInput by remember { mutableStateOf("") }
    var isHexValid by remember { mutableStateOf(true) }

    LaunchedEffect(selectedColor) {
        if (selectedColor != null) {
            val hex = String.format("#%06X", 0xFFFFFF and selectedColor)
            if (hexInput != hex) {
                hexInput = hex
                isHexValid = true
            }
        } else {
            hexInput = ""
            isHexValid = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.theme_color_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.theme_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 若有自动提取的原始颜色，且当前与其不同，提供一键重置
            if (extractedColor != null && selectedColor != extractedColor) {
                AssistChip(
                    onClick = { onColorChanged(extractedColor) },
                    label = { Text(stringResource(R.string.theme_color_restore), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                    ),
                    border = null
                )
            }
        }

        // 当前色彩实时预览与 Hex 输入栏
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 色块预览
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == null) {
                        Icon(
                            imageVector = Icons.Default.FormatColorReset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        val isLight = ColorUtils.calculateLuminance(selectedColor) > 0.5
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (isLight) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 十六进制颜色代码手动输入框
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val trimmed = input.trim()
                        hexInput = trimmed
                        if (trimmed.isBlank()) {
                            isHexValid = true
                            onColorChanged(null)
                        } else {
                            val parsed = PwaManifestParser.parseColorSafely(trimmed)
                            if (parsed != null) {
                                isHexValid = true
                                onColorChanged(parsed)
                            } else {
                                isHexValid = false
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.theme_color_custom_code)) },
                    placeholder = { Text("#24695C 或 rgb(...)") },
                    singleLine = true,
                    isError = !isHexValid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // 快捷推荐调色板 (水平滚动圆点)
        Text(
            text = stringResource(R.string.theme_color_presets),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选项 0：清除颜色 (跟随系统)
            ColorPresetItem(
                color = null,
                isSelected = selectedColor == null,
                onClick = { onColorChanged(null) }
            )

            // 选项 1：原网站提取色 (如果存在)
            if (extractedColor != null) {
                ColorPresetItem(
                    color = extractedColor,
                    isSelected = selectedColor == extractedColor,
                    isExtracted = true,
                    onClick = { onColorChanged(extractedColor) }
                )
            }

            // 预设色板
            presetColors.forEach { c ->
                ColorPresetItem(
                    color = c,
                    isSelected = selectedColor == c,
                    onClick = { onColorChanged(c) }
                )
            }
        }
    }
}

@Composable
private fun ColorPresetItem(
    color: Int?,
    isSelected: Boolean,
    isExtracted: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
            .border(if (isSelected) 2.5.dp else 1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Icon(
                imageVector = Icons.Default.FormatColorReset,
                contentDescription = "默认",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isSelected) {
            val isLight = ColorUtils.calculateLuminance(color) > 0.5
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isLight) Color.Black else Color.White
            )
        }
    }
}
