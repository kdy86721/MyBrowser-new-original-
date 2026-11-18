package com.example.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Customizable toolbar action identifiers across FFI boundary.
 */
@Serializable
enum class ToolbarAction {
    BACK,
    FORWARD,
    RELOAD_OR_STOP,
    TABS,
    NEW_TAB,
    HOME,
    DESKTOP_MODE,
    SHARE,
    PAGE_SUMMARY,
}

/**
 * Customizable quick-toggle actions displayed inside the anchored Liquid Glass dropdown menu.
 * Includes 8 stateful toggles and 6 instant actions (14 total).
 */
@Serializable
enum class QuickToggleAction {
    // Stateful toggles (8)
    DESKTOP_MODE,
    FORCE_DARK,
    AD_BLOCK,
    JAVASCRIPT,
    BLOCK_IMAGES,
    SCROLL_TO_PILL,
    DUAL_PANE,
    INCOGNITO_TAB,
    // Instant actions (6)
    NEW_TAB,
    BOOKMARK,
    HISTORY,
    FIND_IN_PAGE,
    PAGE_SUMMARY,
    COPY_AND_SHARE,
    ;

    val isStatefulToggle: Boolean
        get() = when (this) {
            DESKTOP_MODE,
            FORCE_DARK,
            AD_BLOCK,
            JAVASCRIPT,
            BLOCK_IMAGES,
            SCROLL_TO_PILL,
            DUAL_PANE,
            INCOGNITO_TAB -> true
            NEW_TAB,
            BOOKMARK,
            HISTORY,
            FIND_IN_PAGE,
            PAGE_SUMMARY,
            COPY_AND_SHARE -> false
        }
}

@Serializable
enum class QuickToggleLayoutStyle {
    SINGLE_COLUMN_LIST,
    TWO_COLUMN_TILES,
}

@Serializable
enum class ToggleIndicatorStyle {
    SWITCH,
    PILL,
    DOT,
}

@Serializable
enum class SearchEngine(
    val id: String,
    val displayName: String,
    val description: String,
    val queryTemplate: String,
) {
    DUCKDUCKGO(
        id = "ddg",
        displayName = "DuckDuckGo",
        description = "추적 없는 프라이버시 기본 검색",
        queryTemplate = "https://duckduckgo.com/?q=%s",
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave 검색",
        description = "독립 인덱스 기반 광고 없는 검색",
        queryTemplate = "https://search.brave.com/search?q=%s",
    ),
    STARTPAGE(
        id = "startpage",
        displayName = "Startpage",
        description = "개인정보를 보호하는 웹 검색 결과",
        queryTemplate = "https://www.startpage.com/sp/search?query=%s",
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        description = "친환경 나무 심기 지원 검색 엔진",
        queryTemplate = "https://www.ecosia.org/search?q=%s",
    ),
    GOOGLE(
        id = "google",
        displayName = "Google 웹 검색",
        description = "광범위한 웹 문서 및 통합 검색",
        queryTemplate = "https://www.google.com/search?q=%s",
    ),
}

@Serializable
enum class SecurityState {
    SECURE,
    INSECURE,
    NEUTRAL,
}

@Serializable
data class BrowserTabState(
    val id: String,
    val url: String,
    val title: String = "",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val securityState: SecurityState = SecurityState.NEUTRAL,
    val isDesktopMode: Boolean = false,
    val isIncognito: Boolean = false,
)

@Serializable
data class ToolbarConfig(
    val leftActions: List<ToolbarAction> = defaultLeftActions(),
    val rightActions: List<ToolbarAction> = defaultRightActions(),
) {
    companion object {
        const val MAX_ACTIONS_PER_SIDE = 3

        fun defaultLeftActions(): List<ToolbarAction> =
            listOf(ToolbarAction.BACK, ToolbarAction.FORWARD)

        fun defaultRightActions(): List<ToolbarAction> =
            listOf(ToolbarAction.TABS)
    }
}

@Serializable
data class HistoryEntry(
    val id: String,
    val url: String,
    val title: String,
    val visitedAtEpochMs: Long,
)

@Serializable
data class BookmarkEntry(
    val id: String,
    val url: String,
    val title: String,
    val savedAtEpochMs: Long,
)

@Serializable
data class AiCliProfile(
    val id: String,
    val name: String,
    val cliType: String,
    val endpointOrCommand: String,
    val apiKey: String = "",
    val selectedModel: String,
    val availableModels: List<String> = emptyList(),
    val temperature: Float = 0.4f,
    val systemPrompt: String = DEFAULT_AI_SYSTEM_PROMPT,
    val isCustom: Boolean = false,
) {
    companion object {
        const val CLI_TYPE_GEMINI = "gemini"
        const val CLI_TYPE_CLAUDE = "claude"
        const val CLI_TYPE_OPENAI = "openai"
        const val CLI_TYPE_OLLAMA = "ollama"
        const val CLI_TYPE_CUSTOM = "custom"
        const val DEFAULT_AI_SYSTEM_PROMPT =
            "당신은 한국어로 답변하는 유능한 웹페이지 분석 어시스턴트입니다. 핵심 내용을 구조화하여 명확하고 간결하게 요약하세요."
    }
}

/**
 * Pure primitive floating-bar, dropdown, gesture, AI CLI, and foldable geometry & preference model
 * for seamless Rust FFI mapping.
 */
