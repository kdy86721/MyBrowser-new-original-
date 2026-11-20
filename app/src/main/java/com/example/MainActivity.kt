package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.core.AiChatTurn
import com.example.core.AiCliHttpClient
import com.example.core.AiCliProfile
import com.example.core.BookmarkEntry
import com.example.core.BrowserCoreBridge
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.core.BrowserTabState
import com.example.core.HistoryEntry
import com.example.core.LocalKotlinCore
import com.example.core.MissingApiKeyException
import com.example.core.MissingModelSelectionException
import com.example.core.QuickToggleAction
import com.example.core.QuickToggleLayoutStyle
import com.example.core.SearchEngine
import com.example.core.ToggleIndicatorStyle
import com.example.core.ToolbarAction
import com.example.ui.components.FloatingControlBar
import com.example.ui.components.FoldablePostureUiState
import com.example.ui.components.QuickToggleDropdownMenu
import com.example.ui.screens.AiChatSheet
import com.example.ui.screens.AiChatUiState
import com.example.ui.screens.AiConnectionStatus
import com.example.ui.screens.GridTabSwitcherScreen
import com.example.ui.screens.HistoryAndBookmarksSheet
import com.example.ui.screens.PageSummarySheet
import com.example.ui.screens.PageSummaryState
import com.example.ui.screens.SettingsSheet
import com.example.storage.BrowserStateStorage
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MyBrowserTheme
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PureFrost
import com.example.webview.BLOB_DOWNLOAD_JS_BRIDGE_NAME
import com.example.webview.BlobDownloadBridge
import com.example.webview.WebViewExternalNavigator
import com.example.webview.WebViewWarmupPool
import com.example.webview.dispatchWebViewDownload
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

@Serializable
private data class DomSummaryPayload(
    val title: String = "",
    val url: String = "",
    val description: String = "",
    val headings: List<String> = emptyList(),
    val sentences: List<String> = emptyList(),
    val wordCount: Int = 0,
    val bodyText: String = "",
)

private enum class PushOverlay {
    NONE,
    SETTINGS,
    HISTORY,
    SUMMARY,
    AI_CHAT,
}

private class NonObservableFloatRef(var value: Float = 0f)

@Stable
private class PaneUiState {
    var isEditingAddress by mutableStateOf(false)
    var isTabDrawerOpen by mutableStateOf(false)
    var isFindInPageOpen by mutableStateOf(false)
    var findQuery by mutableStateOf("")
    var rawScrollCollapsed by mutableStateOf(false)
    val scrollDeltaRef = NonObservableFloatRef(0f)

    fun resetScrollCollapse() {
        rawScrollCollapsed = false
        scrollDeltaRef.value = 0f
    }

    fun closeTransientPopups() {
        isTabDrawerOpen = false
        isFindInPageOpen = false
        isEditingAddress = false
        resetScrollCollapse()
    }

    fun onScrollDelta(dy: Int, scrollY: Int, isScrollable: Boolean) {
        if (scrollY <= 0 || !isScrollable) {
            scrollDeltaRef.value = 0f
            rawScrollCollapsed = false
        } else {
            val fdy = dy.toFloat()
            if ((fdy > 0f && scrollDeltaRef.value < 0f) ||
                (fdy < 0f && scrollDeltaRef.value > 0f)
            ) {
                scrollDeltaRef.value = fdy
            } else {
                scrollDeltaRef.value += fdy
            }
            if (scrollDeltaRef.value > 36f) {
                rawScrollCollapsed = true
                scrollDeltaRef.value = 0f
            } else if (scrollDeltaRef.value < -36f) {
                rawScrollCollapsed = false
                scrollDeltaRef.value = 0f
            }
        }
    }
}

private const val MAX_LIVE_WEBVIEWS = 4

class BrowserViewModel(
    private val core: BrowserCoreBridge = LocalKotlinCore(),
    private val storage: BrowserStateStorage? = null,
    legacyStateJson: String? = null,
) : ViewModel() {

    val snapshotFlow: StateFlow<BrowserCoreSnapshot> = core.snapshotFlow

    private val _aiConnectionStatus = MutableStateFlow<AiConnectionStatus?>(null)
    val aiConnectionStatusFlow: StateFlow<AiConnectionStatus?> = _aiConnectionStatus.asStateFlow()

    private val persistMutex = Mutex()

    // Retained per-tab WebView state Bundles across configuration / activity restarts & process death
    private val savedTabBundles = mutableMapOf<String, Bundle>()

    init {
        val diskJson = storage?.loadConfigJson()
        val initialJson = diskJson ?: legacyStateJson
        if (!initialJson.isNullOrBlank()) {
            core.importStateJson(initialJson)
            if (diskJson == null) {
                storage?.saveConfigJson(core.exportStateJson())
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            core.snapshotFlow.drop(1).collect { snap ->
                persistMutex.withLock {
                    storage?.saveConfigJson(core.exportStateJson())
                    val nonIncognitoIds = snap.tabs
                        .filterNot { it.isIncognito }
                        .map { it.id }
                        .toSet()
                    storage?.pruneStaleTabStates(nonIncognitoIds)
                }
            }
        }
    }

    fun persistConfigSync() {
        storage?.saveConfigJson(core.exportStateJson())
    }

    fun saveTabBundle(tabId: String, bundle: Bundle) {
        val isIncognito = core.getSnapshot().tabs.firstOrNull { it.id == tabId }?.isIncognito == true
        savedTabBundles[tabId] = bundle
        if (!isIncognito) {
            storage?.saveTabWebViewState(tabId, bundle)
        }
    }

    fun getTabBundle(tabId: String): Bundle? {
        savedTabBundles[tabId]?.let { return it }
        val isIncognito = core.getSnapshot().tabs.firstOrNull { it.id == tabId }?.isIncognito == true
        if (isIncognito) return null
        return storage?.loadTabWebViewState(tabId)?.also { restored ->
            savedTabBundles[tabId] = restored
        }
    }

    fun removeTabBundle(tabId: String) {
        savedTabBundles.remove(tabId)
        storage?.deleteTabWebViewState(tabId)
    }

    fun restoreSerializedState(json: String?) {
        if (!json.isNullOrBlank()) {
            core.importStateJson(json)
        }
    }

    fun exportSerializedState(): String = core.exportStateJson()

    fun resolveUrlOrSearch(rawInput: String): String = core.normalizeOrSearch(rawInput)

    fun formatDisplayUrl(url: String): String = core.formatDisplayUrl(url)

    fun shouldBlockUrl(url: String): Boolean = core.shouldBlockUrl(url)

    fun createTab(
        initialUrl: String? = null,
        isIncognito: Boolean = false,
    ): BrowserCoreSnapshot = core.createTab(
        initialUrl = initialUrl,
        activate = true,
        isIncognito = isIncognito,
    )

    fun switchTab(tabId: String): BrowserCoreSnapshot = core.switchTab(tabId)

    fun setSecondaryTab(tabId: String?): BrowserCoreSnapshot = core.setSecondaryTab(tabId)

    fun ensureSecondaryTabForDualPane(): BrowserCoreSnapshot = core.ensureSecondaryTabForDualPane()

    fun closeTab(tabId: String): BrowserCoreSnapshot {
        removeTabBundle(tabId)
        return core.closeTab(tabId)
    }

    fun createTabGroup(
        title: String,
        tabIds: List<String>,
        colorIndex: Int = 0,
    ): BrowserCoreSnapshot = core.createTabGroup(
        title = title,
        tabIds = tabIds,
        colorIndex = colorIndex,
    )

    fun addTabToGroup(groupId: String, tabId: String): BrowserCoreSnapshot =
        core.addTabToGroup(groupId, tabId)

    fun removeTabFromGroup(groupId: String, tabId: String): BrowserCoreSnapshot =
        core.removeTabFromGroup(groupId, tabId)

    fun ungroupTabGroup(groupId: String): BrowserCoreSnapshot =
        core.ungroupTabGroup(groupId)

    fun updateTabNavigation(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        progress: Int,
    ) {
        core.updateTabNavigation(
            tabId = tabId,
            url = url,
            title = title,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            isLoading = isLoading,
            progress = progress,
        )
    }

    fun recordVisit(url: String, title: String) {
        core.recordHistoryVisit(
            url = url,
            title = title,
            visitedAtEpochMs = System.currentTimeMillis(),
        )
    }

    fun getHistory(limit: Int = 100): List<HistoryEntry> = core.getHistory(limit)

    fun toggleBookmark(url: String, title: String) {
        core.toggleBookmark(
            url = url,
            title = title,
            savedAtEpochMs = System.currentTimeMillis(),
        )
    }

    fun toggleDesktopMode(tabId: String) {
        core.toggleTabDesktopMode(tabId)
    }

    fun toggleAdBlock() {
        core.toggleAdBlock()
    }

    fun setSearchEngine(engine: SearchEngine) {
        core.setSearchEngine(engine)
    }

    fun setHomeUrl(rawUrl: String) {
        core.setHomeUrl(rawUrl)
    }

    fun setScrollToPillEnabled(enabled: Boolean) {
        core.setScrollToPillEnabled(enabled)
    }

    fun setExpandBarOnAddressEdit(enabled: Boolean) {
        core.setExpandBarOnAddressEdit(enabled)
    }

    fun setTabSwitcherStyle(style: Int) {
        core.setTabSwitcherStyle(style)
    }

    fun updateBarGeometry(
        cornerRadiusDp: Float? = null,
        heightDp: Float? = null,
        widthFraction: Float? = null,
        bottomOffsetDp: Float? = null,
        horizontalBias: Float? = null,
        glassOpacity: Float? = null,
    ) {
        core.updateBarGeometry(
            cornerRadiusDp = cornerRadiusDp,
            heightDp = heightDp,
            widthFraction = widthFraction,
            bottomOffsetDp = bottomOffsetDp,
            horizontalBias = horizontalBias,
            glassOpacity = glassOpacity,
        )
    }

    fun toggleQuickToggleAction(action: QuickToggleAction) {
        val current = core.getSnapshot().preferences.customQuickToggles
        val updated = if (action in current) current - action else current + action
        core.setCustomQuickToggles(updated)
    }

    fun moveQuickToggleAction(action: QuickToggleAction, delta: Int) {
        core.moveQuickToggle(action, delta)
    }

    fun updateDropdownCustomization(
        layoutStyle: QuickToggleLayoutStyle? = null,
        maxHeightDp: Float? = null,
        indicatorStyle: ToggleIndicatorStyle? = null,
    ) {
        core.updateDropdownCustomization(
            layoutStyle = layoutStyle,
            maxHeightDp = maxHeightDp,
            indicatorStyle = indicatorStyle,
        )
    }

    fun updateFoldableSettings(
        dualPaneEnabled: Boolean? = null,
        hingeAvoidanceDp: Float? = null,
        separateBarProfiles: Boolean? = null,
        unfoldedBarWidthFraction: Float? = null,
        unfoldedBarHorizontalBias: Float? = null,
        autoExpandToolbarWhenUnfolded: Boolean? = null,
        longPressSwipeSplitEnabled: Boolean? = null,
        splitCompanionMode: Int? = null,
    ) {
        core.updateFoldableSettings(
            dualPaneEnabled = dualPaneEnabled,
            hingeAvoidanceDp = hingeAvoidanceDp,
            separateBarProfiles = separateBarProfiles,
            unfoldedBarWidthFraction = unfoldedBarWidthFraction,
            unfoldedBarHorizontalBias = unfoldedBarHorizontalBias,
            autoExpandToolbarWhenUnfolded = autoExpandToolbarWhenUnfolded,
            longPressSwipeSplitEnabled = longPressSwipeSplitEnabled,
            splitCompanionMode = splitCompanionMode,
        )
    }

    fun splitTabForFoldable(sourceTabId: String? = null, placeSourceOnLeft: Boolean): BrowserCoreSnapshot =
        core.splitTabForFoldable(
            sourceTabId = sourceTabId,
            placeSourceOnLeft = placeSourceOnLeft,
        )

    fun closeSplitPane(keepLeftPane: Boolean): BrowserCoreSnapshot =
        core.closeSplitPane(keepLeftPane = keepLeftPane)

    fun createTabForPane(
        paneIndex: Int,
        initialUrl: String? = null,
        isIncognito: Boolean = false,
    ): BrowserCoreSnapshot = core.createTabForPane(
        paneIndex = paneIndex,
        initialUrl = initialUrl,
        isIncognito = isIncognito,
    )

    fun assignTabToPane(paneIndex: Int, tabId: String): BrowserCoreSnapshot =
        core.assignTabToPane(paneIndex = paneIndex, tabId = tabId)

    fun resetFoldableSettings() {
        core.resetFoldableSettings()
    }

    fun updateGestureSettings(
        gesturesEnabled: Boolean? = null,
        swipeUpAction: Int? = null,
        swipeDownAction: Int? = null,
        swipeLeftRightTabSwitch: Boolean? = null,
        doubleTapAction: Int? = null,
        gestureSensitivityDp: Float? = null,
        longPressSwipeUpAiChatEnabled: Boolean? = null,
    ) {
        core.updateGestureSettings(
            gesturesEnabled = gesturesEnabled,
            swipeUpAction = swipeUpAction,
            swipeDownAction = swipeDownAction,
            swipeLeftRightTabSwitch = swipeLeftRightTabSwitch,
            doubleTapAction = doubleTapAction,
            gestureSensitivityDp = gestureSensitivityDp,
            longPressSwipeUpAiChatEnabled = longPressSwipeUpAiChatEnabled,
        )
    }

    fun resetGestureSettings() {
        core.resetGestureSettings()
    }

    fun selectAiProfile(profileId: String) {
        core.selectAiProfile(profileId)
    }

    fun testConnectionAndFetchModels(profile: AiCliProfile) {
        viewModelScope.launch {
            _aiConnectionStatus.value = AiConnectionStatus(
                profileId = profile.id,
                isChecking = true,
                isConnected = false,
                modelCount = profile.availableModels.size,
                statusMessage = "${profile.name} 연결 확인 및 모델 목록 불러오는 중...",
                isError = false,
            )
            val result = AiCliHttpClient.fetchAvailableModels(profile)
            result.onSuccess { models ->
                core.setAiProfileModels(profile.id, models)
                _aiConnectionStatus.value = AiConnectionStatus(
                    profileId = profile.id,
                    isChecking = false,
                    isConnected = true,
                    modelCount = models.size,
                    statusMessage = "연결됨 · ${models.size}개 모델 불러옴",
                    isError = false,
                )
            }.onFailure { err ->
                val message = when (err) {
                    is MissingApiKeyException -> "API 키를 입력하면 연결 확인 및 모델 목록을 불러옵니다."
                    else -> err.message ?: "연결 확인 및 모델 목록 조회에 실패했습니다."
                }
                _aiConnectionStatus.value = AiConnectionStatus(
                    profileId = profile.id,
                    isChecking = false,
                    isConnected = false,
                    modelCount = profile.availableModels.size,
                    statusMessage = message,
                    isError = err !is MissingApiKeyException,
                )
            }
        }
    }

    fun updateAiProfile(
        profileId: String,
        name: String? = null,
        endpointOrCommand: String? = null,
        apiKey: String? = null,
        selectedModel: String? = null,
        addCustomModel: String? = null,
        systemPrompt: String? = null,
        providerSubtype: String? = null,
    ) {
        core.updateAiProfile(
            profileId = profileId,
            name = name,
            endpointOrCommand = endpointOrCommand,
            apiKey = apiKey,
            selectedModel = selectedModel,
            addCustomModel = addCustomModel,
            systemPrompt = systemPrompt,
            providerSubtype = providerSubtype,
        )
    }

    fun addCustomAiProfile(
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
    ) {
        core.addCustomAiProfile(
            name = name,
            cliType = cliType,
            endpointOrCommand = endpointOrCommand,
            apiKey = apiKey,
            selectedModel = selectedModel,
        )
    }

    fun deleteAiProfile(profileId: String) {
        core.deleteAiProfile(profileId)
    }

    fun resetBarCustomization() {
        core.resetBarCustomization()
    }

    fun updateSafeAreaPadding(topDp: Float? = null, bottomDp: Float? = null) {
        core.updateSafeAreaPadding(topDp = topDp, bottomDp = bottomDp)
    }

    fun resetSafeAreaPadding() {
        core.resetSafeAreaPadding()
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        core.setJavaScriptEnabled(enabled)
    }

    fun setImagesBlocked(blocked: Boolean) {
        core.setImagesBlocked(blocked)
    }

    fun setAlgorithmicDarkening(enabled: Boolean) {
        core.setAlgorithmicDarkening(enabled)
    }

    fun toggleLeftToolbarAction(action: ToolbarAction) {
        val current = core.getSnapshot().preferences.toolbarConfig
        val updatedLeft = if (action in current.leftActions) {
            current.leftActions - action
        } else {
            (current.leftActions + action).takeLast(3)
        }
        core.setToolbarActions(left = updatedLeft, right = current.rightActions)
    }

    fun toggleRightToolbarAction(action: ToolbarAction) {
        val current = core.getSnapshot().preferences.toolbarConfig
        val updatedRight = if (action in current.rightActions) {
            current.rightActions - action
        } else {
            (current.rightActions + action).takeLast(3)
        }
        core.setToolbarActions(left = current.leftActions, right = updatedRight)
    }

    fun resetToolbarDefaults() {
        core.resetToolbarDefaults()
    }

    fun clearCoreHistory() {
        core.clearHistory()
    }

    override fun onCleared() {
        savedTabBundles.clear()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {

    private val prefsName = "mybrowser_bm_core_prefs"
    private val prefsKeyState = "core_state_json"

    private var viewModelRef: BrowserViewModel? = null
    private var saveAllWebViewsHook: (() -> Unit)? = null
    private var trimMemoryHook: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        WebViewWarmupPool.schedulePrewarm(applicationContext)

        val legacyStateJson = getSharedPreferences(prefsName, MODE_PRIVATE)
            .getString(prefsKeyState, null)
        val stateStorage = BrowserStateStorage(applicationContext)

        // Synchronously restore saved state BEFORE the first Compose frame renders
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BrowserViewModel(
                    storage = stateStorage,
                    legacyStateJson = legacyStateJson,
                ) as T
            }
        }
        val browserViewModel = ViewModelProvider(this, factory)[BrowserViewModel::class.java]
        viewModelRef = browserViewModel

        setContent {
            MyBrowserTheme {
                MyBrowserRootScreen(
                    viewModel = browserViewModel,
                    onRegisterWebViewPersistenceHook = { hook ->
                        saveAllWebViewsHook = hook
                    },
                    onRegisterTrimMemoryHook = { hook ->
                        trimMemoryHook = hook
                    },
                )
            }
        }
    }

    private fun persistFullBrowserState() {
        saveAllWebViewsHook?.invoke()
        viewModelRef?.let { vm ->
            vm.persistConfigSync()
            getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .putString(prefsKeyState, vm.exportSerializedState())
                .apply()
        }
    }

    override fun onPause() {
        persistFullBrowserState()
        super.onPause()
    }

    override fun onStop() {
        persistFullBrowserState()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        persistFullBrowserState()
        super.onSaveInstanceState(outState)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            trimMemoryHook?.invoke(level)
        }
    }
}

