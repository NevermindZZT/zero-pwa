package com.letter.zeropwa.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.letter.zeropwa.data.db.PwaRepository
import com.letter.zeropwa.data.model.PwaItem
import com.letter.zeropwa.data.parser.PwaCandidate
import com.letter.zeropwa.data.parser.PwaManifestParser
import com.letter.zeropwa.shortcut.PwaShortcutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InspectState {
    data object Idle : InspectState
    data object Loading : InspectState
    data class Success(val candidate: PwaCandidate) : InspectState
    data class Error(val message: String) : InspectState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PwaRepository.getInstance(application)

    val pwaList: StateFlow<List<PwaItem>> = repository.pwaList

    private val _inspectState = MutableStateFlow<InspectState>(InspectState.Idle)
    val inspectState: StateFlow<InspectState> = _inspectState.asStateFlow()

    fun resetInspectState() {
        _inspectState.value = InspectState.Idle
    }

    fun inspectUrl(url: String) {
        if (url.isBlank()) {
            _inspectState.value = InspectState.Error("请输入网址")
            return
        }

        viewModelScope.launch {
            _inspectState.value = InspectState.Loading
            val result = PwaManifestParser.inspectUrl(url)
            result.fold(
                onSuccess = { candidate ->
                    _inspectState.value = InspectState.Success(candidate)
                },
                onFailure = { error ->
                    _inspectState.value = InspectState.Error(error.localizedMessage ?: "解析失败，请检查网址与网络")
                }
            )
        }
    }

    fun savePwa(
        candidate: PwaCandidate,
        customName: String? = null,
        customThemeColor: Int? = null,
        displayMode: String = "standalone",
        userAgentMode: Int = 0,
        statusBarIconMode: Int = PwaItem.STATUS_BAR_ICON_AUTO,
        enableFloatingToolHandle: Boolean = true,
        enableSplashScreen: Boolean = true,
        enableImmersiveStatusBar: Boolean = true,
        allowMultipleWindows: Boolean = false,
        openExternalInBrowser: Boolean = true,
        isPinnedToShortcuts: Boolean = false
    ) {
        viewModelScope.launch {
            val itemToInsert = PwaItem(
                name = customName?.ifBlank { null } ?: candidate.name,
                shortName = candidate.shortName,
                url = candidate.pageUrl,
                startUrl = candidate.startUrl,
                scope = candidate.scope,
                iconPath = null,
                iconUrl = candidate.iconUrl,
                themeColor = customThemeColor ?: candidate.themeColor,
                backgroundColor = candidate.backgroundColor,
                displayMode = displayMode,
                orientation = candidate.orientation,
                userAgentMode = userAgentMode,
                statusBarIconMode = statusBarIconMode,
                enableFloatingToolHandle = enableFloatingToolHandle,
                enableSplashScreen = enableSplashScreen,
                enableImmersiveStatusBar = enableImmersiveStatusBar,
                allowMultipleWindows = allowMultipleWindows,
                openExternalLinksInBrowser = openExternalInBrowser,
                isPinnedToShortcuts = isPinnedToShortcuts
            )

            val insertedId = repository.insert(itemToInsert)
            if (insertedId > 0 && !candidate.iconUrl.isNullOrBlank()) {
                // 后台下载并持久化保存图标文件 (支持 SVG/PNG/WebP/ICO 等)
                val bitmap = PwaManifestParser.downloadBitmap(getApplication(), candidate.iconUrl)
                if (bitmap != null) {
                    val localPath = repository.saveIconBitmap(bitmap, insertedId)
                    val updatedItem = itemToInsert.copy(id = insertedId, iconPath = localPath)
                    repository.update(updatedItem)
                }
            }
            resetInspectState()
        }
    }

    fun updatePwa(item: PwaItem) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun deletePwa(pwaId: Long) {
        viewModelScope.launch {
            repository.delete(pwaId)
        }
    }

    fun togglePinnedToShortcuts(item: PwaItem) {
        viewModelScope.launch {
            repository.setPinnedToShortcuts(item.id, !item.isPinnedToShortcuts)
        }
    }

    fun createPinnedShortcut(context: Context, item: PwaItem) {
        PwaShortcutManager.createPinnedShortcut(context, item)
    }

    fun movePwaUp(pwaId: Long) {
        viewModelScope.launch {
            repository.moveUp(pwaId)
        }
    }

    fun movePwaDown(pwaId: Long) {
        viewModelScope.launch {
            repository.moveDown(pwaId)
        }
    }

    fun movePwaToTop(pwaId: Long) {
        viewModelScope.launch {
            repository.moveToTop(pwaId)
        }
    }

    fun setSortOrder(order: com.letter.zeropwa.data.PwaSortOrder) {
        com.letter.zeropwa.data.AppThemeState.setSortOrder(getApplication(), order)
    }

    fun launchPwa(context: Context, pwaId: Long, allowMultipleWindows: Boolean? = null) {
        val allowMulti = allowMultipleWindows ?: pwaList.value.find { it.id == pwaId }?.allowMultipleWindows ?: false
        val intent = PwaShortcutManager.createLaunchIntent(context, pwaId, allowMulti)
        context.startActivity(intent)
    }
}
