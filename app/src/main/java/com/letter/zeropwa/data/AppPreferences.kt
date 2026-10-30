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

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_DEFAULT_IMMERSIVE = "key_default_immersive"
        private const val KEY_DEFAULT_ALLOW_MULTI = "key_default_allow_multi"
    }
}

object AppThemeState {
    private var initialized = false
    private val _themeMode = MutableStateFlow(AppThemeMode.FOLLOW_SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun init(context: Context) {
        if (!initialized) {
            val prefs = AppPreferences(context.applicationContext)
            _themeMode.value = prefs.themeMode
            _dynamicColor.value = prefs.isDynamicColorEnabled
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
}
