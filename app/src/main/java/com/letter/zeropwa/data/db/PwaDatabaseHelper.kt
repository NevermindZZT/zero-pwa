package com.letter.zeropwa.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.letter.zeropwa.data.model.PwaItem

class PwaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_SHORT_NAME TEXT,
                $COL_URL TEXT NOT NULL,
                $COL_START_URL TEXT NOT NULL,
                $COL_SCOPE TEXT,
                $COL_ICON_PATH TEXT,
                $COL_ICON_URL TEXT,
                $COL_THEME_COLOR INTEGER,
                $COL_BACKGROUND_COLOR INTEGER,
                $COL_DISPLAY_MODE TEXT NOT NULL DEFAULT 'standalone',
                $COL_ORIENTATION TEXT NOT NULL DEFAULT 'any',
                $COL_USER_AGENT_MODE INTEGER NOT NULL DEFAULT 0,
                $COL_ENABLE_IMMERSIVE_STATUS_BAR INTEGER NOT NULL DEFAULT 1,
                $COL_ALLOW_MULTIPLE_WINDOWS INTEGER NOT NULL DEFAULT 0,
                $COL_OPEN_EXTERNAL_IN_BROWSER INTEGER NOT NULL DEFAULT 1,
                $COL_IS_PINNED_SHORTCUT INTEGER NOT NULL DEFAULT 0,
                $COL_LAST_OPENED_AT INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_ENABLE_IMMERSIVE_STATUS_BAR INTEGER NOT NULL DEFAULT 1")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_ALLOW_MULTIPLE_WINDOWS INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
        }
    }

    fun insert(item: PwaItem): Long {
        val db = writableDatabase
        val values = item.toContentValues(includeId = false)
        return db.insert(TABLE_NAME, null, values)
    }

    fun update(item: PwaItem): Int {
        val db = writableDatabase
        val values = item.toContentValues(includeId = false)
        return db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(item.id.toString()))
    }

    fun delete(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getById(id: Long): PwaItem? {
        val db = readableDatabase
        db.query(
            TABLE_NAME,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.toPwaItem()
            }
        }
        return null
    }

    fun getAll(): List<PwaItem> {
        val list = mutableListOf<PwaItem>()
        val db = readableDatabase
        db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COL_IS_PINNED_SHORTCUT DESC, $COL_LAST_OPENED_AT DESC, $COL_CREATED_AT DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toPwaItem())
            }
        }
        return list
    }

    fun updateLastOpened(id: Long, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_LAST_OPENED_AT, timestamp)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun updatePinnedShortcut(id: Long, isPinned: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_PINNED_SHORTCUT, if (isPinned) 1 else 0)
        }
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    private fun PwaItem.toContentValues(includeId: Boolean = false): ContentValues {
        return ContentValues().apply {
            if (includeId && id > 0) put(COL_ID, id)
            put(COL_NAME, name)
            put(COL_SHORT_NAME, shortName)
            put(COL_URL, url)
            put(COL_START_URL, startUrl)
            put(COL_SCOPE, scope)
            put(COL_ICON_PATH, iconPath)
            put(COL_ICON_URL, iconUrl)
            if (themeColor != null) put(COL_THEME_COLOR, themeColor) else putNull(COL_THEME_COLOR)
            if (backgroundColor != null) put(COL_BACKGROUND_COLOR, backgroundColor) else putNull(COL_BACKGROUND_COLOR)
            put(COL_DISPLAY_MODE, displayMode)
            put(COL_ORIENTATION, orientation)
            put(COL_USER_AGENT_MODE, userAgentMode)
            put(COL_ENABLE_IMMERSIVE_STATUS_BAR, if (enableImmersiveStatusBar) 1 else 0)
            put(COL_ALLOW_MULTIPLE_WINDOWS, if (allowMultipleWindows) 1 else 0)
            put(COL_OPEN_EXTERNAL_IN_BROWSER, if (openExternalLinksInBrowser) 1 else 0)
            put(COL_IS_PINNED_SHORTCUT, if (isPinnedToShortcuts) 1 else 0)
            put(COL_LAST_OPENED_AT, lastOpenedAt)
            put(COL_CREATED_AT, createdAt)
        }
    }

    private fun Cursor.toPwaItem(): PwaItem {
        val id = getLong(getColumnIndexOrThrow(COL_ID))
        val name = getString(getColumnIndexOrThrow(COL_NAME))
        val shortName = getStringOrNull(getColumnIndexOrThrow(COL_SHORT_NAME))
        val url = getString(getColumnIndexOrThrow(COL_URL))
        val startUrl = getString(getColumnIndexOrThrow(COL_START_URL))
        val scope = getStringOrNull(getColumnIndexOrThrow(COL_SCOPE))
        val iconPath = getStringOrNull(getColumnIndexOrThrow(COL_ICON_PATH))
        val iconUrl = getStringOrNull(getColumnIndexOrThrow(COL_ICON_URL))
        val themeColor = getIntOrNull(getColumnIndexOrThrow(COL_THEME_COLOR))
        val backgroundColor = getIntOrNull(getColumnIndexOrThrow(COL_BACKGROUND_COLOR))
        val displayMode = getString(getColumnIndexOrThrow(COL_DISPLAY_MODE))
        val orientation = getString(getColumnIndexOrThrow(COL_ORIENTATION))
        val userAgentMode = getInt(getColumnIndexOrThrow(COL_USER_AGENT_MODE))
        val enableImmersive = getIntOrNull(getColumnIndex(COL_ENABLE_IMMERSIVE_STATUS_BAR))?.let { it == 1 } ?: true
        val allowMultiple = getIntOrNull(getColumnIndex(COL_ALLOW_MULTIPLE_WINDOWS))?.let { it == 1 } ?: false
        val openExternalInBrowser = getInt(getColumnIndexOrThrow(COL_OPEN_EXTERNAL_IN_BROWSER)) == 1
        val isPinnedShortcut = getInt(getColumnIndexOrThrow(COL_IS_PINNED_SHORTCUT)) == 1
        val lastOpenedAt = getLong(getColumnIndexOrThrow(COL_LAST_OPENED_AT))
        val createdAt = getLong(getColumnIndexOrThrow(COL_CREATED_AT))

        return PwaItem(
            id = id,
            name = name,
            shortName = shortName,
            url = url,
            startUrl = startUrl,
            scope = scope,
            iconPath = iconPath,
            iconUrl = iconUrl,
            themeColor = themeColor,
            backgroundColor = backgroundColor,
            displayMode = displayMode,
            orientation = orientation,
            userAgentMode = userAgentMode,
            enableImmersiveStatusBar = enableImmersive,
            allowMultipleWindows = allowMultiple,
            openExternalLinksInBrowser = openExternalInBrowser,
            isPinnedToShortcuts = isPinnedShortcut,
            lastOpenedAt = lastOpenedAt,
            createdAt = createdAt
        )
    }

    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (columnIndex < 0 || isNull(columnIndex)) null else getString(columnIndex)
    }

    private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (columnIndex < 0 || isNull(columnIndex)) null else getInt(columnIndex)
    }

    companion object {
        const val DATABASE_NAME = "zero_pwa.db"
        const val DATABASE_VERSION = 2

        const val TABLE_NAME = "pwa_items"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_SHORT_NAME = "short_name"
        const val COL_URL = "url"
        const val COL_START_URL = "start_url"
        const val COL_SCOPE = "scope"
        const val COL_ICON_PATH = "icon_path"
        const val COL_ICON_URL = "icon_url"
        const val COL_THEME_COLOR = "theme_color"
        const val COL_BACKGROUND_COLOR = "background_color"
        const val COL_DISPLAY_MODE = "display_mode"
        const val COL_ORIENTATION = "orientation"
        const val COL_USER_AGENT_MODE = "user_agent_mode"
        const val COL_ENABLE_IMMERSIVE_STATUS_BAR = "enable_immersive_status_bar"
        const val COL_ALLOW_MULTIPLE_WINDOWS = "allow_multiple_windows"
        const val COL_OPEN_EXTERNAL_IN_BROWSER = "open_external_in_browser"
        const val COL_IS_PINNED_SHORTCUT = "is_pinned_shortcut"
        const val COL_LAST_OPENED_AT = "last_opened_at"
        const val COL_CREATED_AT = "created_at"
    }
}
