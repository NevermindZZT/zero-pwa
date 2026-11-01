package com.letter.zeropwa.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
}

enum class PwaSortOrder(val label: String, val sqlOrderBy: String) {
    MANUAL("自定义排序", "sort_order ASC, created_at DESC"),
    LAST_OPENED("最近使用优先", "last_opened_at DESC, created_at DESC"),
    NAME("应用名称 (A-Z)", "name COLLATE NOCASE ASC"),
    CREATED_AT("创建时间优先", "created_at DESC")
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zero_pwa_preferences", Context.MODE_PRIVATE)

    var themeMode: AppThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.FOLLOW_SYSTEM.name)
            return try {
                AppThemeMode.valueOf(name ?: AppThemeMode.FOLLOW_SYSTEM.name)
            } catch (_: Exception) {
                AppThemeMode.FOLLOW_SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var isDynamicColorEnabled: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var defaultImmersiveStatusBar: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_IMMERSIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_IMMERSIVE, value).apply()

    var defaultAllowMultipleWindows: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_ALLOW_MULTI, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_ALLOW_MULTI, value).apply()

    var defaultFloatingToolHandle: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_FLOATING_HANDLE, true)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_FLOATING_HANDLE, value).apply()

    var pwaSortOrder: PwaSortOrder
        get() {
            val name = prefs.getString(KEY_PWA_SORT_ORDER, PwaSortOrder.MANUAL.name)
            return try {
                PwaSortOrder.valueOf(name ?: PwaSortOrder.MANUAL.name)
            } catch (_: Exception) {
                PwaSortOrder.MANUAL
            }
        }
        set(value) = prefs.edit().putString(KEY_PWA_SORT_ORDER, value.name).apply()

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_DEFAULT_IMMERSIVE = "key_default_immersive"
        private const val KEY_DEFAULT_ALLOW_MULTI = "key_default_allow_multi"
        private const val KEY_DEFAULT_FLOATING_HANDLE = "key_default_floating_handle"
        private const val KEY_PWA_SORT_ORDER = "key_pwa_sort_order"
    }
}

object AppThemeState {
    private var initialized = false
    private val _themeMode = MutableStateFlow(AppThemeMode.FOLLOW_SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _pwaSortOrder = MutableStateFlow(PwaSortOrder.MANUAL)
    val pwaSortOrder: StateFlow<PwaSortOrder> = _pwaSortOrder.asStateFlow()

    fun init(context: Context) {
        if (!initialized) {
            val prefs = AppPreferences(context.applicationContext)
            _themeMode.value = prefs.themeMode
            _dynamicColor.value = prefs.isDynamicColorEnabled
            _pwaSortOrder.value = prefs.pwaSortOrder
            initialized = true
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = AppPreferences(context.applicationContext)
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        val prefs = AppPreferences(context.applicationContext)
        prefs.isDynamicColorEnabled = enabled
        _dynamicColor.value = enabled
    }

    fun setSortOrder(context: Context, order: PwaSortOrder) {
        val prefs = AppPreferences(context.applicationContext)
        prefs.pwaSortOrder = order
        _pwaSortOrder.value = order
    }
}
