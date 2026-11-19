package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.AiCliProfile
import com.example.core.BookmarkEntry
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.core.HistoryEntry
import com.example.core.QuickToggleAction
import com.example.core.QuickToggleLayoutStyle
import com.example.core.SearchEngine
import com.example.core.ToggleIndicatorStyle
import com.example.core.ToolbarAction
import com.example.core.ToolbarConfig
import com.example.ui.components.FoldablePostureUiState
import com.example.ui.components.quickToggleLabel
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlin.math.roundToInt

private enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
) {
    CUSTOMIZATION("사용자화", Icons.Rounded.Palette, "settings_category_customization"),
    GESTURES("제스처", Icons.Rounded.Gesture, "settings_category_gestures"),
    AI_SETTINGS("AI 설정", Icons.Rounded.AutoAwesome, "settings_category_ai"),
    FOLDABLE("폴더블", Icons.Rounded.Splitscreen, "settings_category_foldable"),
    TOOLBAR("툴바 구성", Icons.Rounded.Tune, "settings_category_toolbar"),
    SEARCH_ENGINE("검색 엔진", Icons.Rounded.Search, "settings_category_search"),
    DISPLAY_GESTURES("화면 및 제스처", Icons.Rounded.TouchApp, "settings_category_display"),
    PRIVACY_DATA("개인정보 및 데이터", Icons.Rounded.Shield, "settings_category_privacy"),
}

/**
 * Full-screen edge-to-edge Korean Settings screen entered via horizontal Push transition.
 * Synchronizes [unifiedRadius] across all category cards, sub-page panels, and option chips.
 */
