package com.letter.zeropwa.data.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.letter.zeropwa.data.AppPreferences
import com.letter.zeropwa.data.AppThemeState
import com.letter.zeropwa.data.PwaSortOrder
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.shortcut.PwaShortcutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PwaRepository private constructor(private val context: Context) {

    private val dbHelper = PwaDatabaseHelper(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pwaList = MutableStateFlow<List<PwaItem>>(emptyList())
    val pwaList: StateFlow<List<PwaItem>> = _pwaList.asStateFlow()

    init {
        refreshList()
        // 响应排序偏好变更
        scope.launch {
            AppThemeState.pwaSortOrder.collect {
                refreshList()
            }
        }
    }

    fun refreshList() {
        scope.launch {
            val sortOrder = AppPreferences(context).pwaSortOrder
            val list = dbHelper.getAll(sortOrder.sqlOrderBy)
            _pwaList.value = list
            // 桌面长按快捷方式始终保持高优先级规则 (置顶项 + 最近使用项)
            val dynamicCandidates = dbHelper.getAll("is_pinned_shortcut DESC, last_opened_at DESC, created_at DESC")
            PwaShortcutManager.updateDynamicShortcuts(context, dynamicCandidates)
        }
    }

    suspend fun getAll(): List<PwaItem> = withContext(Dispatchers.IO) {
        val sortOrder = AppPreferences(context).pwaSortOrder
        dbHelper.getAll(sortOrder.sqlOrderBy)
    }

    suspend fun getById(id: Long): PwaItem? = withContext(Dispatchers.IO) {
        dbHelper.getById(id)
    }

    suspend fun insert(item: PwaItem): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insert(item)
        if (id > 0) {
            refreshList()
        }
        id
    }

    suspend fun update(item: PwaItem): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.update(item) > 0
        if (success) {
            refreshList()
        }
        success
    }

    suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        val existing = dbHelper.getById(id)
        val success = dbHelper.delete(id) > 0
        if (success) {
            existing?.iconPath?.let { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) {}
            }
            refreshList()
        }
        success
    }

    suspend fun markOpened(id: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dbHelper.updateLastOpened(id, now)
        // 仅在当前排序偏好为"最近使用优先"时，打开应用才会前移；手动排序下位置保持不变
        refreshList()
    }

    suspend fun setPinnedToShortcuts(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dbHelper.updatePinnedShortcut(id, isPinned)
        refreshList()
    }

    /**
     * 手动将指定 PWA 上移一位
     */
    suspend fun moveUp(id: Long) = withContext(Dispatchers.IO) {
        ensureManualSortOrder()
        val currentList = _pwaList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            persistListOrder(currentList)
        }
    }

    /**
     * 手动将指定 PWA 下移一位
     */
    suspend fun moveDown(id: Long) = withContext(Dispatchers.IO) {
        ensureManualSortOrder()
        val currentList = _pwaList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index in 0 until currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            persistListOrder(currentList)
        }
    }

    /**
     * 手动将指定 PWA 置顶到最前
     */
    suspend fun moveToTop(id: Long) = withContext(Dispatchers.IO) {
        ensureManualSortOrder()
        val currentList = _pwaList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = currentList.removeAt(index)
            currentList.add(0, item)
            persistListOrder(currentList)
        }
    }

    private fun ensureManualSortOrder() {
        val prefs = AppPreferences(context)
        if (prefs.pwaSortOrder != PwaSortOrder.MANUAL) {
            AppThemeState.setSortOrder(context, PwaSortOrder.MANUAL)
        }
    }

    private fun persistListOrder(orderedList: List<PwaItem>) {
        val batch = orderedList.mapIndexed { idx, pwa -> pwa.id to idx + 1 }
        dbHelper.updateBatchSortOrders(batch)
        refreshList()
    }

    suspend fun saveIconBitmap(bitmap: Bitmap, pwaId: Long): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pwa_icons")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "icon_$pwaId.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }

    suspend fun updatePwaIconFromUrl(pwaId: Long, iconUrl: String): Boolean = withContext(Dispatchers.IO) {
        val bitmap = com.letter.zeropwa.data.parser.PwaManifestParser.downloadBitmap(context, iconUrl) ?: return@withContext false
        val localPath = saveIconBitmap(bitmap, pwaId)
        val current = dbHelper.getById(pwaId) ?: return@withContext false
        val updated = current.copy(iconPath = localPath, iconUrl = iconUrl)
        dbHelper.update(updated)
        refreshList()
        PwaShortcutManager.updateDynamicShortcuts(context, _pwaList.value)
        true
    }

    fun loadIconBitmap(iconPath: String?): Bitmap? {
        if (iconPath.isNullOrBlank()) return null
        return try {
            val file = File(iconPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: PwaRepository? = null

        fun getInstance(context: Context): PwaRepository {
            return instance ?: synchronized(this) {
                instance ?: PwaRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
