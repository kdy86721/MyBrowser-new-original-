package com.example

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
}