@Composable
fun SettingsSheet(
    snapshot: BrowserCoreSnapshot,
    foldablePosture: FoldablePostureUiState,
    initialOpenAiSettings: Boolean = false,
    aiConnectionStatus: AiConnectionStatus? = null,
    onDismiss: () -> Unit,
    onUpdateBarGeometry: (
        cornerRadiusDp: Float?,
        heightDp: Float?,
        widthFraction: Float?,
        bottomOffsetDp: Float?,
        horizontalBias: Float?,
        glassOpacity: Float?,
    ) -> Unit,
    onToggleQuickToggleAction: (QuickToggleAction) -> Unit,
    onMoveQuickToggleAction: (QuickToggleAction, Int) -> Unit,
    onUpdateDropdownCustomization: (
        layoutStyle: QuickToggleLayoutStyle?,
        maxHeightDp: Float?,
        indicatorStyle: ToggleIndicatorStyle?,
    ) -> Unit,
    onUpdateFoldableSettings: (
        dualPaneEnabled: Boolean?,
        hingeAvoidanceDp: Float?,
        separateBarProfiles: Boolean?,
        unfoldedBarWidthFraction: Float?,
        unfoldedBarHorizontalBias: Float?,
        autoExpandToolbarWhenUnfolded: Boolean?,
    ) -> Unit,
    onResetFoldableSettings: () -> Unit,
    onUpdateGestureSettings: (
        gesturesEnabled: Boolean?,
        swipeUpAction: Int?,
        swipeDownAction: Int?,
        swipeLeftRightTabSwitch: Boolean?,
        doubleTapAction: Int?,
        gestureSensitivityDp: Float?,
        longPressSwipeUpAiChatEnabled: Boolean?,
    ) -> Unit,
    onResetGestureSettings: () -> Unit,
    onSelectAiProfile: (String) -> Unit,
    onTestConnectionAndFetchModels: (AiCliProfile) -> Unit,
    onUpdateAiProfile: (
        profileId: String,
        name: String?,
        endpointOrCommand: String?,
        apiKey: String?,
        selectedModel: String?,
        addCustomModel: String?,
        temperature: Float?,
        systemPrompt: String?,
        providerSubtype: String?,
    ) -> Unit,
    onAddCustomAiProfile: (
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
        temperature: Float,
    ) -> Unit,
    onDeleteAiProfile: (String) -> Unit,
    onResetCustomization: () -> Unit,
    onToggleLeftAction: (ToolbarAction) -> Unit,
    onToggleRightAction: (ToolbarAction) -> Unit,
    onResetToolbar: () -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onUpdateHomeUrl: (String) -> Unit,
    onToggleScrollToPill: (Boolean) -> Unit,
    onToggleExpandBarOnAddressEdit: (Boolean) -> Unit,
    onSelectTabSwitcherStyle: (Int) -> Unit,
    onToggleJavaScript: (Boolean) -> Unit,
    onToggleImagesBlocked: (Boolean) -> Unit,
    onToggleAlgorithmicDarkening: (Boolean) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onClearCacheOnly: () -> Unit,
    onClearHistoryOnly: () -> Unit,
    onClearAllBrowsingData: () -> Unit,
) {
    var activeCategory by remember(initialOpenAiSettings) {
        mutableStateOf(if (initialOpenAiSettings) SettingsCategory.AI_SETTINGS else null)
    }
    val prefs = snapshot.preferences
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)

    BackHandler(enabled = true) {
        if (activeCategory != null) {
            activeCategory = null
        } else {
            onDismiss()
        }
    }

    val fullScreenBackground = Brush.verticalGradient(
        colors = listOf(
            ObsidianSurface,
            ObsidianVoid,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fullScreenBackground)
            .testTag("settings_push_root"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .imePadding()
                .testTag("settings_sheet_content"),
        ) {
            AnimatedContent(
                targetState = activeCategory,
                transitionSpec = {
                    val forward = targetState != null
                    if (forward) {
                        (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 5 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(tween(240)) { -it / 5 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(200)) { it / 3 } + fadeOut(tween(160)))
                    }
                },
                label = "settings_category_navigation",
            ) { category ->
                if (category == null) {
                    TopLevelSettingsCategories(
                        snapshot = snapshot,
                        foldablePosture = foldablePosture,
                        unifiedRadius = unifiedRadius,
                        onSelectCategory = { activeCategory = it },
                    )
                } else {
                    SettingsSubPageContainer(
                        category = category,
                        onBack = { activeCategory = null },
                        trailingAction = {
                            when (category) {
                                SettingsCategory.CUSTOMIZATION -> {
                                    TextButton(
                                        onClick = onResetCustomization,
                                        modifier = Modifier.testTag("reset_customization_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "기본값 복원",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceCyan,
                                        )
                                    }
                                }
                                SettingsCategory.FOLDABLE -> {
                                    TextButton(
                                        onClick = onResetFoldableSettings,
                                        modifier = Modifier.testTag("reset_foldable_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "기본값 복원",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceCyan,
                                        )
                                    }
                                }
                                SettingsCategory.GESTURES -> {
                                    TextButton(
                                        onClick = onResetGestureSettings,
                                        modifier = Modifier.testTag("reset_gestures_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "기본값 복원",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceCyan,
                                        )
                                    }
                                }
                                SettingsCategory.TOOLBAR -> {
                                    TextButton(
                                        onClick = onResetToolbar,
                                        modifier = Modifier.testTag("reset_toolbar_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            tint = IceCyan,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "초기화",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceCyan,
                                        )
                                    }
                                }
                                else -> Unit
                            }
                        },
                    ) {
                        when (category) {
                            SettingsCategory.CUSTOMIZATION -> CustomizationPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                innerRadius = innerRadius,
                                onUpdateBarGeometry = onUpdateBarGeometry,
                                onToggleQuickToggleAction = onToggleQuickToggleAction,
                                onMoveQuickToggleAction = onMoveQuickToggleAction,
                                onUpdateDropdownCustomization = onUpdateDropdownCustomization,
                                onResetCustomization = onResetCustomization,
                            )

                            SettingsCategory.GESTURES -> GesturesSettingsPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                onUpdateGestureSettings = onUpdateGestureSettings,
                                onResetGestureSettings = onResetGestureSettings,
                            )

                            SettingsCategory.AI_SETTINGS -> AiSettingsPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                connectionStatus = aiConnectionStatus,
                                onSelectAiProfile = onSelectAiProfile,
                                onTestConnectionAndFetchModels = onTestConnectionAndFetchModels,
                                onToggleLongPressAiChat = { enabled ->
                                    onUpdateGestureSettings(null, null, null, null, null, null, enabled)
                                },
                                onUpdateAiProfile = onUpdateAiProfile,
                                onAddCustomAiProfile = onAddCustomAiProfile,
                                onDeleteAiProfile = onDeleteAiProfile,
                            )

                            SettingsCategory.FOLDABLE -> FoldableSettingsPage(
                                snapshot = snapshot,
                                foldablePosture = foldablePosture,
                                unifiedRadius = unifiedRadius,
                                onUpdateFoldableSettings = onUpdateFoldableSettings,
                                onResetFoldableSettings = onResetFoldableSettings,
                            )

                            SettingsCategory.TOOLBAR -> ToolbarCustomizationPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                innerRadius = innerRadius,
                                onToggleLeftAction = onToggleLeftAction,
                                onToggleRightAction = onToggleRightAction,
                            )

                            SettingsCategory.SEARCH_ENGINE -> SearchEnginePage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                onSelectSearchEngine = onSelectSearchEngine,
                                onUpdateHomeUrl = onUpdateHomeUrl,
                            )

                            SettingsCategory.DISPLAY_GESTURES -> DisplayAndGesturesPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                onToggleScrollToPill = onToggleScrollToPill,
                                onToggleExpandBarOnAddressEdit = onToggleExpandBarOnAddressEdit,
                                onSelectTabSwitcherStyle = onSelectTabSwitcherStyle,
                                onToggleAlgorithmicDarkening = onToggleAlgorithmicDarkening,
                                onToggleDesktopMode = onToggleDesktopMode,
                                onToggleImagesBlocked = onToggleImagesBlocked,
                            )

                            SettingsCategory.PRIVACY_DATA -> PrivacyAndDataPage(
                                snapshot = snapshot,
                                unifiedRadius = unifiedRadius,
                                onToggleAdBlock = onToggleAdBlock,
                                onToggleJavaScript = onToggleJavaScript,
                                onToggleImagesBlocked = onToggleImagesBlocked,
                                onClearCacheOnly = onClearCacheOnly,
                                onClearHistoryOnly = onClearHistoryOnly,
                                onClearAllBrowsingData = onClearAllBrowsingData,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen edge-to-edge History & Bookmarks screen entered via Push transition.
 */
@Composable
fun HistoryAndBookmarksSheet(
    snapshot: BrowserCoreSnapshot,
    historyEntries: List<HistoryEntry>,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onRemoveBookmark: (BookmarkEntry) -> Unit,
    onClearHistory: () -> Unit,
) {
    val prefs = snapshot.preferences
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = 방문 기록, 1 = 북마크

    BackHandler(enabled = true) { onDismiss() }

    val fullScreenBackground = Brush.verticalGradient(
        colors = listOf(ObsidianSurface, ObsidianVoid)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fullScreenBackground)
            .testTag("history_bookmarks_sheet"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로",
                            tint = PureFrost,
                        )
                    }
                    Text(
                        text = if (selectedTabIndex == 0) "방문 기록" else "북마크",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureFrost,
                    )
                }

                if (selectedTabIndex == 0 && historyEntries.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "기록 삭제",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonStop,
                        )
                    }
                }
            }

            // Segmented switcher using unifiedRadius / innerRadius
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(unifiedRadius))
                    .background(ObsidianElevated.copy(alpha = 0.65f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("방문 기록 (${historyEntries.size})", "북마크 (${snapshot.bookmarks.size})").forEachIndexed { idx, label ->
                    val active = selectedTabIndex == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(innerRadius))
                            .background(if (active) IceCyan.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { selectedTabIndex = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) PureFrost else FrostSlate,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (selectedTabIndex == 0) {
                    if (historyEntries.isEmpty()) {
                        EmptyArchiveCard(
                            icon = Icons.Rounded.History,
                            message = "저장된 방문 기록이 없습니다.",
                            unifiedRadius = unifiedRadius,
                        )
                    } else {
                        historyEntries.forEach { entry ->
                            UrlArchiveRow(
                                icon = Icons.Rounded.Public,
                                title = entry.title.ifBlank { entry.url },
                                url = entry.url,
                                unifiedRadius = unifiedRadius,
                                onClick = { onOpenUrl(entry.url) },
                            )
                        }
                    }
                } else {
                    if (snapshot.bookmarks.isEmpty()) {
                        EmptyArchiveCard(
                            icon = Icons.Rounded.Bookmark,
                            message = "저장된 북마크가 없습니다.",
                            unifiedRadius = unifiedRadius,
                        )
                    } else {
                        snapshot.bookmarks.forEach { bm ->
                            UrlArchiveRow(
                                icon = Icons.Rounded.Bookmark,
                                title = bm.title.ifBlank { bm.url },
                                url = bm.url,
                                unifiedRadius = unifiedRadius,
                                onClick = { onOpenUrl(bm.url) },
                                onTrailingRemove = { onRemoveBookmark(bm) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchiveCard(
    icon: ImageVector,
    message: String,
    unifiedRadius: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(unifiedRadius))
            .background(ObsidianElevated.copy(alpha = 0.50f))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FrostSlate,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = FrostSlate,
        )
    }
}

@Composable
private fun UrlArchiveRow(
    icon: ImageVector,
    title: String,
    url: String,
    unifiedRadius: Dp,
    onClick: () -> Unit,
    onTrailingRemove: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.65f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IceCyan,
            modifier = Modifier.size(18.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = PureFrost,
            )
            Text(
                text = url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = FrostSlate,
            )
        }
        if (onTrailingRemove != null) {
            IconButton(onClick = onTrailingRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "북마크 삭제",
                    tint = FrostSlate,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TopLevelSettingsCategories(
    snapshot: BrowserCoreSnapshot,
    foldablePosture: FoldablePostureUiState,
    unifiedRadius: Dp,
    onSelectCategory: (SettingsCategory) -> Unit,
) {
    val prefs = snapshot.preferences
    val toolbar = prefs.toolbarConfig

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_category_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        CategoryRowCard(
            category = SettingsCategory.CUSTOMIZATION,
            summary = "곡률 ${prefs.barCornerRadiusDp.roundToInt()}dp · 너비 ${(prefs.barWidthFraction * 100).roundToInt()}% · 퀵토글 ${prefs.customQuickToggles.size}개",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.CUSTOMIZATION) },
        )

        CategoryRowCard(
            category = SettingsCategory.GESTURES,
            summary = "주소창 제스처 ${if (prefs.gesturesEnabled) "켜짐" else "꺼짐"} · 감도 ${prefs.gestureSensitivityDp.roundToInt()}dp",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.GESTURES) },
        )

        val activeAi = prefs.activeAiProfile()
        CategoryRowCard(
            category = SettingsCategory.AI_SETTINGS,
            summary = "${activeAi.name} · ${activeAi.selectedModel.ifBlank { "모델 미선택" }}",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.AI_SETTINGS) },
        )

        val postureSummary = if (foldablePosture.isUnfolded) "현재 펼침" else "현재 접힘"
        val dualPaneSummary = if (prefs.foldableDualPaneEnabled) "듀얼 뷰 켜짐" else "단일 뷰"
        CategoryRowCard(
            category = SettingsCategory.FOLDABLE,
            summary = "$postureSummary · $dualPaneSummary · 힌지 여백 ${prefs.foldableHingeAvoidanceDp.roundToInt()}dp",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.FOLDABLE) },
        )

        CategoryRowCard(
            category = SettingsCategory.TOOLBAR,
            summary = "좌측 ${toolbar.leftActions.size}개 · 우측 ${toolbar.rightActions.size}개",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.TOOLBAR) },
        )

        CategoryRowCard(
            category = SettingsCategory.SEARCH_ENGINE,
            summary = prefs.searchEngine.displayName,
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.SEARCH_ENGINE) },
        )

        val tabStyleLabel = if (prefs.tabSwitcherStyle == BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW) {
            "그리드 탭 창"
        } else {
            "기존 탭 창"
        }
        CategoryRowCard(
            category = SettingsCategory.DISPLAY_GESTURES,
            summary = "스크롤 축소 ${if (prefs.scrollToPillEnabled) "켜짐" else "꺼짐"} · $tabStyleLabel · 다크 ${if (prefs.algorithmicDarkeningEnabled) "켜짐" else "꺼짐"}",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.DISPLAY_GESTURES) },
        )

        CategoryRowCard(
            category = SettingsCategory.PRIVACY_DATA,
            summary = "방문 기록 ${snapshot.historyCount}개 · 북마크 ${snapshot.bookmarks.size}개",
            unifiedRadius = unifiedRadius,
            onClick = { onSelectCategory(SettingsCategory.PRIVACY_DATA) },
        )
    }
}

