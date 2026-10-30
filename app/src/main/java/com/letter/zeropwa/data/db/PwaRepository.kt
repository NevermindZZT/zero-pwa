package com.letter.zeropwa.data.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    }

    private fun refreshList() {
        scope.launch {
            val list = dbHelper.getAll()
            _pwaList.value = list
            // 自动同步刷新长按桌面图标的 Dynamic Shortcuts
            PwaShortcutManager.updateDynamicShortcuts(context, list)
        }
    }

    suspend fun getAll(): List<PwaItem> = withContext(Dispatchers.IO) {
        dbHelper.getAll()
    }

    suspend fun getById(id: Long): PwaItem? = withContext(Dispatchers.IO) {
        dbHelper.getById(id)
    }

    suspend fun insert(item: PwaItem): Long = withContext(Dispatchers.IO) {
        val id = dbHelper.insert(item)
        if (id > 0) {
            val updated = dbHelper.getAll()
            _pwaList.value = updated
            PwaShortcutManager.updateDynamicShortcuts(context, updated)
        }
        id
    }

    suspend fun update(item: PwaItem): Boolean = withContext(Dispatchers.IO) {
        val success = dbHelper.update(item) > 0
        if (success) {
            val updated = dbHelper.getAll()
            _pwaList.value = updated
            PwaShortcutManager.updateDynamicShortcuts(context, updated)
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
            val updated = dbHelper.getAll()
            _pwaList.value = updated
            PwaShortcutManager.updateDynamicShortcuts(context, updated)
        }
        success
    }

    suspend fun markOpened(id: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dbHelper.updateLastOpened(id, now)
        val updated = dbHelper.getAll()
        _pwaList.value = updated
        PwaShortcutManager.updateDynamicShortcuts(context, updated)
    }

    suspend fun setPinnedToShortcuts(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dbHelper.updatePinnedShortcut(id, isPinned)
        val updated = dbHelper.getAll()
        _pwaList.value = updated
        PwaShortcutManager.updateDynamicShortcuts(context, updated)
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