@Serializable
data class BrowserPreferences(
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val homeUrl: String = "https://duckduckgo.com",
    val javaScriptEnabled: Boolean = true,
    val imagesBlocked: Boolean = false,
    val algorithmicDarkeningEnabled: Boolean = true,
    val adBlockEnabled: Boolean = false,
    val scrollToPillEnabled: Boolean = true,
    val barCornerRadiusDp: Float = DEFAULT_BAR_CORNER_RADIUS_DP,
    val barHeightDp: Float = DEFAULT_BAR_HEIGHT_DP,
    val barWidthFraction: Float = DEFAULT_BAR_WIDTH_FRACTION,
    val barBottomOffsetDp: Float = DEFAULT_BAR_BOTTOM_OFFSET_DP,
    val barHorizontalBias: Float = DEFAULT_BAR_HORIZONTAL_BIAS,
    val glassOpacity: Float = DEFAULT_GLASS_OPACITY,
    val customQuickToggles: List<QuickToggleAction> = defaultQuickToggles(),
    val dropdownLayoutStyle: QuickToggleLayoutStyle = QuickToggleLayoutStyle.SINGLE_COLUMN_LIST,
    val dropdownMaxHeightDp: Float = DEFAULT_DROPDOWN_MAX_HEIGHT_DP,
    val toggleIndicatorStyle: ToggleIndicatorStyle = ToggleIndicatorStyle.SWITCH,
    val foldableDualPaneEnabled: Boolean = false,
    val foldableHingeAvoidanceDp: Float = DEFAULT_FOLDABLE_HINGE_AVOIDANCE_DP,
    val foldableSeparateBarProfiles: Boolean = true,
    val unfoldedBarWidthFraction: Float = DEFAULT_UNFOLDED_BAR_WIDTH_FRACTION,
    val unfoldedBarHorizontalBias: Float = DEFAULT_UNFOLDED_BAR_HORIZONTAL_BIAS,
    val autoExpandToolbarWhenUnfolded: Boolean = true,
    val expandBarOnAddressEdit: Boolean = DEFAULT_EXPAND_BAR_ON_ADDRESS_EDIT,
    val tabSwitcherStyle: Int = DEFAULT_TAB_SWITCHER_STYLE,
    val gesturesEnabled: Boolean = true,
    val swipeUpAction: Int = GESTURE_OPEN_TABS,
    val swipeDownAction: Int = GESTURE_RELOAD,
    val swipeLeftRightTabSwitch: Boolean = true,
    val doubleTapAction: Int = GESTURE_SCROLL_TO_TOP,
    val gestureSensitivityDp: Float = DEFAULT_GESTURE_SENSITIVITY_DP,
    val activeAiProfileId: String = DEFAULT_ACTIVE_AI_PROFILE_ID,
    val aiProfiles: List<AiCliProfile> = defaultAiProfiles(),
    val toolbarConfig: ToolbarConfig = ToolbarConfig(),
) {
    fun activeAiProfile(): AiCliProfile =
        aiProfiles.firstOrNull { it.id == activeAiProfileId }
            ?: aiProfiles.firstOrNull()
            ?: defaultAiProfiles().first()

    companion object {
        const val DEFAULT_BAR_CORNER_RADIUS_DP = 28f
        const val DEFAULT_BAR_HEIGHT_DP = 54f
        const val DEFAULT_BAR_WIDTH_FRACTION = 0.94f
        const val DEFAULT_BAR_BOTTOM_OFFSET_DP = 12f
        const val DEFAULT_BAR_HORIZONTAL_BIAS = 0f
        const val DEFAULT_GLASS_OPACITY = 0.16f
        const val DEFAULT_DROPDOWN_MAX_HEIGHT_DP = 380f
        const val DEFAULT_FOLDABLE_HINGE_AVOIDANCE_DP = 12f
        const val DEFAULT_UNFOLDED_BAR_WIDTH_FRACTION = 0.62f
        const val DEFAULT_UNFOLDED_BAR_HORIZONTAL_BIAS = 0f
        const val DEFAULT_EXPAND_BAR_ON_ADDRESS_EDIT = true
        const val TAB_SWITCHER_CLASSIC = 0
        const val TAB_SWITCHER_GRID_PREVIEW = 1
        const val DEFAULT_TAB_SWITCHER_STYLE = TAB_SWITCHER_GRID_PREVIEW

        const val GESTURE_NONE = 0
        const val GESTURE_OPEN_TABS = 1
        const val GESTURE_RELOAD = 2
        const val GESTURE_SCROLL_TO_TOP = 3
        const val GESTURE_NEW_TAB = 4
        const val GESTURE_QUICK_MENU = 5
        const val GESTURE_BOOKMARK = 6
        const val DEFAULT_GESTURE_SENSITIVITY_DP = 36f

        const val DEFAULT_ACTIVE_AI_PROFILE_ID = "gemini_cli"

        fun defaultAiProfiles(): List<AiCliProfile> = listOf(
            AiCliProfile(
                id = "gemini_cli",
                name = "Gemini CLI",
                cliType = AiCliProfile.CLI_TYPE_GEMINI,
                endpointOrCommand = "https://generativelanguage.googleapis.com/v1beta/models",
                apiKey = "",
                selectedModel = "gemini-2.5-flash",
                availableModels = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash"),
                temperature = 0.4f,
                systemPrompt = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
                isCustom = false,
            ),
            AiCliProfile(
                id = "claude_cli",
                name = "Claude CLI",
                cliType = AiCliProfile.CLI_TYPE_CLAUDE,
                endpointOrCommand = "https://api.anthropic.com/v1/messages",
                apiKey = "",
                selectedModel = "claude-sonnet-4-5",
                availableModels = listOf("claude-sonnet-4-5", "claude-opus-4-1", "claude-haiku-4-5"),
                temperature = 0.4f,
                systemPrompt = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
                isCustom = false,
            ),
            AiCliProfile(
                id = "codex_openai_cli",
                name = "Codex / OpenAI CLI",
                cliType = AiCliProfile.CLI_TYPE_OPENAI,
                endpointOrCommand = "https://api.openai.com/v1/chat/completions",
                apiKey = "",
                selectedModel = "gpt-4.1",
                availableModels = listOf("gpt-4.1", "o4-mini", "codex-mini"),
                temperature = 0.4f,
                systemPrompt = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
                isCustom = false,
            ),
            AiCliProfile(
                id = "ollama_cli",
                name = "Ollama (로컬 CLI)",
                cliType = AiCliProfile.CLI_TYPE_OLLAMA,
                endpointOrCommand = "http://127.0.0.1:11434/api/chat",
                apiKey = "",
                selectedModel = "llama3.3",
                availableModels = listOf("llama3.3", "qwen2.5-coder", "deepseek-r1", "gemma3"),
                temperature = 0.4f,
                systemPrompt = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
                isCustom = false,
            ),
            AiCliProfile(
                id = "custom_cli_preset",
                name = "사용자 지정 AI CLI",
                cliType = AiCliProfile.CLI_TYPE_CUSTOM,
                endpointOrCommand = "http://127.0.0.1:8080/v1/chat/completions",
                apiKey = "",
                selectedModel = "custom-model",
                availableModels = listOf("custom-model"),
                temperature = 0.4f,
                systemPrompt = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
                isCustom = true,
            ),
        )

        fun defaultQuickToggles(): List<QuickToggleAction> = listOf(
            QuickToggleAction.DESKTOP_MODE,
            QuickToggleAction.FORCE_DARK,
            QuickToggleAction.AD_BLOCK,
            QuickToggleAction.JAVASCRIPT,
            QuickToggleAction.BLOCK_IMAGES,
            QuickToggleAction.SCROLL_TO_PILL,
            QuickToggleAction.DUAL_PANE,
            QuickToggleAction.INCOGNITO_TAB,
            QuickToggleAction.NEW_TAB,
            QuickToggleAction.BOOKMARK,
            QuickToggleAction.HISTORY,
            QuickToggleAction.FIND_IN_PAGE,
            QuickToggleAction.PAGE_SUMMARY,
            QuickToggleAction.COPY_AND_SHARE,
        )
    }
}