@Composable
private fun CategoryRowCard(
    category: SettingsCategory,
    summary: String,
    unifiedRadius: Dp,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.72f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(category.testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(unifiedInnerRadius(unifiedRadius)))
                .background(IceCyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                color = PureFrost,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = FrostSlate,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = FrostSlate,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSubPageContainer(
    category: SettingsCategory,
    onBack: () -> Unit,
    trailingAction: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_subpage_back"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "뒤로",
                        tint = PureFrost,
                    )
                }
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PureFrost,
                )
            }
            trailingAction()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomizationPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onUpdateBarGeometry: (Float?, Float?, Float?, Float?, Float?, Float?) -> Unit,
    onToggleQuickToggleAction: (QuickToggleAction) -> Unit,
    onMoveQuickToggleAction: (QuickToggleAction, Int) -> Unit,
    onUpdateDropdownCustomization: (QuickToggleLayoutStyle?, Float?, ToggleIndicatorStyle?) -> Unit,
    onResetCustomization: () -> Unit,
) {
    val prefs = snapshot.preferences

    // Live interactive preview reflecting exact corner radius, height, width fraction, alignment, and glass opacity
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(unifiedRadius))
            .background(ObsidianVoid.copy(alpha = 0.85f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(unifiedRadius))
            .padding(horizontal = 12.dp, vertical = 18.dp),
    ) {
        val previewWidth = maxWidth * prefs.barWidthFraction
        LiquidGlassSurface(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias = prefs.barHorizontalBias, verticalBias = 0f))
                .width(previewWidth)
                .height(prefs.barHeightDp.dp),
            shape = RoundedCornerShape(unifiedRadius),
            glassOpacity = prefs.glassOpacity,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = PureFrost.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height((prefs.barHeightDp - 14f).coerceIn(28f, 54f).dp)
                        .clip(RoundedCornerShape(innerRadius))
                        .background(Color(0xFF0A111F).copy(alpha = 0.78f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(innerRadius))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = EmeraldSecure,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "mybrowser.bm",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = PureFrost,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = PureFrost.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = IceCyan,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    // Geometry Sliders
    GeometrySliderCard(
        title = "플로팅 바 곡률",
        valueLabel = "${prefs.barCornerRadiusDp.roundToInt()}dp",
        value = prefs.barCornerRadiusDp,
        valueRange = 6f..36f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_bar_corner_radius",
        onValueChange = { onUpdateBarGeometry(it, null, null, null, null, null) },
    )

    GeometrySliderCard(
        title = "플로팅 바 높이",
        valueLabel = "${prefs.barHeightDp.roundToInt()}dp",
        value = prefs.barHeightDp,
        valueRange = 44f..72f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_bar_height",
        onValueChange = { onUpdateBarGeometry(null, it, null, null, null, null) },
    )

    GeometrySliderCard(
        title = "플로팅 바 너비",
        valueLabel = "${(prefs.barWidthFraction * 100).roundToInt()}%",
        value = prefs.barWidthFraction,
        valueRange = 0.70f..1.0f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_bar_width",
        onValueChange = { onUpdateBarGeometry(null, null, it, null, null, null) },
    )

    GeometrySliderCard(
        title = "하단 여백 위치",
        valueLabel = "${prefs.barBottomOffsetDp.roundToInt()}dp",
        value = prefs.barBottomOffsetDp,
        valueRange = 4f..64f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_bar_bottom_offset",
        onValueChange = { onUpdateBarGeometry(null, null, null, it, null, null) },
    )

    val alignmentLabel = when {
        prefs.barHorizontalBias < -0.25f -> "좌측 (${(prefs.barHorizontalBias * 100).roundToInt()}%)"
        prefs.barHorizontalBias > 0.25f -> "우측 (+${(prefs.barHorizontalBias * 100).roundToInt()}%)"
        else -> "중앙"
    }
    GeometrySliderCard(
        title = "좌우 위치 정렬",
        valueLabel = alignmentLabel,
        value = prefs.barHorizontalBias,
        valueRange = -1f..1f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_bar_horizontal_bias",
        onValueChange = { onUpdateBarGeometry(null, null, null, null, it, null) },
    )

    GeometrySliderCard(
        title = "글래스 투명도",
        valueLabel = "${(prefs.glassOpacity * 100).roundToInt()}%",
        value = prefs.glassOpacity,
        valueRange = 0.05f..0.45f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_glass_opacity",
        onValueChange = { onUpdateBarGeometry(null, null, null, null, null, it) },
    )

    // Quick-Toggle Dropdown Menu Editor (layout style, indicator style, max height, selection & ordering)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(unifiedRadius))
            .background(ObsidianElevated.copy(alpha = 0.72f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(unifiedRadius))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "퀵토글 메뉴 편집",
                style = MaterialTheme.typography.titleMedium,
                color = PureFrost,
            )
            Text(
                text = "${prefs.customQuickToggles.size} / ${QuickToggleAction.entries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }

        // 1. Dropdown Layout Style (1열 리스트형 vs 2열 컴팩트 타일형)
        Text(
            text = "드롭다운 레이아웃 스타일",
            style = MaterialTheme.typography.labelMedium,
            color = FrostSlate,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                QuickToggleLayoutStyle.SINGLE_COLUMN_LIST to "1열 리스트형",
                QuickToggleLayoutStyle.TWO_COLUMN_TILES to "2열 컴팩트 타일형",
            ).forEach { (style, label) ->
                val selected = prefs.dropdownLayoutStyle == style
                val chipShape = RoundedCornerShape(innerRadius)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(chipShape)
                        .background(
                            if (selected) IceCyan.copy(alpha = 0.22f)
                            else ObsidianVoid.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (selected) IceCyan else Color.White.copy(alpha = 0.12f),
                            shape = chipShape,
                        )
                        .clickable { onUpdateDropdownCustomization(style, null, null) }
                        .padding(vertical = 10.dp)
                        .testTag("dropdown_layout_${style.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PureFrost else FrostSlate,
                    )
                }
            }
        }

        // 2. Toggle Accent Indicator Style
        Text(
            text = "토글 상태 표시 스타일",
            style = MaterialTheme.typography.labelMedium,
            color = FrostSlate,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                ToggleIndicatorStyle.SWITCH to "스위치",
                ToggleIndicatorStyle.PILL to "ON/OFF 배지",
                ToggleIndicatorStyle.DOT to "포인트 도트",
            ).forEach { (style, label) ->
                val selected = prefs.toggleIndicatorStyle == style
                val chipShape = RoundedCornerShape(innerRadius)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(chipShape)
                        .background(
                            if (selected) IceCyan.copy(alpha = 0.22f)
                            else ObsidianVoid.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (selected) IceCyan else Color.White.copy(alpha = 0.12f),
                            shape = chipShape,
                        )
                        .clickable { onUpdateDropdownCustomization(null, null, style) }
                        .padding(vertical = 9.dp)
                        .testTag("toggle_indicator_${style.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) PureFrost else FrostSlate,
                    )
                }
            }
        }

        // 3. Dropdown Max Height
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "드롭다운 최대 높이",
                    style = MaterialTheme.typography.labelMedium,
                    color = FrostSlate,
                )
                Text(
                    text = "${prefs.dropdownMaxHeightDp.roundToInt()}dp",
                    style = MaterialTheme.typography.labelMedium,
                    color = IceCyan,
                )
            }
            Slider(
                value = prefs.dropdownMaxHeightDp,
                onValueChange = { onUpdateDropdownCustomization(null, it, null) },
                valueRange = 220f..540f,
                colors = SliderDefaults.colors(
                    thumbColor = IceCyan,
                    activeTrackColor = IceCyan,
                    inactiveTrackColor = ObsidianVoid,
                ),
                modifier = Modifier.testTag("slider_dropdown_max_height"),
            )
        }

        // 4. Active Quick Toggles Order & Toggle Controls
        Text(
            text = "활성 퀵토글 순서 및 항목 선택 (총 14종)",
            style = MaterialTheme.typography.labelMedium,
            color = FrostSlate,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            prefs.customQuickToggles.forEachIndexed { index, toggle ->
                val rowShape = RoundedCornerShape(innerRadius)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(rowShape)
                        .background(IceCyan.copy(alpha = 0.12f))
                        .border(0.5.dp, IceCyan.copy(alpha = 0.35f), rowShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("active_quick_toggle_${toggle.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceCyan,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        text = quickToggleLabel(toggle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureFrost,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onMoveQuickToggleAction(toggle, -1) },
                        enabled = index > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_up_${toggle.name.lowercase()}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "위로 이동",
                            tint = if (index > 0) PureFrost else FrostSlate.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { onMoveQuickToggleAction(toggle, +1) },
                        enabled = index < prefs.customQuickToggles.lastIndex,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("move_down_${toggle.name.lowercase()}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "아래로 이동",
                            tint = if (index < prefs.customQuickToggles.lastIndex) PureFrost else FrostSlate.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = { onToggleQuickToggleAction(toggle) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_quick_toggle_${toggle.name.lowercase()}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "메뉴에서 제거",
                            tint = FrostSlate,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        val inactiveToggles = QuickToggleAction.entries.filterNot { it in prefs.customQuickToggles }
        if (inactiveToggles.isNotEmpty()) {
            Text(
                text = "추가 가능한 항목",
                style = MaterialTheme.typography.labelSmall,
                color = FrostSlate,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                inactiveToggles.forEach { toggle ->
                    val chipShape = RoundedCornerShape(innerRadius)
                    Row(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(ObsidianVoid.copy(alpha = 0.65f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.12f), chipShape)
                            .clickable { onToggleQuickToggleAction(toggle) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("edit_quick_toggle_${toggle.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = quickToggleLabel(toggle),
                            style = MaterialTheme.typography.labelMedium,
                            color = FrostSlate,
                        )
                    }
                }
            }
        }
    }

    OutlinedButton(
        onClick = onResetCustomization,
        shape = RoundedCornerShape(unifiedRadius),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("reset_customization_bottom_button"),
    ) {
        Icon(
            imageVector = Icons.Rounded.RestartAlt,
            contentDescription = null,
            tint = IceCyan,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "기본값 복원",
            style = MaterialTheme.typography.titleMedium,
            color = IceCyan,
        )
    }
}

@Composable
private fun FoldableSettingsPage(
    snapshot: BrowserCoreSnapshot,
    foldablePosture: FoldablePostureUiState,
    unifiedRadius: Dp,
    onUpdateFoldableSettings: (
        dualPaneEnabled: Boolean?,
        hingeAvoidanceDp: Float?,
        separateBarProfiles: Boolean?,
        unfoldedBarWidthFraction: Float?,
        unfoldedBarHorizontalBias: Float?,
        autoExpandToolbarWhenUnfolded: Boolean?,
    ) -> Unit,
    onResetFoldableSettings: () -> Unit,
) {
    val prefs = snapshot.preferences
    val statusShape = RoundedCornerShape(unifiedRadius)

    // Current Hardware / Layout Posture Banner
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(statusShape)
            .background(
                if (foldablePosture.isUnfolded) IceCyan.copy(alpha = 0.14f)
                else ObsidianElevated.copy(alpha = 0.70f)
            )
            .border(
                width = 0.75.dp,
                color = if (foldablePosture.isUnfolded) IceCyan.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.12f),
                shape = statusShape,
            )
            .padding(14.dp)
            .testTag("foldable_posture_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (foldablePosture.isUnfolded) Icons.Rounded.TabletAndroid else Icons.Rounded.Smartphone,
            contentDescription = null,
            tint = if (foldablePosture.isUnfolded) IceCyan else FrostSlate,
            modifier = Modifier.size(24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val postureLabel = when {
                foldablePosture.isTabletopPosture -> "테이블탑 자세 (반접힘)"
                foldablePosture.isUnfolded -> "펼침 상태 (Unfolded)"
                else -> "접힘 / 일반 화면 (Folded)"
            }
            Text(
                text = "감지된 기기 자세: $postureLabel",
                style = MaterialTheme.typography.titleMedium,
                color = PureFrost,
            )
            val hingeDetail = if (foldablePosture.hasHardwareHinge) {
                "하드웨어 힌지 감지됨 (${foldablePosture.hingeWidthDp.value.roundToInt()}dp)"
            } else {
                "화면 크기 및 WindowInfoTracker 기반 자동 감지"
            }
            Text(
                text = hingeDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = FrostSlate,
            )
        }
    }

    SettingsToggleRow(
        icon = Icons.Rounded.ViewColumn,
        title = "듀얼 뷰 화면 분할",
        subtitle = "펼침 상태에서 두 개의 웹 뷰를 좌우 나란히 표시",
        checked = prefs.foldableDualPaneEnabled,
        unifiedRadius = unifiedRadius,
        onCheckedChange = { onUpdateFoldableSettings(it, null, null, null, null, null) },
        testTag = "toggle_foldable_dual_pane",
    )

    GeometrySliderCard(
        title = "힌지 영역 회피 여백",
        valueLabel = "${prefs.foldableHingeAvoidanceDp.roundToInt()}dp",
        value = prefs.foldableHingeAvoidanceDp,
        valueRange = 0f..32f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_foldable_hinge_avoidance",
        onValueChange = { onUpdateFoldableSettings(null, it, null, null, null, null) },
    )

    SettingsToggleRow(
        icon = Icons.Rounded.Splitscreen,
        title = "접힘/펼침 플로팅 바 개별 설정",
        subtitle = "펼침 화면에서는 별도의 플로팅 바 너비와 한손 위치 프로필 적용",
        checked = prefs.foldableSeparateBarProfiles,
        unifiedRadius = unifiedRadius,
        onCheckedChange = { onUpdateFoldableSettings(null, null, it, null, null, null) },
        testTag = "toggle_foldable_separate_profiles",
    )

    GeometrySliderCard(
        title = "펼침 시 플로팅 바 너비",
        valueLabel = "${(prefs.unfoldedBarWidthFraction * 100).roundToInt()}%",
        value = prefs.unfoldedBarWidthFraction,
        valueRange = 0.40f..1.0f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_unfolded_bar_width",
        onValueChange = { onUpdateFoldableSettings(null, null, null, it, null, null) },
    )

    val unfoldedBiasLabel = when {
        prefs.unfoldedBarHorizontalBias < -0.25f -> "좌측 한손 (${(prefs.unfoldedBarHorizontalBias * 100).roundToInt()}%)"
        prefs.unfoldedBarHorizontalBias > 0.25f -> "우측 한손 (+${(prefs.unfoldedBarHorizontalBias * 100).roundToInt()}%)"
        else -> "중앙"
    }
    GeometrySliderCard(
        title = "펼침 시 한손 조작 위치 정렬",
        valueLabel = unfoldedBiasLabel,
        value = prefs.unfoldedBarHorizontalBias,
        valueRange = -1f..1f,
        unifiedRadius = unifiedRadius,
        testTag = "slider_unfolded_bar_bias",
        onValueChange = { onUpdateFoldableSettings(null, null, null, null, it, null) },
    )

    SettingsToggleRow(
        icon = Icons.Rounded.SpaceBar,
        title = "펼침 시 툴바 아이콘 자동 확장",
        subtitle = "넓은 화면으로 펼칠 때 추가 툴바 단축 아이콘 자동 표시",
        checked = prefs.autoExpandToolbarWhenUnfolded,
        unifiedRadius = unifiedRadius,
        onCheckedChange = { onUpdateFoldableSettings(null, null, null, null, null, it) },
        testTag = "toggle_foldable_auto_expand_toolbar",
    )

    OutlinedButton(
        onClick = onResetFoldableSettings,
        shape = RoundedCornerShape(unifiedRadius),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("reset_foldable_bottom_button"),
    ) {
        Icon(
            imageVector = Icons.Rounded.RestartAlt,
            contentDescription = null,
            tint = IceCyan,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "폴더블 설정 기본값 복원",
            style = MaterialTheme.typography.titleMedium,
            color = IceCyan,
        )
    }
}

@Composable
private fun GeometrySliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unifiedRadius: Dp,
    testTag: String,
    onValueChange: (Float) -> Unit,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PureFrost,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = IceCyan,
                activeTrackColor = IceCyan,
                inactiveTrackColor = ObsidianVoid,
            ),
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun ToolbarCustomizationPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onToggleLeftAction: (ToolbarAction) -> Unit,
    onToggleRightAction: (ToolbarAction) -> Unit,
) {
    val toolbar = snapshot.preferences.toolbarConfig

    Text(
        text = "하단 플로팅 바의 좌우 슬롯에 표시할 아이콘을 최대 ${ToolbarConfig.MAX_ACTIONS_PER_SIDE}개씩 선택하세요.",
        style = MaterialTheme.typography.bodyMedium,
        color = FrostSlate,
    )

    SlotSelectorCard(
        title = "좌측 툴바",
        countLabel = "${toolbar.leftActions.size} / ${ToolbarConfig.MAX_ACTIONS_PER_SIDE}",
        selectedActions = toolbar.leftActions,
        slotPrefix = "left",
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onToggleAction = onToggleLeftAction,
    )

    SlotSelectorCard(
        title = "우측 툴바",
        countLabel = "${toolbar.rightActions.size} / ${ToolbarConfig.MAX_ACTIONS_PER_SIDE}",
        selectedActions = toolbar.rightActions,
        slotPrefix = "right",
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onToggleAction = onToggleRightAction,
    )
}

@Composable
private fun SearchEnginePage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onUpdateHomeUrl: (String) -> Unit,
) {
    val prefs = snapshot.preferences
    var homeUrlDraft by remember(prefs.homeUrl) { mutableStateOf(prefs.homeUrl) }
    var homeSaved by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SearchEngine.entries.forEach { engine ->
            val selected = prefs.searchEngine == engine
            val cardShape = RoundedCornerShape(unifiedRadius)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (selected) IceCyan.copy(alpha = 0.16f)
                        else ObsidianElevated.copy(alpha = 0.65f)
                    )
                    .border(
                        width = 0.75.dp,
                        color = if (selected) IceCyan else Color.White.copy(alpha = 0.10f),
                        shape = cardShape,
                    )
                    .clickable { onSelectSearchEngine(engine) }
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .testTag("search_engine_${engine.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (selected) IceCyan else FrostSlate,
                    modifier = Modifier.size(20.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = engine.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureFrost,
                    )
                    Text(
                        text = engine.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostSlate,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(6.dp))

    Text(
        text = "시작 홈 페이지",
        style = MaterialTheme.typography.titleMedium,
        color = PureFrost,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = homeUrlDraft,
            onValueChange = {
                homeUrlDraft = it
                homeSaved = false
            },
            singleLine = true,
            shape = RoundedCornerShape(unifiedInnerRadius(unifiedRadius)),
            label = {
                Text(
                    text = "홈 URL",
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IceCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                focusedLabelColor = IceCyan,
                unfocusedLabelColor = FrostSlate,
                cursorColor = IceCyan,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("home_url_input"),
        )

        OutlinedButton(
            onClick = {
                onUpdateHomeUrl(homeUrlDraft)
                homeSaved = true
            },
            shape = RoundedCornerShape(unifiedInnerRadius(unifiedRadius)),
            modifier = Modifier.testTag("apply_home_url_button"),
        ) {
            Text(
                text = "적용",
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }
    }

    AnimatedVisibility(visible = homeSaved) {
        Text(
            text = "홈 페이지 주소가 저장되었습니다.",
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldSecure,
        )
    }
}

@Composable
private fun DisplayAndGesturesPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    onToggleScrollToPill: (Boolean) -> Unit,
    onToggleExpandBarOnAddressEdit: (Boolean) -> Unit,
    onSelectTabSwitcherStyle: (Int) -> Unit,
    onToggleAlgorithmicDarkening: (Boolean) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleImagesBlocked: (Boolean) -> Unit,
) {
    val prefs = snapshot.preferences
    val activeTab = snapshot.activeTab
    val innerRadius = unifiedInnerRadius(unifiedRadius)

    SettingsToggleRow(
        icon = Icons.Rounded.UnfoldMore,
        title = "주소 수정 시 주소창 확장",
        subtitle = "주소를 입력할 때 플로팅 바를 넓고 높게 확장하여 전체 URL 줄바꿈 표시",
        checked = prefs.expandBarOnAddressEdit,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleExpandBarOnAddressEdit,
        testTag = "toggle_expand_bar_on_address_edit",
    )

    // Tab Switcher Style selector card (0 = 기존 탭 창, 1 = 그리드 미리보기 탭 창)
    val styleCardShape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(styleCardShape)
            .background(ObsidianElevated.copy(alpha = 0.65f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), styleCardShape)
            .padding(14.dp)
            .testTag("tab_switcher_style_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "탭 창 스타일",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureFrost,
                )
                Text(
                    text = "탭 목록 버튼을 눌렀을 때 표시되는 화면 스타일 선택",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostSlate,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                BrowserPreferences.TAB_SWITCHER_CLASSIC to "기존 탭 창",
                BrowserPreferences.TAB_SWITCHER_GRID_PREVIEW to "그리드 미리보기 탭 창",
            ).forEach { (styleCode, label) ->
                val selected = prefs.tabSwitcherStyle == styleCode
                val chipShape = RoundedCornerShape(innerRadius)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(chipShape)
                        .background(
                            if (selected) IceCyan.copy(alpha = 0.22f)
                            else ObsidianVoid.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (selected) IceCyan else Color.White.copy(alpha = 0.12f),
                            shape = chipShape,
                        )
                        .clickable { onSelectTabSwitcherStyle(styleCode) }
                        .padding(horizontal = 10.dp, vertical = 11.dp)
                        .testTag("tab_switcher_style_$styleCode"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (styleCode == BrowserPreferences.TAB_SWITCHER_CLASSIC) {
                            Icons.Rounded.Layers
                        } else {
                            Icons.Rounded.GridView
                        },
                        contentDescription = null,
                        tint = if (selected) IceCyan else FrostSlate,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PureFrost else FrostSlate,
                    )
                }
            }
        }
    }

    SettingsToggleRow(
        icon = Icons.Rounded.TouchApp,
        title = "스크롤 시 바 축소",
        subtitle = "아래로 스크롤하면 하단 바를 컴팩트 알약 형태로 축소",
        checked = prefs.scrollToPillEnabled,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleScrollToPill,
        testTag = "toggle_scroll_to_pill",
    )

    SettingsToggleRow(
        icon = Icons.Rounded.DarkMode,
        title = "강제 다크 모드",
        subtitle = "웹 페이지에 WebKit 알고리즘 기반 다크 테마 적용",
        checked = prefs.algorithmicDarkeningEnabled,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleAlgorithmicDarkening,
        testTag = "toggle_darkening",
    )

    SettingsToggleRow(
        icon = Icons.Rounded.Computer,
        title = "데스크톱 모드",
        subtitle = "현재 탭에서 데스크톱 화면 비율 요청",
        checked = activeTab?.isDesktopMode == true,
        unifiedRadius = unifiedRadius,
        onCheckedChange = { onToggleDesktopMode() },
        testTag = "toggle_desktop_mode",
    )

    SettingsToggleRow(
        icon = Icons.Rounded.HideImage,
        title = "이미지 차단 (데이터 절약)",
        subtitle = "웹 페이지 이미지 로딩을 차단하여 모바일 데이터 절약",
        checked = prefs.imagesBlocked,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleImagesBlocked,
        testTag = "toggle_images_blocked",
    )
}

