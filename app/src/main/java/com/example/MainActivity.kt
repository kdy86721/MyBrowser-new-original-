package com.example

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.core.AiCliHttpClient
import com.example.core.BookmarkEntry
import com.example.core.BrowserCoreBridge
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.core.BrowserTabState
import com.example.core.HistoryEntry
import com.example.core.LocalKotlinCore
import com.example.core.MissingApiKeyException
import com.example.core.QuickToggleAction
import com.example.core.QuickToggleLayoutStyle
import com.example.core.SearchEngine
import com.example.core.ToggleIndicatorStyle
import com.example.core.ToolbarAction
import com.example.ui.components.FloatingControlBar
import com.example.ui.components.FoldablePostureUiState
import com.example.ui.components.QuickToggleDropdownMenu
import com.example.ui.screens.GridTabSwitcherScreen
import com.example.ui.screens.HistoryAndBookmarksSheet
import com.example.ui.screens.PageSummarySheet
import com.example.ui.screens.PageSummaryState
import com.example.ui.screens.SettingsSheet
import com.example.ui.theme.IceCyan
import com.example.ui.theme.MyBrowserTheme
import com.example.ui.theme.ObsidianVoid
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
}

class BrowserViewModel(
    private val core: BrowserCoreBridge = LocalKotlinCore(),
) : ViewModel() {

    val snapshotFlow: StateFlow<BrowserCoreSnapshot> = core.snapshotFlow

    // Retained per-tab WebView state Bundles across configuration / activity restarts
    private val savedTabBundles = mutableMapOf<String, Bundle>()

    fun saveTabBundle(tabId: String, bundle: Bundle) {
        savedTabBundles[tabId] = bundle
    }

    fun getTabBundle(tabId: String): Bundle? = savedTabBundles[tabId]

    fun removeTabBundle(tabId: String) {
        savedTabBundles.remove(tabId)
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
        savedTabBundles.remove(tabId)
        return core.closeTab(tabId)
    }

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
    ) {
        core.updateFoldableSettings(
            dualPaneEnabled = dualPaneEnabled,
            hingeAvoidanceDp = hingeAvoidanceDp,
            separateBarProfiles = separateBarProfiles,
            unfoldedBarWidthFraction = unfoldedBarWidthFraction,
            unfoldedBarHorizontalBias = unfoldedBarHorizontalBias,
            autoExpandToolbarWhenUnfolded = autoExpandToolbarWhenUnfolded,
        )
    }

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
    ) {
        core.updateGestureSettings(
            gesturesEnabled = gesturesEnabled,
            swipeUpAction = swipeUpAction,
            swipeDownAction = swipeDownAction,
            swipeLeftRightTabSwitch = swipeLeftRightTabSwitch,
            doubleTapAction = doubleTapAction,
            gestureSensitivityDp = gestureSensitivityDp,
        )
    }

    fun resetGestureSettings() {
        core.resetGestureSettings()
    }

    fun selectAiProfile(profileId: String) {
        core.selectAiProfile(profileId)
    }

    fun updateAiProfile(
        profileId: String,
        name: String? = null,
        endpointOrCommand: String? = null,
        apiKey: String? = null,
        selectedModel: String? = null,
        addCustomModel: String? = null,
        temperature: Float? = null,
        systemPrompt: String? = null,
    ) {
        core.updateAiProfile(
            profileId = profileId,
            name = name,
            endpointOrCommand = endpointOrCommand,
            apiKey = apiKey,
            selectedModel = selectedModel,
            addCustomModel = addCustomModel,
            temperature = temperature,
            systemPrompt = systemPrompt,
        )
    }

    fun addCustomAiProfile(
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
        temperature: Float = 0.4f,
    ) {
        core.addCustomAiProfile(
            name = name,
            cliType = cliType,
            endpointOrCommand = endpointOrCommand,
            apiKey = apiKey,
            selectedModel = selectedModel,
            temperature = temperature,
        )
    }

    fun deleteAiProfile(profileId: String) {
        core.deleteAiProfile(profileId)
    }

    fun resetBarCustomization() {
        core.resetBarCustomization()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedStateJson = getSharedPreferences(prefsName, MODE_PRIVATE)
            .getString(prefsKeyState, null)

        setContent {
            val browserViewModel: BrowserViewModel = viewModel()
            viewModelRef = browserViewModel

            LaunchedEffect(Unit) {
                browserViewModel.restoreSerializedState(savedStateJson)
            }

            MyBrowserTheme {
                MyBrowserRootScreen(viewModel = browserViewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModelRef?.let { vm ->
            getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .putString(prefsKeyState, vm.exportSerializedState())
                .apply()
        }
    }
}

@Composable
private fun MyBrowserRootScreen(
    viewModel: BrowserViewModel,
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

    var isEditingAddress by remember { mutableStateOf(false) }
    var isTabDrawerOpen by remember { mutableStateOf(false) }
    var isGridTabSwitcherOpen by remember { mutableStateOf(false) }
    var isQuickMenuOpen by remember { mutableStateOf(false) }
    var isFindInPageOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var focusedPaneIndex by remember { mutableIntStateOf(0) }

    // Scroll-to-pill state with accumulated >36px hysteresis
    var rawScrollCollapsed by remember { mutableStateOf(false) }
    var accumulatedScrollDeltaPx by remember { mutableFloatStateOf(0f) }

    var activeOverlay by remember { mutableStateOf(PushOverlay.NONE) }
    var openSettingsDirectlyToAi by remember { mutableStateOf(false) }
    var pageSummaryState by remember { mutableStateOf(PageSummaryState()) }

    // Fullscreen HTML5 <video> custom view state
    var fullscreenVideoView by remember { mutableStateOf<View?>(null) }
    var fullscreenVideoCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // <input type="file"> chooser launcher (zero storage permissions needed)
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        if (uris.isEmpty()) {
            callback?.onReceiveValue(null)
        } else {
            callback?.onReceiveValue(uris.toTypedArray())
        }
    }

    val isBarCollapsed by remember {
        derivedStateOf {
            snapshotState.value.preferences.scrollToPillEnabled &&
                rawScrollCollapsed &&
                !isEditingAddress &&
                !isQuickMenuOpen &&
                !isTabDrawerOpen &&
                !isGridTabSwitcherOpen &&
                !isFindInPageOpen &&
                fullscreenVideoView == null
        }
    }

    val webViewPool = remember { mutableStateMapOf<String, WebView>() }
    val tabThumbnails = remember { mutableStateMapOf<String, Bitmap>() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun scheduleBitmapRecycle(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        mainHandler.postDelayed({
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }, 180L)
    }

    fun captureTabThumbnail(tabId: String, wv: WebView?) {
        if (wv == null || wv.width <= 0 || wv.height <= 0) return
        val targetWidth = 360
        val ratio = wv.height.toFloat() / wv.width.toFloat()
        val targetHeight = (targetWidth * ratio).roundToInt().coerceIn(180, 620)
        val captured = runCatching {
            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp)
            val scale = targetWidth.toFloat() / wv.width.toFloat()
            canvas.scale(scale, scale)
            canvas.translate(-wv.scrollX.toFloat(), -wv.scrollY.toFloat())
            wv.draw(canvas)
            bmp
        }.getOrNull() ?: return
        val previous = tabThumbnails.put(tabId, captured)
        if (previous != null && previous !== captured) {
            scheduleBitmapRecycle(previous)
        }
    }

    // Keep latest callbacks safe for long-lived WebChromeClient / WebViewClient closures
    val onOpenFileChooser: (ValueCallback<Array<Uri>>?, String) -> Unit = { callback, mimeType ->
        pendingFileCallback?.onReceiveValue(null)
        pendingFileCallback = callback
        runCatching {
            fileChooserLauncher.launch(mimeType)
        }.onFailure {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
        }
    }

    val onShowFullscreenVideo: (View, WebChromeClient.CustomViewCallback?) -> Unit = { view, callback ->
        if (fullscreenVideoView != null) {
            callback?.onCustomViewHidden()
        } else {
            fullscreenVideoView = view
            fullscreenVideoCallback = callback
            isQuickMenuOpen = false
            isTabDrawerOpen = false
            isEditingAddress = false
        }
    }

    val onHideFullscreenVideo: () -> Unit = {
        fullscreenVideoCallback?.onCustomViewHidden()
        fullscreenVideoCallback = null
        fullscreenVideoView = null
    }

    val onScrollDelta: (Int, Int, Boolean) -> Unit = { dy, scrollY, isScrollable ->
        if (scrollY <= 0 || !isScrollable) {
            accumulatedScrollDeltaPx = 0f
            rawScrollCollapsed = false
        } else {
            val fdy = dy.toFloat()
            if ((fdy > 0f && accumulatedScrollDeltaPx < 0f) ||
                (fdy < 0f && accumulatedScrollDeltaPx > 0f)
            ) {
                accumulatedScrollDeltaPx = fdy
            } else {
                accumulatedScrollDeltaPx += fdy
            }
            if (accumulatedScrollDeltaPx > 36f) {
                rawScrollCollapsed = true
                accumulatedScrollDeltaPx = 0f
            } else if (accumulatedScrollDeltaPx < -36f) {
                rawScrollCollapsed = false
                accumulatedScrollDeltaPx = 0f
            }
        }
    }

    // Forward Activity lifecycle events to WebViews & persist WebView Bundles
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewPool.values.forEach { wv ->
                        wv.onResume()
                        wv.resumeTimers()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewPool.forEach { (tabId, wv) ->
                        val bundle = Bundle()
                        wv.saveState(bundle)
                        viewModel.saveTabBundle(tabId, bundle)
                        wv.onPause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cleanly destroy closed tab WebViews & recycle thumbnails to eliminate CPU/memory leaks
    LaunchedEffect(snapshot.tabs) {
        val openIds = snapshot.tabs.map { it.id }.toSet()
        val staleIds = (webViewPool.keys + tabThumbnails.keys).distinct().filter { it !in openIds }
        staleIds.forEach { staleId ->
            viewModel.removeTabBundle(staleId)
            scheduleBitmapRecycle(tabThumbnails.remove(staleId))
            webViewPool.remove(staleId)?.let { wv ->
                destroyWebViewCleanly(wv)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            mainHandler.removeCallbacksAndMessages(null)
            tabThumbnails.values.forEach { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
            tabThumbnails.clear()
            webViewPool.forEach { (tabId, wv) ->
                val bundle = Bundle()
                wv.saveState(bundle)
                viewModel.saveTabBundle(tabId, bundle)
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
        LaunchedEffect(isDualPaneActive) {
            if (isDualPaneActive) {
                viewModel.ensureSecondaryTabForDualPane()
            } else {
                focusedPaneIndex = 0
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
        val isCurrentUrlBookmarked = snapshot.bookmarks.any { it.url == focusedTab?.url }

        // Pause hidden background tabs and resume visible pane tabs
        LaunchedEffect(snapshot.activeTabId, secondaryTab?.id, isDualPaneActive) {
            val visibleIds = buildSet {
                add(snapshot.activeTabId)
                if (isDualPaneActive && secondaryTab != null) {
                    add(secondaryTab.id)
                }
            }
            webViewPool.forEach { (id, wv) ->
                if (id in visibleIds) {
                    wv.onResume()
                } else {
                    wv.onPause()
                }
            }
        }

        val displaySnapshot = remember(snapshot, focusedTabId) {
            if (focusedTabId != snapshot.activeTabId) {
                snapshot.copy(activeTabId = focusedTabId)
            } else {
                snapshot
            }
        }

        // Layered BackHandler Priority 5: WebView back stack (lowest priority)
        BackHandler(
            enabled = fullscreenVideoView == null &&
                !isQuickMenuOpen &&
                activeOverlay == PushOverlay.NONE &&
                !isGridTabSwitcherOpen &&
                !isEditingAddress &&
                !isFindInPageOpen &&
                !isTabDrawerOpen &&
                (focusedTab?.canGoBack == true || focusedWebView?.canGoBack() == true)
        ) {
            focusedWebView?.goBack()
        }

        // Secondary overlays (Find-in-Page / Tab Dock)
        BackHandler(
            enabled = fullscreenVideoView == null &&
                !isQuickMenuOpen &&
                activeOverlay == PushOverlay.NONE &&
                !isGridTabSwitcherOpen &&
                !isEditingAddress &&
                (isFindInPageOpen || isTabDrawerOpen)
        ) {
            when {
                isFindInPageOpen -> {
                    isFindInPageOpen = false
                    findQuery = ""
                    focusedWebView?.clearMatches()
                }
                isTabDrawerOpen -> isTabDrawerOpen = false
            }
        }

        val onCreateNewWindowTab: (Message) -> Boolean = { resultMsg ->
            val transport = resultMsg.obj as? WebView.WebViewTransport
            if (transport == null) {
                false
            } else {
                captureTabThumbnail(focusedTabId, focusedWebView)
                val nextSnap = viewModel.createTab(initialUrl = "about:blank", isIncognito = false)
                val newTab = nextSnap.activeTab
                if (newTab == null) {
                    false
                } else {
                    focusedPaneIndex = 0
                    val popupWebView = createConfiguredWebView(
                        context = context,
                        tabId = newTab.id,
                        initialUrl = null, // WebViewTransport delivers the target URL / POST request
                        restoredBundle = null,
                        preferences = nextSnap.preferences,
                        isDesktopMode = newTab.isDesktopMode,
                        viewModel = viewModel,
                        onScrollDelta = onScrollDelta,
                        onOpenFileChooser = onOpenFileChooser,
                        onCreateNewWindow = { msg -> createPopupHelper(context, viewModel, webViewPool, onScrollDelta, onOpenFileChooser, onShowFullscreenVideo, onHideFullscreenVideo, { focusedPaneIndex = 0 }, { id, wv -> captureTabThumbnail(id, wv) }, msg) },
                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                        onShowCustomView = onShowFullscreenVideo,
                        onHideCustomView = onHideFullscreenVideo,
                        onCaptureThumbnail = { wv -> captureTabThumbnail(newTab.id, wv) },
                    )
                    webViewPool[newTab.id] = popupWebView
                    transport.webView = popupWebView
                    resultMsg.sendToTarget()
                    true
                }
            }
        }

        val switchTabRelative: (Int) -> Unit = { delta ->
            val tabsList = snapshot.tabs
            if (tabsList.size > 1) {
                captureTabThumbnail(focusedTabId, focusedWebView)
                val currentIdx = tabsList.indexOfFirst { it.id == focusedTabId }.coerceAtLeast(0)
                val targetIdx = ((currentIdx + delta) % tabsList.size + tabsList.size) % tabsList.size
                val targetId = tabsList[targetIdx].id
                if (isDualPaneActive && focusedPaneIndex == 1 && targetId != snapshot.activeTabId) {
                    viewModel.setSecondaryTab(targetId)
                } else {
                    focusedPaneIndex = 0
                    viewModel.switchTab(targetId)
                }
                isTabDrawerOpen = false
                isQuickMenuOpen = false
                rawScrollCollapsed = false
                accumulatedScrollDeltaPx = 0f
            }
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

        val triggerPageSummaryAndAi: (String?) -> Unit = { userQuestion ->
            val activeProfile = snapshotState.value.preferences.activeAiProfile()
            if (activeOverlay == PushOverlay.SUMMARY &&
                !pageSummaryState.isLoading &&
                pageSummaryState.errorMessage.isNullOrBlank()
            ) {
                launchAiForSummary(pageSummaryState, userQuestion)
            } else {
                val currentUrl = focusedTab?.url.orEmpty()
                pageSummaryState = PageSummaryState(
                    url = currentUrl,
                    title = focusedTab?.title.orEmpty(),
                    isLoading = true,
                    aiProfileName = activeProfile.name,
                    aiModel = activeProfile.selectedModel,
                    aiIsLoading = true,
                )
                activeOverlay = PushOverlay.SUMMARY
                if (focusedWebView == null) {
                    pageSummaryState = pageSummaryState.copy(
                        isLoading = false,
                        aiIsLoading = false,
                        errorMessage = "활성 웹 페이지를 찾을 수 없습니다.",
                    )
                } else {
                    extractDomPageSummary(
                        webView = focusedWebView,
                        json = summaryJson,
                        fallbackUrl = currentUrl,
                        fallbackTitle = focusedTab?.title.orEmpty(),
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

        val triggerAddressGestureAction: (Int) -> Unit = { action ->
            when (action) {
                BrowserPreferences.GESTURE_OPEN_TABS -> {
                    captureTabThumbnail(focusedTabId, focusedWebView)
                    if (prefs.tabSwitcherStyle == BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW) {
                        isTabDrawerOpen = false
                        isFindInPageOpen = false
                        isEditingAddress = false
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                        isGridTabSwitcherOpen = true
                    } else {
                        isGridTabSwitcherOpen = false
                        isTabDrawerOpen = true
                    }
                }
                BrowserPreferences.GESTURE_RELOAD -> {
                    if (focusedTab?.isLoading == true) {
                        focusedWebView?.stopLoading()
                    } else {
                        focusedWebView?.reload()
                    }
                }
                BrowserPreferences.GESTURE_SCROLL_TO_TOP -> {
                    focusedWebView?.let { wv ->
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
                    captureTabThumbnail(focusedTabId, focusedWebView)
                    isTabDrawerOpen = false
                    isGridTabSwitcherOpen = false
                    rawScrollCollapsed = false
                    accumulatedScrollDeltaPx = 0f
                    focusedPaneIndex = 0
                    viewModel.createTab()
                }
                BrowserPreferences.GESTURE_QUICK_MENU -> {
                    isTabDrawerOpen = false
                    isEditingAddress = false
                    rawScrollCollapsed = false
                    accumulatedScrollDeltaPx = 0f
                    isQuickMenuOpen = true
                }
                BrowserPreferences.GESTURE_BOOKMARK -> {
                    val url = focusedTab?.url.orEmpty()
                    if (url.isNotBlank()) {
                        val wasBookmarked = isCurrentUrlBookmarked
                        viewModel.toggleBookmark(url, focusedTab?.title.orEmpty())
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

        // Underlying browser parallax offset during Push Transition
        val parallaxProgress by animateFloatAsState(
            targetValue = if (activeOverlay != PushOverlay.NONE) 1f else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "browser_push_parallax",
        )

        // 1. Browser Viewport + Floating Bar + Quick-Toggle Dropdown Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -size.width * 0.25f * parallaxProgress
                },
        ) {
            if (isDualPaneActive && primaryTab != null && secondaryTab != null) {
                val centerGapDp = maxOf(
                    prefs.foldableHingeAvoidanceDp.dp,
                    hingeWidthDp + prefs.foldableHingeAvoidanceDp.dp,
                ).coerceAtLeast(2.dp)

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dual_pane_container"),
                ) {
                    WebViewPane(
                        tab = primaryTab,
                        preferences = prefs,
                        webViewPool = webViewPool,
                        viewModel = viewModel,
                        isDualPane = true,
                        isFocusedPane = focusedPaneIndex == 0,
                        onPaneFocused = { focusedPaneIndex = 0 },
                        onScrollDelta = onScrollDelta,
                        onOpenFileChooser = onOpenFileChooser,
                        onCreateNewWindow = onCreateNewWindowTab,
                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                        onShowCustomView = onShowFullscreenVideo,
                        onHideCustomView = onHideFullscreenVideo,
                        onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("dual_pane_left_webview"),
                    )

                    Box(
                        modifier = Modifier
                            .width(centerGapDp)
                            .fillMaxHeight()
                            .background(Color(0xFF050810)),
                    )

                    WebViewPane(
                        tab = secondaryTab,
                        preferences = prefs,
                        webViewPool = webViewPool,
                        viewModel = viewModel,
                        isDualPane = true,
                        isFocusedPane = focusedPaneIndex == 1,
                        onPaneFocused = { focusedPaneIndex = 1 },
                        onScrollDelta = onScrollDelta,
                        onOpenFileChooser = onOpenFileChooser,
                        onCreateNewWindow = onCreateNewWindowTab,
                        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                        onShowCustomView = onShowFullscreenVideo,
                        onHideCustomView = onHideFullscreenVideo,
                        onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("dual_pane_right_webview"),
                    )
                }
            } else if (primaryTab != null) {
                WebViewPane(
                    tab = primaryTab,
                    preferences = prefs,
                    webViewPool = webViewPool,
                    viewModel = viewModel,
                    isDualPane = false,
                    isFocusedPane = true,
                    onPaneFocused = { focusedPaneIndex = 0 },
                    onScrollDelta = onScrollDelta,
                    onOpenFileChooser = onOpenFileChooser,
                    onCreateNewWindow = onCreateNewWindowTab,
                    onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
                    onShowCustomView = onShowFullscreenVideo,
                    onHideCustomView = onHideFullscreenVideo,
                    onCaptureThumbnail = { id, wv -> captureTabThumbnail(id, wv) },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("fullscreen_webview"),
                )
            }

            // Subtle top status-bar gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(16.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
            )

            // Outside-tap scrim while editing the address bar: consumes taps, hides IME, and re-locks capsule
            if (isEditingAddress && fullscreenVideoView == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    isEditingAddress = false
                                }
                            )
                        }
                        .testTag("address_editing_outside_scrim")
                )
            }

            // Floating Bottom Control Bar (hidden cleanly during fullscreen HTML5 video playback)
            if (fullscreenVideoView == null) {
                FloatingControlBar(
                    snapshot = displaySnapshot,
                    displayUrl = viewModel.formatDisplayUrl(focusedTab?.url.orEmpty()),
                    foldablePosture = foldablePosture,
                    isEditingAddress = isEditingAddress,
                    isTabDrawerOpen = isTabDrawerOpen,
                    isQuickMenuOpen = isQuickMenuOpen,
                    isFindInPageOpen = isFindInPageOpen,
                    findQuery = findQuery,
                    isCollapsedToPill = isBarCollapsed,
                    onEnterAddressEdit = {
                        isTabDrawerOpen = false
                        isQuickMenuOpen = false
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                        isEditingAddress = true
                    },
                    onExitAddressEdit = {
                        isEditingAddress = false
                    },
                    onSubmitAddress = { rawInput ->
                        isEditingAddress = false
                        val targetUrl = viewModel.resolveUrlOrSearch(rawInput)
                        focusedWebView?.loadUrl(targetUrl)
                    },
                    onReloadOrStop = {
                        if (focusedTab?.isLoading == true) {
                            focusedWebView?.stopLoading()
                        } else {
                            focusedWebView?.reload()
                        }
                    },
                    onToolbarAction = { action ->
                        isQuickMenuOpen = false
                        when (action) {
                            ToolbarAction.BACK -> focusedWebView?.goBack()
                            ToolbarAction.FORWARD -> focusedWebView?.goForward()
                            ToolbarAction.RELOAD_OR_STOP -> {
                                if (focusedTab?.isLoading == true) {
                                    focusedWebView?.stopLoading()
                                } else {
                                    focusedWebView?.reload()
                                }
                            }
                            ToolbarAction.TABS -> {
                                captureTabThumbnail(focusedTabId, focusedWebView)
                                if (prefs.tabSwitcherStyle == BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW) {
                                    isTabDrawerOpen = false
                                    isFindInPageOpen = false
                                    isEditingAddress = false
                                    rawScrollCollapsed = false
                                    accumulatedScrollDeltaPx = 0f
                                    isGridTabSwitcherOpen = true
                                } else {
                                    isGridTabSwitcherOpen = false
                                    isTabDrawerOpen = !isTabDrawerOpen
                                }
                            }
                            ToolbarAction.NEW_TAB -> {
                                captureTabThumbnail(focusedTabId, focusedWebView)
                                isTabDrawerOpen = false
                                isGridTabSwitcherOpen = false
                                rawScrollCollapsed = false
                                accumulatedScrollDeltaPx = 0f
                                focusedPaneIndex = 0
                                viewModel.createTab()
                            }
                            ToolbarAction.HOME -> {
                                focusedWebView?.loadUrl(prefs.homeUrl)
                            }
                            ToolbarAction.DESKTOP_MODE -> {
                                viewModel.toggleDesktopMode(focusedTabId)
                                focusedWebView?.let { wv ->
                                    val nextDesktop = !(focusedTab?.isDesktopMode ?: false)
                                    applyRuntimeWebSettings(wv, prefs, nextDesktop)
                                    wv.reload()
                                }
                            }
                            ToolbarAction.SHARE -> {
                                shareUrl(context, focusedTab?.url.orEmpty(), copyToClipboard = false)
                            }
                            ToolbarAction.PAGE_SUMMARY -> {
                                triggerPageSummaryAndAi(null)
                            }
                        }
                    },
                    onSwitchTab = { tabId ->
                        captureTabThumbnail(focusedTabId, focusedWebView)
                        if (isDualPaneActive && focusedPaneIndex == 1 && tabId != snapshot.activeTabId) {
                            viewModel.setSecondaryTab(tabId)
                        } else {
                            focusedPaneIndex = 0
                            viewModel.switchTab(tabId)
                        }
                        isTabDrawerOpen = false
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                    },
                    onCloseTab = { tabId ->
                        viewModel.closeTab(tabId)
                    },
                    onCreateTab = {
                        captureTabThumbnail(focusedTabId, focusedWebView)
                        focusedPaneIndex = 0
                        viewModel.createTab()
                        isTabDrawerOpen = false
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                    },
                    onToggleQuickMenu = {
                        isTabDrawerOpen = false
                        isEditingAddress = false
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                        isQuickMenuOpen = !isQuickMenuOpen
                    },
                    onFindQueryChange = { q ->
                        findQuery = q
                        if (q.isEmpty()) {
                            focusedWebView?.clearMatches()
                        } else {
                            focusedWebView?.findAllAsync(q)
                        }
                    },
                    onFindNext = { forward ->
                        focusedWebView?.findNext(forward)
                    },
                    onCloseFindInPage = {
                        isFindInPageOpen = false
                        findQuery = ""
                        focusedWebView?.clearMatches()
                    },
                    onAddressSwipeUp = { triggerAddressGestureAction(prefs.swipeUpAction) },
                    onAddressSwipeDown = { triggerAddressGestureAction(prefs.swipeDownAction) },
                    onAddressSwipeTab = { delta -> switchTabRelative(delta) },
                    onAddressDoubleTap = { triggerAddressGestureAction(prefs.doubleTapAction) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Anchored Quick-Toggle Dropdown Menu (with full-screen tap-consuming scrim behind it)
            if (fullscreenVideoView == null) {
                QuickToggleDropdownMenu(
                    visible = isQuickMenuOpen,
                    snapshot = displaySnapshot,
                    foldablePosture = foldablePosture,
                    isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                    onDismiss = { isQuickMenuOpen = false },
                    onQuickToggle = { toggle ->
                        when (toggle) {
                            QuickToggleAction.DESKTOP_MODE -> {
                                viewModel.toggleDesktopMode(focusedTabId)
                                focusedWebView?.let { wv ->
                                    val nextDesktop = !(focusedTab?.isDesktopMode ?: false)
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
                                focusedWebView?.reload()
                            }
                            QuickToggleAction.JAVASCRIPT -> {
                                val nextJs = !prefs.javaScriptEnabled
                                viewModel.setJavaScriptEnabled(nextJs)
                                val updatedPrefs = prefs.copy(javaScriptEnabled = nextJs)
                                webViewPool.forEach { (id, wv) ->
                                    val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                                    applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                                }
                                focusedWebView?.reload()
                            }
                            QuickToggleAction.BLOCK_IMAGES -> {
                                val nextBlocked = !prefs.imagesBlocked
                                viewModel.setImagesBlocked(nextBlocked)
                                val updatedPrefs = prefs.copy(imagesBlocked = nextBlocked)
                                webViewPool.forEach { (id, wv) ->
                                    val tabDesktop = snapshot.tabs.firstOrNull { it.id == id }?.isDesktopMode == true
                                    applyRuntimeWebSettings(wv, updatedPrefs, tabDesktop)
                                }
                                focusedWebView?.reload()
                            }
                            QuickToggleAction.SCROLL_TO_PILL -> {
                                val nextScroll = !prefs.scrollToPillEnabled
                                viewModel.setScrollToPillEnabled(nextScroll)
                                if (!nextScroll) {
                                    rawScrollCollapsed = false
                                    accumulatedScrollDeltaPx = 0f
                                }
                            }
                            QuickToggleAction.DUAL_PANE -> {
                                val nextDual = !prefs.foldableDualPaneEnabled
                                viewModel.updateFoldableSettings(dualPaneEnabled = nextDual)
                                if (nextDual && !isUnfolded) {
                                    Toast.makeText(
                                        context,
                                        "듀얼 뷰 활성화됨 (펼침 화면에서 좌우 분할 표시)",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                            QuickToggleAction.INCOGNITO_TAB -> {
                                isQuickMenuOpen = false
                                captureTabThumbnail(focusedTabId, focusedWebView)
                                if (focusedTab?.isIncognito == true) {
                                    val nonIncognito = snapshot.tabs.firstOrNull { !it.isIncognito }
                                    if (nonIncognito != null) {
                                        viewModel.switchTab(nonIncognito.id)
                                    } else {
                                        viewModel.createTab(isIncognito = false)
                                    }
                                } else {
                                    viewModel.createTab(isIncognito = true)
                                }
                            }
                            QuickToggleAction.NEW_TAB -> {
                                isQuickMenuOpen = false
                                captureTabThumbnail(focusedTabId, focusedWebView)
                                focusedPaneIndex = 0
                                viewModel.createTab(isIncognito = false)
                            }
                            QuickToggleAction.BOOKMARK -> {
                                val url = focusedTab?.url.orEmpty()
                                if (url.isNotBlank()) {
                                    viewModel.toggleBookmark(url, focusedTab?.title.orEmpty())
                                }
                            }
                            QuickToggleAction.HISTORY -> {
                                isQuickMenuOpen = false
                                activeOverlay = PushOverlay.HISTORY
                            }
                            QuickToggleAction.FIND_IN_PAGE -> {
                                isQuickMenuOpen = false
                                isFindInPageOpen = true
                            }
                            QuickToggleAction.PAGE_SUMMARY -> {
                                isQuickMenuOpen = false
                                triggerPageSummaryAndAi(null)
                            }
                            QuickToggleAction.COPY_AND_SHARE -> {
                                isQuickMenuOpen = false
                                shareUrl(context, focusedTab?.url.orEmpty(), copyToClipboard = true)
                            }
                        }
                    },
                    onOpenFullSettings = {
                        isQuickMenuOpen = false
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
                onUpdateFoldableSettings = { dual, hinge, sep, width, bias, autoExpand ->
                    viewModel.updateFoldableSettings(
                        dualPaneEnabled = dual,
                        hingeAvoidanceDp = hinge,
                        separateBarProfiles = sep,
                        unfoldedBarWidthFraction = width,
                        unfoldedBarHorizontalBias = bias,
                        autoExpandToolbarWhenUnfolded = autoExpand,
                    )
                },
                onResetFoldableSettings = { viewModel.resetFoldableSettings() },
                onUpdateGestureSettings = { enabled, upAction, downAction, leftRightTab, dblTap, sensitivity ->
                    viewModel.updateGestureSettings(
                        gesturesEnabled = enabled,
                        swipeUpAction = upAction,
                        swipeDownAction = downAction,
                        swipeLeftRightTabSwitch = leftRightTab,
                        doubleTapAction = dblTap,
                        gestureSensitivityDp = sensitivity,
                    )
                },
                onResetGestureSettings = { viewModel.resetGestureSettings() },
                onSelectAiProfile = { viewModel.selectAiProfile(it) },
                onUpdateAiProfile = { id, name, endpoint, apiKey, model, addModel, temp, prompt ->
                    viewModel.updateAiProfile(
                        profileId = id,
                        name = name,
                        endpointOrCommand = endpoint,
                        apiKey = apiKey,
                        selectedModel = model,
                        addCustomModel = addModel,
                        temperature = temp,
                        systemPrompt = prompt,
                    )
                },
                onAddCustomAiProfile = { name, cliType, endpoint, apiKey, model, temp ->
                    viewModel.addCustomAiProfile(
                        name = name,
                        cliType = cliType,
                        endpointOrCommand = endpoint,
                        apiKey = apiKey,
                        selectedModel = model,
                        temperature = temp,
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
                        rawScrollCollapsed = false
                        accumulatedScrollDeltaPx = 0f
                    }
                },
                onToggleExpandBarOnAddressEdit = { enabled ->
                    viewModel.setExpandBarOnAddressEdit(enabled)
                },
                onSelectTabSwitcherStyle = { style ->
                    viewModel.setTabSwitcherStyle(style)
                    isTabDrawerOpen = false
                    isGridTabSwitcherOpen = false
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
                onDismiss = { activeOverlay = PushOverlay.NONE },
                onAskAiQuestion = { q -> triggerPageSummaryAndAi(q) },
                onOpenAiSettings = {
                    openSettingsDirectlyToAi = true
                    activeOverlay = PushOverlay.SETTINGS
                },
            )
        }

        // 5. Full-Screen 2-Column Grid Preview Tab Switcher
        AnimatedVisibility(
            visible = isGridTabSwitcherOpen && activeOverlay == PushOverlay.NONE && fullscreenVideoView == null,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f),
        ) {
            GridTabSwitcherScreen(
                snapshot = displaySnapshot,
                thumbnails = tabThumbnails,
                formatDisplayUrl = { url -> viewModel.formatDisplayUrl(url) },
                onSelectTab = { tabId ->
                    captureTabThumbnail(focusedTabId, focusedWebView)
                    if (isDualPaneActive && focusedPaneIndex == 1 && tabId != snapshot.activeTabId) {
                        viewModel.setSecondaryTab(tabId)
                    } else {
                        focusedPaneIndex = 0
                        viewModel.switchTab(tabId)
                    }
                    isGridTabSwitcherOpen = false
                    rawScrollCollapsed = false
                    accumulatedScrollDeltaPx = 0f
                },
                onCloseTab = { tabId ->
                    viewModel.closeTab(tabId)
                },
                onCreateTab = {
                    captureTabThumbnail(focusedTabId, focusedWebView)
                    focusedPaneIndex = 0
                    viewModel.createTab()
                    isGridTabSwitcherOpen = false
                    rawScrollCollapsed = false
                    accumulatedScrollDeltaPx = 0f
                },
                onDismiss = {
                    isGridTabSwitcherOpen = false
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
    }
}

@Composable
private fun WebViewPane(
    tab: BrowserTabState,
    preferences: BrowserPreferences,
    webViewPool: MutableMap<String, WebView>,
    viewModel: BrowserViewModel,
    isDualPane: Boolean,
    isFocusedPane: Boolean,
    onPaneFocused: () -> Unit,
    onScrollDelta: (dy: Int, scrollY: Int, isScrollable: Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, String) -> Unit,
    onCreateNewWindow: (Message) -> Boolean,
    onCloseWindow: (WebView?) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onCaptureThumbnail: (tabId: String, webView: WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnPaneFocused by rememberUpdatedState(onPaneFocused)
    val latestOnScrollDelta by rememberUpdatedState(onScrollDelta)
    val latestOnOpenFileChooser by rememberUpdatedState(onOpenFileChooser)
    val latestOnCreateNewWindow by rememberUpdatedState(onCreateNewWindow)
    val latestOnCloseWindow by rememberUpdatedState(onCloseWindow)
    val latestOnShowCustomView by rememberUpdatedState(onShowCustomView)
    val latestOnHideCustomView by rememberUpdatedState(onHideCustomView)
    val latestOnCaptureThumbnail by rememberUpdatedState(onCaptureThumbnail)

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
                }
            },
            update = { container ->
                val targetWebView = webViewPool.getOrPut(tab.id) {
                    createConfiguredWebView(
                        context = container.context,
                        tabId = tab.id,
                        initialUrl = tab.url,
                        restoredBundle = viewModel.getTabBundle(tab.id),
                        preferences = preferences,
                        isDesktopMode = tab.isDesktopMode,
                        viewModel = viewModel,
                        onScrollDelta = { dy, sy, scrollable ->
                            latestOnScrollDelta(dy, sy, scrollable)
                        },
                        onOpenFileChooser = { cb, mime ->
                            latestOnOpenFileChooser(cb, mime)
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
                        onCaptureThumbnail = { wv ->
                            latestOnCaptureThumbnail(tab.id, wv)
                        },
                    )
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
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun createPopupHelper(
    context: Context,
    viewModel: BrowserViewModel,
    webViewPool: MutableMap<String, WebView>,
    onScrollDelta: (Int, Int, Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, String) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
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
        preferences = nextSnap.preferences,
        isDesktopMode = newTab.isDesktopMode,
        viewModel = viewModel,
        onScrollDelta = onScrollDelta,
        onOpenFileChooser = onOpenFileChooser,
        onCreateNewWindow = { nestedMsg ->
            createPopupHelper(
                context = context,
                viewModel = viewModel,
                webViewPool = webViewPool,
                onScrollDelta = onScrollDelta,
                onOpenFileChooser = onOpenFileChooser,
                onShowCustomView = onShowCustomView,
                onHideCustomView = onHideCustomView,
                onFocusPrimaryPane = onFocusPrimaryPane,
                onCaptureThumbnail = onCaptureThumbnail,
                resultMsg = nestedMsg,
            )
        },
        onCloseWindow = { wv -> closeWindowByWebView(wv, webViewPool, viewModel) },
        onShowCustomView = onShowCustomView,
        onHideCustomView = onHideCustomView,
        onCaptureThumbnail = { wv -> onCaptureThumbnail(newTab.id, wv) },
    )
    webViewPool[newTab.id] = popupWebView
    transport.webView = popupWebView
    resultMsg.sendToTarget()
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
    (webView.parent as? ViewGroup)?.removeView(webView)
    webView.stopLoading()
    webView.onPause()
    webView.loadUrl("about:blank")
    webView.clearFormData()
    webView.clearHistory()
    webView.removeAllViews()
    webView.destroy()
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

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    tabId: String,
    initialUrl: String?,
    restoredBundle: Bundle?,
    preferences: BrowserPreferences,
    isDesktopMode: Boolean,
    viewModel: BrowserViewModel,
    onScrollDelta: (dy: Int, scrollY: Int, isScrollable: Boolean) -> Unit,
    onOpenFileChooser: (ValueCallback<Array<Uri>>?, String) -> Unit,
    onCreateNewWindow: (Message) -> Boolean,
    onCloseWindow: (WebView?) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustomView: () -> Unit,
    onCaptureThumbnail: (WebView) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setBackgroundColor(android.graphics.Color.parseColor("#090D16"))

        val targetWebView = this
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(targetWebView, true)
        }

        settings.apply {
            domStorageEnabled = true
            databaseEnabled = true
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
        }

        applyRuntimeWebSettings(
            webView = this,
            preferences = preferences,
            isDesktopMode = isDesktopMode,
        )

        setOnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            val isScrollable = v.canScrollVertically(1) || v.canScrollVertically(-1)
            onScrollDelta(dy, scrollY, isScrollable)
        }

        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            enqueueNativeDownload(
                context = context,
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
                val urlStr = uri.toString()
                val scheme = uri.scheme?.lowercase().orEmpty()
                if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob") {
                    return false
                }

                // Safely parse intent://, tel:, mailto:, and external app schemes
                val parsedIntent = runCatching {
                    Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        component = null
                        selector = null
                    }
                }.getOrNull()

                if (parsedIntent != null) {
                    val launched = runCatching {
                        context.startActivity(parsedIntent)
                    }.isSuccess
                    if (launched) return true

                    val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
                    if (!fallbackUrl.isNullOrBlank() &&
                        (fallbackUrl.startsWith("http://", ignoreCase = true) ||
                            fallbackUrl.startsWith("https://", ignoreCase = true))
                    ) {
                        view?.loadUrl(fallbackUrl)
                        return true
                    }
                } else {
                    val fallbackLaunch = runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }.isSuccess
                    if (fallbackLaunch) return true
                }

                Toast.makeText(
                    context,
                    "이 링크를 열 수 있는 앱이 설치되어 있지 않습니다.",
                    Toast.LENGTH_SHORT,
                ).show()
                return true
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
                    onCaptureThumbnail(view)
                }
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
                val rawAccept = fileChooserParams
                    ?.acceptTypes
                    ?.firstOrNull { it.contains('/') }
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "*/*"
                onOpenFileChooser(filePathCallback, rawAccept)
                return true
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
            restoreState(restoredBundle) != null
        } else {
            false
        }
        if (!restored && !initialUrl.isNullOrBlank()) {
            loadUrl(initialUrl)
        }
    }
}

private fun enqueueNativeDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
) {
    val parsedUri = runCatching { Uri.parse(url) }.getOrNull()
    val scheme = parsedUri?.scheme?.lowercase().orEmpty()
    if (parsedUri == null || (scheme != "http" && scheme != "https")) {
        Toast.makeText(context, "지원되지 않는 다운로드 주소입니다.", Toast.LENGTH_SHORT).show()
        return
    }

    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (downloadManager == null) {
        Toast.makeText(context, "다운로드 관리자를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        return
    }

    val request = DownloadManager.Request(parsedUri).apply {
        if (!mimeType.isNullOrBlank()) {
            setMimeType(mimeType)
        }
        val cookie = CookieManager.getInstance().getCookie(url)
        if (!cookie.isNullOrBlank()) {
            addRequestHeader("Cookie", cookie)
        }
        if (!userAgent.isNullOrBlank()) {
            addRequestHeader("User-Agent", userAgent)
        }
        setTitle(fileName)
        setDescription(url)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        try {
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } catch (_: SecurityException) {
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
    }

    runCatching {
        downloadManager.enqueue(request)
    }.onSuccess {
        Toast.makeText(context, "다운로드 시작: $fileName", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "다운로드를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
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
        val desiredUa = if (isDesktopMode) DESKTOP_USER_AGENT else null
        if (userAgentString != desiredUa) {
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