@Serializable
data class BrowserCoreSnapshot(
    val tabs: List<BrowserTabState>,
    val activeTabId: String,
    val secondaryTabId: String? = null,
    val preferences: BrowserPreferences,
    val historyCount: Int,
    val bookmarks: List<BookmarkEntry> = emptyList(),
    val engineBackendName: String = "LocalKotlinCore (FFI-Ready)",
) {
    val activeTab: BrowserTabState?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    val secondaryTab: BrowserTabState?
        get() = secondaryTabId?.let { secId ->
            tabs.firstOrNull { it.id == secId && it.id != activeTabId }
        } ?: tabs.firstOrNull { it.id != activeTabId }
}

@Serializable
data class PersistedCoreState(
    val tabs: List<BrowserTabState>,
    val activeTabId: String,
    val secondaryTabId: String? = null,
    val preferences: BrowserPreferences,
    val history: List<HistoryEntry>,
    val bookmarks: List<BookmarkEntry> = emptyList(),
)

/**
 * Rust-ready FFI boundary interface.
 * Uses only primitive types and @Serializable data models with zero Android Context/View
 * dependencies so it can be swapped for a Rust cdylib (JNI / UniFFI) without altering UI code.
 */
interface BrowserCoreBridge {
    val snapshotFlow: StateFlow<BrowserCoreSnapshot>

    fun getSnapshot(): BrowserCoreSnapshot

    fun normalizeOrSearch(rawInput: String): String

    fun formatDisplayUrl(url: String): String

    fun classifySecurity(url: String): SecurityState

    fun shouldBlockUrl(url: String): Boolean

    fun shouldBlockRequest(url: String): Boolean = shouldBlockUrl(url)

    fun createTab(
        initialUrl: String? = null,
        activate: Boolean = true,
        isIncognito: Boolean = false,
    ): BrowserCoreSnapshot

    fun switchTab(tabId: String): BrowserCoreSnapshot

    fun setSecondaryTab(tabId: String?): BrowserCoreSnapshot

    fun ensureSecondaryTabForDualPane(): BrowserCoreSnapshot

    fun closeTab(tabId: String): BrowserCoreSnapshot

    fun updateTabNavigation(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        progress: Int,
    ): BrowserCoreSnapshot

    fun toggleTabDesktopMode(tabId: String): BrowserCoreSnapshot

    fun setSearchEngine(engine: SearchEngine): BrowserCoreSnapshot

    fun setHomeUrl(rawUrl: String): BrowserCoreSnapshot

    fun setJavaScriptEnabled(enabled: Boolean): BrowserCoreSnapshot

    fun setImagesBlocked(blocked: Boolean): BrowserCoreSnapshot

    fun setAlgorithmicDarkening(enabled: Boolean): BrowserCoreSnapshot

    fun toggleAdBlock(): BrowserCoreSnapshot

    fun setScrollToPillEnabled(enabled: Boolean): BrowserCoreSnapshot

    fun setExpandBarOnAddressEdit(enabled: Boolean): BrowserCoreSnapshot

    fun setTabSwitcherStyle(style: Int): BrowserCoreSnapshot

    fun updateBarGeometry(
        cornerRadiusDp: Float? = null,
        heightDp: Float? = null,
        widthFraction: Float? = null,
        bottomOffsetDp: Float? = null,
        horizontalBias: Float? = null,
        glassOpacity: Float? = null,
    ): BrowserCoreSnapshot

    fun setCustomQuickToggles(toggles: List<QuickToggleAction>): BrowserCoreSnapshot

    fun moveQuickToggle(action: QuickToggleAction, delta: Int): BrowserCoreSnapshot

    fun updateDropdownCustomization(
        layoutStyle: QuickToggleLayoutStyle? = null,
        maxHeightDp: Float? = null,
        indicatorStyle: ToggleIndicatorStyle? = null,
    ): BrowserCoreSnapshot

    fun updateFoldableSettings(
        dualPaneEnabled: Boolean? = null,
        hingeAvoidanceDp: Float? = null,
        separateBarProfiles: Boolean? = null,
        unfoldedBarWidthFraction: Float? = null,
        unfoldedBarHorizontalBias: Float? = null,
        autoExpandToolbarWhenUnfolded: Boolean? = null,
    ): BrowserCoreSnapshot

    fun resetBarCustomization(): BrowserCoreSnapshot

    fun resetFoldableSettings(): BrowserCoreSnapshot

    fun updateGestureSettings(
        gesturesEnabled: Boolean? = null,
        swipeUpAction: Int? = null,
        swipeDownAction: Int? = null,
        swipeLeftRightTabSwitch: Boolean? = null,
        doubleTapAction: Int? = null,
        gestureSensitivityDp: Float? = null,
    ): BrowserCoreSnapshot

    fun resetGestureSettings(): BrowserCoreSnapshot

    fun selectAiProfile(profileId: String): BrowserCoreSnapshot

    fun updateAiProfile(
        profileId: String,
        name: String? = null,
        endpointOrCommand: String? = null,
        apiKey: String? = null,
        selectedModel: String? = null,
        addCustomModel: String? = null,
        temperature: Float? = null,
        systemPrompt: String? = null,
    ): BrowserCoreSnapshot

    fun addCustomAiProfile(
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
        temperature: Float = 0.4f,
        systemPrompt: String = AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT,
    ): BrowserCoreSnapshot

    fun deleteAiProfile(profileId: String): BrowserCoreSnapshot

    fun setToolbarActions(
        left: List<ToolbarAction>,
        right: List<ToolbarAction>,
    ): BrowserCoreSnapshot

    fun resetToolbarDefaults(): BrowserCoreSnapshot

    fun recordHistoryVisit(
        url: String,
        title: String,
        visitedAtEpochMs: Long,
    ): BrowserCoreSnapshot

    fun getHistory(limit: Int = 50): List<HistoryEntry>

    fun clearHistory(): BrowserCoreSnapshot

    fun toggleBookmark(
        url: String,
        title: String,
        savedAtEpochMs: Long,
    ): BrowserCoreSnapshot