@Composable
private fun PrivacyAndDataPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    onToggleAdBlock: () -> Unit,
    onToggleJavaScript: (Boolean) -> Unit,
    onToggleImagesBlocked: (Boolean) -> Unit,
    onClearCacheOnly: () -> Unit,
    onClearHistoryOnly: () -> Unit,
    onClearAllBrowsingData: () -> Unit,
) {
    val prefs = snapshot.preferences
    var statusMessage by remember { mutableStateOf<String?>(null) }

    SettingsToggleRow(
        icon = Icons.Rounded.Shield,
        title = "광고 및 추적 차단",
        subtitle = "주요 광고 및 추적 도메인 요청 차단",
        checked = prefs.adBlockEnabled,
        unifiedRadius = unifiedRadius,
        onCheckedChange = { onToggleAdBlock() },
        testTag = "toggle_ad_block",
    )

    SettingsToggleRow(
        icon = Icons.Rounded.Javascript,
        title = "자바스크립트 허용",
        subtitle = "웹 페이지 내 스크립트 실행 허용",
        checked = prefs.javaScriptEnabled,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleJavaScript,
        testTag = "toggle_javascript",
    )

    SettingsToggleRow(
        icon = Icons.Rounded.HideImage,
        title = "이미지 차단 (데이터 절약)",
        subtitle = "이미지 자동 로드를 비활성화하여 트래픽 절감",
        checked = prefs.imagesBlocked,
        unifiedRadius = unifiedRadius,
        onCheckedChange = onToggleImagesBlocked,
        testTag = "toggle_privacy_images_blocked",
    )

    PrivacyActionButton(
        icon = Icons.Rounded.DeleteOutline,
        label = "캐시 삭제",
        containerColor = ObsidianElevated.copy(alpha = 0.72f),
        contentColor = PureFrost,
        borderColor = Color.White.copy(alpha = 0.14f),
        unifiedRadius = unifiedRadius,
        testTag = "clear_cache_button",
        onClick = {
            onClearCacheOnly()
            statusMessage = "웹 캐시가 삭제되었습니다."
        },
    )

    PrivacyActionButton(
        icon = Icons.Rounded.History,
        label = "방문 기록 삭제",
        containerColor = ObsidianElevated.copy(alpha = 0.72f),
        contentColor = PureFrost,
        borderColor = Color.White.copy(alpha = 0.14f),
        unifiedRadius = unifiedRadius,
        testTag = "clear_history_button",
        onClick = {
            onClearHistoryOnly()
            statusMessage = "방문 기록이 삭제되었습니다."
        },
    )

    PrivacyActionButton(
        icon = Icons.Rounded.DeleteSweep,
        label = "캐시 · 쿠키 · 방문 기록 전체 삭제",
        containerColor = CrimsonStop.copy(alpha = 0.18f),
        contentColor = CrimsonStop,
        borderColor = CrimsonStop.copy(alpha = 0.45f),
        unifiedRadius = unifiedRadius,
        testTag = "clear_all_data_button",
        onClick = {
            onClearAllBrowsingData()
            statusMessage = "캐시, 쿠키 및 방문 기록이 모두 삭제되었습니다."
        },
    )

    AnimatedVisibility(visible = statusMessage != null) {
        Text(
            text = statusMessage.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldSecure,
            modifier = Modifier.testTag("privacy_status_confirmation"),
        )
    }

    // FFI Core Telemetry Card
    val cardShape = RoundedCornerShape(unifiedRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.45f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(unifiedInnerRadius(unifiedRadius)))
                .background(IceCyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = snapshot.engineBackendName,
                style = MaterialTheme.typography.labelMedium,
                color = PureFrost,
            )
            Text(
                text = "열린 탭 ${snapshot.tabs.size}개 · 방문 기록 ${snapshot.historyCount}개 · 북마크 ${snapshot.bookmarks.size}개",
                style = MaterialTheme.typography.labelSmall,
                color = FrostSlate,
            )
        }
    }
}

