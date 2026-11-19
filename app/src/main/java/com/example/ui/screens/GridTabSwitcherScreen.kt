package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserTabState
import com.example.core.SecurityState
import com.example.core.TabGroup
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlinx.coroutines.launch
import kotlin.math.abs

private val TabGroupAccentColors = listOf(
    IceCyan,
    EmeraldSecure,
    Color(0xFFA78BFA), // Violet
    AmberWarn,
    Color(0xFFF472B6), // Rose
    Color(0xFF60A5FA), // Sky Blue
)

/**
 * Full-screen 2-column grid preview tab switcher styled with Liquid Glass and [unifiedRadius]:
 * - Top drag handle + segmented pill (`탭` | `그룹`) synced with a 2-page [HorizontalPager]
 * - Swipe Down to Dismiss Tab Switcher when scrolled to the top (`firstVisibleItemIndex == 0`)
 * - Swipe Up on any individual tab preview card to smoothly close that tab with haptic feedback
 * - Dedicated `그룹` page with persistent custom [TabGroup]s ("새 그룹" / "그룹 해제") and automatic domain groups
 */
@Composable
fun GridTabSwitcherScreen(
    snapshot: BrowserCoreSnapshot,
    thumbnails: Map<String, Bitmap>,
    formatDisplayUrl: (String) -> String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: () -> Unit,
    onCreateTabGroup: (title: String, tabIds: List<String>, colorIndex: Int) -> Unit,
    onRemoveTabFromGroup: (groupId: String, tabId: String) -> Unit,
    onUngroupTabGroup: (groupId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val prefs = snapshot.preferences
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val glassOpacity = prefs.glassOpacity
    val tabs = snapshot.tabs
    val activeTabId = snapshot.activeTabId
    val tabGroups = snapshot.tabGroups

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val gridState = rememberLazyGridState()
    val groupListState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }

    // Window Swipe-Down-to-Dismiss offset state
    val dismissOffsetY = remember { Animatable(0f) }
    var dismissTriggered by remember { mutableStateOf(false) }
    val dismissThresholdPx = with(density) { 112.dp.toPx() }
    val flingMinOffsetPx = with(density) { 42.dp.toPx() }
    val maxDismissVisualPx = with(density) { 360.dp.toPx() }
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    fun isCurrentListAtTop(): Boolean =
        if (pagerState.currentPage == 0) {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        } else {
            groupListState.firstVisibleItemIndex == 0 && groupListState.firstVisibleItemScrollOffset == 0
        }

    fun finishOrResetWindowDismiss(velocityY: Float = 0f) {
        if (dismissTriggered) return
        val currentOffset = dismissOffsetY.value
        val shouldDismiss = currentOffset >= dismissThresholdPx ||
            (currentOffset >= flingMinOffsetPx && velocityY > 850f)
        if (shouldDismiss) {
            dismissTriggered = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                dismissOffsetY.animateTo(
                    targetValue = maxDismissVisualPx,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                )
                latestOnDismiss()
            }
        } else if (currentOffset > 0f) {
            coroutineScope.launch {
                dismissOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
        }
    }

    val swipeDownDismissConnection = remember(pagerState, gridState, groupListState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dismissTriggered) return Offset.Zero
                // If user already started pulling the window down and drags back up, retract offset first
                if (available.y < 0f && dismissOffsetY.value > 0f) {
                    val newOffset = (dismissOffsetY.value + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - dismissOffsetY.value
                    coroutineScope.launch { dismissOffsetY.snapTo(newOffset) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (dismissTriggered) return Offset.Zero
                if (source == NestedScrollSource.UserInput && available.y > 0f && isCurrentListAtTop()) {
                    val damped = available.y * 0.85f
                    val next = (dismissOffsetY.value + damped).coerceIn(0f, maxDismissVisualPx)
                    coroutineScope.launch { dismissOffsetY.snapTo(next) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dismissOffsetY.value > 0f) {
                    finishOrResetWindowDismiss(velocityY = available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    val filteredTabs = remember(tabs, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            tabs
        } else {
            tabs.filter { tab ->
                tab.title.lowercase().contains(q) || tab.url.lowercase().contains(q)
            }
        }
    }

    val backdropBrush = Brush.verticalGradient(
        colors = listOf(
            ObsidianSurface,
            ObsidianVoid,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val offset = dismissOffsetY.value
                val progress = (offset / maxDismissVisualPx).coerceIn(0f, 1f)
                translationY = offset
                alpha = 1f - progress * 0.35f
                val scale = 1f - progress * 0.045f
                scaleX = scale
                scaleY = scale
            }
            .background(backdropBrush)
            .nestedScroll(swipeDownDismissConnection)
            .testTag("grid_tab_switcher_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 1. Top Drag Handle + Segmented Switcher (draggable downward to dismiss window)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (dismissTriggered) return@detectVerticalDragGestures
                                if (dragAmount > 0f || dismissOffsetY.value > 0f) {
                                    val next = (dismissOffsetY.value + dragAmount * 0.9f)
                                        .coerceIn(0f, maxDismissVisualPx)
                                    coroutineScope.launch { dismissOffsetY.snapTo(next) }
                                }
                            },
                            onDragEnd = { finishOrResetWindowDismiss() },
                            onDragCancel = { finishOrResetWindowDismiss() },
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f))
                        .testTag("tab_switcher_drag_handle"),
                )

                TopTabSegmentedPill(
                    selectedSegment = pagerState.currentPage,
                    totalTabCount = tabs.size,
                    groupCount = tabGroups.size,
                    unifiedRadius = unifiedRadius,
                    innerRadius = innerRadius,
                    glassOpacity = glassOpacity,
                    onSelectSegment = { targetPage ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
                )
            }

            // 2. Rounded Pill Tab Search Bar ("탭 검색")
            TabSearchPillBar(
                query = searchQuery,
                unifiedRadius = unifiedRadius,
                glassOpacity = glassOpacity,
                onQueryChange = { searchQuery = it },
                onClearQuery = { searchQuery = "" },
                onDoneSearch = { keyboardController?.hide() },
            )

            // 3. 2-Page HorizontalPager: Page 0 = 탭 그리드, Page 1 = 그룹 (커스텀 + 도메인 자동 그룹)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("tab_switcher_horizontal_pager"),
                beyondViewportPageCount = 1,
            ) { page ->
                if (page == 0) {
                    if (filteredTabs.isEmpty()) {
                        EmptyTabSearchState(
                            hasSearchQuery = searchQuery.isNotBlank(),
                            unifiedRadius = unifiedRadius,
                            onCreateTab = onCreateTab,
                        )
                    } else {
                        TabPreviewGridPage(
                            tabs = filteredTabs,
                            activeTabId = activeTabId,
                            tabGroups = tabGroups,
                            thumbnails = thumbnails,
                            gridState = gridState,
                            formatDisplayUrl = formatDisplayUrl,
                            unifiedRadius = unifiedRadius,
                            innerRadius = innerRadius,
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                        )
                    }
                } else {
                    TabGroupsPage(
                        allTabs = tabs,
                        filteredTabs = filteredTabs,
                        activeTabId = activeTabId,
                        tabGroups = tabGroups,
                        thumbnails = thumbnails,
                        listState = groupListState,
                        formatDisplayUrl = formatDisplayUrl,
                        unifiedRadius = unifiedRadius,
                        innerRadius = innerRadius,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onCreateTabGroup = onCreateTabGroup,
                        onRemoveTabFromGroup = onRemoveTabFromGroup,
                        onUngroupTabGroup = onUngroupTabGroup,
                    )
                }
            }

            // 4. Bottom Liquid Glass Control Bar
            TabSwitcherBottomControlBar(
                tabCount = tabs.size,
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                glassOpacity = glassOpacity,
                onCreateTab = onCreateTab,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun TabPreviewGridPage(
    tabs: List<BrowserTabState>,
    activeTabId: String,
    tabGroups: List<TabGroup>,
    thumbnails: Map<String, Bitmap>,
    gridState: LazyGridState,
    formatDisplayUrl: (String) -> String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    val groupByTabId = remember(tabGroups) {
        buildMap {
            tabGroups.forEach { group ->
                group.tabIds.forEach { id -> put(id, group) }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_preview_grid"),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(
            items = tabs,
            key = { it.id },
        ) { tab ->
            val isActive = tab.id == activeTabId
            val bitmap = thumbnails[tab.id]?.takeIf { !it.isRecycled }
            val assignedGroup = groupByTabId[tab.id]
            TabPreviewGridCard(
                tab = tab,
                assignedGroup = assignedGroup,
                bitmap = bitmap,
                isActive = isActive,
                displayHost = formatDisplayUrl(tab.url),
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                onSelect = { onSelectTab(tab.id) },
                onClose = { onCloseTab(tab.id) },
            )
        }
    }
}

@Composable
private fun TopTabSegmentedPill(
    selectedSegment: Int,
    totalTabCount: Int,
    groupCount: Int,
    unifiedRadius: Dp,
    innerRadius: Dp,
    glassOpacity: Float,
    onSelectSegment: (Int) -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .widthIn(min = 248.dp, max = 330.dp)
            .testTag("tab_switcher_segmented_pill"),
        shape = RoundedCornerShape(unifiedRadius),
        shadowElevation = 10.dp,
        glassOpacity = glassOpacity,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tabSelected = selectedSegment == 0
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(
                        if (tabSelected) IceCyan.copy(alpha = 0.24f) else Color.Transparent
                    )
                    .border(
                        width = if (tabSelected) 0.75.dp else 0.dp,
                        color = if (tabSelected) IceCyan.copy(alpha = 0.65f) else Color.Transparent,
                        shape = RoundedCornerShape(innerRadius),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = IceCyan),
                        onClick = { onSelectSegment(0) },
                    )
                    .padding(horizontal = 10.dp)
                    .testTag("segment_open_tabs"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = if (tabSelected) IceCyan else FrostSlate,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "탭",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (tabSelected) PureFrost else FrostSlate,
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (tabSelected) IceCyan else Color.White.copy(alpha = 0.14f)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = totalTabCount.toString(),
                        style = TextStyle(
                            color = if (tabSelected) ObsidianVoid else PureFrost,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                        ),
                    )
                }
            }

            val groupSelected = selectedSegment == 1
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(
                        if (groupSelected) IceCyan.copy(alpha = 0.24f) else Color.Transparent
                    )
                    .border(
                        width = if (groupSelected) 0.75.dp else 0.dp,
                        color = if (groupSelected) IceCyan.copy(alpha = 0.65f) else Color.Transparent,
                        shape = RoundedCornerShape(innerRadius),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = IceCyan),
                        onClick = { onSelectSegment(1) },
                    )
                    .padding(horizontal = 10.dp)
                    .testTag("segment_tab_groups"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderCopy,
                    contentDescription = null,
                    tint = if (groupSelected) IceCyan else FrostSlate,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "그룹",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (groupSelected) PureFrost else FrostSlate,
                )
                if (groupCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (groupSelected) IceCyan else Color.White.copy(alpha = 0.14f)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = groupCount.toString(),
                            style = TextStyle(
                                color = if (groupSelected) ObsidianVoid else PureFrost,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabSearchPillBar(
    query: String,
    unifiedRadius: Dp,
    glassOpacity: Float,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onDoneSearch: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_search_bar"),
        shape = RoundedCornerShape(unifiedRadius),
        shadowElevation = 8.dp,
        glassOpacity = glassOpacity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "탭 검색",
                tint = IceCyan,
                modifier = Modifier.size(19.dp),
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "탭 검색",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = FrostSlate.copy(alpha = 0.78f),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                        ),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PureFrost,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                    ),
                    cursorBrush = SolidColor(IceCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onDoneSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tab_search_input"),
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClick = onClearQuery)
                        .testTag("tab_search_clear_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "검색어 지우기",
                        tint = FrostSlate,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPreviewGridCard(
    tab: BrowserTabState,
    assignedGroup: TabGroup?,
    bitmap: Bitmap?,
    isActive: Boolean,
    displayHost: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val swipeCloseOffsetY = remember(tab.id) { Animatable(0f) }
    var isClosing by remember(tab.id) { mutableStateOf(false) }
    val closeThresholdPx = with(density) { 78.dp.toPx() }
    val flyOutPx = with(density) { 240.dp.toPx() }
    val latestOnClose by rememberUpdatedState(onClose)

    val groupColor = assignedGroup?.let {
        TabGroupAccentColors[it.colorIndex.coerceIn(0, TabGroupAccentColors.lastIndex)]
    }

    val accentBorderColor = when {
        swipeCloseOffsetY.value <= -closeThresholdPx -> CrimsonStop
        isActive -> IceCyan
        groupColor != null -> groupColor.copy(alpha = 0.65f)
        tab.isIncognito -> Color(0xFFA78BFA).copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val borderWidth = if (isActive || swipeCloseOffsetY.value <= -closeThresholdPx) 2.5.dp else 0.75.dp
    val headerBg = if (tab.isIncognito) {
        Color(0xFF1D1533).copy(alpha = 0.92f)
    } else {
        ObsidianElevated.copy(alpha = 0.90f)
    }

    val dragProgress = (-swipeCloseOffsetY.value / closeThresholdPx).coerceIn(0f, 1.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = swipeCloseOffsetY.value
                val fade = (dragProgress * 0.55f).coerceIn(0f, 0.85f)
                alpha = if (isClosing) (1f - (-swipeCloseOffsetY.value / flyOutPx).coerceIn(0f, 1f)) else (1f - fade * 0.5f)
                val scale = 1f - (dragProgress * 0.05f).coerceAtMost(0.08f)
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(tab.id) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    var decided = false
                    var trackingUpSwipe = false
                    var crossedThresholdHaptic = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!decided) {
                            val absX = abs(totalDx)
                            val absY = abs(totalDy)
                            if (absX > touchSlop || absY > touchSlop) {
                                decided = true
                                // Only claim upward vertical drags; let horizontal go to HorizontalPager
                                // and downward go to LazyVerticalGrid / Window Swipe-Down-to-Dismiss
                                trackingUpSwipe = absY > absX && totalDy < -touchSlop
                            }
                        }

                        if (trackingUpSwipe && !isClosing) {
                            change.consume()
                            val nextY = (swipeCloseOffsetY.value + delta.y).coerceIn(-flyOutPx, 0f)
                            coroutineScope.launch { swipeCloseOffsetY.snapTo(nextY) }
                            if (!crossedThresholdHaptic && nextY <= -closeThresholdPx) {
                                crossedThresholdHaptic = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (crossedThresholdHaptic && nextY > -closeThresholdPx) {
                                crossedThresholdHaptic = false
                            }
                        } else if (decided) {
                            // Yield gesture cleanly to parent pager or vertical scroll
                            break
                        }
                    }

                    if (trackingUpSwipe && !isClosing) {
                        if (swipeCloseOffsetY.value <= -closeThresholdPx) {
                            isClosing = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                swipeCloseOffsetY.animateTo(
                                    targetValue = -flyOutPx,
                                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                                )
                                latestOnClose()
                            }
                        } else {
                            coroutineScope.launch {
                                swipeCloseOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.78f,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(ObsidianElevated.copy(alpha = 0.65f))
                .border(width = borderWidth, color = accentBorderColor, shape = cardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = IceCyan),
                    onClick = onSelect,
                )
                .testTag("grid_tab_card_${tab.id}"),
        ) {
            // Card header: site icon + single-line ellipsized title + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (groupColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(groupColor),
                    )
                } else {
                    Icon(
                        imageVector = when {
                            tab.isIncognito -> Icons.Rounded.PrivacyTip
                            tab.isDesktopMode -> Icons.Rounded.Computer
                            else -> Icons.Rounded.Public
                        },
                        contentDescription = null,
                        tint = when {
                            tab.isIncognito -> Color(0xFFC4B5FD)
                            isActive -> IceCyan
                            tab.securityState == SecurityState.SECURE -> EmeraldSecure
                            else -> FrostSlate
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = tab.title.ifBlank { displayHost.ifBlank { "새 탭" } },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isActive) PureFrost else PureFrost.copy(alpha = 0.90f),
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(innerRadius))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = IceCyan),
                            onClick = onClose,
                        )
                        .testTag("grid_tab_close_${tab.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "탭 닫기",
                        tint = FrostSlate,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Card body: scaled WebView Bitmap thumbnail preview or dark themed placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.76f)
                    .background(ObsidianVoid),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null && !bitmap.isRecycled) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "${tab.title.ifBlank { displayHost }} 미리보기",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (tab.isIncognito) {
                                        listOf(Color(0xFF151024), Color(0xFF0A0814))
                                    } else {
                                        listOf(Color(0xFF111827), Color(0xFF070B14))
                                    }
                                )
                            )
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tab.isIncognito) Color(0xFFA78BFA).copy(alpha = 0.14f)
                                    else IceCyan.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (tab.isIncognito) Icons.Rounded.PrivacyTip else Icons.Rounded.Public,
                                contentDescription = null,
                                tint = if (tab.isIncognito) Color(0xFFC4B5FD) else IceCyan.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = displayHost.ifBlank { "새 탭" },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = FrostSlate.copy(alpha = 0.85f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                // Upward swipe-to-close overlay hint when lifting the card
                if (dragProgress > 0.18f) {
                    val readyToClose = swipeCloseOffsetY.value <= -closeThresholdPx
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        (if (readyToClose) CrimsonStop else ObsidianVoid)
                                            .copy(alpha = (dragProgress * 0.42f).coerceAtMost(0.55f)),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (readyToClose) CrimsonStop.copy(alpha = 0.90f)
                                    else ObsidianElevated.copy(alpha = 0.86f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = PureFrost,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (readyToClose) "놓아서 닫기" else "위로 밀어 닫기",
                                style = MaterialTheme.typography.labelSmall,
                                color = PureFrost,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabGroupsPage(
    allTabs: List<BrowserTabState>,
    filteredTabs: List<BrowserTabState>,
    activeTabId: String,
    tabGroups: List<TabGroup>,
    thumbnails: Map<String, Bitmap>,
    listState: LazyListState,
    formatDisplayUrl: (String) -> String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTabGroup: (title: String, tabIds: List<String>, colorIndex: Int) -> Unit,
    onRemoveTabFromGroup: (groupId: String, tabId: String) -> Unit,
    onUngroupTabGroup: (groupId: String) -> Unit,
) {
    var showCreateGroupForm by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }
    var newGroupColorIdx by remember { mutableIntStateOf(0) }
    val selectedTabIds = remember { mutableStateListOf<String>() }

    val filteredTabById = remember(filteredTabs) { filteredTabs.associateBy { it.id } }

    // Custom groups matching current search filter
    val visibleCustomGroups = remember(tabGroups, filteredTabById) {
        tabGroups.mapNotNull { group ->
            val members = group.tabIds.mapNotNull { filteredTabById[it] }
            if (members.isNotEmpty()) group to members else null
        }
    }
    val customAssignedIds = remember(tabGroups) {
        tabGroups.flatMap { it.tabIds }.toSet()
    }

    // Automatic domain-based groups for ungrouped tabs
    val domainGroups = remember(filteredTabs, customAssignedIds) {
        filteredTabs
            .filterNot { it.id in customAssignedIds }
            .groupBy { tab ->
                if (tab.isIncognito) {
                    "시크릿 탭"
                } else {
                    formatDisplayUrl(tab.url).substringBefore('/').ifBlank { "일반 탭" }
                }
            }
            .entries
            .toList()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_group_overview_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        // Header row with "+ 새 그룹" button
        item(key = "groups_header_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "탭 그룹 · ${tabGroups.size}개 지정 / ${domainGroups.size}개 도메인",
                    style = MaterialTheme.typography.labelMedium,
                    color = FrostSlate,
                )
                OutlinedButton(
                    onClick = {
                        showCreateGroupForm = !showCreateGroupForm
                        if (showCreateGroupForm) {
                            selectedTabIds.clear()
                            val defaultId = allTabs.firstOrNull { it.id == activeTabId }?.id
                                ?: allTabs.firstOrNull()?.id
                            if (defaultId != null) selectedTabIds.add(defaultId)
                        }
                    },
                    shape = RoundedCornerShape(innerRadius),
                    modifier = Modifier.testTag("new_tab_group_button"),
                ) {
                    Icon(
                        imageVector = if (showCreateGroupForm) Icons.Rounded.Close else Icons.Rounded.CreateNewFolder,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (showCreateGroupForm) "닫기" else "새 그룹",
                        style = MaterialTheme.typography.labelMedium,
                        color = IceCyan,
                    )
                }
            }
        }

        // Inline New Tab Group Creator Card
        item(key = "create_group_form") {
            AnimatedVisibility(
                visible = showCreateGroupForm,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
            ) {
                val formShape = RoundedCornerShape(unifiedRadius)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(formShape)
                        .background(IceCyan.copy(alpha = 0.08f))
                        .border(0.75.dp, IceCyan.copy(alpha = 0.48f), formShape)
                        .padding(14.dp)
                        .testTag("create_tab_group_form"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "새 탭 그룹 만들기",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureFrost,
                    )

                    OutlinedTextField(
                        value = newGroupTitle,
                        onValueChange = { newGroupTitle = it },
                        singleLine = true,
                        shape = RoundedCornerShape(innerRadius),
                        label = {
                            Text("그룹 이름 (예: 리서치, 쇼핑)", style = MaterialTheme.typography.labelSmall)
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
                            .fillMaxWidth()
                            .testTag("new_group_title_input"),
                    )

                    // Group accent color picker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "색상",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostSlate,
                        )
                        TabGroupAccentColors.forEachIndexed { idx, color ->
                            val selected = idx == newGroupColorIdx
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) PureFrost else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable { newGroupColorIdx = idx },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = ObsidianVoid,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Select tabs to include in the new group
                    Text(
                        text = "포함할 탭 선택 (${selectedTabIds.size}개)",
                        style = MaterialTheme.typography.labelSmall,
                        color = FrostSlate,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        allTabs.forEach { tab ->
                            val picked = tab.id in selectedTabIds
                            val chipShape = RoundedCornerShape(innerRadius)
                            val label = tab.title.ifBlank { formatDisplayUrl(tab.url).ifBlank { "새 탭" } }
                            Row(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(
                                        if (picked) IceCyan.copy(alpha = 0.22f)
                                        else ObsidianVoid.copy(alpha = 0.65f)
                                    )
                                    .border(
                                        width = 0.75.dp,
                                        color = if (picked) IceCyan else Color.White.copy(alpha = 0.12f),
                                        shape = chipShape,
                                    )
                                    .clickable {
                                        if (picked) {
                                            selectedTabIds.remove(tab.id)
                                        } else {
                                            selectedTabIds.add(tab.id)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (picked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = IceCyan,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                                Text(
                                    text = label.take(18),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (picked) PureFrost else FrostSlate,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showCreateGroupForm = false },
                            shape = RoundedCornerShape(innerRadius),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("취소", style = MaterialTheme.typography.labelMedium, color = FrostSlate)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(innerRadius))
                                .background(IceCyan)
                                .clickable {
                                    val idsToGroup = selectedTabIds.toList()
                                    onCreateTabGroup(newGroupTitle, idsToGroup, newGroupColorIdx)
                                    newGroupTitle = ""
                                    showCreateGroupForm = false
                                }
                                .testTag("confirm_create_tab_group_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "그룹 만들기",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ObsidianVoid,
                            )
                        }
                    }
                }
            }
        }

        // Custom Tab Groups Section
        items(
            items = visibleCustomGroups,
            key = { (group, _) -> "custom_${group.id}" },
        ) { (group, groupTabs) ->
            val accent = TabGroupAccentColors[group.colorIndex.coerceIn(0, TabGroupAccentColors.lastIndex)]
            TabGroupCard(
                groupTitle = group.title,
                isCustomGroup = true,
                isIncognitoGroup = false,
                accentColor = accent,
                groupTabs = groupTabs,
                activeTabId = activeTabId,
                thumbnails = thumbnails,
                formatDisplayUrl = formatDisplayUrl,
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
                onHeaderAction = { onUngroupTabGroup(group.id) },
                onRemoveTabFromGroup = { tabId -> onRemoveTabFromGroup(group.id, tabId) },
                cardTestTag = "custom_tab_group_${group.id}",
            )
        }

        // Automatic Domain-Based Groups Section
        items(
            items = domainGroups,
            key = { (hostKey, _) -> "domain_$hostKey" },
        ) { (groupTitle, groupTabs) ->
            val isIncognitoGroup = groupTitle == "시크릿 탭"
            TabGroupCard(
                groupTitle = groupTitle,
                isCustomGroup = false,
                isIncognitoGroup = isIncognitoGroup,
                accentColor = if (isIncognitoGroup) Color(0xFFA78BFA) else IceCyan,
                groupTabs = groupTabs,
                activeTabId = activeTabId,
                thumbnails = thumbnails,
                formatDisplayUrl = formatDisplayUrl,
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
                onHeaderAction = if (!isIncognitoGroup) {
                    {
                        onCreateTabGroup(
                            groupTitle,
                            groupTabs.map { it.id },
                            abs(groupTitle.hashCode()) % TabGroupAccentColors.size,
                        )
                    }
                } else {
                    null
                },
                onRemoveTabFromGroup = null,
                cardTestTag = "domain_tab_group_$groupTitle",
            )
        }
    }
}

@Composable
private fun TabGroupCard(
    groupTitle: String,
    isCustomGroup: Boolean,
    isIncognitoGroup: Boolean,
    accentColor: Color,
    groupTabs: List<BrowserTabState>,
    activeTabId: String,
    thumbnails: Map<String, Bitmap>,
    formatDisplayUrl: (String) -> String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onHeaderAction: (() -> Unit)?,
    onRemoveTabFromGroup: ((String) -> Unit)?,
    cardTestTag: String,
) {
    val groupShape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(groupShape)
            .background(ObsidianElevated.copy(alpha = 0.64f))
            .border(
                width = if (isCustomGroup) 0.9.dp else 0.5.dp,
                color = if (isCustomGroup) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                shape = groupShape,
            )
            .padding(12.dp)
            .testTag(cardTestTag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
                Icon(
                    imageVector = if (isIncognitoGroup) Icons.Rounded.PrivacyTip else Icons.Rounded.FolderCopy,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = groupTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureFrost,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${groupTabs.size}개",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                    )
                }
            }

            if (onHeaderAction != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(innerRadius))
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable(onClick = onHeaderAction)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag(if (isCustomGroup) "ungroup_button_$groupTitle" else "save_domain_group_$groupTitle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (isCustomGroup) Icons.Rounded.FolderOff else Icons.Rounded.BookmarkAdd,
                        contentDescription = null,
                        tint = if (isCustomGroup) FrostSlate else IceCyan,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (isCustomGroup) "그룹 해제" else "그룹 저장",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomGroup) FrostSlate else IceCyan,
                    )
                }
            }
        }

        groupTabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            val itemShape = RoundedCornerShape(innerRadius)
            val bmp = thumbnails[tab.id]?.takeIf { !it.isRecycled }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(itemShape)
                    .background(
                        if (isActive) accentColor.copy(alpha = 0.16f)
                        else ObsidianVoid.copy(alpha = 0.58f)
                    )
                    .border(
                        width = if (isActive) 1.5.dp else 0.5.dp,
                        color = if (isActive) accentColor else Color.White.copy(alpha = 0.08f),
                        shape = itemShape,
                    )
                    .clickable { onSelectTab(tab.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape((innerRadius - 2.dp).coerceAtLeast(4.dp)))
                        .background(ObsidianElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = if (tab.isIncognito) Icons.Rounded.PrivacyTip else Icons.Rounded.Public,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = tab.title.ifBlank { formatDisplayUrl(tab.url).ifBlank { "새 탭" } },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureFrost,
                    )
                    Text(
                        text = formatDisplayUrl(tab.url),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = FrostSlate,
                    )
                }

                if (onRemoveTabFromGroup != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(innerRadius))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable { onRemoveTabFromGroup(tab.id) }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "제외",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostSlate,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onCloseTab(tab.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "탭 닫기",
                        tint = FrostSlate,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTabSearchState(
    hasSearchQuery: Boolean,
    unifiedRadius: Dp,
    onCreateTab: () -> Unit,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.55f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = FrostSlate,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = if (hasSearchQuery) "검색어와 일치하는 탭이 없습니다." else "열린 탭이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = FrostSlate,
        )
        if (!hasSearchQuery) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(unifiedInnerRadius(unifiedRadius)))
                    .background(IceCyan.copy(alpha = 0.20f))
                    .border(
                        0.75.dp,
                        IceCyan.copy(alpha = 0.65f),
                        RoundedCornerShape(unifiedInnerRadius(unifiedRadius)),
                    )
                    .clickable(onClick = onCreateTab)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "새 탭 열기",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureFrost,
                )
            }
        }
    }
}

