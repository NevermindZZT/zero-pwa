package com.letter.zeropwa.shortcut

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            // 关键：NEW_TASK + NEW_DOCUMENT 确保拥有系统级独立任务卡片
            // 默认单窗口模式：不加 MULTIPLE_TASK，再次点击自动带到前台已打开的窗口；
            // 若用户配置允许打开多个窗口，才添加 MULTIPLE_TASK
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
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "当前桌面启动器不支持添加桌面快捷方式", Toast.LENGTH_SHORT).show()
            return false
        }

        val launchIntent = createLaunchIntent(context, pwa.id, pwa.allowMultipleWindows)
        val repo = PwaRepository.getInstance(context)
        val bitmap = repo.loadIconBitmap(pwa.iconPath)

        val icon = if (bitmap != null) {
            IconCompat.createWithAdaptiveBitmap(bitmap)
        } else {
            IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        }

        val shortcutInfo = ShortcutInfoCompat.Builder(context, "pinned_pwa_${pwa.id}")
            .setShortLabel(pwa.effectiveShortName)
            .setLongLabel(pwa.displayName)
            .setIcon(icon)
            .setIntent(launchIntent)
            .setAlwaysBadged()
            .build()

        val callbackIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcutInfo)
        val successPendingIntent = PendingIntent.getBroadcast(
            context,
            pwa.id.toInt(),
            callbackIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val requested = ShortcutManagerCompat.requestPinShortcut(
            context,
            shortcutInfo,
            successPendingIntent.intentSender
        )

        if (requested) {
            Toast.makeText(context, "已发送添加快捷方式请求", Toast.LENGTH_SHORT).show()
        }
        return requested
    }

    /**
     * 更新长按应用图标弹出的动态快捷方式列表 (Dynamic Shortcuts)
     * 系统限制一般为 4~5 个
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
                val icon = if (bitmap != null) {
                    IconCompat.createWithAdaptiveBitmap(bitmap)
                } else {
                    IconCompat.createWithResource(context, R.mipmap.ic_launcher)
                }

                ShortcutInfoCompat.Builder(context, "dynamic_pwa_${pwa.id}")
                    .setShortLabel(pwa.effectiveShortName)
                    .setLongLabel(pwa.displayName)
                    .setIcon(icon)
                    .setIntent(createLaunchIntent(context, pwa.id, pwa.allowMultipleWindows))
                    .setRank(if (pwa.isPinnedToShortcuts) 0 else 1)
                    .build()
            }

            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {}
    }
}
