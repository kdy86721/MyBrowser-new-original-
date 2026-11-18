package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserTabState
import com.example.core.SecurityState
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

/**
 * Full-screen 2-column grid preview tab switcher styled with Liquid Glass and [unifiedRadius]:
 * - Top segmented switcher (Open tabs with numeric badge vs. Grid/Group overview)
 * - Rounded pill search bar filtering tabs by title or URL ("탭 검색")
 * - 2-column [LazyVerticalGrid] of scaled WebView Bitmap previews + active tab 2.5.dp border
 * - Liquid Glass bottom control bar with tab count badge, centered '+' new tab, and close/back action
 */
@Composable
fun GridTabSwitcherScreen(
    snapshot: BrowserCoreSnapshot,
    thumbnails: Map<String, Bitmap>,
    formatDisplayUrl: (String) -> String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    val prefs = snapshot.preferences
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val glassOpacity = prefs.glassOpacity
    val tabs = snapshot.tabs
    val activeTabId = snapshot.activeTabId

    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = 열린 탭 그리드, 1 = 그룹 요약
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

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
            .background(backdropBrush)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Top Segmented Switcher (centered rounded pill)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TopTabSegmentedPill(
                    selectedSegment = selectedSegment,
                    totalTabCount = tabs.size,
                    unifiedRadius = unifiedRadius,
                    innerRadius = innerRadius,
                    glassOpacity = glassOpacity,
                    onSelectSegment = { selectedSegment = it },
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

            // 3. Tab Grid OR Domain/Mode Group Overview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (filteredTabs.isEmpty()) {
                    EmptyTabSearchState(
                        hasSearchQuery = searchQuery.isNotBlank(),
                        unifiedRadius = unifiedRadius,
                        onCreateTab = onCreateTab,
                    )
                } else if (selectedSegment == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tab_preview_grid"),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(
                            items = filteredTabs,
                            key = { it.id },
                        ) { tab ->
                            val isActive = tab.id == activeTabId
                            val bitmap = thumbnails[tab.id]?.takeIf { !it.isRecycled }
                            TabPreviewGridCard(
                                tab = tab,
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
                } else {
                    TabGroupOverviewList(
                        tabs = filteredTabs,
                        activeTabId = activeTabId,
                        thumbnails = thumbnails,
                        formatDisplayUrl = formatDisplayUrl,
                        unifiedRadius = unifiedRadius,
                        innerRadius = innerRadius,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
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
private fun TopTabSegmentedPill(
    selectedSegment: Int,
    totalTabCount: Int,
    unifiedRadius: Dp,
    innerRadius: Dp,
    glassOpacity: Float,
    onSelectSegment: (Int) -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 320.dp)
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
    bitmap: Bitmap?,
    isActive: Boolean,
    displayHost: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    val accentBorderColor = when {
        isActive -> IceCyan
        tab.isIncognito -> Color(0xFFA78BFA).copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val borderWidth = if (isActive) 2.5.dp else 0.75.dp
    val headerBg = if (tab.isIncognito) {
        Color(0xFF1D1533).copy(alpha = 0.92f)
    } else {
        ObsidianElevated.copy(alpha = 0.90f)
    }

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
        }
    }
}

@Composable
private fun TabGroupOverviewList(
    tabs: List<BrowserTabState>,
    activeTabId: String,
    thumbnails: Map<String, Bitmap>,
    formatDisplayUrl: (String) -> String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    val grouped = remember(tabs) {
        tabs.groupBy { tab ->
            if (tab.isIncognito) {
                "시크릿 탭"
            } else {
                val host = formatDisplayUrl(tab.url).substringBefore('/').ifBlank { "일반 탭" }
                host
            }
        }.entries.toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tab_group_overview_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        items(
            items = grouped,
            key = { it.key },
        ) { (groupTitle, groupTabs) ->
            val groupShape = RoundedCornerShape(unifiedRadius)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(groupShape)
                    .background(ObsidianElevated.copy(alpha = 0.60f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), groupShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (groupTitle == "시크릿 탭") Icons.Rounded.PrivacyTip else Icons.Rounded.FolderCopy,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = groupTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureFrost,
                        )
                    }
                    Text(
                        text = "${groupTabs.size}개",
                        style = MaterialTheme.typography.labelMedium,
                        color = IceCyan,
                    )
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
                                if (isActive) IceCyan.copy(alpha = 0.16f)
                                else ObsidianVoid.copy(alpha = 0.58f)
                            )
                            .border(
                                width = if (isActive) 1.5.dp else 0.5.dp,
                                color = if (isActive) IceCyan else Color.White.copy(alpha = 0.08f),
                                shape = itemShape,
                            )
                            .clickable { onSelectTab(tab.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                    tint = IceCyan,
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
                    .border(0.75.dp, IceCyan.copy(alpha = 0.65f), RoundedCornerShape(unifiedInnerRadius(unifiedRadius)))
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
