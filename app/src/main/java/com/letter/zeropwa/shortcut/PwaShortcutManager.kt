package com.letter.zeropwa.shortcut

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
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

    /**
     * 规范化快捷方式图标：按系统允许的最大尺寸安全缩放，避免超大位图触发 Binder 传输异常
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
                minOf(mw, mh).coerceIn(96, 256)
            } else {
                192
            }

            val scaled = if (originalBitmap.width > maxDim || originalBitmap.height > maxDim) {
                Bitmap.createScaledBitmap(originalBitmap, maxDim, maxDim, true)
            } else {
                originalBitmap
            }

            IconCompat.createWithAdaptiveBitmap(scaled)
        } catch (_: Throwable) {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }
    }
}
