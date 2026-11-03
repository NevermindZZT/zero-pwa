package com.letter.zeropwa

import com.letter.zeropwa.shortcut.PwaShortcutManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaAdaptiveIconTest {

    @Test
    fun calculateAdaptiveInsetsEnforcesSafeRatioAndCentering() {
        // 画布尺寸 192，正方形原图 512x512
        val insets = PwaShortcutManager.calculateAdaptiveInsets(192, 512, 512, 0.68f)

        // 目标核心尺寸应为 192 * 0.68 = 131
        assertEquals(131, insets.width)
        assertEquals(131, insets.height)

        // 四边各留出 (192 - 131) / 2 = 30px 安全呼吸边距，彻底杜绝图标被放大和裁边
        assertEquals(30, insets.left)
        assertEquals(30, insets.top)
        assertEquals(161, insets.right)
        assertEquals(161, insets.bottom)
    }

    @Test
    fun calculateAdaptiveInsetsHandlesNonSquareAspectRatios() {
        // 宽长比不等的图片 (例如 400x200)
        val insets = PwaShortcutManager.calculateAdaptiveInsets(200, 400, 200, 0.70f)

        // 核心区域最大边应为 200 * 0.7 = 140
        assertEquals(140, insets.width)
        assertEquals(70, insets.height)

        // 左右边距 (200 - 140) / 2 = 30
        assertEquals(30, insets.left)
        assertEquals(170, insets.right)

        // 上下边距 (200 - 70) / 2 = 65
        assertEquals(65, insets.top)
        assertEquals(135, insets.bottom)
    }
}
