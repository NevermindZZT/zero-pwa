package com.letter.zeropwa.shortcut

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.letter.zeropwa.R
import com.letter.zeropwa.data.db.PwaRepository
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.ui.container.PwaContainerActivity

object PwaShortcutManager {

    const val URI_SCHEME = "zeropwa"
    const val URI_HOST = "pwa"
    const val EXTRA_PWA_ID = "extra_pwa_id"

    fun createLaunchIntent(context: Context, pwaId: Long, allowMultipleWindows: Boolean = false): Intent {
        return Intent(context, PwaContainerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("$URI_SCHEME://$URI_HOST/$pwaId")
            var flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            if (allowMultipleWindows) {
                flags = flags or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }
            this.flags = flags
            putExtra(EXTRA_PWA_ID, pwaId)
        }
    }

    /**
     * 创建桌面快捷方式 (Pinned Shortcut)
     */
    fun createPinnedShortcut(context: Context, pwa: PwaItem): Boolean {
        try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                Toast.makeText(context, "当前桌面启动器不支持添加桌面快捷方式", Toast.LENGTH_SHORT).show()
                return false
            }

            val launchIntent = createLaunchIntent(context, pwa.id, pwa.allowMultipleWindows)
            val repo = PwaRepository.getInstance(context)
            val bitmap = repo.loadIconBitmap(pwa.iconPath)
            val icon = createShortcutIcon(context, bitmap)

            val shortcutInfo = ShortcutInfoCompat.Builder(context, "pinned_pwa_${pwa.id}")
                .setShortLabel(pwa.effectiveShortName)
                .setLongLabel(pwa.displayName)
                .setIcon(icon)
                .setIntent(launchIntent)
                .setAlwaysBadged()
                .build()

            // 彻底解决 Android 14+ (Target SDK 34+) 的 PendingIntent 安全限制闪退问题：
            // Android 官方规范：无需回调时传入 null，由系统桌面直接唤起原生的添加快捷方式确认对话框
            val requested = ShortcutManagerCompat.requestPinShortcut(
                context,
                shortcutInfo,
                null
            )

            if (requested) {
                Toast.makeText(context, context.getString(R.string.shortcut_toast_requested), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.shortcut_toast_desktop_unresponsive), Toast.LENGTH_LONG).show()
            }
            return requested
        } catch (se: SecurityException) {
            Toast.makeText(context, context.getString(R.string.shortcut_toast_permission), Toast.LENGTH_LONG).show()
            return false
        } catch (e: Throwable) {
            Toast.makeText(context, e.message.orEmpty(), Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * 更新长按应用图标弹出的动态快捷方式列表 (Dynamic Shortcuts)
     */
    fun updateDynamicShortcuts(context: Context, pwaList: List<PwaItem>) {
        try {
            val maxCount = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            val sortedList = pwaList.sortedWith(
                compareByDescending<PwaItem> { it.isPinnedToShortcuts }
                    .thenByDescending { it.lastOpenedAt }
                    .thenByDescending { it.createdAt }
            ).take(maxCount)

            val repo = PwaRepository.getInstance(context)
            val shortcuts = sortedList.map { pwa ->
                val bitmap = repo.loadIconBitmap(pwa.iconPath)
                val icon = createShortcutIcon(context, bitmap)

                ShortcutInfoCompat.Builder(context, "dynamic_pwa_${pwa.id}")
                    .setShortLabel(pwa.effectiveShortName)
                    .setLongLabel(pwa.displayName)
                    .setIcon(icon)
                    .setIntent(createLaunchIntent(context, pwa.id, pwa.allowMultipleWindows))
                    .setRank(if (pwa.isPinnedToShortcuts) 0 else 1)
                    .build()
            }

            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Throwable) {}
    }

    data class AdaptiveRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    /**
     * 计算自适应安全区视口坐标，严格将原图等比限制在 68% 的黄金视口内，留出 16% 四边呼吸边距
     */
    fun calculateAdaptiveInsets(canvasSize: Int, srcWidth: Int, srcHeight: Int, safeRatio: Float = 0.68f): AdaptiveRect {
        val safeAreaSize = (canvasSize * safeRatio).roundToInt().coerceAtLeast(1)
        val scale = minOf(safeAreaSize.toFloat() / srcWidth, safeAreaSize.toFloat() / srcHeight)
        val dstW = (srcWidth * scale).roundToInt().coerceAtLeast(1)
        val dstH = (srcHeight * scale).roundToInt().coerceAtLeast(1)
        val dstLeft = (canvasSize - dstW) / 2
        val dstTop = (canvasSize - dstH) / 2
        return AdaptiveRect(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH)
    }

    /**
     * 构建符合 Android 官方标准的自适应图标画布：
     * 1. 彻底解决桌面图标被强行放大 1.5 倍的问题：严格将原图标置于 68% 中心安全区，留出呼吸边距；
     * 2. 彻底解决透明图标在 Launcher 变成纯黑块的问题：铺设纯净防黑底板 (纯白)，通透锐利。
     */
    fun buildAdaptiveIconBitmap(originalBitmap: Bitmap, targetCanvasSize: Int): Bitmap {
        val canvasSize = targetCanvasSize.coerceIn(108, 384)
        val output = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 铺设计算后的不透明安全底色 (纯白，杜绝任何 Alpha 穿透引发的 Launcher 黑色清屏)
        canvas.drawColor(AndroidColor.WHITE)

        val insets = calculateAdaptiveInsets(canvasSize, originalBitmap.width, originalBitmap.height)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val srcRect = Rect(0, 0, originalBitmap.width, originalBitmap.height)
        val dstRect = Rect(insets.left, insets.top, insets.right, insets.bottom)
        canvas.drawBitmap(originalBitmap, srcRect, dstRect, paint)

        return output
    }

    /**
     * 规范化快捷方式图标：构建自适应底板并按系统允许尺寸安全返回
     */
    private fun createShortcutIcon(context: Context, originalBitmap: Bitmap?): IconCompat {
        if (originalBitmap == null) {
            return IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }

        return try {
            val maxDim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val sm = context.getSystemService(ShortcutManager::class.java)
                val mw = sm?.iconMaxWidth ?: 192
                val mh = sm?.iconMaxHeight ?: 192
                minOf(mw, mh).coerceIn(108, 256)
            } else {
                192
            }

            val adaptiveBitmap = buildAdaptiveIconBitmap(originalBitmap, maxDim)
            IconCompat.createWithAdaptiveBitmap(adaptiveBitmap)
        } catch (_: Throwable) {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }
    }
}
