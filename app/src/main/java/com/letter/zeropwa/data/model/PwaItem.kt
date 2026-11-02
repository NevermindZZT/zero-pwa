package com.letter.zeropwa.data.model

data class PwaItem(
    val id: Long = 0L,
    val name: String,
    val shortName: String? = null,
    val url: String,
    val startUrl: String,
    val scope: String? = null,
    val iconPath: String? = null,
    val iconUrl: String? = null,
    val themeColor: Int? = null,
    val backgroundColor: Int? = null,
    val displayMode: String = DISPLAY_STANDALONE,
    val orientation: String = ORIENTATION_ANY,
    val userAgentMode: Int = UA_DEFAULT,
    val statusBarIconMode: Int = STATUS_BAR_ICON_AUTO,
    val enableFloatingToolHandle: Boolean = true,
    val enableSplashScreen: Boolean = true,
    val enableJavaScript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableImmersiveStatusBar: Boolean = true,
    val allowMultipleWindows: Boolean = false,
    val openExternalLinksInBrowser: Boolean = true,
    val isPinnedToShortcuts: Boolean = false,
    val sortOrder: Int = 0,
    val lastOpenedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = name.ifBlank { shortName ?: url }

    val effectiveShortName: String
        get() = shortName?.ifBlank { null } ?: name.ifBlank { url }

    val effectiveStartUrl: String
        get() = startUrl.ifBlank { url }

    companion object {
        const val DISPLAY_STANDALONE = "standalone"
        const val DISPLAY_FULLSCREEN = "fullscreen"
        const val DISPLAY_MINIMAL_UI = "minimal-ui"
        const val DISPLAY_BROWSER = "browser"

        const val ORIENTATION_ANY = "any"
        const val ORIENTATION_PORTRAIT = "portrait"
        const val ORIENTATION_LANDSCAPE = "landscape"

        const val UA_DEFAULT = 0
        const val UA_MOBILE = 1
        const val UA_DESKTOP = 2

        const val STATUS_BAR_ICON_AUTO = 0
        const val STATUS_BAR_ICON_DARK = 1
        const val STATUS_BAR_ICON_LIGHT = 2
    }
}