@Composable
private fun MyBrowserRootScreen(
    viewModel: BrowserViewModel,
    onRegisterWebViewPersistenceHook: ((() -> Unit)?) -> Unit = {},
    onRegisterTrimMemoryHook: (((Int) -> Unit)?) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val snapshotState = viewModel.snapshotFlow.collectAsStateWithLifecycle()
    val snapshot = snapshotState.value
    val prefs = snapshot.preferences
    val aiConnectionStatus by viewModel.aiConnectionStatusFlow.collectAsStateWithLifecycle()

    var windowLayoutInfo by remember { mutableStateOf<WindowLayoutInfo?>(null) }
    DisposableEffect(activity) {
        if (activity == null) {
            onDispose {}
        } else {
            val job = activity.lifecycleScope.launch {
                WindowInfoTracker.getOrCreate(activity)
                    .windowLayoutInfo(activity)
                    .collect { info -> windowLayoutInfo = info }
            }
            onDispose { job.cancel() }
        }
    }

    val leftPaneUi = remember { PaneUiState() }
    val rightPaneUi = remember { PaneUiState() }
    var gridTabSwitcherPaneIndex by remember { mutableStateOf<Int?>(null) }
    var quickMenuPaneIndex by remember { mutableStateOf<Int?>(null) }
    var focusedPaneIndex by remember { mutableIntStateOf(0) }

    val isQuickMenuOpen = quickMenuPaneIndex != null
    val isGridTabSwitcherOpen = gridTabSwitcherPaneIndex != null

    var activeOverlay by remember { mutableStateOf(PushOverlay.NONE) }
    var openSettingsDirectlyToAi by remember { mutableStateOf(false) }
    var pageSummaryState by remember { mutableStateOf(PageSummaryState()) }
    var aiChatUiState by remember { mutableStateOf(AiChatUiState()) }

    // Fullscreen HTML5 <video> custom view state
    var fullscreenVideoView by remember { mutableStateOf<View?>(null) }
    var fullscreenVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // SSL/TLS certificate error confirmation state (strictly defaults to cancel unless user explicitly proceeds)
    var pendingSslError by remember { mutableStateOf<PendingSslError?>(null) }

    // <input type="file"> chooser launcher via SAF (zero storage permissions needed; supports multiple & MIME types)
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            extractChosenFileUris(result.data)
        } else {
            null
        }
        callback?.onReceiveValue(uris)
    }

    // WebRTC / Media (camera, microphone, DRM) runtime permission launcher
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val request = pendingWebPermissionRequest
        pendingWebPermissionRequest = null
        if (request != null) {
            val grantedResources = resolveGrantableWebResources(
                context = context,
                requestedResources = request.resources,
                overrideGrantMap = grantMap,
            )
            runCatching {
                if (grantedResources.isNotEmpty()) {
                    request.grant(grantedResources.toTypedArray())
                } else {
                    request.deny()
                }
            }
        }
    }

    // HTML5 Geolocation runtime permission launcher
    var pendingGeolocationRequest by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }
    val geolocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        val pending = pendingGeolocationRequest
        pendingGeolocationRequest = null
        if (pending != null) {
            val granted = grantMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grantMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            runCatching {
                pending.second.invoke(pending.first, granted, false)
            }
        }
    }

    val isLeftBarCollapsed by remember {
        derivedStateOf {
            snapshotState.value.preferences.scrollToPillEnabled &&
                leftPaneUi.rawScrollCollapsed &&
                !leftPaneUi.isEditingAddress &&
                quickMenuPaneIndex != 0 &&
                !leftPaneUi.isTabDrawerOpen &&
                gridTabSwitcherPaneIndex == null &&
                !leftPaneUi.isFindInPageOpen &&
                fullscreenVideoView == null
        }
    }
    val isRightBarCollapsed by remember {
        derivedStateOf {
            snapshotState.value.preferences.scrollToPillEnabled &&
                rightPaneUi.rawScrollCollapsed &&
                !rightPaneUi.isEditingAddress &&
                quickMenuPaneIndex != 1 &&
                !rightPaneUi.isTabDrawerOpen &&
                gridTabSwitcherPaneIndex == null &&
                !rightPaneUi.isFindInPageOpen &&
                fullscreenVideoView == null
        }
    }

    val webViewPool = remember { mutableMapOf<String, WebView>() }
    val tabLastUsedMs = remember { mutableMapOf<String, Long>() }
    val tabScrollPositions = remember { mutableMapOf<String, Pair<Int, Int>>() }
    val tabScrollCallbacks = remember { mutableMapOf<String, (Int, Int, Boolean) -> Unit>() }
    val tabThumbnails = remember { mutableStateMapOf<String, Bitmap>() }

    fun captureTabThumbnail(tabId: String, wv: WebView?): Bitmap? {
        if (wv == null) return null
        val viewWidth = wv.width
        val viewHeight = wv.height
        if (viewWidth <= 0 || viewHeight <= 0) return null
        val ratio = viewHeight.toFloat() / viewWidth.toFloat()
        if (!ratio.isFinite() || ratio <= 0f) return null
        val targetWidth = 360
        val targetHeight = (targetWidth * ratio).roundToInt().coerceIn(180, 620)
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val captured = runCatching {
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
            try {
                val canvas = Canvas(bmp)
                val scale = targetWidth.toFloat() / viewWidth.toFloat()
                if (!scale.isFinite() || scale <= 0f) {
                    bmp.recycle()
                    return@runCatching null
                }
                canvas.scale(scale, scale)
                canvas.translate(-wv.scrollX.toFloat(), -wv.scrollY.toFloat())
                wv.draw(canvas)
                bmp
            } catch (t: Throwable) {
                if (!bmp.isRecycled) bmp.recycle()
                throw t
            }
        }.getOrNull() ?: return null
        tabThumbnails[tabId] = captured
        return captured
    }

    fun saveWebViewSnapshot(tabId: String, wv: WebView) {
        tabScrollPositions[tabId] = Pair(wv.scrollX, wv.scrollY)
        runCatching {
            val bundle = Bundle()
            wv.saveState(bundle)
            if (!bundle.isEmpty) {
                viewModel.saveTabBundle(tabId, bundle)
            }
        }
    }

    fun evictBackgroundWebViews(visibleIds: Set<String>, maxLiveCount: Int) {
        if (webViewPool.size <= maxLiveCount) return
        val candidates = webViewPool.keys
            .filter { it !in visibleIds }
            .sortedBy { tabLastUsedMs[it] ?: 0L }
        var toEvict = (webViewPool.size - maxLiveCount).coerceAtLeast(0)
        for (tabId in candidates) {
            if (toEvict <= 0) break
            val wv = webViewPool.remove(tabId) ?: continue
            saveWebViewSnapshot(tabId, wv)
            destroyWebViewCleanly(wv)
            toEvict--
        }
    }

    // Keep latest callbacks safe for long-lived WebChromeClient / WebViewClient closures
    val onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit =
        { callback, params ->
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = callback
            val intent = buildFileChooserIntent(params)
            val launched = runCatching {
                fileChooserLauncher.launch(intent)
                true
            }.getOrDefault(false)
            if (!launched) {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = null
                Toast.makeText(context, "파일 선택기를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    val onWebPermissionRequest: (PermissionRequest) -> Unit = { request ->
        val requested = request.resources ?: emptyArray()
        val missingRuntimePerms = mutableListOf<String>()
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in requested &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            missingRuntimePerms += Manifest.permission.CAMERA
        }
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in requested &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            missingRuntimePerms += Manifest.permission.RECORD_AUDIO
        }

        if (missingRuntimePerms.isEmpty()) {
            val grantable = resolveGrantableWebResources(context, requested)
            runCatching {
                if (grantable.isNotEmpty()) {
                    request.grant(grantable.toTypedArray())
                } else {
                    request.deny()
                }
            }
        } else {
            pendingWebPermissionRequest?.let { old -> runCatching { old.deny() } }
            pendingWebPermissionRequest = request
            runCatching {
                mediaPermissionLauncher.launch(missingRuntimePerms.toTypedArray())
            }.onFailure {
                pendingWebPermissionRequest = null
                runCatching { request.deny() }
            }
        }
    }

    val onGeolocationPrompt: (String, GeolocationPermissions.Callback) -> Unit = { origin, callback ->
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            runCatching { callback.invoke(origin, true, false) }
        } else {
            pendingGeolocationRequest?.let { (oldOrigin, oldCb) ->
                runCatching { oldCb.invoke(oldOrigin, false, false) }
            }
            pendingGeolocationRequest = origin to callback
            runCatching {
                geolocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }.onFailure {
                pendingGeolocationRequest = null
                runCatching { callback.invoke(origin, false, false) }
            }
        }
    }

    val onShowFullscreenVideo: (View, WebChromeClient.CustomViewCallback?) -> Unit = { view, callback ->
        if (fullscreenVideoView != null) {
            callback?.onCustomViewHidden()
        } else {
            fullscreenVideoView = view
            fullscreenVideoCallback = callback
            quickMenuPaneIndex = null
            leftPaneUi.isTabDrawerOpen = false
            leftPaneUi.isEditingAddress = false
            rightPaneUi.isTabDrawerOpen = false
            rightPaneUi.isEditingAddress = false
        }
    }

    val onHideFullscreenVideo: () -> Unit = {
        fullscreenVideoCallback?.onCustomViewHidden()
        fullscreenVideoCallback = null
        fullscreenVideoView = null
    }

    val onSslErrorReceived: (SslErrorHandler, SslError?) -> Unit = { handler, error ->
        pendingSslError?.let { old -> runCatching { old.handler.cancel() } }
        val rawUrl = error?.url.orEmpty()
        val host = runCatching { Uri.parse(rawUrl).host }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: rawUrl.ifBlank { "알 수 없는 사이트" }
        pendingSslError = PendingSslError(
            host = host,
            errorCode = error?.primaryError ?: -1,
            handler = handler,
        )
    }

    val onLeftScrollDelta: (Int, Int, Boolean) -> Unit = { dy, scrollY, isScrollable ->
        leftPaneUi.onScrollDelta(dy, scrollY, isScrollable)
    }
    val onRightScrollDelta: (Int, Int, Boolean) -> Unit = { dy, scrollY, isScrollable ->
        rightPaneUi.onScrollDelta(dy, scrollY, isScrollable)
    }

    fun saveAllNonIncognitoWebViews() {
        val nonIncognitoIds = viewModel.snapshotFlow.value.tabs
            .filterNot { it.isIncognito }
            .map { it.id }
            .toSet()
        webViewPool.forEach { (tabId, wv) ->
            tabScrollPositions[tabId] = Pair(wv.scrollX, wv.scrollY)
            if (tabId in nonIncognitoIds) {
                runCatching {
                    val bundle = Bundle()
                    wv.saveState(bundle)
                    if (!bundle.isEmpty) {
                        viewModel.saveTabBundle(tabId, bundle)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onRegisterWebViewPersistenceHook {
            saveAllNonIncognitoWebViews()
        }
        onRegisterTrimMemoryHook { _ ->
            val currentSnap = viewModel.snapshotFlow.value
            val keepIds = buildSet {
                add(currentSnap.activeTabId)
                currentSnap.secondaryTabId?.let { add(it) }
            }
            evictBackgroundWebViews(visibleIds = keepIds, maxLiveCount = keepIds.size.coerceAtLeast(1))
        }
        onDispose {
            onRegisterWebViewPersistenceHook(null)
            onRegisterTrimMemoryHook(null)
        }
    }

    // Forward Activity lifecycle events to WebViews & persist WebView Bundles across background death
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val currentSnap = viewModel.snapshotFlow.value
                    val visibleIds = buildSet {
                        add(currentSnap.activeTabId)
                        currentSnap.secondaryTabId?.let { add(it) }
                    }
                    webViewPool.forEach { (id, wv) ->
                        if (id in visibleIds) {
                            wv.onResume()
                            wv.resumeTimers()
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    saveAllNonIncognitoWebViews()
                    flushCookiesSafely()
                    webViewPool.values.forEach { wv ->
                        wv.onPause()
                    }
                    viewModel.persistConfigSync()
                }
                Lifecycle.Event.ON_STOP -> {
                    saveAllNonIncognitoWebViews()
                    flushCookiesSafely()
                    viewModel.persistConfigSync()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cleanly destroy closed tab WebViews & remove stale thumbnails/scroll state to eliminate CPU/memory leaks
    LaunchedEffect(snapshot.tabs) {
        val openIds = snapshot.tabs.map { it.id }.toSet()
        val staleIds = (webViewPool.keys + tabThumbnails.keys + tabLastUsedMs.keys + tabScrollPositions.keys + tabScrollCallbacks.keys)
            .distinct()
            .filter { it !in openIds }
        staleIds.forEach { staleId ->
            viewModel.removeTabBundle(staleId)
            tabThumbnails.remove(staleId)
            tabLastUsedMs.remove(staleId)
            tabScrollPositions.remove(staleId)
            tabScrollCallbacks.remove(staleId)
            webViewPool.remove(staleId)?.let { wv ->
                destroyWebViewCleanly(wv)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingSslError?.let { runCatching { it.handler.cancel() } }
            pendingSslError = null
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            pendingWebPermissionRequest?.let { runCatching { it.deny() } }
            pendingWebPermissionRequest = null
            pendingGeolocationRequest?.let { (origin, cb) -> runCatching { cb.invoke(origin, false, false) } }
            pendingGeolocationRequest = null
            tabThumbnails.clear()
            tabLastUsedMs.clear()
            tabScrollPositions.clear()
            tabScrollCallbacks.clear()
            saveAllNonIncognitoWebViews()
            flushCookiesSafely()
            viewModel.persistConfigSync()
            webViewPool.values.forEach { wv ->
                destroyWebViewCleanly(wv)
            }
            webViewPool.clear()
        }
    }

    val summaryJson = remember {
        Json { ignoreUnknownKeys = true }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("browser_root_container"),
    ) {
        val foldFeature = windowLayoutInfo
            ?.displayFeatures
            ?.filterIsInstance<FoldingFeature>()
            ?.firstOrNull()

        val hasHardwareHinge = foldFeature != null
        val isTabletop = foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
            foldFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
        val isUnfolded = (foldFeature != null &&
            (foldFeature.state == FoldingFeature.State.FLAT ||
                foldFeature.state == FoldingFeature.State.HALF_OPENED)) ||
            maxWidth >= 600.dp
        val isVerticalHinge = foldFeature?.orientation == FoldingFeature.Orientation.VERTICAL ||
            (isUnfolded && maxWidth >= 600.dp)
        val hingeWidthDp = if (foldFeature != null && foldFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
            with(density) { foldFeature.bounds.width().toDp() }
        } else {
            0.dp
        }

        val foldablePosture = FoldablePostureUiState(
            isUnfolded = isUnfolded,
            hasHardwareHinge = hasHardwareHinge,
            isSeparatingHinge = foldFeature?.isSeparating == true,
            isVerticalHinge = isVerticalHinge,
            isTabletopPosture = isTabletop,
            hingeWidthDp = hingeWidthDp,
        )

        val isDualPaneActive = isUnfolded && prefs.foldableDualPaneEnabled
        var mergeAnchorSide by remember { mutableIntStateOf(0) }
        var mergingOutgoingTab by remember { mutableStateOf<BrowserTabState?>(null) }
        var lastDualHingeGapDp by remember { mutableStateOf(12.dp) }
        val splitExpandProgress by animateFloatAsState(
            targetValue = if (isDualPaneActive) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 360f),
            label = "dual_pane_split_merge_progress",
        )

        LaunchedEffect(isDualPaneActive) {
            if (isDualPaneActive) {
                mergingOutgoingTab = null
                rightPaneUi.resetScrollCollapse()
                viewModel.ensureSecondaryTabForDualPane()
            } else {
                focusedPaneIndex = 0
                rightPaneUi.closeTransientPopups()
                if (quickMenuPaneIndex == 1) quickMenuPaneIndex = null
                if (gridTabSwitcherPaneIndex == 1) gridTabSwitcherPaneIndex = null
            }
        }
        LaunchedEffect(isDualPaneActive, splitExpandProgress) {
            if (!isDualPaneActive && splitExpandProgress <= 0.01f) {
                mergingOutgoingTab = null
                mergeAnchorSide = 0
            }
        }

        val primaryTab = snapshot.activeTab
        val secondaryTab = if (isDualPaneActive) snapshot.secondaryTab else null
        val focusedTab = if (isDualPaneActive && focusedPaneIndex == 1 && secondaryTab != null) {
            secondaryTab
        } else {
            primaryTab
        }
        val focusedTabId = focusedTab?.id ?: snapshot.activeTabId
        val focusedWebView = webViewPool[focusedTabId]

        val activeQuickMenuPane = if (isDualPaneActive) (quickMenuPaneIndex ?: focusedPaneIndex) else 0
        val quickMenuTab = if (isDualPaneActive && activeQuickMenuPane == 1 && secondaryTab != null) {
            secondaryTab
        } else {
            primaryTab
        }
        val quickMenuTabId = quickMenuTab?.id ?: snapshot.activeTabId
        val quickMenuWebView = webViewPool[quickMenuTabId]
        val quickMenuPaneUi = if (activeQuickMenuPane == 1 && isDualPaneActive) rightPaneUi else leftPaneUi
        val isQuickMenuUrlBookmarked = snapshot.bookmarks.any { it.url == quickMenuTab?.url }

        val centerHingeGapDp = if (isDualPaneActive) {
            maxOf(
                prefs.foldableHingeAvoidanceDp.dp,
                hingeWidthDp + prefs.foldableHingeAvoidanceDp.dp,
            ).coerceAtLeast(2.dp)
        } else {
            0.dp
        }
        if (isDualPaneActive) {
            lastDualHingeGapDp = centerHingeGapDp
        }

        // Pause hidden background tabs, resume visible pane tabs, and apply LRU eviction cap
        LaunchedEffect(snapshot.activeTabId, secondaryTab?.id, isDualPaneActive, mergingOutgoingTab?.id) {
            val now = System.currentTimeMillis()
            val visibleIds = buildSet {
                add(snapshot.activeTabId)
                if (isDualPaneActive && secondaryTab != null) {
                    add(secondaryTab.id)
                }
                mergingOutgoingTab?.id?.let { add(it) }
            }
            visibleIds.forEach { id -> tabLastUsedMs[id] = now }
            webViewPool.forEach { (id, wv) ->
                if (id in visibleIds) {
                    wv.onResume()
                    wv.resumeTimers()
                } else {
                    tabScrollPositions[id] = Pair(wv.scrollX, wv.scrollY)
                    wv.onPause()
                }
            }
            evictBackgroundWebViews(
                visibleIds = visibleIds,
                maxLiveCount = maxOf(MAX_LIVE_WEBVIEWS, visibleIds.size),
            )
        }

        val leftDisplaySnapshot = remember(snapshot, primaryTab?.id) {
            val targetId = primaryTab?.id ?: snapshot.activeTabId
            if (targetId != snapshot.activeTabId) snapshot.copy(activeTabId = targetId) else snapshot
        }
        val rightDisplaySnapshot = remember(snapshot, secondaryTab?.id) {
            val targetId = secondaryTab?.id ?: snapshot.activeTabId
            if (targetId != snapshot.activeTabId) snapshot.copy(activeTabId = targetId) else snapshot
        }
        val quickMenuDisplaySnapshot = if (activeQuickMenuPane == 1 && isDualPaneActive) {
            rightDisplaySnapshot
        } else {
            leftDisplaySnapshot
        }
        val gridSwitcherTargetPane = if (isDualPaneActive) (gridTabSwitcherPaneIndex ?: focusedPaneIndex) else 0
        val gridSwitcherSnapshot = if (gridSwitcherTargetPane == 1 && isDualPaneActive) {
            rightDisplaySnapshot
        } else {
            leftDisplaySnapshot
        }

        val anyEditingAddress = leftPaneUi.isEditingAddress || (isDualPaneActive && rightPaneUi.isEditingAddress)
        val anyFindOrDrawerOpen = leftPaneUi.isFindInPageOpen || leftPaneUi.isTabDrawerOpen ||
            (isDualPaneActive && (rightPaneUi.isFindInPageOpen || rightPaneUi.isTabDrawerOpen))

        // Layered BackHandler Priority 5: WebView back stack (lowest priority)
        BackHandler(
            enabled = fullscreenVideoView == null &&
                !isQuickMenuOpen &&
                activeOverlay == PushOverlay.NONE &&
                !isGridTabSwitcherOpen &&
                !anyEditingAddress &&
                !anyFindOrDrawerOpen &&
                focusedTab?.canGoBack == true
        ) {
            webViewPool[focusedTabId]?.let { wv ->
                if (wv.canGoBack()) {
                    wv.goBack()
                }
            }
        }

        // Secondary overlays (Find-in-Page / Tab Dock)
        BackHandler(
            enabled = fullscreenVideoView == null &&
                !isQuickMenuOpen &&
                activeOverlay == PushOverlay.NONE &&
                !isGridTabSwitcherOpen &&
                !anyEditingAddress &&
                anyFindOrDrawerOpen
        ) {
            val targetPaneUi = if (isDualPaneActive && focusedPaneIndex == 1 && (rightPaneUi.isFindInPageOpen || rightPaneUi.isTabDrawerOpen)) {
                rightPaneUi
            } else if (leftPaneUi.isFindInPageOpen || leftPaneUi.isTabDrawerOpen) {
                leftPaneUi
            } else {
                rightPaneUi
            }
            val targetWebView = if (targetPaneUi === rightPaneUi) {
                secondaryTab?.id?.let { webViewPool[it] }
            } else {
                primaryTab?.id?.let { webViewPool[it] }
            }
            when {
                targetPaneUi.isFindInPageOpen -> {
                    targetPaneUi.isFindInPageOpen = false
                    targetPaneUi.findQuery = ""
                    targetWebView?.clearMatches()
                }
                targetPaneUi.isTabDrawerOpen -> targetPaneUi.isTabDrawerOpen = false
            }
        }

        fun createOnNewWindowForPane(paneIndex: Int): (Message) -> Boolean = { resultMsg ->
            val transport = resultMsg.obj as? WebView.WebViewTransport
            if (transport == null) {
                false
            } else {
                val targetPaneUi = if (paneIndex == 1 && isDualPaneActive) rightPaneUi else leftPaneUi
                val sourceTabId = if (paneIndex == 1 && isDualPaneActive) {
                    secondaryTab?.id ?: snapshot.activeTabId
                } else {
                    primaryTab?.id ?: snapshot.activeTabId
                }
                captureTabThumbnail(sourceTabId, webViewPool[sourceTabId])
                targetPaneUi.isTabDrawerOpen = false
                gridTabSwitcherPaneIndex = null
                targetPaneUi.resetScrollCollapse()
                val targetPane = if (isDualPaneActive) paneIndex else 0
                val nextSnap = viewModel.createTabForPane(
                    paneIndex = targetPane,
                    initialUrl = "about:blank",
                    isIncognito = false,
                )
                val newTab = if (targetPane == 1) nextSnap.secondaryTab else nextSnap.activeTab
                if (newTab == null) {
                    false
                } else {
                    focusedPaneIndex = targetPane
                    val livePopupScrollCallback: (Int, Int, Boolean) -> Unit = { dy, sy, scrollable ->
                        val registered = tabScrollCallbacks[newTab.id]
                        if (registered != null) {
                            registered(dy, sy, scrollable)
                        } else if (targetPane == 1) {
                            onRightScrollDelta(dy, sy, scrollable)
                        } else {
                            onLeftScrollDelta(dy, sy, scrollable)
                        }
                    }
                    val popupWebView = createConfiguredWebView(
                        context = context,
                        tabId = newTab.id,
                        initialUrl = null, // WebViewTransport delivers the target URL / POST request
                        restoredBundle = null,
                        restoredScrollPosition = null,
                        preferences = nextSnap.preferences,
                        isDesktopMode = newTab.isDesktopMode,
                        viewModel = viewModel,
                        onScrollDelta = livePopupScrollCallback,
                        onOpenFileChooser = onOpenFileChooser,
                        onPermissionRequest = onWebPermissionRequest,
                        onGeolocationPrompt = onGeolocationPrompt,
                        onCreateNewWindow = { msg ->
                            createPopupHelper(
                                context = context,
                                viewModel = viewModel,
                                webViewPool = webViewPool,
                                tabLastUsedMs = tabLastUsedMs,
                                tabScrollCallbacks = tabScrollCallbacks,
                                fallbackScrollDelta = if (targetPane == 1) onRightScrollDelta else onLeftScrollDelta,
                                onOpenFileChooser = onOpenFileChooser,
                                onPermissionRequest = onWebPermissionRequest,
                                onGeolocationPrompt = onGeolocationPrompt,
                                onShowCustomView = onShowFullscreenVideo,
                                onHideCustomView = onHideFullscreenVideo,
                                onSslError = onSslErrorReceived,
                                onFocusPrimaryPane = { focusedPaneIndex = targetPane },
                                onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                                resultMsg = msg,
                            )
                        },
                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                        onShowCustomView = onShowFullscreenVideo,
                        onHideCustomView = onHideFullscreenVideo,
                        onSslError = onSslErrorReceived,
                        onCaptureThumbnail = { wv -> captureTabThumbnail(newTab.id, wv) },
                    )
                    webViewPool[newTab.id] = popupWebView
                    tabLastUsedMs[newTab.id] = System.currentTimeMillis()
                    transport.webView = popupWebView
                    runCatching { resultMsg.sendToTarget() }
                    true
                }
            }
        }

        fun switchTabRelativeForPane(paneIndex: Int, delta: Int) {
            val tabsList = snapshot.tabs
            if (tabsList.size <= 1) return
            val effectivePane = if (isDualPaneActive) paneIndex else 0
            val currentTabId = if (effectivePane == 1) {
                secondaryTab?.id ?: snapshot.activeTabId
            } else {
                primaryTab?.id ?: snapshot.activeTabId
            }
            val otherPaneTabId = if (isDualPaneActive) {
                if (effectivePane == 1) primaryTab?.id else secondaryTab?.id
            } else {
                null
            }
            captureTabThumbnail(currentTabId, webViewPool[currentTabId])
            val lastIdx = (tabsList.size - 1).coerceAtLeast(0)
            val foundIdx = tabsList.indexOfFirst { it.id == currentTabId }
            val currentIdx = (if (foundIdx >= 0) foundIdx else 0).coerceIn(0, lastIdx)
            var candidateIdx = (((currentIdx + delta) % tabsList.size + tabsList.size) % tabsList.size)
                .coerceIn(0, lastIdx)
            // Skip the tab currently displayed in the opposite split pane when 3+ tabs exist
            if (otherPaneTabId != null && tabsList.size > 2 && tabsList.getOrNull(candidateIdx)?.id == otherPaneTabId) {
                candidateIdx = (((candidateIdx + delta) % tabsList.size + tabsList.size) % tabsList.size)
                    .coerceIn(0, lastIdx)
            }
            val targetId = tabsList.getOrNull(candidateIdx)?.id ?: return
            focusedPaneIndex = effectivePane
            viewModel.assignTabToPane(effectivePane, targetId)
            val paneUi = if (effectivePane == 1) rightPaneUi else leftPaneUi
            paneUi.isTabDrawerOpen = false
            quickMenuPaneIndex = null
            paneUi.resetScrollCollapse()
        }

        val launchAiForSummary: (PageSummaryState, String?) -> Unit = { baseSummary, userQuestion ->
            val activeProfile = snapshotState.value.preferences.activeAiProfile()
            pageSummaryState = baseSummary.copy(
                aiProfileName = activeProfile.name,
                aiModel = activeProfile.selectedModel,
                aiIsLoading = true,
                aiErrorMessage = null,
            )
            val fallbackCombinedText = buildString {
                if (baseSummary.metaDescription.isNotBlank()) appendLine(baseSummary.metaDescription)
                if (baseSummary.headings.isNotEmpty()) appendLine(baseSummary.headings.joinToString("\n"))
                if (baseSummary.keySentences.isNotEmpty()) appendLine(baseSummary.keySentences.joinToString("\n"))
            }
            val contentToSend = baseSummary.bodyPreviewText.ifBlank { fallbackCombinedText }
            coroutineScope.launch {
                val result = AiCliHttpClient.requestPageAnalysis(
                    profile = activeProfile,
                    pageTitle = baseSummary.title,
                    pageUrl = baseSummary.url,
                    pageContentText = contentToSend,
                    userQuestion = userQuestion,
                )
                result.onSuccess { resp ->
                    pageSummaryState = pageSummaryState.copy(
                        aiProfileName = resp.profileName,
                        aiModel = resp.model,
                        aiResponseText = resp.text,
                        aiIsLoading = false,
                        aiErrorMessage = null,
                    )
                }.onFailure { err ->
                    val friendlyMessage = when (err) {
                        is MissingApiKeyException ->
                            "${err.profileName} API 키가 설정되지 않았습니다. 아래 'AI 설정' 버튼을 눌러 키를 등록하거나 로컬 Ollama CLI를 선택해 주세요. (로컬 DOM 요약은 아래에서 바로 확인 가능합니다)"
                        is MissingModelSelectionException ->
                            err.message ?: "사용할 AI 모델이 선택되지 않았습니다. 'AI 설정'에서 연결 확인 및 모델 불러오기를 실행해 주세요."
                        else ->
                            "AI 응답 호출 실패: ${err.message ?: "네트워크 또는 엔드포인트 연결을 확인해 주세요."}"
                    }
                    pageSummaryState = pageSummaryState.copy(
                        aiIsLoading = false,
                        aiErrorMessage = friendlyMessage,
                    )
                }
            }
        }

        fun launchAiChatForTab(targetTab: BrowserTabState?, targetWebView: WebView?) {
            leftPaneUi.closeTransientPopups()
            rightPaneUi.closeTransientPopups()
            gridTabSwitcherPaneIndex = null
            quickMenuPaneIndex = null

            val cleanUrl = targetTab?.url.orEmpty().trim()
            val cleanTitle = targetTab?.title.orEmpty()
            val seed = aiChatUiState.sessionSeed + 1
            aiChatUiState = aiChatUiState.copy(
                sessionSeed = seed,
                prefillUrl = cleanUrl,
                pageTitle = cleanTitle,
                pageBodyText = "",
                errorMessage = null,
            )
            activeOverlay = PushOverlay.AI_CHAT

            // Optionally pre-extract visible DOM text in background without blocking or auto-sending
            if (targetWebView != null && prefs.javaScriptEnabled) {
                extractDomPageSummary(
                    webView = targetWebView,
                    json = summaryJson,
                    fallbackUrl = cleanUrl,
                    fallbackTitle = cleanTitle,
                ) { parsed ->
                    if (parsed.errorMessage == null && parsed.bodyPreviewText.isNotBlank()) {
                        aiChatUiState = aiChatUiState.copy(
                            pageTitle = parsed.title.ifBlank { cleanTitle },
                            pageBodyText = parsed.bodyPreviewText,
                        )
                    }
                }
            }
        }

        val sendAiChatMessage: (String) -> Unit = { rawMessage ->
            val cleanMessage = rawMessage.trim()
            if (cleanMessage.isNotEmpty() && !aiChatUiState.isLoading) {
                val activeProfile = snapshotState.value.preferences.activeAiProfile()
                val lastTurn = aiChatUiState.messages.lastOrNull()
                val updatedHistory = if (lastTurn != null && lastTurn.isUser && lastTurn.text == cleanMessage) {
                    // Prevent duplicate identical user messages from stacking on retry / rapid tap
                    aiChatUiState.messages
                } else {
                    aiChatUiState.messages + AiChatTurn(isUser = true, text = cleanMessage)
                }
                aiChatUiState = aiChatUiState.copy(
                    messages = updatedHistory,
                    isLoading = true,
                    errorMessage = null,
                )
                coroutineScope.launch {
                    val result = AiCliHttpClient.requestAiChat(
                        profile = activeProfile,
                        conversation = updatedHistory,
                        pageTitle = aiChatUiState.pageTitle,
                        pageUrl = aiChatUiState.prefillUrl,
                        pageContentText = aiChatUiState.pageBodyText,
                    )
                    result.onSuccess { resp ->
                        aiChatUiState = aiChatUiState.copy(
                            messages = aiChatUiState.messages + AiChatTurn(isUser = false, text = resp.text),
                            isLoading = false,
                            errorMessage = null,
                        )
                    }.onFailure { err ->
                        val friendlyMessage = when (err) {
                            is MissingApiKeyException ->
                                "${err.profileName} API 키가 설정되지 않았습니다. 상단 'AI 설정'을 눌러 키를 등록하거나 로컬 Ollama CLI를 선택해 주세요."
                            is MissingModelSelectionException ->
                                err.message ?: "사용할 모델이 선택되지 않았습니다. 상단 'AI 설정'에서 연결 확인 및 모델 불러오기를 실행해 주세요."
                            else ->
                                "AI 응답 호출 실패: ${err.message ?: "네트워크 또는 엔드포인트 연결을 확인해 주세요."}"
                        }
                        aiChatUiState = aiChatUiState.copy(
                            isLoading = false,
                            errorMessage = friendlyMessage,
                        )
                    }
                }
            }
        }

        fun triggerPageSummaryForTab(
            targetTab: BrowserTabState?,
            targetWebView: WebView?,
            userQuestion: String?,
        ) {
            val activeProfile = snapshotState.value.preferences.activeAiProfile()
            if (activeOverlay == PushOverlay.SUMMARY &&
                !pageSummaryState.isLoading &&
                pageSummaryState.errorMessage.isNullOrBlank()
            ) {
                launchAiForSummary(pageSummaryState, userQuestion)
            } else {
                val currentUrl = targetTab?.url.orEmpty()
                pageSummaryState = PageSummaryState(
                    url = currentUrl,
                    title = targetTab?.title.orEmpty(),
                    isLoading = true,
                    aiProfileName = activeProfile.name,
                    aiModel = activeProfile.selectedModel,
                    aiIsLoading = true,
                )
                activeOverlay = PushOverlay.SUMMARY
                if (targetWebView == null) {
                    pageSummaryState = pageSummaryState.copy(
                        isLoading = false,
                        aiIsLoading = false,
                        errorMessage = "활성 웹 페이지를 찾을 수 없습니다.",
                    )
                } else {
                    extractDomPageSummary(
                        webView = targetWebView,
                        json = summaryJson,
                        fallbackUrl = currentUrl,
                        fallbackTitle = targetTab?.title.orEmpty(),
                    ) { parsed ->
                        if (parsed.errorMessage != null) {
                            pageSummaryState = parsed.copy(
                                aiProfileName = activeProfile.name,
                                aiModel = activeProfile.selectedModel,
                                aiIsLoading = false,
                            )
                        } else {
                            launchAiForSummary(parsed, userQuestion)
                        }
                    }
                }
            }
        }

        fun triggerAddressGestureActionForPane(
            paneIndex: Int,
            paneTab: BrowserTabState?,
            paneUi: PaneUiState,
            action: Int,
        ) {
            val effectivePane = if (isDualPaneActive) paneIndex else 0
            val tabId = paneTab?.id ?: snapshot.activeTabId
            val paneWebView = webViewPool[tabId]
            focusedPaneIndex = effectivePane
            when (action) {
                BrowserPreferences.GESTURE_OPEN_TABS -> {
                    captureTabThumbnail(tabId, paneWebView)
                    if (prefs.tabSwitcherStyle == BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW) {
                        paneUi.closeTransientPopups()
                        gridTabSwitcherPaneIndex = effectivePane
                    } else {
                        gridTabSwitcherPaneIndex = null
                        paneUi.isTabDrawerOpen = true
                    }
                }
                BrowserPreferences.GESTURE_RELOAD -> {
                    if (paneTab?.isLoading == true) {
                        paneWebView?.stopLoading()
                    } else {
                        paneWebView?.reload()
                    }
                }
                BrowserPreferences.GESTURE_SCROLL_TO_TOP -> {
                    paneWebView?.let { wv ->
                        if (prefs.javaScriptEnabled) {
                            wv.evaluateJavascript(
                                "window.scrollTo({top: 0, behavior: 'smooth'});",
                                null,
                            )
                        } else {
                            wv.scrollTo(0, 0)
                        }
                    }
                }
                BrowserPreferences.GESTURE_NEW_TAB -> {
                    captureTabThumbnail(tabId, paneWebView)
                    paneUi.isTabDrawerOpen = false
                    gridTabSwitcherPaneIndex = null
                    paneUi.resetScrollCollapse()
                    viewModel.createTabForPane(effectivePane)
                }
                BrowserPreferences.GESTURE_QUICK_MENU -> {
                    paneUi.isTabDrawerOpen = false
                    paneUi.isEditingAddress = false
                    paneUi.resetScrollCollapse()
                    quickMenuPaneIndex = effectivePane
                }
                BrowserPreferences.GESTURE_BOOKMARK -> {
                    val url = paneTab?.url.orEmpty()
                    if (url.isNotBlank()) {
                        val wasBookmarked = snapshot.bookmarks.any { it.url == url }
                        viewModel.toggleBookmark(url, paneTab?.title.orEmpty())
                        Toast.makeText(
                            context,
                            if (wasBookmarked) "북마크에서 제거되었습니다." else "북마크에 저장되었습니다.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                else -> Unit
            }
        }

        fun triggerCloseSplitPane(keepLeftPane: Boolean) {
            leftPaneUi.closeTransientPopups()
            rightPaneUi.closeTransientPopups()
            quickMenuPaneIndex = null
            gridTabSwitcherPaneIndex = null
            primaryTab?.let { captureTabThumbnail(it.id, webViewPool[it.id]) }
            secondaryTab?.let { captureTabThumbnail(it.id, webViewPool[it.id]) }
            mergeAnchorSide = if (keepLeftPane) 0 else 1
            mergingOutgoingTab = if (keepLeftPane) secondaryTab else primaryTab
            viewModel.closeSplitPane(keepLeftPane = keepLeftPane)
            focusedPaneIndex = 0
        }

        fun handleLongPressSwipeSplit(
            paneIndex: Int,
            sourceTab: BrowserTabState?,
            swipeLeft: Boolean,
        ) {
            if (!isUnfolded) return
            if (isDualPaneActive) {
                // Reverse swipe-to-merge:
                // Left pane (0) + Swipe Right -> keep Left pane active, preserve Right tab in open tabs
                // Right pane (1) + Swipe Left -> keep Right pane active, preserve Left tab in open tabs
                when {
                    paneIndex == 0 && !swipeLeft -> triggerCloseSplitPane(keepLeftPane = true)
                    paneIndex == 1 && swipeLeft -> triggerCloseSplitPane(keepLeftPane = false)
                }
                return
            }
            if (!prefs.foldableLongPressSwipeSplitEnabled) return
            leftPaneUi.closeTransientPopups()
            rightPaneUi.closeTransientPopups()
            quickMenuPaneIndex = null
            gridTabSwitcherPaneIndex = null
            val sourceId = sourceTab?.id ?: snapshot.activeTabId
            captureTabThumbnail(sourceId, webViewPool[sourceId])
            mergeAnchorSide = if (swipeLeft) 0 else 1
            mergingOutgoingTab = null
            viewModel.splitTabForFoldable(
                sourceTabId = sourceId,
                placeSourceOnLeft = swipeLeft,
            )
            rightPaneUi.resetScrollCollapse()
            focusedPaneIndex = if (swipeLeft) 0 else 1
        }

        @Composable
        fun BoxScope.PaneFloatingBarOverlay(
            paneIndex: Int,
            paneTab: BrowserTabState,
            paneSnapshot: BrowserCoreSnapshot,
            paneUi: PaneUiState,
            isCollapsed: Boolean,
            isSplitPaneBar: Boolean,
            paneTagPrefix: String,
        ) {
            val paneWebView = webViewPool[paneTab.id]

            // Outside-tap scrim while editing this pane's address bar
            if (paneUi.isEditingAddress && fullscreenVideoView == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(paneIndex) {
                            detectTapGestures(
                                onTap = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    paneUi.isEditingAddress = false
                                }
                            )
                        }
                        .testTag("${paneTagPrefix}address_editing_outside_scrim")
                )
            }

            if (fullscreenVideoView == null) {
                FloatingControlBar(
                    snapshot = paneSnapshot,
                    displayUrl = viewModel.formatDisplayUrl(paneTab.url),
                    foldablePosture = foldablePosture,
                    isEditingAddress = paneUi.isEditingAddress,
                    isTabDrawerOpen = paneUi.isTabDrawerOpen,
                    isQuickMenuOpen = quickMenuPaneIndex == paneIndex,
                    isFindInPageOpen = paneUi.isFindInPageOpen,
                    findQuery = paneUi.findQuery,
                    isCollapsedToPill = isCollapsed,
                    isSplitPaneBar = isSplitPaneBar,
                    splitPaneSide = paneIndex,
                    paneTagPrefix = paneTagPrefix,
                    onCloseSplitPane = if (isSplitPaneBar) {
                        {
                            // Closing Left pane (0) keeps Right pane (keepLeftPane = false); closing Right pane (1) keeps Left pane (keepLeftPane = true)
                            triggerCloseSplitPane(keepLeftPane = paneIndex == 1)
                        }
                    } else {
                        null
                    },
                    onEnterAddressEdit = {
                        focusedPaneIndex = paneIndex
                        paneUi.isTabDrawerOpen = false
                        quickMenuPaneIndex = null
                        paneUi.resetScrollCollapse()
                        paneUi.isEditingAddress = true
                    },
                    onExitAddressEdit = {
                        paneUi.isEditingAddress = false
                    },
                    onSubmitAddress = { rawInput ->
                        focusedPaneIndex = paneIndex
                        paneUi.isEditingAddress = false
                        val targetUrl = viewModel.resolveUrlOrSearch(rawInput)
                        paneWebView?.loadUrl(targetUrl)
                    },
                    onReloadOrStop = {
                        focusedPaneIndex = paneIndex
                        if (paneTab.isLoading) {
                            paneWebView?.stopLoading()
                        } else {
                            paneWebView?.reload()
                        }
                    },
                    onToolbarAction = { action ->
                        focusedPaneIndex = paneIndex
                        quickMenuPaneIndex = null
                        when (action) {
                            ToolbarAction.BACK -> paneWebView?.goBack()
                            ToolbarAction.FORWARD -> paneWebView?.goForward()
                            ToolbarAction.RELOAD_OR_STOP -> {
                                if (paneTab.isLoading) {
                                    paneWebView?.stopLoading()
                                } else {
                                    paneWebView?.reload()
                                }
                            }
                            ToolbarAction.TABS -> {
                                captureTabThumbnail(paneTab.id, paneWebView)
                                if (prefs.tabSwitcherStyle == BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW) {
                                    paneUi.closeTransientPopups()
                                    gridTabSwitcherPaneIndex = paneIndex
                                } else {
                                    gridTabSwitcherPaneIndex = null
                                    paneUi.isTabDrawerOpen = !paneUi.isTabDrawerOpen
                                }
                            }
                            ToolbarAction.NEW_TAB -> {
                                captureTabThumbnail(paneTab.id, paneWebView)
                                paneUi.isTabDrawerOpen = false
                                gridTabSwitcherPaneIndex = null
                                paneUi.resetScrollCollapse()
                                viewModel.createTabForPane(paneIndex)
                            }
                            ToolbarAction.HOME -> {
                                paneWebView?.loadUrl(prefs.homeUrl)
                            }
                            ToolbarAction.DESKTOP_MODE -> {
                                viewModel.toggleDesktopMode(paneTab.id)
                                paneWebView?.let { wv ->
                                    val nextDesktop = !paneTab.isDesktopMode
                                    applyRuntimeWebSettings(wv, prefs, nextDesktop)
                                    wv.reload()
                                }
                            }
                            ToolbarAction.SHARE -> {
                                shareUrl(context, paneTab.url, copyToClipboard = false)
                            }
                            ToolbarAction.PAGE_SUMMARY -> {
                                triggerPageSummaryForTab(paneTab, paneWebView, null)
                            }
                        }
                    },
                    onSwitchTab = { tabId ->
                        focusedPaneIndex = paneIndex
                        captureTabThumbnail(paneTab.id, paneWebView)
                        viewModel.assignTabToPane(paneIndex, tabId)
                        paneUi.isTabDrawerOpen = false
                        paneUi.resetScrollCollapse()
                    },
                    onCloseTab = { tabId ->
                        viewModel.closeTab(tabId)
                    },
                    onCreateTab = {
                        focusedPaneIndex = paneIndex
                        captureTabThumbnail(paneTab.id, paneWebView)
                        viewModel.createTabForPane(paneIndex)
                        paneUi.isTabDrawerOpen = false
                        paneUi.resetScrollCollapse()
                    },
                    onToggleQuickMenu = {
                        focusedPaneIndex = paneIndex
                        paneUi.isTabDrawerOpen = false
                        paneUi.isEditingAddress = false
                        paneUi.resetScrollCollapse()
                        quickMenuPaneIndex = if (quickMenuPaneIndex == paneIndex) null else paneIndex
                    },
                    onFindQueryChange = { q ->
                        focusedPaneIndex = paneIndex
                        paneUi.findQuery = q
                        if (q.isEmpty()) {
                            paneWebView?.clearMatches()
                        } else {
                            paneWebView?.findAllAsync(q)
                        }
                    },
                    onFindNext = { forward ->
                        focusedPaneIndex = paneIndex
                        paneWebView?.findNext(forward)
                    },
                    onCloseFindInPage = {
                        paneUi.isFindInPageOpen = false
                        paneUi.findQuery = ""
                        paneWebView?.clearMatches()
                    },
                    onAddressSwipeUp = {
                        triggerAddressGestureActionForPane(paneIndex, paneTab, paneUi, prefs.swipeUpAction)
                    },
                    onAddressSwipeDown = {
                        triggerAddressGestureActionForPane(paneIndex, paneTab, paneUi, prefs.swipeDownAction)
                    },
                    onAddressSwipeTab = { delta -> switchTabRelativeForPane(paneIndex, delta) },
                    onAddressDoubleTap = {
                        triggerAddressGestureActionForPane(paneIndex, paneTab, paneUi, prefs.doubleTapAction)
                    },
                    onLongPressSwipeUpAiChat = {
                        focusedPaneIndex = paneIndex
                        launchAiChatForTab(paneTab, paneWebView)
                    },
                    onLongPressSwipeSplit = { swipeLeft ->
                        handleLongPressSwipeSplit(paneIndex, paneTab, swipeLeft)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // Underlying browser parallax offset read strictly inside graphicsLayer (zero root recomposition per frame)
        val parallaxProgressState = animateFloatAsState(
            targetValue = if (
                activeOverlay == PushOverlay.SETTINGS ||
                activeOverlay == PushOverlay.HISTORY ||
                activeOverlay == PushOverlay.SUMMARY
            ) {
                1f
            } else {
                0f
            },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "browser_push_parallax",
        )

        val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val viewportTopInset = statusBarTopPadding + prefs.topSafeAreaPaddingDp.dp

        // 1. Browser Viewport + Independent Per-Pane Floating Bars + Quick-Toggle Dropdown Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianVoid)
                .graphicsLayer {
                    translationX = -size.width * 0.25f * parallaxProgressState.value
                },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = viewportTopInset)
                    .testTag("browser_viewport_container"),
            ) {
                val viewportWidth = maxWidth
                val activeOutgoingTab = if (!isDualPaneActive) {
                    mergingOutgoingTab?.takeIf { out -> out.id != primaryTab?.id }
                } else {
                    null
                }
                val showSplitLayout = (isDualPaneActive && primaryTab != null && secondaryTab != null) ||
                    (activeOutgoingTab != null && primaryTab != null && splitExpandProgress > 0.01f)

                if (primaryTab != null) {
                    val effectiveSplitProgress = if (showSplitLayout) splitExpandProgress.coerceIn(0f, 1f) else 0f
                    val effectiveGapDp = if (showSplitLayout) {
                        (if (isDualPaneActive) centerHingeGapDp else lastDualHingeGapDp) * effectiveSplitProgress
                    } else {
                        0.dp
                    }
                    val halfWidth = ((viewportWidth - effectiveGapDp) / 2f).coerceAtLeast(0.dp)
                    val shrinkingWidth = (halfWidth * effectiveSplitProgress).coerceAtLeast(0.dp)
                    val expandingWidth = (viewportWidth - effectiveGapDp - shrinkingWidth).coerceAtLeast(0.dp)

                    val leftSlotTab: BrowserTabState?
                    val rightSlotTab: BrowserTabState?
                    val leftWidth: Dp
                    val rightWidth: Dp
                    val leftIsOutgoing: Boolean
                    val rightIsOutgoing: Boolean

                    if (!showSplitLayout) {
                        leftSlotTab = primaryTab
                        rightSlotTab = null
                        leftWidth = viewportWidth
                        rightWidth = 0.dp
                        leftIsOutgoing = false
                        rightIsOutgoing = false
                    } else if (isDualPaneActive) {
                        leftSlotTab = primaryTab
                        rightSlotTab = secondaryTab
                        leftWidth = if (mergeAnchorSide == 0) expandingWidth else shrinkingWidth
                        rightWidth = if (mergeAnchorSide == 0) shrinkingWidth else expandingWidth
                        leftIsOutgoing = false
                        rightIsOutgoing = false
                    } else if (mergeAnchorSide == 0) {
                        leftSlotTab = primaryTab
                        rightSlotTab = activeOutgoingTab
                        leftWidth = expandingWidth
                        rightWidth = shrinkingWidth
                        leftIsOutgoing = false
                        rightIsOutgoing = true
                    } else {
                        leftSlotTab = activeOutgoingTab
                        rightSlotTab = primaryTab
                        leftWidth = shrinkingWidth
                        rightWidth = expandingWidth
                        leftIsOutgoing = true
                        rightIsOutgoing = false
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(if (isDualPaneActive) "dual_pane_container" else "single_pane_viewport"),
                    ) {
                        if (leftSlotTab != null && leftWidth > 0.dp) {
                            key(leftSlotTab.id) {
                                Box(
                                    modifier = Modifier
                                        .width(leftWidth)
                                        .fillMaxHeight()
                                        .clipToBounds()
                                        .graphicsLayer {
                                            alpha = if (leftIsOutgoing) effectiveSplitProgress else 1f
                                        }
                                        .testTag(
                                            if (isDualPaneActive) "dual_pane_left_container"
                                            else "single_pane_container"
                                        ),
                                ) {
                                    WebViewPane(
                                        tab = leftSlotTab,
                                        preferences = prefs,
                                        webViewPool = webViewPool,
                                        tabLastUsedMs = tabLastUsedMs,
                                        tabScrollCallbacks = tabScrollCallbacks,
                                        consumeSavedScrollPosition = { id -> tabScrollPositions.remove(id) },
                                        viewModel = viewModel,
                                        isDualPane = isDualPaneActive,
                                        isFocusedPane = if (isDualPaneActive) focusedPaneIndex == 0 else true,
                                        onPaneFocused = { focusedPaneIndex = 0 },
                                        onScrollDelta = onLeftScrollDelta,
                                        onOpenFileChooser = onOpenFileChooser,
                                        onPermissionRequest = onWebPermissionRequest,
                                        onGeolocationPrompt = onGeolocationPrompt,
                                        onCreateNewWindow = createOnNewWindowForPane(0),
                                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                                        onShowCustomView = onShowFullscreenVideo,
                                        onHideCustomView = onHideFullscreenVideo,
                                        onSslError = onSslErrorReceived,
                                        onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(
                                                if (isDualPaneActive) "dual_pane_left_webview"
                                                else "fullscreen_webview"
                                            ),
                                    )

                                    if (!leftIsOutgoing) {
                                        PaneFloatingBarOverlay(
                                            paneIndex = 0,
                                            paneTab = leftSlotTab,
                                            paneSnapshot = leftDisplaySnapshot,
                                            paneUi = leftPaneUi,
                                            isCollapsed = isLeftBarCollapsed,
                                            isSplitPaneBar = isDualPaneActive,
                                            paneTagPrefix = if (isDualPaneActive) "left_" else "",
                                        )
                                    }
                                }
                            }
                        }

                        if (showSplitLayout && effectiveGapDp > 0.dp) {
                            key("dual_pane_hinge_divider") {
                                Box(
                                    modifier = Modifier
                                        .width(effectiveGapDp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF050810))
                                        .graphicsLayer { alpha = effectiveSplitProgress }
                                        .testTag("dual_pane_hinge_divider"),
                                )
                            }
                        }

                        if (rightSlotTab != null && rightWidth > 0.dp) {
                            key(rightSlotTab.id) {
                                val isRightKeptSingle = !isDualPaneActive && !rightIsOutgoing
                                Box(
                                    modifier = Modifier
                                        .width(rightWidth)
                                        .fillMaxHeight()
                                        .clipToBounds()
                                        .graphicsLayer {
                                            alpha = if (rightIsOutgoing) effectiveSplitProgress else 1f
                                        }
                                        .testTag(
                                            if (isDualPaneActive) "dual_pane_right_container"
                                            else "merging_right_container"
                                        ),
                                ) {
                                    WebViewPane(
                                        tab = rightSlotTab,
                                        preferences = prefs,
                                        webViewPool = webViewPool,
                                        tabLastUsedMs = tabLastUsedMs,
                                        tabScrollCallbacks = tabScrollCallbacks,
                                        consumeSavedScrollPosition = { id -> tabScrollPositions.remove(id) },
                                        viewModel = viewModel,
                                        isDualPane = isDualPaneActive,
                                        isFocusedPane = if (isDualPaneActive) focusedPaneIndex == 1 else true,
                                        onPaneFocused = { focusedPaneIndex = if (isDualPaneActive) 1 else 0 },
                                        onScrollDelta = if (isRightKeptSingle) onLeftScrollDelta else onRightScrollDelta,
                                        onOpenFileChooser = onOpenFileChooser,
                                        onPermissionRequest = onWebPermissionRequest,
                                        onGeolocationPrompt = onGeolocationPrompt,
                                        onCreateNewWindow = createOnNewWindowForPane(if (isRightKeptSingle) 0 else 1),
                                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                                        onShowCustomView = onShowFullscreenVideo,
                                        onHideCustomView = onHideFullscreenVideo,
                                        onSslError = onSslErrorReceived,
                                        onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag(
                                                if (isDualPaneActive) "dual_pane_right_webview"
                                                else "fullscreen_webview"
                                            ),
                                    )

                                    if (!rightIsOutgoing) {
                                        PaneFloatingBarOverlay(
                                            paneIndex = if (isRightKeptSingle) 0 else 1,
                                            paneTab = rightSlotTab,
                                            paneSnapshot = if (isRightKeptSingle) leftDisplaySnapshot else rightDisplaySnapshot,
                                            paneUi = if (isRightKeptSingle) leftPaneUi else rightPaneUi,
                                            isCollapsed = if (isRightKeptSingle) isLeftBarCollapsed else isRightBarCollapsed,
                                            isSplitPaneBar = isDualPaneActive,
                                            paneTagPrefix = if (isDualPaneActive) "right_" else "",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Anchored Quick-Toggle Dropdown Menu (anchored to active pane's gear in dual-pane mode)
            if (fullscreenVideoView == null) {
                QuickToggleDropdownMenu(
                    visible = isQuickMenuOpen,
                    snapshot = quickMenuDisplaySnapshot,
                    foldablePosture = foldablePosture,
                    isCurrentUrlBookmarked = isQuickMenuUrlBookmarked,
                    anchorPaneIndex = if (isDualPaneActive) quickMenuPaneIndex else null,
                    centerHingeGapDp = centerHingeGapDp,
                    onDismiss = { quickMenuPaneIndex = null },
                    onQuickToggle = { toggle ->
                        when (toggle) {
                            QuickToggleAction.DESKTOP_MODE -> {
                                viewModel.toggleDesktopMode(quickMenuTabId)
                                quickMenuWebView?.let { wv ->
                                    val nextDesktop = !(quickMenuTab?.isDesktopMode ?: false)
                                    applyRuntimeWebSettings(wv, prefs, nextDesktop)
                                    wv.reload()
                                }
                            }
                            QuickToggleAction.FORCE_DARK -> {
                                val nextDark = !prefs.algorithmicDarkeningEnabled
                                viewModel.setAlgorithmicDarkening(nextDark)
                                val updatedPrefs = prefs.copy(algorithmicDarkeningEnabled = nextDark)
                                webViewPool.forEach { (id, wv) ->
                                    val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                                    applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                                }
                            }
                            QuickToggleAction.AD_BLOCK -> {
                                viewModel.toggleAdBlock()
                                quickMenuWebView?.reload()
                            }
                            QuickToggleAction.JAVASCRIPT -> {
                                val nextJs = !prefs.javaScriptEnabled
                                viewModel.setJavaScriptEnabled(nextJs)
                                val updatedPrefs = prefs.copy(javaScriptEnabled = nextJs)
                                webViewPool.forEach { (id, wv) ->
                                    val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                                    applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                                }
                                quickMenuWebView?.reload()
                            }
                            QuickToggleAction.BLOCK_IMAGES -> {
                                val nextBlocked = !prefs.imagesBlocked
                                viewModel.setImagesBlocked(nextBlocked)
                                val updatedPrefs = prefs.copy(imagesBlocked = nextBlocked)
                                webViewPool.forEach { (id, wv) ->
                                    val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                                    applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                                }
                                quickMenuWebView?.reload()
                            }
                            QuickToggleAction.SCROLL_TO_PILL -> {
                                val nextScroll = !prefs.scrollToPillEnabled
                                viewModel.setScrollToPillEnabled(nextScroll)
                                if (!nextScroll) {
                                    leftPaneUi.resetScrollCollapse()
                                    rightPaneUi.resetScrollCollapse()
                                }
                            }
                            QuickToggleAction.DUAL_PANE -> {
                                val nextDual = !prefs.foldableDualPaneEnabled
                                if (!nextDual && isDualPaneActive) {
                                    triggerCloseSplitPane(keepLeftPane = activeQuickMenuPane == 0)
                                } else {
                                    viewModel.updateFoldableSettings(dualPaneEnabled = nextDual)
                                    if (nextDual && !isUnfolded) {
                                        Toast.makeText(
                                            context,
                                            "듀얼 뷰 활성화됨 (펼침 화면에서 좌우 분할 표시)",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                            QuickToggleAction.INCOGNITO_TAB -> {
                                quickMenuPaneIndex = null
                                captureTabThumbnail(quickMenuTabId, quickMenuWebView)
                                if (quickMenuTab?.isIncognito == true) {
                                    val nonIncognito = snapshot.tabs.firstOrNull { !it.isIncognito }
                                    if (nonIncognito != null) {
                                        viewModel.assignTabToPane(activeQuickMenuPane, nonIncognito.id)
                                    } else {
                                        viewModel.createTabForPane(activeQuickMenuPane, isIncognito = false)
                                    }
                                } else {
                                    viewModel.createTabForPane(activeQuickMenuPane, isIncognito = true)
                                }
                            }
                            QuickToggleAction.NEW_TAB -> {
                                quickMenuPaneIndex = null
                                captureTabThumbnail(quickMenuTabId, quickMenuWebView)
                                viewModel.createTabForPane(activeQuickMenuPane, isIncognito = false)
                            }
                            QuickToggleAction.BOOKMARK -> {
                                val url = quickMenuTab?.url.orEmpty()
                                if (url.isNotBlank()) {
                                    viewModel.toggleBookmark(url, quickMenuTab?.title.orEmpty())
                                }
                            }
                            QuickToggleAction.HISTORY -> {
                                quickMenuPaneIndex = null
                                activeOverlay = PushOverlay.HISTORY
                            }
                            QuickToggleAction.FIND_IN_PAGE -> {
                                quickMenuPaneIndex = null
                                quickMenuPaneUi.isFindInPageOpen = true
                            }
                            QuickToggleAction.PAGE_SUMMARY -> {
                                quickMenuPaneIndex = null
                                triggerPageSummaryForTab(quickMenuTab, quickMenuWebView, null)
                            }
                            QuickToggleAction.COPY_AND_SHARE -> {
                                quickMenuPaneIndex = null
                                shareUrl(context, quickMenuTab?.url.orEmpty(), copyToClipboard = true)
                            }
                        }
                    },
                    onOpenFullSettings = {
                        quickMenuPaneIndex = null
                        openSettingsDirectlyToAi = false
                        activeOverlay = PushOverlay.SETTINGS
                    },
                )
            }
        }

        // 2. Native Push Transition: Full-Screen Edge-to-Edge Settings
        AnimatedVisibility(
            visible = activeOverlay == PushOverlay.SETTINGS,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
        ) {
            SettingsSheet(
                snapshot = snapshot,
                foldablePosture = foldablePosture,
                initialOpenAiSettings = openSettingsDirectlyToAi,
                aiConnectionStatus = aiConnectionStatus,
                onDismiss = {
                    activeOverlay = PushOverlay.NONE
                    openSettingsDirectlyToAi = false
                },
                onUpdateBarGeometry = { corner, height, width, bottomOffset, bias, opacity ->
                    viewModel.updateBarGeometry(
                        cornerRadiusDp = corner,
                        heightDp = height,
                        widthFraction = width,
                        bottomOffsetDp = bottomOffset,
                        horizontalBias = bias,
                        glassOpacity = opacity,
                    )
                },
                onUpdateSafeAreaPadding = { topDp, bottomDp ->
                    viewModel.updateSafeAreaPadding(topDp = topDp, bottomDp = bottomDp)
                },
                onResetSafeAreaPadding = { viewModel.resetSafeAreaPadding() },
                onToggleQuickToggleAction = { viewModel.toggleQuickToggleAction(it) },
                onMoveQuickToggleAction = { action, delta ->
                    viewModel.moveQuickToggleAction(action, delta)
                },
                onUpdateDropdownCustomization = { layout, maxHeight, indicator ->
                    viewModel.updateDropdownCustomization(
                        layoutStyle = layout,
                        maxHeightDp = maxHeight,
                        indicatorStyle = indicator,
                    )
                },
                onUpdateFoldableSettings = { dual, hinge, sep, width, bias, autoExpand, longPressSplit, companionMode ->
                    viewModel.updateFoldableSettings(
                        dualPaneEnabled = dual,
                        hingeAvoidanceDp = hinge,
                        separateBarProfiles = sep,
                        unfoldedBarWidthFraction = width,
                        unfoldedBarHorizontalBias = bias,
                        autoExpandToolbarWhenUnfolded = autoExpand,
                        longPressSwipeSplitEnabled = longPressSplit,
                        splitCompanionMode = companionMode,
                    )
                },
                onResetFoldableSettings = { viewModel.resetFoldableSettings() },
                onUpdateGestureSettings = { enabled, upAction, downAction, leftRightTab, dblTap, sensitivity, longPressAi ->
                    viewModel.updateGestureSettings(
                        gesturesEnabled = enabled,
                        swipeUpAction = upAction,
                        swipeDownAction = downAction,
                        swipeLeftRightTabSwitch = leftRightTab,
                        doubleTapAction = dblTap,
                        gestureSensitivityDp = sensitivity,
                        longPressSwipeUpAiChatEnabled = longPressAi,
                    )
                },
                onResetGestureSettings = { viewModel.resetGestureSettings() },
                onSelectAiProfile = { viewModel.selectAiProfile(it) },
                onTestConnectionAndFetchModels = { profile ->
                    viewModel.testConnectionAndFetchModels(profile)
                },
                onUpdateAiProfile = { id, name, endpoint, apiKey, model, addModel, prompt, subtype ->
                    viewModel.updateAiProfile(
                        profileId = id,
                        name = name,
                        endpointOrCommand = endpoint,
                        apiKey = apiKey,
                        selectedModel = model,
                        addCustomModel = addModel,
                        systemPrompt = prompt,
                        providerSubtype = subtype,
                    )
                },
                onAddCustomAiProfile = { name, cliType, endpoint, apiKey, model ->
                    viewModel.addCustomAiProfile(
                        name = name,
                        cliType = cliType,
                        endpointOrCommand = endpoint,
                        apiKey = apiKey,
                        selectedModel = model,
                    )
                },
                onDeleteAiProfile = { viewModel.deleteAiProfile(it) },
                onResetCustomization = { viewModel.resetBarCustomization() },
                onToggleLeftAction = { viewModel.toggleLeftToolbarAction(it) },
                onToggleRightAction = { viewModel.toggleRightToolbarAction(it) },
                onResetToolbar = { viewModel.resetToolbarDefaults() },
                onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                onUpdateHomeUrl = { viewModel.setHomeUrl(it) },
                onToggleScrollToPill = { enabled ->
                    viewModel.setScrollToPillEnabled(enabled)
                    if (!enabled) {
                        leftPaneUi.resetScrollCollapse()
                        rightPaneUi.resetScrollCollapse()
                    }
                },
                onToggleExpandBarOnAddressEdit = { enabled ->
                    viewModel.setExpandBarOnAddressEdit(enabled)
                },
                onSelectTabSwitcherStyle = { style ->
                    viewModel.setTabSwitcherStyle(style)
                    leftPaneUi.isTabDrawerOpen = false
                    rightPaneUi.isTabDrawerOpen = false
                    gridTabSwitcherPaneIndex = null
                },
                onToggleJavaScript = { enabled ->
                    viewModel.setJavaScriptEnabled(enabled)
                    val updatedPrefs = prefs.copy(javaScriptEnabled = enabled)
                    webViewPool.forEach { (id, wv) ->
                        val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                        applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                    }
                    focusedWebView?.reload()
                },
                onToggleImagesBlocked = { blocked ->
                    viewModel.setImagesBlocked(blocked)
                    val updatedPrefs = prefs.copy(imagesBlocked = blocked)
                    webViewPool.forEach { (id, wv) ->
                        val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                        applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                    }
                    focusedWebView?.reload()
                },
                onToggleAlgorithmicDarkening = { enabled ->
                    viewModel.setAlgorithmicDarkening(enabled)
                    val updatedPrefs = prefs.copy(algorithmicDarkeningEnabled = enabled)
                    webViewPool.forEach { (id, wv) ->
                        val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                        applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                    }
                },
                onToggleDesktopMode = {
                    viewModel.toggleDesktopMode(focusedTabId)
                    focusedWebView?.let { wv ->
                        val nextDesktop = !(focusedTab?.isDesktopMode ?: false)
                        applyRuntimeWebSettings(wv, prefs, nextDesktop)
                        wv.reload()
                    }
                },
                onToggleAdBlock = {
                    viewModel.toggleAdBlock()
                    focusedWebView?.reload()
                },
                onClearCacheOnly = {
                    webViewPool.values.forEach { wv -> wv.clearCache(true) }
                },
                onClearHistoryOnly = {
                    webViewPool.values.forEach { wv -> wv.clearHistory() }
                    viewModel.clearCoreHistory()
                },
                onClearAllBrowsingData = {
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().apply {
                        removeAllCookies(null)
                        flush()
                    }
                    webViewPool.values.forEach { wv ->
                        wv.clearCache(true)
                        wv.clearFormData()
                        wv.clearHistory()
                    }
                    viewModel.clearCoreHistory()
                },
            )
        }

        // 3. Native Push Transition: History & Bookmarks
        AnimatedVisibility(
            visible = activeOverlay == PushOverlay.HISTORY,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
        ) {
            HistoryAndBookmarksSheet(
                snapshot = snapshot,
                historyEntries = viewModel.getHistory(100),
                onDismiss = { activeOverlay = PushOverlay.NONE },
                onOpenUrl = { url ->
                    activeOverlay = PushOverlay.NONE
                    focusedWebView?.loadUrl(url)
                },
                onRemoveBookmark = { bm: BookmarkEntry ->
                    viewModel.toggleBookmark(bm.url, bm.title)
                },
                onClearHistory = {
                    webViewPool.values.forEach { wv -> wv.clearHistory() }
                    viewModel.clearCoreHistory()
                },
            )
        }

        // 4. Native Push Transition: AI & Local DOM Page Summary
        AnimatedVisibility(
            visible = activeOverlay == PushOverlay.SUMMARY,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            ),
        ) {
            PageSummarySheet(
                summaryState = pageSummaryState,
                unifiedRadius = prefs.barCornerRadiusDp.dp,
                topSafeAreaPaddingDp = prefs.topSafeAreaPaddingDp.dp,
                bottomSafeAreaPaddingDp = prefs.bottomSafeAreaPaddingDp.dp,
                onDismiss = { activeOverlay = PushOverlay.NONE },
                onAskAiQuestion = { q -> triggerPageSummaryForTab(focusedTab, focusedWebView, q) },
                onOpenAiSettings = {
                    openSettingsDirectlyToAi = true
                    activeOverlay = PushOverlay.SETTINGS
                },
            )
        }

        // 4.5 Physical Bottom-Origin Modal Expansion from Address Bar: Liquid Glass AI Chat
        AnimatedVisibility(
            visible = activeOverlay == PushOverlay.AI_CHAT,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 170)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.56f))
                    .pointerInput(Unit) {
                        detectTapGestures { /* Consume outside touches; dismiss via Back or header swipe-down */ }
                    }
                    .testTag("ai_chat_modal_backdrop"),
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = activeOverlay == PushOverlay.AI_CHAT,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> (fullHeight * 0.72f).toInt() },
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + scaleIn(
                    initialScale = 0.88f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> (fullHeight * 0.72f).toInt() },
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + scaleOut(
                    targetScale = 0.88f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                AiChatSheet(
                    chatState = aiChatUiState,
                    activeProfile = prefs.activeAiProfile(),
                    allProfiles = prefs.aiProfiles,
                    connectionStatus = aiConnectionStatus,
                    unifiedRadius = prefs.barCornerRadiusDp.dp,
                    glassOpacity = prefs.glassOpacity,
                    topSafeAreaPaddingDp = prefs.topSafeAreaPaddingDp.dp,
                    bottomSafeAreaPaddingDp = prefs.bottomSafeAreaPaddingDp.dp,
                    onDismiss = { activeOverlay = PushOverlay.NONE },
                    onSendMessage = sendAiChatMessage,
                    onClearChat = {
                        aiChatUiState = aiChatUiState.copy(
                            messages = emptyList(),
                            errorMessage = null,
                        )
                    },
                    onSelectAiProfile = { profileId ->
                        viewModel.selectAiProfile(profileId)
                    },
                    onSelectAiModel = { profileId, modelName ->
                        viewModel.updateAiProfile(
                            profileId = profileId,
                            selectedModel = modelName,
                        )
                    },
                    onRefreshModels = { profile ->
                        viewModel.testConnectionAndFetchModels(profile)
                    },
                    onOpenAiSettings = {
                        openSettingsDirectlyToAi = true
                        activeOverlay = PushOverlay.SETTINGS
                    },
                )
            }
        }

        // 5. Full-Screen 2-Column Grid Preview Tab Switcher
        AnimatedVisibility(
            visible = isGridTabSwitcherOpen && activeOverlay == PushOverlay.NONE && fullscreenVideoView == null,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f),
        ) {
            GridTabSwitcherScreen(
                snapshot = gridSwitcherSnapshot,
                thumbnails = tabThumbnails,
                formatDisplayUrl = { url -> viewModel.formatDisplayUrl(url) },
                onSelectTab = { tabId ->
                    val targetPane = gridSwitcherTargetPane
                    val sourceTabId = if (targetPane == 1 && isDualPaneActive) {
                        secondaryTab?.id ?: snapshot.activeTabId
                    } else {
                        primaryTab?.id ?: snapshot.activeTabId
                    }
                    captureTabThumbnail(sourceTabId, webViewPool[sourceTabId])
                    focusedPaneIndex = targetPane
                    viewModel.assignTabToPane(targetPane, tabId)
                    gridTabSwitcherPaneIndex = null
                    (if (targetPane == 1 && isDualPaneActive) rightPaneUi else leftPaneUi).resetScrollCollapse()
                },
                onCloseTab = { tabId ->
                    viewModel.closeTab(tabId)
                },
                onCreateTab = {
                    val targetPane = gridSwitcherTargetPane
                    val sourceTabId = if (targetPane == 1 && isDualPaneActive) {
                        secondaryTab?.id ?: snapshot.activeTabId
                    } else {
                        primaryTab?.id ?: snapshot.activeTabId
                    }
                    captureTabThumbnail(sourceTabId, webViewPool[sourceTabId])
                    focusedPaneIndex = targetPane
                    viewModel.createTabForPane(targetPane)
                    gridTabSwitcherPaneIndex = null
                    (if (targetPane == 1 && isDualPaneActive) rightPaneUi else leftPaneUi).resetScrollCollapse()
                },
                onCreateTabGroup = { title, tabIds, colorIndex ->
                    viewModel.createTabGroup(title = title, tabIds = tabIds, colorIndex = colorIndex)
                },
                onRemoveTabFromGroup = { groupId, tabId ->
                    viewModel.removeTabFromGroup(groupId = groupId, tabId = tabId)
                },
                onUngroupTabGroup = { groupId ->
                    viewModel.ungroupTabGroup(groupId)
                },
                onDismiss = {
                    gridTabSwitcherPaneIndex = null
                },
            )
        }

        // 6. Fullscreen HTML5 Video Playback Host (renders above browser & hides floating bar)
        val activeCustomVideo = fullscreenVideoView
        if (activeCustomVideo != null) {
            BackHandler(enabled = true) {
                onHideFullscreenVideo()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("fullscreen_video_overlay"),
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    update = { container ->
                        val current = if (container.childCount > 0) container.getChildAt(0) else null
                        if (current !== activeCustomVideo) {
                            container.removeAllViews()
                            (activeCustomVideo.parent as? ViewGroup)?.removeView(activeCustomVideo)
                            container.addView(
                                activeCustomVideo,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // 7. SSL/TLS Certificate Error Confirmation Dialog (defaults to safe cancel)
        val activeSslError = pendingSslError
        if (activeSslError != null) {
            val dialogShape = RoundedCornerShape(prefs.barCornerRadiusDp.dp)
            AlertDialog(
                onDismissRequest = {
                    runCatching { activeSslError.handler.cancel() }
                    pendingSslError = null
                },
                shape = dialogShape,
                containerColor = ObsidianElevated,
                titleContentColor = AmberWarn,
                textContentColor = PureFrost,
                title = {
                    Text(
                        text = "안전하지 않은 보안 연결",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberWarn,
                    )
                },
                text = {
                    Text(
                        text = "${activeSslError.host}\n\n${describeSslErrorReason(activeSslError.errorCode)}\n안전하게 이전 화면으로 돌아가는 것을 권장합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostSlate,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            runCatching { activeSslError.handler.cancel() }
                            pendingSslError = null
                        },
                        shape = RoundedCornerShape((prefs.barCornerRadiusDp - 4f).coerceAtLeast(6f).dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyan,
                            contentColor = ObsidianVoid,
                        ),
                        modifier = Modifier.testTag("ssl_error_cancel_button"),
                    ) {
                        Text("안전하게 돌아가기")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            runCatching { activeSslError.handler.proceed() }
                            pendingSslError = null
                        },
                        modifier = Modifier.testTag("ssl_error_proceed_button"),
                    ) {
                        Text("위험 감수하고 계속", color = AmberWarn)
                    }
                },
            )
        }
    }
}

private data class PendingSslError(
    val host: String,
    val errorCode: Int,
    val handler: SslErrorHandler,
)

private fun describeSslErrorReason(errorCode: Int): String = when (errorCode) {
    SslError.SSL_EXPIRED -> "이 사이트의 보안 인증서가 만료되었습니다."
    SslError.SSL_IDMISMATCH -> "인증서의 도메인 이름이 접속하려는 주소와 일치하지 않습니다."
    SslError.SSL_UNTRUSTED -> "신뢰할 수 없는 기관에서 발급한 인증서입니다."
    SslError.SSL_NOTYETVALID -> "아직 유효 기간이 시작되지 않은 인증서입니다."
    SslError.SSL_DATE_INVALID -> "인증서 날짜가 올바르지 않습니다."
    else -> "보안 연결(SSL/TLS) 인증서 검증에 실패했습니다."
}

private fun flushCookiesSafely() {
    runCatching { CookieManager.getInstance().flush() }
}

@Composable
private fun WebViewPane(
    tab: BrowserTabState,
    preferences: BrowserPreferences,
    webViewPool: MutableMap<String, WebView>,
    tabLastUsedMs: MutableMap<String, Long>,
    tabScrollCallbacks: MutableMap<String, (Int, Int, Boolean) -> Unit>,
    consumeSavedScrollPosition: (String) -> Pair<Int, Int>?,
    viewModel: BrowserViewModel,
    isDualPane: Boolean,
    isFocusedPane: Boolean,
    onPaneFocused: () -> Unit,
    onScrollDelta: (dy: Int, scrollY: Int, isScrollable: Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit,
    onPermissionRequest: (PermissionRequest) -> Unit,
    onGeolocationPrompt: (String, GeolocationPermissions.Callback) -> Unit,
    onCreateNewWindow: (Message) -> Boolean,
    onCloseWindow: (WebView?) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onSslError: (SslErrorHandler, SslError?) -> Unit,
    onCaptureThumbnail: (tabId: String, webView: WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnPaneFocused by rememberUpdatedState(onPaneFocused)
    val latestOnScrollDelta by rememberUpdatedState(onScrollDelta)
    val latestOnOpenFileChooser by rememberUpdatedState(onOpenFileChooser)
    val latestOnPermissionRequest by rememberUpdatedState(onPermissionRequest)
    val latestOnGeolocationPrompt by rememberUpdatedState(onGeolocationPrompt)
    val latestOnCreateNewWindow by rememberUpdatedState(onCreateNewWindow)
    val latestOnCloseWindow by rememberUpdatedState(onCloseWindow)
    val latestOnShowCustomView by rememberUpdatedState(onShowCustomView)
    val latestOnHideCustomView by rememberUpdatedState(onHideCustomView)
    val latestOnSslError by rememberUpdatedState(onSslError)
    val latestOnCaptureThumbnail by rememberUpdatedState(onCaptureThumbnail)

    var attachedContainer by remember { mutableStateOf<FrameLayout?>(null) }
    val paneScrollCallback = remember(tab.id) {
        { dy: Int, scrollY: Int, isScrollable: Boolean ->
            latestOnScrollDelta(dy, scrollY, isScrollable)
        }
    }

    DisposableEffect(tab.id) {
        tabScrollCallbacks[tab.id] = paneScrollCallback
        onDispose {
            if (tabScrollCallbacks[tab.id] === paneScrollCallback) {
                tabScrollCallbacks.remove(tab.id)
            }
            val host = attachedContainer
            webViewPool[tab.id]?.let { wv ->
                if (host != null && wv.parent === host) {
                    wv.setOnTouchListener(null)
                    wv.setOnScrollChangeListener(null)
                    host.removeView(wv)
                }
            }
        }
    }

    Column(modifier = modifier) {
        if (isDualPane) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(if (isFocusedPane) IceCyan else Color.White.copy(alpha = 0.08f))
            )
        }

        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }.also { attachedContainer = it }
            },
            update = { container ->
                attachedContainer = container
                tabScrollCallbacks[tab.id] = paneScrollCallback
                val targetWebView = webViewPool.getOrPut(tab.id) {
                    tabLastUsedMs[tab.id] = System.currentTimeMillis()
                    createConfiguredWebView(
                        context = container.context,
                        tabId = tab.id,
                        initialUrl = tab.url,
                        restoredBundle = viewModel.getTabBundle(tab.id),
                        restoredScrollPosition = consumeSavedScrollPosition(tab.id),
                        preferences = preferences,
                        isDesktopMode = tab.isDesktopMode,
                        viewModel = viewModel,
                        onScrollDelta = { dy, sy, scrollable ->
                            tabScrollCallbacks[tab.id]?.invoke(dy, sy, scrollable)
                        },
                        onOpenFileChooser = { cb, params ->
                            latestOnOpenFileChooser(cb, params)
                        },
                        onPermissionRequest = { req ->
                            latestOnPermissionRequest(req)
                        },
                        onGeolocationPrompt = { origin, cb ->
                            latestOnGeolocationPrompt(origin, cb)
                        },
                        onCreateNewWindow = { msg ->
                            latestOnCreateNewWindow(msg)
                        },
                        onCloseWindow = { wv ->
                            latestOnCloseWindow(wv)
                        },
                        onShowCustomView = { view, cb ->
                            latestOnShowCustomView(view, cb)
                        },
                        onHideCustomView = {
                            latestOnHideCustomView()
                        },
                        onSslError = { handler, err ->
                            latestOnSslError(handler, err)
                        },
                        onCaptureThumbnail = { wv ->
                            latestOnCaptureThumbnail(tab.id, wv)
                        },
                    )
                }

                // Always re-bind independent scroll & touch listeners to this pane's live callbacks
                // so pooled WebViews moved between Single, Left, and Right panes never keep stale hooks.
                targetWebView.setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
                    val dy = scrollY - oldScrollY
                    val isScrollable = v.canScrollVertically(1) || v.canScrollVertically(-1)
                    paneScrollCallback(dy, scrollY, isScrollable)
                }

                targetWebView.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        latestOnPaneFocused()
                    }
                    false
                }

                applyRuntimeWebSettings(
                    webView = targetWebView,
                    preferences = preferences,
                    isDesktopMode = tab.isDesktopMode,
                )

                val currentChild = if (container.childCount > 0) container.getChildAt(0) else null
                if (currentChild !== targetWebView) {
                    container.removeAllViews()
                    (targetWebView.parent as? ViewGroup)?.removeView(targetWebView)
                    container.addView(
                        targetWebView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            },
            onRelease = { container ->
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child is WebView) {
                        child.setOnTouchListener(null)
                        child.setOnScrollChangeListener(null)
                    }
                }
                container.removeAllViews()
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun createPopupHelper(
    context: Context,
    viewModel: BrowserViewModel,
    webViewPool: MutableMap<String, WebView>,
    tabLastUsedMs: MutableMap<String, Long>,
    tabScrollCallbacks: MutableMap<String, (Int, Int, Boolean) -> Unit>,
    fallbackScrollDelta: (Int, Int, Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit,
    onPermissionRequest: (PermissionRequest) -> Unit,
    onGeolocationPrompt: (String, GeolocationPermissions.Callback) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onSslError: (SslErrorHandler, SslError?) -> Unit,
    onFocusPrimaryPane: () -> Unit,
    onCaptureThumbnail: (String, WebView) -> Unit,
    resultMsg: Message,
): Boolean {
    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
    val nextSnap = viewModel.createTab(initialUrl = "about:blank", isIncognito = false)
    val newTab = nextSnap.activeTab ?: return false
    onFocusPrimaryPane()
    val popupWebView = createConfiguredWebView(
        context = context,
        tabId = newTab.id,
        initialUrl = null,
        restoredBundle = null,
        restoredScrollPosition = null,
        preferences = nextSnap.preferences,
        isDesktopMode = newTab.isDesktopMode,
        viewModel = viewModel,
        onScrollDelta = { dy, sy, scrollable ->
            val registered = tabScrollCallbacks[newTab.id]
            if (registered != null) {
                registered(dy, sy, scrollable)
            } else {
                fallbackScrollDelta(dy, sy, scrollable)
            }
        },
        onOpenFileChooser = onOpenFileChooser,
        onPermissionRequest = onPermissionRequest,
        onGeolocationPrompt = onGeolocationPrompt,
        onCreateNewWindow = { nestedMsg ->
            createPopupHelper(
                context = context,
                viewModel = viewModel,
                webViewPool = webViewPool,
                tabLastUsedMs = tabLastUsedMs,
                tabScrollCallbacks = tabScrollCallbacks,
                fallbackScrollDelta = fallbackScrollDelta,
                onOpenFileChooser = onOpenFileChooser,
                onPermissionRequest = onPermissionRequest,
                onGeolocationPrompt = onGeolocationPrompt,
                onShowCustomView = onShowCustomView,
                onHideCustomView = onHideCustomView,
                onSslError = onSslError,
                onFocusPrimaryPane = onFocusPrimaryPane,
                onCaptureThumbnail = onCaptureThumbnail,
                resultMsg = nestedMsg,
            )
        },
        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
        onShowCustomView = onShowCustomView,
        onHideCustomView = onHideCustomView,
        onSslError = onSslError,
        onCaptureThumbnail = { wv -> onCaptureThumbnail(newTab.id, wv) },
    )
    webViewPool[newTab.id] = popupWebView
    tabLastUsedMs[newTab.id] = System.currentTimeMillis()
    transport.webView = popupWebView
    runCatching { resultMsg.sendToTarget() }
    return true
}

private fun closeWindowByWebView(
    window: WebView?,
    webViewPool: MutableMap<String, WebView>,
    viewModel: BrowserViewModel,
) {
    if (window == null) return
    val tabId = webViewPool.entries.firstOrNull { it.value === window }?.key ?: return
    viewModel.closeTab(tabId)
}

private fun destroyWebViewCleanly(webView: WebView) {
    runCatching {
        webView.setOnScrollChangeListener(null)
        webView.setOnTouchListener(null)
        webView.setDownloadListener(null)
        webView.webChromeClient = null
        webView.webViewClient = WebViewClient()
    }
    runCatching { (webView.parent as? ViewGroup)?.removeView(webView) }
    runCatching { webView.removeJavascriptInterface(BLOB_DOWNLOAD_JS_BRIDGE_NAME) }
    runCatching { webView.stopLoading() }
    runCatching { webView.onPause() }
    runCatching { webView.loadUrl("about:blank") }
    runCatching { webView.clearFormData() }
    runCatching { webView.clearHistory() }
    runCatching { webView.removeAllViews() }
    runCatching { webView.destroy() }
}

private fun shareUrl(
    context: Context,
    url: String,
    copyToClipboard: Boolean,
) {
    if (url.isBlank()) return
    if (copyToClipboard) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("URL", url))
        Toast.makeText(context, "URL이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    val chooser = Intent.createChooser(sendIntent, null)
    runCatching { context.startActivity(chooser) }
}

private fun extractDomPageSummary(
    webView: WebView,
    json: Json,
    fallbackUrl: String,
    fallbackTitle: String,
    onResult: (PageSummaryState) -> Unit,
) {
    val script = """
        (function() {
          try {
            var metaDescEl = document.querySelector('meta[name="description"], meta[property="og:description"]');
            var desc = metaDescEl ? (metaDescEl.getAttribute('content') || '').trim() : '';
            var headings = Array.prototype.slice.call(document.querySelectorAll('h1, h2, h3'))
              .map(function(el) { return (el.innerText || '').trim().replace(/\s+/g, ' '); })
              .filter(function(t) { return t.length > 3; })
              .slice(0, 6);
            var blocks = Array.prototype.slice.call(document.querySelectorAll('article p, main p, p'))
              .map(function(el) { return (el.innerText || '').trim().replace(/\s+/g, ' '); })
              .filter(function(t) { return t.length > 30; });
            var seen = {};
            var uniqueSentences = [];
            for (var i = 0; i < blocks.length && uniqueSentences.length < 5; i++) {
              if (!seen[blocks[i]]) {
                seen[blocks[i]] = true;
                uniqueSentences.push(blocks[i]);
              }
            }
            var bodyText = (document.body ? (document.body.innerText || '') : '').trim();
            var words = bodyText ? bodyText.split(/\s+/).length : 0;
            return JSON.stringify({
              title: (document.title || '').trim(),
              url: location.href || '',
              description: desc,
              headings: headings,
              sentences: uniqueSentences,
              wordCount: words,
              bodyText: bodyText.slice(0, 6500)
            });
          } catch (e) {
            return JSON.stringify({ title: '', url: '', description: '', headings: [], sentences: [], wordCount: 0, bodyText: '' });
          }
        })();
    """.trimIndent()

    webView.evaluateJavascript(script) { rawResult ->
        val parsed = runCatching {
            val unquoted = if (rawResult != null && rawResult.startsWith("\"")) {
                json.decodeFromString<String>(rawResult)
            } else {
                rawResult.orEmpty()
            }
            if (unquoted.isBlank() || unquoted == "null") {
                null
            } else {
                json.decodeFromString<DomSummaryPayload>(unquoted)
            }
        }.getOrNull()

        if (parsed == null) {
            onResult(
                PageSummaryState(
                    url = fallbackUrl,
                    title = fallbackTitle,
                    isLoading = false,
                    errorMessage = "JavaScript가 비활성화되었거나 본문을 추출할 수 없는 페이지입니다.",
                )
            )
        } else {
            onResult(
                PageSummaryState(
                    url = parsed.url.ifBlank { fallbackUrl },
                    title = parsed.title.ifBlank { fallbackTitle },
                    metaDescription = parsed.description,
                    headings = parsed.headings,
                    keySentences = parsed.sentences,
                    wordCount = parsed.wordCount,
                    bodyPreviewText = parsed.bodyText,
                    isLoading = false,
                )
            )
        }
    }
}

private fun buildFileChooserIntent(params: WebChromeClient.FileChooserParams?): Intent {
    val rawAcceptTypes = params?.acceptTypes
        ?.mapNotNull { entry ->
            entry.trim().takeIf { it.isNotEmpty() }
        }
        ?.flatMap { it.split(',') }
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        .orEmpty()

    val validMimes = rawAcceptTypes.filter { it.contains('/') }
    val primaryMime = if (validMimes.size == 1) validMimes.first() else "*/*"

    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = primaryMime
        if (validMimes.size > 1) {
            putExtra(Intent.EXTRA_MIME_TYPES, validMimes.toTypedArray())
        }
        if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }
}

private fun extractChosenFileUris(data: Intent?): Array<Uri>? {
    if (data == null) return null
    val clipData = data.clipData
    if (clipData != null && clipData.itemCount > 0) {
        val list = ArrayList<Uri>(clipData.itemCount)
        for (i in 0 until clipData.itemCount) {
            clipData.getItemAt(i)?.uri?.let { list.add(it) }
        }
        return if (list.isEmpty()) null else list.toTypedArray()
    }
    val singleUri = data.data
    return if (singleUri != null) arrayOf(singleUri) else null
}

private fun resolveGrantableWebResources(
    context: Context,
    requestedResources: Array<String>?,
    overrideGrantMap: Map<String, Boolean> = emptyMap(),
): List<String> {
    if (requestedResources.isNullOrEmpty()) return emptyList()
    val granted = mutableListOf<String>()
    for (resource in requestedResources) {
        when (resource) {
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                granted += resource
            }
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                val ok = overrideGrantMap[Manifest.permission.CAMERA]
                    ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED)
                if (ok) granted += resource
            }
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                val ok = overrideGrantMap[Manifest.permission.RECORD_AUDIO]
                    ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED)
                if (ok) granted += resource
            }
        }
    }
    return granted
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    tabId: String,
    initialUrl: String?,
    restoredBundle: Bundle?,
    restoredScrollPosition: Pair<Int, Int>?,
    preferences: BrowserPreferences,
    isDesktopMode: Boolean,
    viewModel: BrowserViewModel,
    onScrollDelta: (dy: Int, scrollY: Int, isScrollable: Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit,
    onPermissionRequest: (PermissionRequest) -> Unit,
    onGeolocationPrompt: (String, GeolocationPermissions.Callback) -> Unit,
    onCreateNewWindow: (Message) -> Boolean,
    onCloseWindow: (WebView?) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onSslError: (SslErrorHandler, SslError?) -> Unit,
    onCaptureThumbnail: (WebView) -> Unit,
): WebView {
    var pendingScrollRestore = restoredScrollPosition

    return WebViewWarmupPool.acquireOrCreate(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.parseColor("#090D16"))

        val targetWebView = this
        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(targetWebView, true)
            }
        }

        settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            loadsImagesAutomatically = !preferences.imagesBlocked
            blockNetworkImage = preferences.imagesBlocked
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }

        applyRuntimeWebSettings(
            webView = this,
            preferences = preferences,
            isDesktopMode = isDesktopMode,
        )

        runCatching {
            removeJavascriptInterface(BLOB_DOWNLOAD_JS_BRIDGE_NAME)
            addJavascriptInterface(BlobDownloadBridge(context), BLOB_DOWNLOAD_JS_BRIDGE_NAME)
        }

        setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            val isScrollable = v.canScrollVertically(1) || v.canScrollVertically(-1)
            onScrollDelta(dy, scrollY, isScrollable)
        }

        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            dispatchWebViewDownload(
                context = context,
                webView = targetWebView,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
            )
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val url = request?.url?.toString().orEmpty()
                if (url.isNotEmpty() && viewModel.shouldBlockUrl(url)) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream(ByteArray(0)),
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val uri = request?.url ?: return false
                return WebViewExternalNavigator.handleUrl(
                    context = context,
                    webView = view,
                    uri = uri,
                )
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onScrollDelta(0, 0, false)
                val resolved = url ?: view?.url.orEmpty()
                viewModel.updateTabNavigation(
                    tabId = tabId,
                    url = resolved,
                    title = view?.title.orEmpty(),
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                    isLoading = true,
                    progress = view?.progress ?: 10,
                )
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean,
            ) {
                val resolved = url ?: view?.url.orEmpty()
                viewModel.updateTabNavigation(
                    tabId = tabId,
                    url = resolved,
                    title = view?.title.orEmpty(),
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                    isLoading = (view?.progress ?: 100) < 100,
                    progress = view?.progress ?: 100,
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val resolved = url ?: view?.url.orEmpty()
                val title = view?.title.orEmpty()
                viewModel.updateTabNavigation(
                    tabId = tabId,
                    url = resolved,
                    title = title,
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                    isLoading = false,
                    progress = 100,
                )
                viewModel.recordVisit(resolved, title)
                if (view != null) {
                    val savedPos = pendingScrollRestore
                    if (savedPos != null) {
                        pendingScrollRestore = null
                        if (savedPos.first > 0 || savedPos.second > 0) {
                            view.post {
                                runCatching { view.scrollTo(savedPos.first, savedPos.second) }
                            }
                        }
                    }
                    onCaptureThumbnail(view)
                    val isIncognito = viewModel.snapshotFlow.value.tabs
                        .firstOrNull { it.id == tabId }
                        ?.isIncognito == true
                    if (!isIncognito) {
                        flushCookiesSafely()
                        runCatching {
                            val bundle = Bundle()
                            view.saveState(bundle)
                            if (!bundle.isEmpty) {
                                viewModel.saveTabBundle(tabId, bundle)
                            }
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val resolved = request.url?.toString() ?: view?.url.orEmpty()
                    viewModel.updateTabNavigation(
                        tabId = tabId,
                        url = resolved,
                        title = view?.title.orEmpty(),
                        canGoBack = view?.canGoBack() == true,
                        canGoForward = view?.canGoForward() == true,
                        isLoading = false,
                        progress = 100,
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                if (handler == null) return
                onSslError(handler, error)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val resolved = view?.url.orEmpty()
                viewModel.updateTabNavigation(
                    tabId = tabId,
                    url = resolved,
                    title = view?.title.orEmpty(),
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                    isLoading = newProgress in 1..99,
                    progress = newProgress,
                )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                viewModel.updateTabNavigation(
                    tabId = tabId,
                    url = view?.url.orEmpty(),
                    title = title.orEmpty(),
                    canGoBack = view?.canGoBack() == true,
                    canGoForward = view?.canGoForward() == true,
                    isLoading = (view?.progress ?: 100) < 100,
                    progress = view?.progress ?: 100,
                )
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                return if (resultMsg != null) {
                    onCreateNewWindow(resultMsg)
                } else {
                    false
                }
            }

            override fun onCloseWindow(window: WebView?) {
                onCloseWindow(window)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                onOpenFileChooser(filePathCallback, fileChooserParams)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request != null) {
                    onPermissionRequest(request)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?,
            ) {
                if (!origin.isNullOrBlank() && callback != null) {
                    onGeolocationPrompt(origin, callback)
                } else {
                    callback?.invoke(origin.orEmpty(), false, false)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null) {
                    onShowCustomView(view, callback)
                } else {
                    callback?.onCustomViewHidden()
                }
            }

            override fun onHideCustomView() {
                onHideCustomView()
            }
        }

        val restored = if (restoredBundle != null && !restoredBundle.isEmpty) {
            runCatching { restoreState(restoredBundle) != null }.getOrDefault(false)
        } else {
            false
        }
        if (restored) {
            viewModel.updateTabNavigation(
                tabId = tabId,
                url = url.orEmpty().ifBlank { initialUrl.orEmpty() },
                title = title.orEmpty(),
                canGoBack = canGoBack(),
                canGoForward = canGoForward(),
                isLoading = false,
                progress = 100,
            )
        } else {
            pendingScrollRestore = null
            if (!initialUrl.isNullOrBlank()) {
                loadUrl(initialUrl)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun applyRuntimeWebSettings(
    webView: WebView,
    preferences: BrowserPreferences,
    isDesktopMode: Boolean,
) {
    webView.settings.apply {
        javaScriptEnabled = preferences.javaScriptEnabled
        loadsImagesAutomatically = !preferences.imagesBlocked
        blockNetworkImage = preferences.imagesBlocked
        useWideViewPort = true
        loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = false
        }
        val desiredUa = if (isDesktopMode) {
            DESKTOP_USER_AGENT
        } else {
            WebViewWarmupPool.getCleanMobileUserAgent(webView.context, userAgentString)
        }
        if (!desiredUa.isNullOrBlank() && userAgentString != desiredUa) {
            userAgentString = desiredUa
        }
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(
            webView.settings,
            preferences.algorithmicDarkeningEnabled,
        )
    }
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