@Composable
private fun TabSwitcherBottomControlBar(
    tabCount: Int,
    unifiedRadius: Dp,
    innerRadius: Dp,
    glassOpacity: Float,
    onCreateTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grid_tab_switcher_bottom_bar"),
        shape = RoundedCornerShape(unifiedRadius),
        shadowElevation = 18.dp,
        glassOpacity = glassOpacity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Tab count badge
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(38.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.25.dp, IceCyan.copy(alpha = 0.72f), RoundedCornerShape(innerRadius))
                    .testTag("grid_tab_count_badge"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tabCount.toString(),
                    style = TextStyle(
                        color = PureFrost,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                )
            }

            // Center: '+' New Tab button
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .defaultMinSize(minWidth = 64.dp, minHeight = 44.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(IceCyan.copy(alpha = 0.22f))
                    .border(0.75.dp, IceCyan.copy(alpha = 0.65f), RoundedCornerShape(innerRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = IceCyan),
                        onClick = onCreateTab,
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("grid_new_tab_button"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "새 탭",
                        tint = IceCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "새 탭",
                        style = MaterialTheme.typography.labelMedium,
                        color = PureFrost,
                    )
                }
            }

            // Right: Back / Close control
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(44.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = IceCyan),
                        onClick = onDismiss,
                    )
                    .testTag("grid_tab_switcher_close_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "닫기",
                    tint = PureFrost,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
