package com.example

import com.example.core.AiChatTurn
import com.example.core.AiCliHttpClient
import com.example.core.AiCliProfile
import com.example.core.BrowserPreferences
import com.example.core.LocalKotlinCore
import com.example.core.QuickToggleAction
import com.example.core.QuickToggleLayoutStyle
import com.example.core.SearchEngine
import com.example.core.SecurityState
import com.example.core.ToggleIndicatorStyle
import com.example.core.ToolbarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `normalizeOrSearch resolves domains, local IPs, and search queries`() {
        val core = LocalKotlinCore()
        assertEquals("https://example.com", core.normalizeOrSearch("example.com"))
        assertEquals("http://localhost:8080/api", core.normalizeOrSearch("localhost:8080/api"))
        assertEquals("http://192.168.1.1", core.normalizeOrSearch("192.168.1.1"))
        assertEquals("https://duckduckgo.com/?q=rust+ffi+android", core.normalizeOrSearch("rust ffi android"))

        core.setSearchEngine(SearchEngine.BRAVE)
        assertEquals("https://search.brave.com/search?q=kotlin", core.normalizeOrSearch("kotlin"))
    }

    @Test
    fun `tab lifecycle, incognito isolation, bookmarks, and adblock work cleanly`() {
        val core = LocalKotlinCore(initialHomeUrl = "https://duckduckgo.com")
        val initialId = core.getSnapshot().activeTabId

        val created = core.createTab("rust-lang.org", activate = true)
        assertEquals(2, created.tabs.size)
        val secondId = created.activeTabId
        assertEquals("https://rust-lang.org", created.activeTab?.url)
        assertEquals(SecurityState.SECURE, created.activeTab?.securityState)

        core.recordHistoryVisit("https://rust-lang.org", "Rust", 1000L)
        assertEquals(1, core.getSnapshot().historyCount)

        // Incognito tab must not record into history
        val incognito = core.createTab("example.com", activate = true, isIncognito = true)
        assertTrue(incognito.activeTab?.isIncognito == true)
        core.recordHistoryVisit("https://example.com", "Example", 2000L)
        assertEquals(1, core.getSnapshot().historyCount)

        // Bookmarks & O(1) HashSet AdBlock
        core.toggleBookmark("https://rust-lang.org", "Rust", 3000L)
        assertTrue(core.isBookmarked("https://rust-lang.org"))
        core.toggleAdBlock()
        assertTrue(core.shouldBlockUrl("https://pagead2.googlesyndication.com/pagead/js"))
        assertTrue(core.shouldBlockUrl("https://sub.tracker.doubleclick.net:443/pixel?id=1"))
        assertTrue(core.shouldBlockRequest("https://www.googletagmanager.com/gtm.js"))
        assertFalse(core.shouldBlockUrl("https://notdoubleclick.net/article"))
        assertFalse(core.shouldBlockUrl("https://rust-lang.org/learn"))

        val afterClose = core.closeTab(incognito.activeTabId)
        assertEquals(2, afterClose.tabs.size)
        core.closeTab(secondId)
        assertEquals(initialId, core.getSnapshot().activeTabId)

        core.clearHistory()
        assertEquals(0, core.getSnapshot().historyCount)
    }

    @Test
    fun `14 quick toggles classification, ordering, dropdown customization, and foldable settings`() {
        assertEquals(14, QuickToggleAction.entries.size)
        assertEquals(8, QuickToggleAction.entries.count { it.isStatefulToggle })
        assertEquals(6, QuickToggleAction.entries.count { !it.isStatefulToggle })

        val core = LocalKotlinCore()
        core.setCustomQuickToggles(
            listOf(
                QuickToggleAction.DESKTOP_MODE,
                QuickToggleAction.PAGE_SUMMARY,
                QuickToggleAction.COPY_AND_SHARE,
            )
        )
        core.moveQuickToggle(QuickToggleAction.COPY_AND_SHARE, -1)
        assertEquals(
            listOf(
                QuickToggleAction.DESKTOP_MODE,
                QuickToggleAction.COPY_AND_SHARE,
                QuickToggleAction.PAGE_SUMMARY,
            ),
            core.getSnapshot().preferences.customQuickToggles,
        )

        core.updateDropdownCustomization(
            layoutStyle = QuickToggleLayoutStyle.TWO_COLUMN_TILES,
            maxHeightDp = 460f,
            indicatorStyle = ToggleIndicatorStyle.PILL,
        )
        core.setImagesBlocked(true)
        core.updateFoldableSettings(
            dualPaneEnabled = true,
            hingeAvoidanceDp = 18f,
            separateBarProfiles = false,
            unfoldedBarWidthFraction = 0.52f,
            unfoldedBarHorizontalBias = 0.75f,
            autoExpandToolbarWhenUnfolded = false,
        )

        val snap = core.getSnapshot()
        assertEquals(2, snap.tabs.size)
        assertNotNull(snap.secondaryTab)
        assertTrue(snap.preferences.imagesBlocked)
        assertEquals(QuickToggleLayoutStyle.TWO_COLUMN_TILES, snap.preferences.dropdownLayoutStyle)
        assertEquals(460f, snap.preferences.dropdownMaxHeightDp, 0.001f)
        assertEquals(ToggleIndicatorStyle.PILL, snap.preferences.toggleIndicatorStyle)
        assertTrue(snap.preferences.foldableDualPaneEnabled)
        assertEquals(18f, snap.preferences.foldableHingeAvoidanceDp, 0.001f)
        assertFalse(snap.preferences.foldableSeparateBarProfiles)
        assertEquals(0.52f, snap.preferences.unfoldedBarWidthFraction, 0.001f)
        assertEquals(0.75f, snap.preferences.unfoldedBarHorizontalBias, 0.001f)
        assertFalse(snap.preferences.autoExpandToolbarWhenUnfolded)

        // JSON roundtrip preserves foldable + dropdown + secondary tab
        val json = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(json))
        val rp = restored.getSnapshot().preferences
        assertEquals(QuickToggleLayoutStyle.TWO_COLUMN_TILES, rp.dropdownLayoutStyle)
        assertEquals(ToggleIndicatorStyle.PILL, rp.toggleIndicatorStyle)
        assertEquals(18f, rp.foldableHingeAvoidanceDp, 0.001f)
        assertTrue(rp.imagesBlocked)
        assertTrue(rp.foldableDualPaneEnabled)
        assertNotNull(restored.getSnapshot().secondaryTab)

        core.resetFoldableSettings()
        assertFalse(core.getSnapshot().preferences.foldableDualPaneEnabled)
        assertEquals(
            BrowserPreferences.DEFAULT_UNFOLDED_BAR_WIDTH_FRACTION,
            core.getSnapshot().preferences.unfoldedBarWidthFraction,
            0.001f,
        )
    }

    @Test
    fun `bar geometry customization, quick toggles, and json export roundtrip`() {
        val core = LocalKotlinCore()
        core.updateBarGeometry(
            cornerRadiusDp = 12f,
            heightDp = 64f,
            widthFraction = 0.85f,
            bottomOffsetDp = 24f,
            horizontalBias = -0.5f,
            glassOpacity = 0.30f,
        )
        core.setCustomQuickToggles(listOf(QuickToggleAction.DESKTOP_MODE, QuickToggleAction.AD_BLOCK))
        core.setToolbarActions(
            left = listOf(ToolbarAction.HOME, ToolbarAction.BACK),
            right = listOf(ToolbarAction.DESKTOP_MODE, ToolbarAction.SHARE),
        )

        val json = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(json))

        val restoredPrefs = restored.getSnapshot().preferences
        assertEquals(12f, restoredPrefs.barCornerRadiusDp, 0.001f)
        assertEquals(64f, restoredPrefs.barHeightDp, 0.001f)
        assertEquals(0.85f, restoredPrefs.barWidthFraction, 0.001f)
        assertEquals(24f, restoredPrefs.barBottomOffsetDp, 0.001f)
        assertEquals(-0.5f, restoredPrefs.barHorizontalBias, 0.001f)
        assertEquals(0.30f, restoredPrefs.glassOpacity, 0.001f)
        assertEquals(
            listOf(QuickToggleAction.DESKTOP_MODE, QuickToggleAction.AD_BLOCK),
            restoredPrefs.customQuickToggles,
        )
        assertFalse(restored.importStateJson("{invalid"))

        // Clamping & reset check
        core.updateBarGeometry(cornerRadiusDp = 2f, glassOpacity = 0.01f)
        assertEquals(6f, core.getSnapshot().preferences.barCornerRadiusDp, 0.001f)
        assertEquals(0.05f, core.getSnapshot().preferences.glassOpacity, 0.001f)

        core.resetBarCustomization()
        assertEquals(
            BrowserPreferences.DEFAULT_BAR_CORNER_RADIUS_DP,
            core.getSnapshot().preferences.barCornerRadiusDp,
            0.001f,
        )
        assertEquals(
            BrowserPreferences.DEFAULT_GLASS_OPACITY,
            core.getSnapshot().preferences.glassOpacity,
            0.001f,
        )
    }

    @Test
    fun `expandBarOnAddressEdit and tabSwitcherStyle defaults, clamping, and roundtrip`() {
        val core = LocalKotlinCore()
        assertTrue(core.getSnapshot().preferences.expandBarOnAddressEdit)
        assertEquals(
            BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW,
            core.getSnapshot().preferences.tabSwitcherStyle,
        )

        core.setExpandBarOnAddressEdit(false)
        core.setTabSwitcherStyle(BrowserPreferences.TAB_SWITCHER_CLASSIC)
        assertFalse(core.getSnapshot().preferences.expandBarOnAddressEdit)
        assertEquals(
            BrowserPreferences.TAB_SWITCHER_CLASSIC,
            core.getSnapshot().preferences.tabSwitcherStyle,
        )

        // Out-of-range tab switcher style clamps cleanly
        core.setTabSwitcherStyle(99)
        assertEquals(
            BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW,
            core.getSnapshot().preferences.tabSwitcherStyle,
        )
        core.setTabSwitcherStyle(-5)
        assertEquals(
            BrowserPreferences.TAB_SWITCHER_CLASSIC,
            core.getSnapshot().preferences.tabSwitcherStyle,
        )

        val json = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(json))
        assertFalse(restored.getSnapshot().preferences.expandBarOnAddressEdit)
        assertEquals(
            BrowserPreferences.TAB_SWITCHER_CLASSIC,
            restored.getSnapshot().preferences.tabSwitcherStyle,
        )
    }

    @Test
    fun `gesture settings defaults, clamping, long-press AI chat toggle, and reset work cleanly`() {
        val core = LocalKotlinCore()
        val defaults = core.getSnapshot().preferences
        assertTrue(defaults.gesturesEnabled)
        assertTrue(defaults.longPressSwipeUpAiChatEnabled)
        assertEquals(BrowserPreferences.GESTURE_OPEN_TABS, defaults.swipeUpAction)
        assertEquals(BrowserPreferences.GESTURE_RELOAD, defaults.swipeDownAction)
        assertTrue(defaults.swipeLeftRightTabSwitch)
        assertEquals(BrowserPreferences.GESTURE_SCROLL_TO_TOP, defaults.doubleTapAction)
        assertEquals(BrowserPreferences.DEFAULT_GESTURE_SENSITIVITY_DP, defaults.gestureSensitivityDp, 0.001f)

        core.updateGestureSettings(
            gesturesEnabled = false,
            swipeUpAction = BrowserPreferences.GESTURE_NEW_TAB,
            swipeDownAction = BrowserPreferences.GESTURE_QUICK_MENU,
            swipeLeftRightTabSwitch = false,
            doubleTapAction = BrowserPreferences.GESTURE_BOOKMARK,
            gestureSensitivityDp = 999f, // clamps to 64f
            longPressSwipeUpAiChatEnabled = false,
        )
        core.setToolbarActions(
            left = listOf(ToolbarAction.BACK, ToolbarAction.PAGE_SUMMARY),
            right = listOf(ToolbarAction.TABS),
        )

        val updated = core.getSnapshot().preferences
        assertFalse(updated.gesturesEnabled)
        assertFalse(updated.longPressSwipeUpAiChatEnabled)
        assertEquals(BrowserPreferences.GESTURE_NEW_TAB, updated.swipeUpAction)
        assertEquals(BrowserPreferences.GESTURE_QUICK_MENU, updated.swipeDownAction)
        assertFalse(updated.swipeLeftRightTabSwitch)
        assertEquals(BrowserPreferences.GESTURE_BOOKMARK, updated.doubleTapAction)
        assertEquals(64f, updated.gestureSensitivityDp, 0.001f)
        assertTrue(ToolbarAction.PAGE_SUMMARY in updated.toolbarConfig.leftActions)

        core.updateGestureSettings(gestureSensitivityDp = 5f)
        assertEquals(20f, core.getSnapshot().preferences.gestureSensitivityDp, 0.001f)

        val json = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(json))
        val rp = restored.getSnapshot().preferences
        assertFalse(rp.gesturesEnabled)
        assertFalse(rp.longPressSwipeUpAiChatEnabled)
        assertEquals(BrowserPreferences.GESTURE_BOOKMARK, rp.doubleTapAction)
        assertEquals(20f, rp.gestureSensitivityDp, 0.001f)
        assertTrue(ToolbarAction.PAGE_SUMMARY in rp.toolbarConfig.leftActions)

        core.resetGestureSettings()
        val resetPrefs = core.getSnapshot().preferences
        assertTrue(resetPrefs.gesturesEnabled)
        assertTrue(resetPrefs.longPressSwipeUpAiChatEnabled)
        assertEquals(BrowserPreferences.GESTURE_OPEN_TABS, resetPrefs.swipeUpAction)
        assertEquals(BrowserPreferences.DEFAULT_GESTURE_SENSITIVITY_DP, resetPrefs.gestureSensitivityDp, 0.001f)
    }

    @Test
    fun `kimi cli preset, dynamic model discovery, and multi-AI profile management`() {
        val core = LocalKotlinCore()
        val defaults = core.getSnapshot().preferences
        assertEquals(6, defaults.aiProfiles.size)
        assertEquals("gemini_cli", defaults.activeAiProfile().id)
        assertTrue(defaults.activeAiProfile().availableModels.isEmpty())
        assertEquals("", defaults.activeAiProfile().selectedModel)

        val kimiPreset = defaults.aiProfiles.firstOrNull { it.id == "kimi_cli" }
        assertNotNull(kimiPreset)
        assertEquals(AiCliProfile.CLI_TYPE_KIMI, kimiPreset!!.cliType)
        assertEquals(BrowserPreferences.KIMI_GLOBAL_ENDPOINT, kimiPreset.endpointOrCommand)

        // Dynamic model injection auto-selects the first fetched model when empty
        core.selectAiProfile("kimi_cli")
        core.setAiProfileModels(
            profileId = "kimi_cli",
            models = listOf("moonshot-v1-8k", "kimi-k2-0711-preview", "moonshot-v1-8k"),
        )
        val populatedKimi = core.getSnapshot().preferences.activeAiProfile()
        assertEquals(listOf("moonshot-v1-8k", "kimi-k2-0711-preview"), populatedKimi.availableModels)
        assertEquals("moonshot-v1-8k", populatedKimi.selectedModel)

        // Select a second model and verify subsequent fetch preserves valid selectedModel
        core.updateAiProfile(profileId = "kimi_cli", selectedModel = "kimi-k2-0711-preview")
        core.setAiProfileModels(
            profileId = "kimi_cli",
            models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "kimi-k2-0711-preview"),
        )
        assertEquals("kimi-k2-0711-preview", core.getSnapshot().preferences.activeAiProfile().selectedModel)

        // Verify JSON parsing for OpenAI/Kimi, Ollama, and Gemini /models payloads
        val kimiParsed = AiCliHttpClient.parseAvailableModelsResponse(
            cliType = AiCliProfile.CLI_TYPE_KIMI,
            rawJson = """{"data":[{"id":"moonshot-v1-8k"},{"id":"kimi-k2-0711-preview"}]}""",
        )
        assertEquals(listOf("moonshot-v1-8k", "kimi-k2-0711-preview"), kimiParsed)

        val ollamaParsed = AiCliHttpClient.parseAvailableModelsResponse(
            cliType = AiCliProfile.CLI_TYPE_OLLAMA,
            rawJson = """{"models":[{"name":"llama3.2:latest"},{"name":"qwen2.5:7b"}]}""",
        )
        assertEquals(listOf("llama3.2:latest", "qwen2.5:7b"), ollamaParsed)

        val geminiParsed = AiCliHttpClient.parseAvailableModelsResponse(
            cliType = AiCliProfile.CLI_TYPE_GEMINI,
            rawJson = """{"models":[{"name":"models/gemini-2.5-flash","supportedGenerationMethods":["generateContent"]},{"name":"models/embedding-001","supportedGenerationMethods":["embedContent"]}]}""",
        )
        assertEquals(listOf("gemini-2.5-flash"), geminiParsed)

        // Customize Claude CLI + temperature clamping + delete protection
        core.selectAiProfile("claude_cli")
        core.updateAiProfile(
            profileId = "claude_cli",
            apiKey = "test-claude-key",
            addCustomModel = "claude-sonnet-4-thinking",
            temperature = 2.5f, // clamps to 1.5f
            systemPrompt = "3줄 핵심 요약",
        )
        val claude = core.getSnapshot().preferences.activeAiProfile()
        assertEquals("test-claude-key", claude.apiKey)
        assertEquals("claude-sonnet-4-thinking", claude.selectedModel)
        assertTrue("claude-sonnet-4-thinking" in claude.availableModels)
        assertEquals(1.5f, claude.temperature, 0.001f)
        assertEquals("3줄 핵심 요약", claude.systemPrompt)

        core.deleteAiProfile("claude_cli")
        assertTrue(core.getSnapshot().preferences.aiProfiles.any { it.id == "claude_cli" })

        // Add custom AI CLI profile -> JSON roundtrip -> delete safely
        core.addCustomAiProfile(
            name = "DeepSeek Bridge",
            cliType = AiCliProfile.CLI_TYPE_CUSTOM,
            endpointOrCommand = "http://127.0.0.1:9000/v1/chat/completions",
            apiKey = "bridge-token",
            selectedModel = "deepseek-chat",
            temperature = 0.7f,
        )
        val customActive = core.getSnapshot().preferences.activeAiProfile()
        assertTrue(customActive.isCustom)
        assertEquals("DeepSeek Bridge", customActive.name)
        assertEquals("deepseek-chat", customActive.selectedModel)
        assertEquals(7, core.getSnapshot().preferences.aiProfiles.size)

        val json = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(json))
        assertEquals(customActive.id, restored.getSnapshot().preferences.activeAiProfile().id)
        assertEquals("DeepSeek Bridge", restored.getSnapshot().preferences.activeAiProfile().name)
        assertEquals(7, restored.getSnapshot().preferences.aiProfiles.size)

        core.deleteAiProfile(customActive.id)
        assertEquals(6, core.getSnapshot().preferences.aiProfiles.size)
        assertEquals("gemini_cli", core.getSnapshot().preferences.activeAiProfile().id)
    }

    @Test
    fun `rapid tab creation, closing last tab, and corrupted activeTabId coercion never throw`() {
        val core = LocalKotlinCore(initialHomeUrl = "https://duckduckgo.com")
        val createdIds = mutableListOf(core.getSnapshot().activeTabId)

        repeat(20) { idx ->
            val snap = core.createTab(initialUrl = "example$idx.com", activate = true)
            assertEquals(idx + 2, snap.tabs.size)
            assertEquals(snap.tabs.last().id, snap.activeTabId)
            assertNotNull(snap.activeTab)
            createdIds += snap.activeTabId
        }

        // Switching to a nonexistent tab keeps current valid activeTab safely
        val beforeInvalidSwitch = core.getSnapshot().activeTabId
        val afterInvalidSwitch = core.switchTab("nonexistent_tab_999")
        assertEquals(beforeInvalidSwitch, afterInvalidSwitch.activeTabId)

        // Closing all tabs always regenerates 1 clean fallback tab without empty-list crash
        createdIds.forEach { id ->
            val snap = core.closeTab(id)
            assertTrue(snap.tabs.isNotEmpty())
            assertNotNull(snap.activeTab)
        }
        assertEquals(1, core.getSnapshot().tabs.size)

        // Importing JSON with a dangling activeTabId coerces safely to index 0
        val exported = core.exportStateJson()
        val activeId = core.getSnapshot().activeTabId
        val corrupted = exported.replace("\"activeTabId\":\"$activeId\"", "\"activeTabId\":\"missing_id\"")
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(corrupted))
        assertEquals(restored.getSnapshot().tabs.first().id, restored.getSnapshot().activeTabId)
        assertNotNull(restored.getSnapshot().activeTab)
    }

    @Test
    fun `kimi 4 modes, kimi code headers, url normalization, and all 7 ai cli specs are accurate`() {
        val sampleTurn = listOf(AiChatTurn(isUser = true, text = "hello"))

        // URL normalization prevents double slashes like //models
        assertEquals(
            "https://api.moonshot.ai/v1",
            AiCliHttpClient.normalizeBaseUrl("  https://api.moonshot.ai/v1/  "),
        )

        // 1. Kimi API (Global & China) - no KimiCLI User-Agent
        val kimiGlobal = AiCliProfile(
            id = "kimi_cli",
            name = "Kimi CLI",
            cliType = AiCliProfile.CLI_TYPE_KIMI,
            providerSubtype = AiCliProfile.KIMI_SUBTYPE_GLOBAL,
            endpointOrCommand = "https://api.moonshot.ai/v1/",
            apiKey = "sk-kimi-global",
            selectedModel = "moonshot-v1-8k",
        )
        val (kimiGlobalModelsUrl, kimiGlobalHeaders) = AiCliHttpClient.buildModelsFetchSpec(
            kimiGlobal,
            kimiGlobal.endpointOrCommand,
        )
        assertEquals("https://api.moonshot.ai/v1/models", kimiGlobalModelsUrl)
        assertEquals("Bearer sk-kimi-global", kimiGlobalHeaders["Authorization"])
        assertFalse(kimiGlobalHeaders.containsKey("User-Agent"))

        val kimiChina = kimiGlobal.copy(
            providerSubtype = AiCliProfile.KIMI_SUBTYPE_CHINA,
            endpointOrCommand = BrowserPreferences.KIMI_CHINA_ENDPOINT,
        )
        val (kimiChinaChatUrl, kimiChinaChatHeaders, _) = AiCliHttpClient.buildHttpSpec(
            kimiChina,
            kimiChina.endpointOrCommand,
            sampleTurn,
        )
        assertEquals("https://api.moonshot.cn/v1/chat/completions", kimiChinaChatUrl)
        assertFalse(kimiChinaChatHeaders.containsKey("User-Agent"))

        // 2. Kimi Code - sends User-Agent: KimiCLI/1.0 + Bearer
        val kimiCode = kimiGlobal.copy(
            providerSubtype = AiCliProfile.KIMI_SUBTYPE_CODE,
            endpointOrCommand = BrowserPreferences.KIMI_CODE_ENDPOINT,
            apiKey = "sk-kimi-code",
            selectedModel = "kimi-for-coding",
        )
        assertTrue(AiCliHttpClient.isKimiCodeMode(kimiCode, BrowserPreferences.KIMI_CODE_ENDPOINT))
        val (kimiCodeModelsUrl, kimiCodeModelsHeaders) = AiCliHttpClient.buildModelsFetchSpec(
            kimiCode,
            kimiCode.endpointOrCommand,
        )
        assertEquals("https://api.kimi.com/coding/v1/models", kimiCodeModelsUrl)
        assertEquals("Bearer sk-kimi-code", kimiCodeModelsHeaders["Authorization"])
        assertEquals(AiCliHttpClient.KIMI_CODE_USER_AGENT, kimiCodeModelsHeaders["User-Agent"])

        val (kimiCodeChatUrl, kimiCodeChatHeaders, _) = AiCliHttpClient.buildHttpSpec(
            kimiCode,
            kimiCode.endpointOrCommand,
            sampleTurn,
        )
        assertEquals("https://api.kimi.com/coding/v1/chat/completions", kimiCodeChatUrl)
        assertEquals(AiCliHttpClient.KIMI_CODE_USER_AGENT, kimiCodeChatHeaders["User-Agent"])

        // Local Kimi CLI bridge allows blank key
        val kimiLocal = kimiGlobal.copy(
            providerSubtype = AiCliProfile.KIMI_SUBTYPE_LOCAL,
            endpointOrCommand = BrowserPreferences.KIMI_LOCAL_CLI_ENDPOINT,
            apiKey = "",
        )
        val (kimiLocalModelsUrl, kimiLocalHeaders) = AiCliHttpClient.buildModelsFetchSpec(
            kimiLocal,
            kimiLocal.endpointOrCommand,
        )
        assertEquals("http://127.0.0.1:8080/v1/models", kimiLocalModelsUrl)
        assertFalse(kimiLocalHeaders.containsKey("Authorization"))

        // 3. Gemini CLI (?key= query param for both /models and :generateContent)
        val gemini = AiCliProfile(
            id = "gemini_cli",
            name = "Gemini CLI",
            cliType = AiCliProfile.CLI_TYPE_GEMINI,
            endpointOrCommand = "https://generativelanguage.googleapis.com/v1beta/models/",
            apiKey = "gm-secret-key",
            selectedModel = "gemini-2.5-flash",
        )
        val (geminiModelsUrl, _) = AiCliHttpClient.buildModelsFetchSpec(gemini, gemini.endpointOrCommand)
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models?key=gm-secret-key",
            geminiModelsUrl,
        )
        val (geminiChatUrl, _, _) = AiCliHttpClient.buildHttpSpec(gemini, gemini.endpointOrCommand, sampleTurn)
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=gm-secret-key",
            geminiChatUrl,
        )

        // 4. Claude CLI (x-api-key + anthropic-version: 2023-06-01)
        val claude = AiCliProfile(
            id = "claude_cli",
            name = "Claude CLI",
            cliType = AiCliProfile.CLI_TYPE_CLAUDE,
            endpointOrCommand = "https://api.anthropic.com/v1/",
            apiKey = "sk-ant-key",
            selectedModel = "claude-sonnet-4-20250514",
        )
        val (claudeModelsUrl, claudeModelsHeaders) = AiCliHttpClient.buildModelsFetchSpec(claude, claude.endpointOrCommand)
        assertEquals("https://api.anthropic.com/v1/models", claudeModelsUrl)
        assertEquals("sk-ant-key", claudeModelsHeaders["x-api-key"])
        assertEquals("2023-06-01", claudeModelsHeaders["anthropic-version"])
        val (claudeChatUrl, claudeChatHeaders, _) = AiCliHttpClient.buildHttpSpec(claude, claude.endpointOrCommand, sampleTurn)
        assertEquals("https://api.anthropic.com/v1/messages", claudeChatUrl)
        assertEquals("sk-ant-key", claudeChatHeaders["x-api-key"])
        assertEquals("2023-06-01", claudeChatHeaders["anthropic-version"])

        // 5. Codex / OpenAI CLI
        val openai = AiCliProfile(
            id = "codex_cli",
            name = "Codex / OpenAI CLI",
            cliType = AiCliProfile.CLI_TYPE_OPENAI,
            endpointOrCommand = "https://api.openai.com/v1/",
            apiKey = "sk-openai-key",
            selectedModel = "gpt-4o",
        )
        val (openaiModelsUrl, openaiHeaders) = AiCliHttpClient.buildModelsFetchSpec(openai, openai.endpointOrCommand)
        assertEquals("https://api.openai.com/v1/models", openaiModelsUrl)
        assertEquals("Bearer sk-openai-key", openaiHeaders["Authorization"])
        val (openaiChatUrl, _, _) = AiCliHttpClient.buildHttpSpec(openai, openai.endpointOrCommand, sampleTurn)
        assertEquals("https://api.openai.com/v1/chat/completions", openaiChatUrl)

        // 6. Ollama Local (/api/tags & /api/chat, no API key required)
        val ollama = AiCliProfile(
            id = "ollama_local",
            name = "Ollama (로컬)",
            cliType = AiCliProfile.CLI_TYPE_OLLAMA,
            endpointOrCommand = "http://127.0.0.1:11434/",
            apiKey = "",
            selectedModel = "llama3.2",
        )
        val (ollamaTagsUrl, ollamaHeaders) = AiCliHttpClient.buildModelsFetchSpec(ollama, ollama.endpointOrCommand)
        assertEquals("http://127.0.0.1:11434/api/tags", ollamaTagsUrl)
        assertTrue(ollamaHeaders.isEmpty())
        val (ollamaChatUrl, _, _) = AiCliHttpClient.buildHttpSpec(ollama, ollama.endpointOrCommand, sampleTurn)
        assertEquals("http://127.0.0.1:11434/api/chat", ollamaChatUrl)

        // 7. Custom AI CLI
        val custom = AiCliProfile(
            id = "custom_cli",
            name = "Custom Bridge",
            cliType = AiCliProfile.CLI_TYPE_CUSTOM,
            endpointOrCommand = "http://127.0.0.1:8080/v1/",
            apiKey = "tok-123",
            selectedModel = "custom-model",
        )
        val (customModelsUrl, customHeaders) = AiCliHttpClient.buildModelsFetchSpec(custom, custom.endpointOrCommand)
        assertEquals("http://127.0.0.1:8080/v1/models", customModelsUrl)
        assertEquals("Bearer tok-123", customHeaders["Authorization"])
        val (customChatUrl, _, _) = AiCliHttpClient.buildHttpSpec(custom, custom.endpointOrCommand, sampleTurn)
        assertEquals("http://127.0.0.1:8080/v1/chat/completions", customChatUrl)
    }

    @Test
    fun `cold restart restores tabs, kimi subtype, api keys, and advances tab sequence safely`() {
        val original = LocalKotlinCore(initialHomeUrl = "https://duckduckgo.com")
        original.createTab("https://kotlinlang.org", activate = true)
        val thirdSnap = original.createTab("https://rust-lang.org", activate = true)
        val savedActiveId = thirdSnap.activeTabId
        // Incognito tab should not survive cold restart
        original.createTab("https://private.example.com", activate = false, isIncognito = true)

        original.selectAiProfile("kimi_cli")
        original.updateAiProfile(
            profileId = "kimi_cli",
            endpointOrCommand = BrowserPreferences.KIMI_CODE_ENDPOINT,
            apiKey = "sk-kimi-persisted",
            providerSubtype = AiCliProfile.KIMI_SUBTYPE_CODE,
            addCustomModel = "kimi-for-coding",
        )

        val serialized = original.exportStateJson()

        // Simulate fresh process after OS background kill
        val coldRestarted = LocalKotlinCore()
        assertTrue(coldRestarted.importStateJson(serialized))

        val restoredSnap = coldRestarted.getSnapshot()
        assertEquals(3, restoredSnap.tabs.size)
        assertFalse(restoredSnap.tabs.any { it.isIncognito })
        assertEquals(savedActiveId, restoredSnap.activeTabId)

        val restoredKimi = restoredSnap.preferences.activeAiProfile()
        assertEquals("kimi_cli", restoredKimi.id)
        assertEquals(AiCliProfile.KIMI_SUBTYPE_CODE, restoredKimi.providerSubtype)
        assertEquals(BrowserPreferences.KIMI_CODE_ENDPOINT, restoredKimi.endpointOrCommand)
        assertEquals("sk-kimi-persisted", restoredKimi.apiKey)
        assertEquals("kimi-for-coding", restoredKimi.selectedModel)

        // Creating a new tab after cold restart MUST NOT duplicate any restored tab ID
        val existingIds = restoredSnap.tabs.map { it.id }.toSet()
        val afterNewTab = coldRestarted.createTab("https://example.org", activate = true)
        assertEquals(4, afterNewTab.tabs.size)
        assertEquals(4, afterNewTab.tabs.map { it.id }.distinct().size)
        assertFalse(afterNewTab.activeTabId in existingIds)
    }

    @Test
    fun `tab groups create, reassign, prune on tab close, ungroup, and survive json cold restart`() {
        val core = LocalKotlinCore(initialHomeUrl = "https://duckduckgo.com")
        val tab1 = core.getSnapshot().activeTabId
        val tab2 = core.createTab("https://kotlinlang.org", activate = true).activeTabId
        val tab3 = core.createTab("https://rust-lang.org", activate = true).activeTabId

        val afterCreate = core.createTabGroup(
            title = "개발 문서",
            tabIds = listOf(tab2, tab3),
            colorIndex = 2,
        )
        assertEquals(1, afterCreate.tabGroups.size)
        val group1 = afterCreate.tabGroups.first()
        assertEquals("개발 문서", group1.title)
        assertEquals(2, group1.colorIndex)
        assertEquals(listOf(tab2, tab3), group1.tabIds)

        // Adding tab1 joins group1 cleanly
        val afterAdd = core.addTabToGroup(group1.id, tab1)
        assertEquals(listOf(tab2, tab3, tab1), afterAdd.tabGroups.first().tabIds)

        // Removing tab2 leaves tab3 and tab1 in group1
        val afterRemove = core.removeTabFromGroup(group1.id, tab2)
        assertEquals(listOf(tab3, tab1), afterRemove.tabGroups.first().tabIds)

        // Export & cold restart preserves tabGroups and advances tabGroupSequence
        val exported = core.exportStateJson()
        val restored = LocalKotlinCore()
        assertTrue(restored.importStateJson(exported))
        assertEquals(1, restored.getSnapshot().tabGroups.size)
        assertEquals("개발 문서", restored.getSnapshot().tabGroups.first().title)
        assertEquals(listOf(tab3, tab1), restored.getSnapshot().tabGroups.first().tabIds)

        // Creating a second group after restore never collides with restored group ID
        val secondGroupSnap = restored.createTabGroup("읽기 목록", listOf(tab2), colorIndex = 1)
        assertEquals(2, secondGroupSnap.tabGroups.size)
        assertEquals(2, secondGroupSnap.tabGroups.map { it.id }.distinct().size)

        // Closing tab2 automatically prunes the emptied "읽기 목록" group
        val afterCloseTab2 = restored.closeTab(tab2)
        assertEquals(1, afterCloseTab2.tabGroups.size)
        assertEquals(group1.id, afterCloseTab2.tabGroups.first().id)

        // Ungrouping removes group while keeping its tabs open
        val tabsBeforeUngroup = afterCloseTab2.tabs.size
        val afterUngroup = restored.ungroupTabGroup(group1.id)
        assertTrue(afterUngroup.tabGroups.isEmpty())
        assertEquals(tabsBeforeUngroup, afterUngroup.tabs.size)
    }
}