    fun isBookmarked(url: String): Boolean

    fun exportStateJson(): String

    fun importStateJson(json: String): Boolean
}

/**
 * Pure-Kotlin reference implementation of [BrowserCoreBridge].
 * Zero Android framework imports. Thread-safe in-memory state with JSON serialization support.
 */
class LocalKotlinCore(
    initialHomeUrl: String = "https://duckduckgo.com",
) : BrowserCoreBridge {

    private val lock = Any()
    private var tabSequence = 1L
    private var historySequence = 1L
    private var bookmarkSequence = 1L
    private var aiProfileSequence = 1L
    private val historyBuffer = ArrayDeque<HistoryEntry>()
    private val bookmarkList = mutableListOf<BookmarkEntry>()

    private val jsonCodec = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val initialTab = BrowserTabState(
        id = nextTabId(),
        url = initialHomeUrl,
        title = "",
        securityState = classifySecurityInternal(initialHomeUrl),
    )

    private val _snapshotFlow = MutableStateFlow(
        BrowserCoreSnapshot(
            tabs = listOf(initialTab),
            activeTabId = initialTab.id,
            secondaryTabId = null,
            preferences = BrowserPreferences(homeUrl = initialHomeUrl),
            historyCount = 0,
            bookmarks = emptyList(),
            engineBackendName = "LocalKotlinCore (Pure Kotlin / FFI-Ready)",
        )
    )
    override val snapshotFlow: StateFlow<BrowserCoreSnapshot> = _snapshotFlow.asStateFlow()

    override fun getSnapshot(): BrowserCoreSnapshot = _snapshotFlow.value

    override fun normalizeOrSearch(rawInput: String): String {
        val trimmed = rawInput.trim()
        val prefs = _snapshotFlow.value.preferences
        if (trimmed.isEmpty()) {
            return prefs.homeUrl
        }

        val lower = trimmed.lowercase()
        if (lower.startsWith("https://") ||
            lower.startsWith("http://") ||
            lower.startsWith("about:") ||
            lower.startsWith("file://") ||
            lower.startsWith("data:")
        ) {
            return when {
                lower.startsWith("https://") -> "https://" + trimmed.substring(8)
                lower.startsWith("http://") -> "http://" + trimmed.substring(7)
                else -> trimmed
            }
        }

        val hasWhitespace = trimmed.any { it.isWhitespace() }
        if (!hasWhitespace) {
            val hostPart = trimmed.substringBefore('/').substringBefore('?')
            val hostWithoutPort = hostPart.substringBefore(':')
            val portPart = hostPart.substringAfter(':', "")
            val validPort = portPart.isEmpty() || portPart.all { it.isDigit() }

            if (validPort && isLocalOrIpHost(hostWithoutPort)) {
                return "http://$trimmed"
            }

            if (validPort && looksLikeDomain(hostWithoutPort)) {
                return "https://$trimmed"
            }
        }

        val encodedQuery = percentEncodeUtf8(trimmed)
        return prefs.searchEngine.queryTemplate.replace("%s", encodedQuery)
    }

    override fun formatDisplayUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val withoutScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
            else -> trimmed
        }
        val withoutWww = if (withoutScheme.startsWith("www.", ignoreCase = true)) {
            withoutScheme.substring(4)
        } else {
            withoutScheme
        }
        return withoutWww.trimEnd('/')
    }

    override fun classifySecurity(url: String): SecurityState = classifySecurityInternal(url)

    override fun shouldBlockUrl(url: String): Boolean {
        if (!_snapshotFlow.value.preferences.adBlockEnabled) return false
        val lower = url.trim().lowercase()
        if (lower.isEmpty()) return false
        val host = lower
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
        if (host.isEmpty()) return false
        if (host in AD_TRACKER_DOMAINS) return true

        // Walk parent domains for O(1) HashSet lookup per subdomain level
        var dotIdx = host.indexOf('.')
        while (dotIdx >= 0 && dotIdx < host.lastIndex) {
            val parent = host.substring(dotIdx + 1)
            if (parent in AD_TRACKER_DOMAINS) return true
            dotIdx = host.indexOf('.', dotIdx + 1)
        }
        return false
    }

    override fun shouldBlockRequest(url: String): Boolean = shouldBlockUrl(url)

    override fun createTab(
        initialUrl: String?,
        activate: Boolean,
        isIncognito: Boolean,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val resolvedUrl = initialUrl?.let { normalizeOrSearch(it) }
            ?: _snapshotFlow.value.preferences.homeUrl
        val newTab = BrowserTabState(
            id = nextTabId(),
            url = resolvedUrl,
            securityState = classifySecurityInternal(resolvedUrl),
            isIncognito = isIncognito,
        )
        _snapshotFlow.update { current ->
            val updatedTabs = current.tabs + newTab
            val nextActive = if (activate) newTab.id else current.activeTabId
            val nextSecondary = when {
                current.secondaryTabId != null && current.secondaryTabId != nextActive -> current.secondaryTabId
                !activate && current.secondaryTabId == null -> newTab.id
                else -> updatedTabs.firstOrNull { it.id != nextActive }?.id
            }
            current.copy(
                tabs = updatedTabs,
                activeTabId = nextActive,
                secondaryTabId = nextSecondary,
            )
        }
        _snapshotFlow.value
    }

    override fun switchTab(tabId: String): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                if (current.tabs.any { it.id == tabId }) {
                    val nextSecondary = if (current.secondaryTabId == tabId) {
                        current.activeTabId.takeIf { it != tabId }
                            ?: current.tabs.firstOrNull { it.id != tabId }?.id
                    } else {
                        current.secondaryTabId
                    }
                    current.copy(
                        activeTabId = tabId,
                        secondaryTabId = nextSecondary,
                    )
                } else {
                    current
                }
            }
            _snapshotFlow.value
        }

    override fun setSecondaryTab(tabId: String?): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                val validId = tabId?.takeIf { id ->
                    id != current.activeTabId && current.tabs.any { it.id == id }
                }
                current.copy(secondaryTabId = validId)
            }
            _snapshotFlow.value
        }

    override fun ensureSecondaryTabForDualPane(): BrowserCoreSnapshot =
        synchronized(lock) {
            val current = _snapshotFlow.value
            val existingOther = current.tabs.firstOrNull {
                it.id == current.secondaryTabId && it.id != current.activeTabId
            } ?: current.tabs.firstOrNull { it.id != current.activeTabId }

            if (existingOther != null) {
                if (current.secondaryTabId != existingOther.id) {
                    _snapshotFlow.update { it.copy(secondaryTabId = existingOther.id) }
                }
            } else {
                val secUrl = current.preferences.homeUrl
                val secTab = BrowserTabState(
                    id = nextTabId(),
                    url = secUrl,
                    securityState = classifySecurityInternal(secUrl),
                )
                _snapshotFlow.update { snap ->
                    snap.copy(
                        tabs = snap.tabs + secTab,
                        secondaryTabId = secTab.id,
                    )
                }
            }
            _snapshotFlow.value
        }

    override fun closeTab(tabId: String): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                val index = current.tabs.indexOfFirst { it.id == tabId }
                if (index == -1) return@update current

                val remaining = current.tabs.filterNot { it.id == tabId }
                if (remaining.isEmpty()) {
                    val freshTab = BrowserTabState(
                        id = nextTabId(),
                        url = current.preferences.homeUrl,
                        securityState = classifySecurityInternal(current.preferences.homeUrl),
                    )
                    current.copy(
                        tabs = listOf(freshTab),
                        activeTabId = freshTab.id,
                        secondaryTabId = null,
                    )
                } else {
                    val nextActive = if (current.activeTabId == tabId) {
                        val fallbackIdx = (index - 1).coerceAtLeast(0).coerceAtMost(remaining.lastIndex)
                        remaining[fallbackIdx].id
                    } else {
                        current.activeTabId
                    }
                    val nextSecondary = current.secondaryTabId
                        ?.takeIf { secId -> secId != tabId && secId != nextActive && remaining.any { it.id == secId } }
                        ?: remaining.firstOrNull { it.id != nextActive }?.id
                    current.copy(
                        tabs = remaining,
                        activeTabId = nextActive,
                        secondaryTabId = nextSecondary,
                    )
                }
            }
            _snapshotFlow.value
        }

    override fun updateTabNavigation(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        progress: Int,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val clampedProgress = progress.coerceIn(0, 100)
        val security = classifySecurityInternal(url)
        _snapshotFlow.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = url.ifBlank { tab.url },
                        title = title.ifBlank { tab.title },
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        isLoading = isLoading,
                        progress = clampedProgress,
                        securityState = security,
                    )
                } else {
                    tab
                }
            }
            current.copy(tabs = updatedTabs)
        }
        _snapshotFlow.value
    }

    override fun toggleTabDesktopMode(tabId: String): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    tabs = current.tabs.map { tab ->
                        if (tab.id == tabId) tab.copy(isDesktopMode = !tab.isDesktopMode) else tab
                    }
                )
            }
            _snapshotFlow.value
        }

    override fun setSearchEngine(engine: SearchEngine): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(searchEngine = engine)
                )
            }
            _snapshotFlow.value
        }

    override fun setHomeUrl(rawUrl: String): BrowserCoreSnapshot =
        synchronized(lock) {
            val normalized = normalizeOrSearch(rawUrl)
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(homeUrl = normalized)
                )
            }
            _snapshotFlow.value
        }

    override fun setJavaScriptEnabled(enabled: Boolean): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(javaScriptEnabled = enabled)
                )
            }
            _snapshotFlow.value
        }

    override fun setImagesBlocked(blocked: Boolean): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(imagesBlocked = blocked)
                )
            }
            _snapshotFlow.value
        }

    override fun setAlgorithmicDarkening(enabled: Boolean): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(algorithmicDarkeningEnabled = enabled)
                )
            }
            _snapshotFlow.value
        }

    override fun toggleAdBlock(): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        adBlockEnabled = !current.preferences.adBlockEnabled
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun setScrollToPillEnabled(enabled: Boolean): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(scrollToPillEnabled = enabled)
                )
            }
            _snapshotFlow.value
        }

    override fun setExpandBarOnAddressEdit(enabled: Boolean): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(expandBarOnAddressEdit = enabled)
                )
            }
            _snapshotFlow.value
        }

    override fun setTabSwitcherStyle(style: Int): BrowserCoreSnapshot =
        synchronized(lock) {
            val clamped = style.coerceIn(
                BrowserPreferences.TAB_SWITCHER_CLASSIC,
                BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW,
            )
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(tabSwitcherStyle = clamped)
                )
            }
            _snapshotFlow.value
        }

    override fun updateBarGeometry(
        cornerRadiusDp: Float?,
        heightDp: Float?,
        widthFraction: Float?,
        bottomOffsetDp: Float?,
        horizontalBias: Float?,
        glassOpacity: Float?,
    ): BrowserCoreSnapshot = synchronized(lock) {
        _snapshotFlow.update { current ->
            val p = current.preferences
            current.copy(
                preferences = p.copy(
                    barCornerRadiusDp = (cornerRadiusDp ?: p.barCornerRadiusDp).coerceIn(6f, 36f),
                    barHeightDp = (heightDp ?: p.barHeightDp).coerceIn(44f, 72f),
                    barWidthFraction = (widthFraction ?: p.barWidthFraction).coerceIn(0.70f, 1.0f),
                    barBottomOffsetDp = (bottomOffsetDp ?: p.barBottomOffsetDp).coerceIn(4f, 64f),
                    barHorizontalBias = (horizontalBias ?: p.barHorizontalBias).coerceIn(-1f, 1f),
                    glassOpacity = (glassOpacity ?: p.glassOpacity).coerceIn(0.05f, 0.45f),
                )
            )
        }
        _snapshotFlow.value
    }

    override fun setCustomQuickToggles(toggles: List<QuickToggleAction>): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        customQuickToggles = toggles.distinct()
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun moveQuickToggle(action: QuickToggleAction, delta: Int): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                val list = current.preferences.customQuickToggles.toMutableList()
                val index = list.indexOf(action)
                if (index == -1) return@update current
                val targetIndex = (index + delta).coerceIn(0, list.lastIndex)
                if (targetIndex != index) {
                    val item = list.removeAt(index)
                    list.add(targetIndex, item)
                }
                current.copy(
                    preferences = current.preferences.copy(customQuickToggles = list)
                )
            }
            _snapshotFlow.value
        }

    override fun updateDropdownCustomization(
        layoutStyle: QuickToggleLayoutStyle?,
        maxHeightDp: Float?,
        indicatorStyle: ToggleIndicatorStyle?,
    ): BrowserCoreSnapshot = synchronized(lock) {
        _snapshotFlow.update { current ->
            val p = current.preferences
            current.copy(
                preferences = p.copy(
                    dropdownLayoutStyle = layoutStyle ?: p.dropdownLayoutStyle,
                    dropdownMaxHeightDp = (maxHeightDp ?: p.dropdownMaxHeightDp).coerceIn(220f, 540f),
                    toggleIndicatorStyle = indicatorStyle ?: p.toggleIndicatorStyle,
                )
            )
        }
        _snapshotFlow.value
    }

    override fun updateFoldableSettings(
        dualPaneEnabled: Boolean?,
        hingeAvoidanceDp: Float?,
        separateBarProfiles: Boolean?,
        unfoldedBarWidthFraction: Float?,
        unfoldedBarHorizontalBias: Float?,
        autoExpandToolbarWhenUnfolded: Boolean?,
    ): BrowserCoreSnapshot = synchronized(lock) {
        _snapshotFlow.update { current ->
            val p = current.preferences
            current.copy(
                preferences = p.copy(
                    foldableDualPaneEnabled = dualPaneEnabled ?: p.foldableDualPaneEnabled,
                    foldableHingeAvoidanceDp = (hingeAvoidanceDp ?: p.foldableHingeAvoidanceDp).coerceIn(0f, 32f),
                    foldableSeparateBarProfiles = separateBarProfiles ?: p.foldableSeparateBarProfiles,
                    unfoldedBarWidthFraction = (unfoldedBarWidthFraction ?: p.unfoldedBarWidthFraction).coerceIn(0.40f, 1.0f),
                    unfoldedBarHorizontalBias = (unfoldedBarHorizontalBias ?: p.unfoldedBarHorizontalBias).coerceIn(-1f, 1f),
                    autoExpandToolbarWhenUnfolded = autoExpandToolbarWhenUnfolded ?: p.autoExpandToolbarWhenUnfolded,
                )
            )
        }
        if (_snapshotFlow.value.preferences.foldableDualPaneEnabled) {
            ensureSecondaryTabForDualPane()
        }
        _snapshotFlow.value
    }

    override fun resetBarCustomization(): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        barCornerRadiusDp = BrowserPreferences.DEFAULT_BAR_CORNER_RADIUS_DP,
                        barHeightDp = BrowserPreferences.DEFAULT_BAR_HEIGHT_DP,
                        barWidthFraction = BrowserPreferences.DEFAULT_BAR_WIDTH_FRACTION,
                        barBottomOffsetDp = BrowserPreferences.DEFAULT_BAR_BOTTOM_OFFSET_DP,
                        barHorizontalBias = BrowserPreferences.DEFAULT_BAR_HORIZONTAL_BIAS,
                        glassOpacity = BrowserPreferences.DEFAULT_GLASS_OPACITY,
                        customQuickToggles = BrowserPreferences.defaultQuickToggles(),
                        dropdownLayoutStyle = QuickToggleLayoutStyle.SINGLE_COLUMN_LIST,
                        dropdownMaxHeightDp = BrowserPreferences.DEFAULT_DROPDOWN_MAX_HEIGHT_DP,
                        toggleIndicatorStyle = ToggleIndicatorStyle.SWITCH,
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun resetFoldableSettings(): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        foldableDualPaneEnabled = false,
                        foldableHingeAvoidanceDp = BrowserPreferences.DEFAULT_FOLDABLE_HINGE_AVOIDANCE_DP,
                        foldableSeparateBarProfiles = true,
                        unfoldedBarWidthFraction = BrowserPreferences.DEFAULT_UNFOLDED_BAR_WIDTH_FRACTION,
                        unfoldedBarHorizontalBias = BrowserPreferences.DEFAULT_UNFOLDED_BAR_HORIZONTAL_BIAS,
                        autoExpandToolbarWhenUnfolded = true,
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun updateGestureSettings(
        gesturesEnabled: Boolean?,
        swipeUpAction: Int?,
        swipeDownAction: Int?,
        swipeLeftRightTabSwitch: Boolean?,
        doubleTapAction: Int?,
        gestureSensitivityDp: Float?,
    ): BrowserCoreSnapshot = synchronized(lock) {
        _snapshotFlow.update { current ->
            val p = current.preferences
            current.copy(
                preferences = p.copy(
                    gesturesEnabled = gesturesEnabled ?: p.gesturesEnabled,
                    swipeUpAction = (swipeUpAction ?: p.swipeUpAction).coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    swipeDownAction = (swipeDownAction ?: p.swipeDownAction).coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    swipeLeftRightTabSwitch = swipeLeftRightTabSwitch ?: p.swipeLeftRightTabSwitch,
                    doubleTapAction = (doubleTapAction ?: p.doubleTapAction).coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    gestureSensitivityDp = (gestureSensitivityDp ?: p.gestureSensitivityDp)
                        .coerceIn(20f, 64f),
                )
            )
        }
        _snapshotFlow.value
    }

    override fun resetGestureSettings(): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        gesturesEnabled = true,
                        swipeUpAction = BrowserPreferences.GESTURE_OPEN_TABS,
                        swipeDownAction = BrowserPreferences.GESTURE_RELOAD,
                        swipeLeftRightTabSwitch = true,
                        doubleTapAction = BrowserPreferences.GESTURE_SCROLL_TO_TOP,
                        gestureSensitivityDp = BrowserPreferences.DEFAULT_GESTURE_SENSITIVITY_DP,
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun selectAiProfile(profileId: String): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                if (current.preferences.aiProfiles.any { it.id == profileId }) {
                    current.copy(
                        preferences = current.preferences.copy(activeAiProfileId = profileId)
                    )
                } else {
                    current
                }
            }
            _snapshotFlow.value
        }

    override fun updateAiProfile(
        profileId: String,
        name: String?,
        endpointOrCommand: String?,
        apiKey: String?,
        selectedModel: String?,
        addCustomModel: String?,
        temperature: Float?,
        systemPrompt: String?,
    ): BrowserCoreSnapshot = synchronized(lock) {
        _snapshotFlow.update { current ->
            val updatedList = current.preferences.aiProfiles.map { profile ->
                if (profile.id != profileId) {
                    profile
                } else {
                    val trimmedCustomModel = addCustomModel?.trim()?.takeIf { it.isNotEmpty() }
                    val nextModels = if (trimmedCustomModel != null && trimmedCustomModel !in profile.availableModels) {
                        profile.availableModels + trimmedCustomModel
                    } else {
                        profile.availableModels
                    }
                    val nextSelectedModel = trimmedCustomModel
                        ?: selectedModel?.trim()?.takeIf { it.isNotEmpty() }
                        ?: profile.selectedModel
                    val ensuredModels = if (nextSelectedModel !in nextModels) {
                        nextModels + nextSelectedModel
                    } else {
                        nextModels
                    }
                    profile.copy(
                        name = name?.trim()?.takeIf { it.isNotEmpty() } ?: profile.name,
                        endpointOrCommand = endpointOrCommand?.trim() ?: profile.endpointOrCommand,
                        apiKey = apiKey ?: profile.apiKey,
                        selectedModel = nextSelectedModel,
                        availableModels = ensuredModels,
                        temperature = (temperature ?: profile.temperature).coerceIn(0f, 1.5f),
                        systemPrompt = systemPrompt ?: profile.systemPrompt,
                    )
                }
            }
            current.copy(
                preferences = current.preferences.copy(aiProfiles = updatedList)
            )
        }
        _snapshotFlow.value
    }

    override fun addCustomAiProfile(
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
        temperature: Float,
        systemPrompt: String,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val cleanName = name.trim().ifEmpty { "사용자 지정 AI CLI" }
        val cleanModel = selectedModel.trim().ifEmpty { "custom-model" }
        val cleanEndpoint = endpointOrCommand.trim().ifEmpty { "http://127.0.0.1:8080/v1/chat/completions" }
        val newProfile = AiCliProfile(
            id = "custom_ai_${aiProfileSequence++}",
            name = cleanName,
            cliType = cliType.ifBlank { AiCliProfile.CLI_TYPE_CUSTOM },
            endpointOrCommand = cleanEndpoint,
            apiKey = apiKey,
            selectedModel = cleanModel,
            availableModels = listOf(cleanModel),
            temperature = temperature.coerceIn(0f, 1.5f),
            systemPrompt = systemPrompt.ifBlank { AiCliProfile.DEFAULT_AI_SYSTEM_PROMPT },
            isCustom = true,
        )
        _snapshotFlow.update { current ->
            val updated = current.preferences.aiProfiles + newProfile
            current.copy(
                preferences = current.preferences.copy(
                    activeAiProfileId = newProfile.id,
                    aiProfiles = updated,
                )
            )
        }
        _snapshotFlow.value
    }

    override fun deleteAiProfile(profileId: String): BrowserCoreSnapshot =
        synchronized(lock) {
            _snapshotFlow.update { current ->
                val list = current.preferences.aiProfiles
                if (list.size <= 1) return@update current
                val target = list.firstOrNull { it.id == profileId } ?: return@update current
                if (!target.isCustom) return@update current
                val remaining = list.filterNot { it.id == profileId }
                val nextActiveId = if (current.preferences.activeAiProfileId == profileId) {
                    remaining.first().id
                } else {
                    current.preferences.activeAiProfileId
                }
                current.copy(
                    preferences = current.preferences.copy(
                        activeAiProfileId = nextActiveId,
                        aiProfiles = remaining,
                    )
                )
            }
            _snapshotFlow.value
        }

    override fun setToolbarActions(
        left: List<ToolbarAction>,
        right: List<ToolbarAction>,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val sanitizedLeft = left.distinct().take(ToolbarConfig.MAX_ACTIONS_PER_SIDE)
        val sanitizedRight = right.distinct().take(ToolbarConfig.MAX_ACTIONS_PER_SIDE)
        _snapshotFlow.update { current ->
            current.copy(
                preferences = current.preferences.copy(
                    toolbarConfig = ToolbarConfig(
                        leftActions = sanitizedLeft,
                        rightActions = sanitizedRight,
                    )
                )
            )
        }
        _snapshotFlow.value
    }

    override fun resetToolbarDefaults(): BrowserCoreSnapshot =
        setToolbarActions(
            left = ToolbarConfig.defaultLeftActions(),
            right = ToolbarConfig.defaultRightActions(),
        )

    override fun recordHistoryVisit(
        url: String,
        title: String,
        visitedAtEpochMs: Long,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val activeTab = _snapshotFlow.value.activeTab
        if (activeTab?.isIncognito == true) {
            return@synchronized _snapshotFlow.value
        }

        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty() ||
            cleanUrl.startsWith("about:") ||
            cleanUrl.startsWith("data:")
        ) {
            return@synchronized _snapshotFlow.value
        }

        val last = historyBuffer.firstOrNull()
        if (last == null || last.url != cleanUrl) {
            historyBuffer.addFirst(
                HistoryEntry(
                    id = "hist_${historySequence++}",
                    url = cleanUrl,
                    title = title.ifBlank { cleanUrl },
                    visitedAtEpochMs = visitedAtEpochMs,
                )
            )
            while (historyBuffer.size > MAX_HISTORY_ENTRIES) {
                historyBuffer.removeLast()
            }
        } else if (title.isNotBlank() && last.title != title) {
            historyBuffer.removeFirst()
            historyBuffer.addFirst(last.copy(title = title, visitedAtEpochMs = visitedAtEpochMs))
        }

        _snapshotFlow.update { current ->
            current.copy(historyCount = historyBuffer.size)
        }
        _snapshotFlow.value
    }

    override fun getHistory(limit: Int): List<HistoryEntry> =
        synchronized(lock) {
            historyBuffer.take(limit.coerceAtLeast(0))
        }

    override fun clearHistory(): BrowserCoreSnapshot =
        synchronized(lock) {
            historyBuffer.clear()
            _snapshotFlow.update { current ->
                current.copy(historyCount = 0)
            }
            _snapshotFlow.value
        }

    override fun toggleBookmark(
        url: String,
        title: String,
        savedAtEpochMs: Long,
    ): BrowserCoreSnapshot = synchronized(lock) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty() || cleanUrl.startsWith("about:") || cleanUrl.startsWith("data:")) {
            return@synchronized _snapshotFlow.value
        }
        val existingIdx = bookmarkList.indexOfFirst { it.url == cleanUrl }
        if (existingIdx >= 0) {
            bookmarkList.removeAt(existingIdx)
        } else {
            bookmarkList.add(
                0,
                BookmarkEntry(
                    id = "bm_${bookmarkSequence++}",
                    url = cleanUrl,
                    title = title.ifBlank { formatDisplayUrl(cleanUrl) },
                    savedAtEpochMs = savedAtEpochMs,
                ),
            )
        }
        _snapshotFlow.update { current ->
            current.copy(bookmarks = bookmarkList.toList())
        }
        _snapshotFlow.value
    }

    override fun isBookmarked(url: String): Boolean =
        synchronized(lock) {
            bookmarkList.any { it.url == url.trim() }
        }

    override fun exportStateJson(): String =
        synchronized(lock) {
            val snap = _snapshotFlow.value
            val nonIncognitoTabs = snap.tabs.filterNot { it.isIncognito }
            val safeTabs = nonIncognitoTabs.ifEmpty {
                listOf(
                    BrowserTabState(
                        id = nextTabId(),
                        url = snap.preferences.homeUrl,
                        securityState = classifySecurityInternal(snap.preferences.homeUrl),
                    )
                )
            }
            val safeActiveId = if (safeTabs.any { it.id == snap.activeTabId }) {
                snap.activeTabId
            } else {
                safeTabs.first().id
            }
            val safeSecondaryId = snap.secondaryTabId
                ?.takeIf { sec -> sec != safeActiveId && safeTabs.any { it.id == sec } }
            val payload = PersistedCoreState(
                tabs = safeTabs,
                activeTabId = safeActiveId,
                secondaryTabId = safeSecondaryId,
                preferences = snap.preferences,
                history = historyBuffer.toList(),
                bookmarks = bookmarkList.toList(),
            )
            jsonCodec.encodeToString(payload)
        }

    override fun importStateJson(json: String): Boolean =
        synchronized(lock) {
            runCatching {
                val decoded = jsonCodec.decodeFromString<PersistedCoreState>(json)
                if (decoded.tabs.isEmpty()) return@runCatching false
                historyBuffer.clear()
                decoded.history.take(MAX_HISTORY_ENTRIES).forEach { historyBuffer.addLast(it) }
                bookmarkList.clear()
                bookmarkList.addAll(decoded.bookmarks)
                val activeId = if (decoded.tabs.any { it.id == decoded.activeTabId }) {
                    decoded.activeTabId
                } else {
                    decoded.tabs.first().id
                }
                val secondaryId = decoded.secondaryTabId
                    ?.takeIf { sec -> sec != activeId && decoded.tabs.any { it.id == sec } }
                val p = decoded.preferences
                val safeAiProfiles = p.aiProfiles
                    .ifEmpty { BrowserPreferences.defaultAiProfiles() }
                    .map { profile ->
                        profile.copy(temperature = profile.temperature.coerceIn(0f, 1.5f))
                    }
                val safeActiveAiId = p.activeAiProfileId.takeIf { id ->
                    safeAiProfiles.any { it.id == id }
                } ?: safeAiProfiles.first().id

                val clampedPrefs = p.copy(
                    barCornerRadiusDp = p.barCornerRadiusDp.coerceIn(6f, 36f),
                    barHeightDp = p.barHeightDp.coerceIn(44f, 72f),
                    barWidthFraction = p.barWidthFraction.coerceIn(0.70f, 1.0f),
                    barBottomOffsetDp = p.barBottomOffsetDp.coerceIn(4f, 64f),
                    barHorizontalBias = p.barHorizontalBias.coerceIn(-1f, 1f),
                    glassOpacity = p.glassOpacity.coerceIn(0.05f, 0.45f),
                    dropdownMaxHeightDp = p.dropdownMaxHeightDp.coerceIn(220f, 540f),
                    foldableHingeAvoidanceDp = p.foldableHingeAvoidanceDp.coerceIn(0f, 32f),
                    unfoldedBarWidthFraction = p.unfoldedBarWidthFraction.coerceIn(0.40f, 1.0f),
                    unfoldedBarHorizontalBias = p.unfoldedBarHorizontalBias.coerceIn(-1f, 1f),
                    tabSwitcherStyle = p.tabSwitcherStyle.coerceIn(
                        BrowserPreferences.TAB_SWITCHER_CLASSIC,
                        BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW,
                    ),
                    swipeUpAction = p.swipeUpAction.coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    swipeDownAction = p.swipeDownAction.coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    doubleTapAction = p.doubleTapAction.coerceIn(
                        BrowserPreferences.GESTURE_NONE,
                        BrowserPreferences.GESTURE_BOOKMARK,
                    ),
                    gestureSensitivityDp = p.gestureSensitivityDp.coerceIn(20f, 64f),
                    activeAiProfileId = safeActiveAiId,
                    aiProfiles = safeAiProfiles,
                )
                _snapshotFlow.value = BrowserCoreSnapshot(
                    tabs = decoded.tabs.map { it.copy(isLoading = false, progress = 0) },
                    activeTabId = activeId,
                    secondaryTabId = secondaryId,
                    preferences = clampedPrefs,
                    historyCount = historyBuffer.size,
                    bookmarks = bookmarkList.toList(),
                    engineBackendName = "LocalKotlinCore (Pure Kotlin / FFI-Ready)",
                )
                true
            }.getOrDefault(false)
        }

    private fun nextTabId(): String = "tab_${tabSequence++}"

    private fun classifySecurityInternal(url: String): SecurityState {
        val lower = url.trim().lowercase()
        return when {
            lower.startsWith("https://") -> SecurityState.SECURE
            lower.startsWith("http://") -> SecurityState.INSECURE
            else -> SecurityState.NEUTRAL
        }
    }

    private fun isLocalOrIpHost(host: String): Boolean {
        if (host.equals("localhost", ignoreCase = true)) return true
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        ) {
            return true
        }
        return false
    }

    private fun looksLikeDomain(host: String): Boolean {
        if (host.length < 3 || !host.contains('.')) return false
        if (host.startsWith('.') || host.endsWith('.')) return false
        val labels = host.split('.')
        val tld = labels.last()
        if (tld.length < 2 || !tld.all { it.isLetter() }) return false
        return labels.all { label ->
            label.isNotEmpty() &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    private fun percentEncodeUtf8(input: String): String {
        val bytes = input.encodeToByteArray()
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val code = b.toInt() and 0xFF
            val c = code.toChar()
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' ||
                    c == '-' || c == '_' || c == '.' || c == '~' -> sb.append(c)
                c == ' ' -> sb.append('+')
                else -> {
                    sb.append('%')
                    sb.append(HEX_DIGITS[code ushr 4])
                    sb.append(HEX_DIGITS[code and 0x0F])
                }
            }
        }
        return sb.toString()
    }

    private companion object {
        const val MAX_HISTORY_ENTRIES = 500
        val HEX_DIGITS = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        )
        val AD_TRACKER_DOMAINS: HashSet<String> = hashSetOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "adnxs.com",
            "adsrvr.org",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "scorecardresearch.com",
            "quantserve.com",
            "amazon-adsystem.com",
            "google-analytics.com",
            "googletagmanager.com",
        )
    }
}