@Composable
private fun PrivacyActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    unifiedRadius: Dp,
    testTag: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(width = 0.75.dp, color = borderColor, shape = shape)
            .testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotSelectorCard(
    title: String,
    countLabel: String,
    selectedActions: List<ToolbarAction>,
    slotPrefix: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onToggleAction: (ToolbarAction) -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.72f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PureFrost,
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolbarAction.entries.forEach { action ->
                val isSelected = action in selectedActions
                val chipShape = RoundedCornerShape(innerRadius)
                Row(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(
                            if (isSelected) IceCyan.copy(alpha = 0.20f)
                            else ObsidianVoid.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (isSelected) IceCyan else Color.White.copy(alpha = 0.10f),
                            shape = chipShape,
                        )
                        .clickable { onToggleAction(action) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("${slotPrefix}_slot_${action.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = toolbarActionIcon(action),
                        contentDescription = null,
                        tint = if (isSelected) IceCyan else FrostSlate,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = toolbarActionLabel(action),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) PureFrost else FrostSlate,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    unifiedRadius: Dp,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.60f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) IceCyan else FrostSlate,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PureFrost,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = FrostSlate,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = IceCyan,
                uncheckedThumbColor = FrostSlate,
                uncheckedTrackColor = ObsidianVoid,
            ),
        )
    }
}

private fun toolbarActionIcon(action: ToolbarAction): ImageVector =
    when (action) {
        ToolbarAction.BACK -> Icons.AutoMirrored.Rounded.ArrowBack
        ToolbarAction.FORWARD -> Icons.AutoMirrored.Rounded.ArrowForward
        ToolbarAction.RELOAD_OR_STOP -> Icons.Rounded.Refresh
        ToolbarAction.TABS -> Icons.Rounded.Layers
        ToolbarAction.NEW_TAB -> Icons.Rounded.Add
        ToolbarAction.HOME -> Icons.Rounded.Home
        ToolbarAction.DESKTOP_MODE -> Icons.Rounded.Computer
        ToolbarAction.SHARE -> Icons.Rounded.Share
        ToolbarAction.PAGE_SUMMARY -> Icons.Rounded.AutoAwesome
    }

private fun toolbarActionLabel(action: ToolbarAction): String =
    when (action) {
        ToolbarAction.BACK -> "뒤로"
        ToolbarAction.FORWARD -> "앞으로"
        ToolbarAction.RELOAD_OR_STOP -> "새로고침 / 중지"
        ToolbarAction.TABS -> "탭"
        ToolbarAction.NEW_TAB -> "새 탭"
        ToolbarAction.HOME -> "홈"
        ToolbarAction.DESKTOP_MODE -> "데스크톱 모드"
        ToolbarAction.SHARE -> "공유"
        ToolbarAction.PAGE_SUMMARY -> "페이지 요약 / AI"
    }
