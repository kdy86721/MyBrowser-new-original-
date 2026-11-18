package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.core.BrowserTabState
import com.example.core.QuickToggleAction
import com.example.core.QuickToggleLayoutStyle
import com.example.core.SecurityState
import com.example.core.ToggleIndicatorStyle
import com.example.core.ToolbarAction
import com.example.core.ToolbarConfig
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PerimeterLoadingRing
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
data class FoldablePostureUiState(
    val isUnfolded: Boolean = false,
    val hasHardwareHinge: Boolean = false,
    val isSeparatingHinge: Boolean = false,
    val isVerticalHinge: Boolean = false,
    val isTabletopPosture: Boolean = false,
    val hingeWidthDp: Dp = 0.dp,
)

/**
 * Resolves active bar width fraction and horizontal alignment bias accounting for folded vs.
 * unfolded device posture and [BrowserPreferences.foldableSeparateBarProfiles].
 */
fun resolveEffectiveBarGeometry(
    prefs: BrowserPreferences,
    foldablePosture: FoldablePostureUiState,
): Pair<Float, Float> {
    return if (foldablePosture.isUnfolded && prefs.foldableSeparateBarProfiles) {
        prefs.unfoldedBarWidthFraction to prefs.unfoldedBarHorizontalBias
    } else {
        prefs.barWidthFraction to prefs.barHorizontalBias
    }
}

/**
 * Floating bottom control bar driven by [BrowserCoreSnapshot.preferences] geometry & foldable posture:
 * - Synchronized `unifiedRadius = barCornerRadiusDp.dp` across outer shell, capsule, buttons, and PathMeasure ring
 * - Configurable height, width fraction, bottom offset, horizontal bias, and glass opacity
 * - Foldable posture adaptation (unfolded bar profile + optional auto-expanded toolbar icons)
 * - Icon-only toolbar & action buttons + frosted address capsule (Lock + URL + Reload/Stop)
 * - Scroll-to-pill morphing + 360° perimeter loading ring + gear button triggering anchored Quick-Toggle menu
 */
@Composable
fun FloatingControlBar(
    snapshot: BrowserCoreSnapshot,
    displayUrl: String,
    foldablePosture: FoldablePostureUiState,
    isEditingAddress: Boolean,
    isTabDrawerOpen: Boolean,
    isQuickMenuOpen: Boolean,
    isFindInPageOpen: Boolean,
    findQuery: String,
    isCollapsedToPill: Boolean,
    onEnterAddressEdit: () -> Unit,
    onExitAddressEdit: () -> Unit,
    onSubmitAddress: (String) -> Unit,
    onReloadOrStop: () -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
    onSwitchTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: () -> Unit,
    onToggleQuickMenu: () -> Unit,
    onFindQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFindInPage: () -> Unit,
    onAddressSwipeUp: () -> Unit,
    onAddressSwipeDown: () -> Unit,
    onAddressSwipeTab: (delta: Int) -> Unit,
    onAddressDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTab = snapshot.activeTab
    val prefs = snapshot.preferences
    val toolbarConfig = prefs.toolbarConfig
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)

    val (effectiveWidthFraction, effectiveHorizontalBias) = resolveEffectiveBarGeometry(
        prefs = prefs,
        foldablePosture = foldablePosture,
    )

    val effectiveLeftActions = remember(toolbarConfig.leftActions, foldablePosture.isUnfolded, prefs.autoExpandToolbarWhenUnfolded) {
        if (foldablePosture.isUnfolded && prefs.autoExpandToolbarWhenUnfolded && toolbarConfig.leftActions.size < ToolbarConfig.MAX_ACTIONS_PER_SIDE) {
            val extras = listOf(ToolbarAction.HOME, ToolbarAction.BACK, ToolbarAction.FORWARD)
                .filterNot { it in toolbarConfig.leftActions || it in toolbarConfig.rightActions }
            (toolbarConfig.leftActions + extras).take(ToolbarConfig.MAX_ACTIONS_PER_SIDE)
        } else {
            toolbarConfig.leftActions
        }
    }

    val effectiveRightActions = remember(toolbarConfig.rightActions, effectiveLeftActions, foldablePosture.isUnfolded, prefs.autoExpandToolbarWhenUnfolded) {
        if (foldablePosture.isUnfolded && prefs.autoExpandToolbarWhenUnfolded && toolbarConfig.rightActions.size < ToolbarConfig.MAX_ACTIONS_PER_SIDE) {
            val extras = listOf(ToolbarAction.NEW_TAB, ToolbarAction.DESKTOP_MODE, ToolbarAction.SHARE)
                .filterNot { it in toolbarConfig.rightActions || it in effectiveLeftActions }
            (toolbarConfig.rightActions + extras).take(ToolbarConfig.MAX_ACTIONS_PER_SIDE)
        } else {
            toolbarConfig.rightActions
        }
    }

    val effectiveCollapsed = prefs.scrollToPillEnabled &&
        isCollapsedToPill &&
        !isEditingAddress &&
        !isTabDrawerOpen &&
        !isQuickMenuOpen &&
        !isFindInPageOpen

    val hingeSideMargin = if (foldablePosture.isUnfolded && foldablePosture.isVerticalHinge) {
        maxOf(12.dp, prefs.foldableHingeAvoidanceDp.dp)
    } else {
        12.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = hingeSideMargin,
                end = hingeSideMargin,
                bottom = prefs.barBottomOffsetDp.dp,
                top = 6.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Optional Find-in-Page frosted capsule above the floating bar
        AnimatedVisibility(
            visible = isFindInPageOpen && !isEditingAddress,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.94f),
        ) {
            FindInPageBar(
                query = findQuery,
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                glassOpacity = prefs.glassOpacity,
                onQueryChange = onFindQueryChange,
                onFindNext = onFindNext,
                onClose = onCloseFindInPage,
            )
        }

        // Icon-only floating tab dock revealed with clean vertical fade/scale & synchronized unifiedRadius
        AnimatedVisibility(
            visible = isTabDrawerOpen && !isEditingAddress,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f) + slideInVertically(tween(220)) { it / 4 },
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.94f) + slideOutVertically(tween(160)) { it / 4 },
        ) {
            IconOnlyTabDock(
                tabs = snapshot.tabs,
                activeTabId = snapshot.activeTabId,
                unifiedRadius = unifiedRadius,
                innerRadius = innerRadius,
                glassOpacity = prefs.glassOpacity,
                onSwitchTab = onSwitchTab,
                onCloseTab = onCloseTab,
                onCreateTab = onCreateTab,
            )
        }

        // Main floating liquid-glass bar respecting user geometry & foldable posture bias
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val maxCap = if (foldablePosture.isUnfolded) 740.dp else 640.dp
            val expandedWidth = minOf(maxWidth * effectiveWidthFraction, maxCap)
            val collapsedPillWidth = minOf(maxWidth, 212.dp)
            val isExpandedAddressEdit = isEditingAddress && prefs.expandBarOnAddressEdit
            val targetBarWidth = when {
                effectiveCollapsed -> collapsedPillWidth
                isExpandedAddressEdit -> minOf(maxWidth, maxCap)
                else -> expandedWidth
            }

            val animatedBarWidth by animateDpAsState(
                targetValue = targetBarWidth,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 360f),
                label = "floating_bar_width_morph",
            )

            val targetBarHeight = if (isExpandedAddressEdit) 96.dp else prefs.barHeightDp.dp
            val barHeight by animateDpAsState(
                targetValue = targetBarHeight,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
                label = "floating_bar_height",
            )

            val targetBias = when {
                effectiveCollapsed || isExpandedAddressEdit -> 0f
                else -> effectiveHorizontalBias
            }
            val animatedBias by animateFloatAsState(
                targetValue = targetBias,
                animationSpec = spring(stiffness = 380f),
                label = "floating_bar_horizontal_bias",
            )

            val isLoading = activeTab?.isLoading == true
            val progress = (activeTab?.progress ?: 0).coerceIn(0, 100)

            Box(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = animatedBias, verticalBias = 0f))
                    .width(animatedBarWidth)
                    .testTag("floating_control_bar"),
                contentAlignment = Alignment.Center,
            ) {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(unifiedRadius),
                    shadowElevation = 20.dp,
                    glassOpacity = prefs.glassOpacity,
                    isHighlighted = isEditingAddress || isQuickMenuOpen,
                    accentColor = IceCyan,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // 1. Left Customizable Toolbar (icon-only)
                        AnimatedVisibility(
                            visible = !isEditingAddress && !effectiveCollapsed && effectiveLeftActions.isNotEmpty(),
                            enter = fadeIn(tween(180)) + expandHorizontally(spring(stiffness = 380f)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(spring(stiffness = 380f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                effectiveLeftActions.forEach { action ->
                                    ToolbarActionIconButton(
                                        action = action,
                                        activeTab = activeTab,
                                        tabCount = snapshot.tabs.size,
                                        isTabDrawerOpen = isTabDrawerOpen,
                                        buttonCornerRadius = innerRadius,
                                        onClick = { onToolbarAction(action) },
                                    )
                                }
                            }
                        }

                        // 2. Center Frosted Address Capsule (Lock icon + URL text + inner Reload/Stop)
                        val targetCapsuleHeight = if (isExpandedAddressEdit) {
                            82.dp
                        } else {
                            (prefs.barHeightDp - 10f).coerceIn(34f, 60f).dp
                        }
                        val capsuleHeight by animateDpAsState(
                            targetValue = targetCapsuleHeight,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
                            label = "address_capsule_height",
                        )
                        AddressCapsule(
                            activeTab = activeTab,
                            displayUrl = displayUrl,
                            capsuleHeight = capsuleHeight,
                            capsuleCornerRadius = innerRadius,
                            buttonCornerRadius = (innerRadius - 2.dp).coerceAtLeast(4.dp),
                            glassOpacity = prefs.glassOpacity,
                            isEditing = isEditingAddress,
                            expandOnEdit = prefs.expandBarOnAddressEdit,
                            gesturesEnabled = prefs.gesturesEnabled,
                            swipeUpAction = prefs.swipeUpAction,
                            swipeDownAction = prefs.swipeDownAction,
                            swipeLeftRightTabSwitch = prefs.swipeLeftRightTabSwitch,
                            doubleTapAction = prefs.doubleTapAction,
                            gestureSensitivityDp = prefs.gestureSensitivityDp,
                            onTapToEdit = onEnterAddressEdit,
                            onExitEdit = onExitAddressEdit,
                            onSubmit = onSubmitAddress,
                            onReloadOrStop = onReloadOrStop,
                            onSwipeUp = onAddressSwipeUp,
                            onSwipeDown = onAddressSwipeDown,
                            onSwipeTab = onAddressSwipeTab,
                            onDoubleTap = onAddressDoubleTap,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = if (effectiveCollapsed) 2.dp else 4.dp),
                        )

                        // 3. Right Customizable Toolbar (icon-only)
                        AnimatedVisibility(
                            visible = !isEditingAddress && !effectiveCollapsed && effectiveRightActions.isNotEmpty(),
                            enter = fadeIn(tween(180)) + expandHorizontally(spring(stiffness = 380f)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(spring(stiffness = 380f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                effectiveRightActions.forEach { action ->
                                    ToolbarActionIconButton(
                                        action = action,
                                        activeTab = activeTab,
                                        tabCount = snapshot.tabs.size,
                                        isTabDrawerOpen = isTabDrawerOpen,
                                        buttonCornerRadius = innerRadius,
                                        onClick = { onToolbarAction(action) },
                                    )
                                }
                            }
                        }

                        // 4. Far-Right Fixed Gear Button (toggles Quick-Toggle dropdown menu)
                        AnimatedVisibility(
                            visible = !isEditingAddress && !effectiveCollapsed,
                            enter = fadeIn(tween(180)) + expandHorizontally(spring(stiffness = 380f)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(spring(stiffness = 380f)),
                        ) {
                            GlassIconButton(
                                icon = Icons.Rounded.Settings,
                                contentDescription = "빠른 메뉴 및 설정",
                                tint = if (isQuickMenuOpen) IceCyan else PureFrost,
                                isActive = isQuickMenuOpen,
                                cornerRadius = innerRadius,
                                onClick = onToggleQuickMenu,
                                modifier = Modifier.testTag("settings_button"),
                            )
                        }
                    }

                    // 5. Continuous 360° Perimeter Loading Progress Indicator via drawWithCache & lambda reads
                    val targetFraction = if (isLoading) (progress / 100f).coerceIn(0.06f, 1f) else 0f
                    val animatedFractionState = animateFloatAsState(
                        targetValue = if (isLoading) targetFraction else 1f,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 320f),
                        label = "perimeter_progress_fraction",
                    )
                    val ringAlphaState = animateFloatAsState(
                        targetValue = if (isLoading) 1f else 0f,
                        animationSpec = tween(durationMillis = if (isLoading) 120 else 340),
                        label = "perimeter_ring_alpha",
                    )

                    PerimeterLoadingRing(
                        cornerRadius = unifiedRadius,
                        progressProvider = { animatedFractionState.value },
                        alphaProvider = { ringAlphaState.value },
                        modifier = Modifier
                            .matchParentSize()
                            .testTag("perimeter_loading_ring"),
                    )
                }
            }
        }
    }
}

/**
 * Anchored Liquid Glass dropdown menu with:
 * - Animated container entrance (`fadeIn` + `scaleIn` + `slideInVertically`)
 * - Staggered sequential entrance animation per item (`index * 28L` ms delay, `alpha: 0f -> 1f`, `translationY: 14.dp -> 0.dp`)
 * - Single-column list OR 2-column compact tile grid (`prefs.dropdownLayoutStyle`)
 * - Interactive Switch / ON-OFF Pill / Dot indicators (`prefs.toggleIndicatorStyle`) for all 8 stateful toggles + 6 instant actions
 * - Separated bottom "설정" row that launches full Settings via Push transition
 */
@Composable
fun QuickToggleDropdownMenu(
    visible: Boolean,
    snapshot: BrowserCoreSnapshot,
    foldablePosture: FoldablePostureUiState,
    isCurrentUrlBookmarked: Boolean,
    onDismiss: () -> Unit,
    onQuickToggle: (QuickToggleAction) -> Unit,
    onOpenFullSettings: () -> Unit,
) {
    val prefs = snapshot.preferences
    val activeTab = snapshot.activeTab
    val unifiedRadius = prefs.barCornerRadiusDp.dp
    val innerRadius = unifiedInnerRadius(unifiedRadius)

    val (effectiveWidthFraction, effectiveHorizontalBias) = resolveEffectiveBarGeometry(
        prefs = prefs,
        foldablePosture = foldablePosture,
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(230)),
    ) {
        BackHandler(enabled = visible) {
            onDismiss()
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(visible) {
                    detectTapGestures(onTap = { onDismiss() })
                }
                .navigationBarsPadding()
                .imePadding()
                .testTag("quick_toggle_backdrop"),
        ) {
            val isTwoColumn = prefs.dropdownLayoutStyle == QuickToggleLayoutStyle.TWO_COLUMN_TILES
            val desiredMenuWidth = if (isTwoColumn) 320.dp else 274.dp
            val hingePad = if (foldablePosture.isUnfolded && foldablePosture.isVerticalHinge) {
                maxOf(12.dp, prefs.foldableHingeAvoidanceDp.dp)
            } else {
                12.dp
            }
            val menuWidth = minOf(desiredMenuWidth, (maxWidth - hingePad * 2).coerceAtLeast(220.dp))
            val bottomClearance = (prefs.barBottomOffsetDp + prefs.barHeightDp + 10f).dp

            // Anchor horizontally to the right edge of the active floating bar (where the gear sits)
            val maxCap = if (foldablePosture.isUnfolded) 740.dp else 640.dp
            val usableRowWidth = (maxWidth - hingePad * 2).coerceAtLeast(220.dp)
            val barWidth = minOf(usableRowWidth * effectiveWidthFraction, maxCap)
            val barLeftOffset = (usableRowWidth - barWidth) * ((effectiveHorizontalBias + 1f) / 2f)
            val barRightEdge = hingePad + barLeftOffset + barWidth
            val targetMenuLeft = (barRightEdge - menuWidth).coerceIn(
                minimumValue = hingePad,
                maximumValue = (maxWidth - menuWidth - hingePad).coerceAtLeast(hingePad),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = targetMenuLeft, bottom = bottomClearance)
                    .width(menuWidth)
                    .pointerInput(Unit) {
                        detectTapGestures { /* consume taps inside menu */ }
                    }
                    .testTag("quick_toggle_dropdown_menu"),
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    ) + scaleIn(
                        initialScale = 0.85f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    ) + scaleOut(
                        targetScale = 0.85f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(durationMillis = 160)),
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(unifiedRadius),
                        shadowElevation = 24.dp,
                        glassOpacity = (prefs.glassOpacity + 0.07f).coerceAtMost(0.45f),
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val toggles = prefs.customQuickToggles
                        val listModifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = prefs.dropdownMaxHeightDp.dp)
                            .verticalScroll(rememberScrollState())

                        if (isTwoColumn) {
                            Column(
                                modifier = listModifier,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val rows = toggles.chunked(2)
                                rows.forEachIndexed { rowIdx, pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        pair.forEachIndexed { colIdx, toggle ->
                                            val itemIndex = rowIdx * 2 + colIdx
                                            val isActive = isQuickToggleActive(
                                                toggle = toggle,
                                                snapshot = snapshot,
                                                isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                                            )
                                            StaggeredEntranceItem(
                                                visible = visible,
                                                index = itemIndex,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                QuickToggleTileItem(
                                                    toggle = toggle,
                                                    isActive = isActive,
                                                    indicatorStyle = prefs.toggleIndicatorStyle,
                                                    innerRadius = innerRadius,
                                                    onClick = { onQuickToggle(toggle) },
                                                )
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = listModifier,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                toggles.forEachIndexed { index, toggle ->
                                    val isActive = isQuickToggleActive(
                                        toggle = toggle,
                                        snapshot = snapshot,
                                        isCurrentUrlBookmarked = isCurrentUrlBookmarked,
                                    )
                                    StaggeredEntranceItem(
                                        visible = visible,
                                        index = index,
                                    ) {
                                        QuickToggleRowItem(
                                            toggle = toggle,
                                            isActive = isActive,
                                            indicatorStyle = prefs.toggleIndicatorStyle,
                                            innerRadius = innerRadius,
                                            onClick = { onQuickToggle(toggle) },
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.padding(vertical = 1.dp),
                        )

                        // Bottom "설정" item participating in the staggered sequential entrance animation
                        StaggeredEntranceItem(
                            visible = visible,
                            index = toggles.size.coerceAtMost(12),
                        ) {
                            val settingsShape = RoundedCornerShape(innerRadius)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(settingsShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), settingsShape)
                                    .clickable(onClick = onOpenFullSettings)
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                                    .testTag("quick_menu_settings_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = IceCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "설정",
                                    style = TextStyle(
                                        color = PureFrost,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = FrostSlate,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun StaggeredEntranceItem(
    visible: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alphaAnim = remember { Animatable(0f) }
    val translationYDp = remember { Animatable(14f) }

    LaunchedEffect(visible, index) {
        if (visible) {
            alphaAnim.snapTo(0f)
            translationYDp.snapTo(14f)
            delay(index * 28L)
            coroutineScope {
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    )
                }
                launch {
                    translationYDp.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 440f),
                    )
                }
            }
        }
    }

    val density = LocalDensity.current
    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = with(density) { translationYDp.value.dp.toPx() }
        },
    ) {
        content()
    }
}

@Composable
private fun QuickToggleRowItem(
    toggle: QuickToggleAction,
    isActive: Boolean,
    indicatorStyle: ToggleIndicatorStyle,
    innerRadius: Dp,
    onClick: () -> Unit,
) {
    val itemShape = RoundedCornerShape(innerRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(
                if (isActive) IceCyan.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.04f)
            )
            .border(
                width = 0.5.dp,
                color = if (isActive) IceCyan.copy(alpha = 0.52f)
                else Color.White.copy(alpha = 0.06f),
                shape = itemShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("quick_toggle_${toggle.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = quickToggleIcon(toggle, isActive),
            contentDescription = null,
            tint = if (isActive) IceCyan else PureFrost.copy(alpha = 0.90f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = quickToggleLabel(toggle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = if (isActive) PureFrost else PureFrost.copy(alpha = 0.92f),
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
            ),
            modifier = Modifier.weight(1f),
        )

        if (toggle.isStatefulToggle) {
            QuickToggleStateIndicator(
                isActive = isActive,
                style = indicatorStyle,
                compact = false,
            )
        } else if (isActive) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(IceCyan)
            )
        }
    }
}

@Composable
private fun QuickToggleTileItem(
    toggle: QuickToggleAction,
    isActive: Boolean,
    indicatorStyle: ToggleIndicatorStyle,
    innerRadius: Dp,
    onClick: () -> Unit,
) {
    val tileShape = RoundedCornerShape(innerRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .background(
                if (isActive) IceCyan.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 0.5.dp,
                color = if (isActive) IceCyan.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.08f),
                shape = tileShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("quick_toggle_${toggle.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = quickToggleIcon(toggle, isActive),
                contentDescription = null,
                tint = if (isActive) IceCyan else PureFrost.copy(alpha = 0.90f),
                modifier = Modifier.size(18.dp),
            )
            if (toggle.isStatefulToggle) {
                QuickToggleStateIndicator(
                    isActive = isActive,
                    style = indicatorStyle,
                    compact = true,
                )
            } else if (isActive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(IceCyan)
                )
            }
        }
        Text(
            text = quickToggleCompactLabel(toggle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = if (isActive) PureFrost else PureFrost.copy(alpha = 0.90f),
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun QuickToggleStateIndicator(
    isActive: Boolean,
    style: ToggleIndicatorStyle,
    compact: Boolean,
) {
    when (style) {
        ToggleIndicatorStyle.SWITCH -> {
            val trackWidth = if (compact) 30.dp else 36.dp
            val trackHeight = if (compact) 17.dp else 20.dp
            val thumbSize = if (compact) 13.dp else 15.dp
            val trackColor by animateColorAsState(
                targetValue = if (isActive) IceCyan else Color.White.copy(alpha = 0.16f),
                animationSpec = tween(180),
                label = "toggle_switch_track",
            )
            val thumbOffset by animateDpAsState(
                targetValue = if (isActive) (trackWidth - thumbSize - 2.dp) else 2.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 460f),
                label = "toggle_switch_thumb",
            )

            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(trackColor),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(if (isActive) ObsidianVoid else PureFrost)
                )
            }
        }

        ToggleIndicatorStyle.PILL -> {
            val bgColor by animateColorAsState(
                targetValue = if (isActive) IceCyan.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                animationSpec = tween(180),
                label = "toggle_pill_bg",
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(
                        width = 0.5.dp,
                        color = if (isActive) IceCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f),
                        shape = CircleShape,
                    )
                    .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isActive) "ON" else "OFF",
                    style = TextStyle(
                        color = if (isActive) IceCyan else FrostSlate,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 9.5.sp else 10.sp,
                    ),
                )
            }
        }

        ToggleIndicatorStyle.DOT -> {
            val dotColor by animateColorAsState(
                targetValue = if (isActive) IceCyan else Color.White.copy(alpha = 0.20f),
                animationSpec = tween(180),
                label = "toggle_dot_color",
            )
            Box(
                modifier = Modifier
                    .size(if (compact) 8.dp else 9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

private fun isQuickToggleActive(
    toggle: QuickToggleAction,
    snapshot: BrowserCoreSnapshot,
    isCurrentUrlBookmarked: Boolean,
): Boolean {
    val activeTab = snapshot.activeTab
    val prefs = snapshot.preferences
    return when (toggle) {
        QuickToggleAction.DESKTOP_MODE -> activeTab?.isDesktopMode == true
        QuickToggleAction.FORCE_DARK -> prefs.algorithmicDarkeningEnabled
        QuickToggleAction.AD_BLOCK -> prefs.adBlockEnabled
        QuickToggleAction.JAVASCRIPT -> prefs.javaScriptEnabled
        QuickToggleAction.BLOCK_IMAGES -> prefs.imagesBlocked
        QuickToggleAction.SCROLL_TO_PILL -> prefs.scrollToPillEnabled
        QuickToggleAction.DUAL_PANE -> prefs.foldableDualPaneEnabled
        QuickToggleAction.INCOGNITO_TAB -> activeTab?.isIncognito == true
        QuickToggleAction.BOOKMARK -> isCurrentUrlBookmarked
        QuickToggleAction.NEW_TAB,
        QuickToggleAction.HISTORY,
        QuickToggleAction.FIND_IN_PAGE,
        QuickToggleAction.PAGE_SUMMARY,
        QuickToggleAction.COPY_AND_SHARE -> false
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    glassOpacity: Float,
    onQueryChange: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }

    LiquidGlassSurface(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .testTag("find_in_page_bar"),
        shape = RoundedCornerShape(unifiedRadius),
        shadowElevation = 16.dp,
        glassOpacity = glassOpacity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.FindInPage,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(18.dp),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(innerRadius))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "페이지 내 검색어 입력",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = FrostSlate.copy(alpha = 0.75f),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
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
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(IceCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onFindNext(true) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("find_in_page_input"),
                )
            }

            GlassIconButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "이전 일치 항목",
                tint = PureFrost,
                cornerRadius = innerRadius,
                onClick = { onFindNext(false) },
            )
            GlassIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "다음 일치 항목",
                tint = PureFrost,
                cornerRadius = innerRadius,
                onClick = { onFindNext(true) },
            )
            GlassIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "페이지 내 찾기 닫기",
                tint = FrostSlate,
                cornerRadius = innerRadius,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun AddressCapsule(
    activeTab: BrowserTabState?,
    displayUrl: String,
    capsuleHeight: Dp,
    capsuleCornerRadius: Dp,
    buttonCornerRadius: Dp,
    glassOpacity: Float,
    isEditing: Boolean,
    expandOnEdit: Boolean,
    gesturesEnabled: Boolean,
    swipeUpAction: Int,
    swipeDownAction: Int,
    swipeLeftRightTabSwitch: Boolean,
    doubleTapAction: Int,
    gestureSensitivityDp: Float,
    onTapToEdit: () -> Unit,
    onExitEdit: () -> Unit,
    onSubmit: (String) -> Unit,
    onReloadOrStop: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeTab: (delta: Int) -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUrl = activeTab?.url.orEmpty()
    val securityState = activeTab?.securityState ?: SecurityState.NEUTRAL
    val isLoading = activeTab?.isLoading == true
    val isIncognito = activeTab?.isIncognito == true

    val density = LocalDensity.current
    val sensitivityPx = with(density) { gestureSensitivityDp.dp.toPx() }
    val maxVisualDragPx = with(density) { 28.dp.toPx() }

    val latestOnTapToEdit by rememberUpdatedState(onTapToEdit)
    val latestOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val latestOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val latestOnSwipeTab by rememberUpdatedState(onSwipeTab)
    val latestOnDoubleTap by rememberUpdatedState(onDoubleTap)

    var targetVisualDragX by remember(isEditing) { mutableFloatStateOf(0f) }
    val animatedDragX by animateFloatAsState(
        targetValue = if (isEditing) 0f else targetVisualDragX,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f),
        label = "address_capsule_drag_offset",
    )

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var fieldValue by remember(isEditing, currentUrl) {
        mutableStateOf(
            TextFieldValue(
                text = currentUrl,
                selection = TextRange(0, currentUrl.length),
            )
        )
    }
    var hasGainedFocus by remember(isEditing) { mutableStateOf(false) }

    BackHandler(enabled = isEditing) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onExitEdit()
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            fieldValue = TextFieldValue(
                text = currentUrl,
                selection = TextRange(0, currentUrl.length),
            )
            awaitFrame()
            runCatching { focusRequester.requestFocus() }
            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    val capsuleShape = RoundedCornerShape(capsuleCornerRadius)
    val capsuleTintAlpha = (0.64f + glassOpacity * 0.65f).coerceIn(0.66f, 0.94f)
    val capsuleBaseTint = if (isIncognito) {
        Color(0xFF1A132B).copy(alpha = capsuleTintAlpha)
    } else {
        Color(0xFF0A111F).copy(alpha = capsuleTintAlpha)
    }
    val capsuleSpecular = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = (glassOpacity * 0.85f).coerceIn(0.05f, 0.35f)),
            Color.White.copy(alpha = 0.03f),
        )
    )
    val capsuleRimBrush = if (isEditing) {
        Brush.verticalGradient(
            listOf(IceCyan.copy(alpha = 0.85f), IceCyan.copy(alpha = 0.35f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.10f))
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = animatedDragX
            }
            .height(capsuleHeight)
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .clip(capsuleShape)
            .background(capsuleBaseTint)
            .background(capsuleSpecular)
            .border(width = 0.5.dp, brush = capsuleRimBrush, shape = capsuleShape)
            .then(
                if (!isEditing) {
                    Modifier
                        .pointerInput(gesturesEnabled, doubleTapAction) {
                            detectTapGestures(
                                onTap = { latestOnTapToEdit() },
                                onDoubleTap = if (gesturesEnabled && doubleTapAction != BrowserPreferences.GESTURE_NONE) {
                                    { latestOnDoubleTap() }
                                } else {
                                    null
                                },
                            )
                        }
                        .pointerInput(
                            gesturesEnabled,
                            swipeUpAction,
                            swipeDownAction,
                            swipeLeftRightTabSwitch,
                            sensitivityPx,
                        ) {
                            if (!gesturesEnabled) return@pointerInput
                            var dragX = 0f
                            var dragY = 0f
                            detectDragGestures(
                                onDragStart = {
                                    dragX = 0f
                                    dragY = 0f
                                    targetVisualDragX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragX += dragAmount.x
                                    dragY += dragAmount.y
                                    if (swipeLeftRightTabSwitch && abs(dragX) > abs(dragY)) {
                                        targetVisualDragX = (dragX * 0.42f)
                                            .coerceIn(-maxVisualDragPx, maxVisualDragPx)
                                    } else {
                                        targetVisualDragX = 0f
                                    }
                                },
                                onDragEnd = {
                                    if (abs(dragY) >= abs(dragX)) {
                                        if (dragY < -sensitivityPx && swipeUpAction != BrowserPreferences.GESTURE_NONE) {
                                            latestOnSwipeUp()
                                        } else if (dragY > sensitivityPx && swipeDownAction != BrowserPreferences.GESTURE_NONE) {
                                            latestOnSwipeDown()
                                        }
                                    } else if (swipeLeftRightTabSwitch && abs(dragX) > sensitivityPx) {
                                        latestOnSwipeTab(if (dragX < 0f) 1 else -1)
                                    }
                                    dragX = 0f
                                    dragY = 0f
                                    targetVisualDragX = 0f
                                },
                                onDragCancel = {
                                    dragX = 0f
                                    dragY = 0f
                                    targetVisualDragX = 0f
                                },
                            )
                        }
                        .testTag("address_capsule_resting")
                } else {
                    Modifier.testTag("address_capsule_editing")
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(capsuleHeight)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isIncognito) {
                        Icons.Rounded.PrivacyTip
                    } else {
                        padlockIconFor(securityState)
                    },
                    contentDescription = when {
                        isIncognito -> "시크릿 탭, 탭하여 주소 입력"
                        securityState == SecurityState.SECURE -> "보안 HTTPS 연결, 탭하여 주소 입력"
                        securityState == SecurityState.INSECURE -> "암호화되지 않은 HTTP 연결, 탭하여 주소 입력"
                        else -> "주소창, 탭하여 주소 입력"
                    },
                    tint = if (isIncognito) IceCyan else padlockTintFor(securityState),
                    modifier = Modifier
                        .size(15.dp)
                        .testTag("address_padlock_icon"),
                )

                Text(
                    text = displayUrl.ifBlank { "새 탭" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = PureFrost,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("address_resting_url_text"),
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(buttonCornerRadius))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = IceCyan),
                            onClick = onReloadOrStop,
                        )
                        .testTag("address_inner_reload_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f))
                        },
                        label = "inner_reload_stop_morph",
                    ) { loading ->
                        Icon(
                            imageVector = if (loading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                            contentDescription = if (loading) "로딩 중지" else "새로고침",
                            tint = if (loading) IceCyan else PureFrost.copy(alpha = 0.90f),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = if (expandOnEdit) 8.dp else 0.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "검색 또는 URL 입력",
                    tint = IceCyan,
                    modifier = Modifier.size(18.dp),
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (fieldValue.text.isEmpty()) {
                        Text(
                            text = "검색 또는 URL 입력",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = FrostSlate.copy(alpha = 0.72f),
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { incoming ->
                            if (expandOnEdit && (incoming.text.contains('\n') || incoming.text.contains('\r'))) {
                                val cleanText = incoming.text.replace("\r", "").replace("\n", "")
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onSubmit(cleanText)
                            } else {
                                fieldValue = incoming
                            }
                        },
                        singleLine = !expandOnEdit,
                        maxLines = if (expandOnEdit) 3 else 1,
                        textStyle = TextStyle(
                            color = PureFrost,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        cursorBrush = SolidColor(IceCyan),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onSubmit(fieldValue.text)
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    hasGainedFocus = true
                                } else if (hasGainedFocus) {
                                    onExitEdit()
                                }
                            }
                            .testTag("address_text_field"),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(buttonCornerRadius))
                        .background(IceCyan.copy(alpha = 0.22f))
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onSubmit(fieldValue.text)
                        }
                        .testTag("address_submit_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "이동",
                        tint = IceCyan,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarActionIconButton(
    action: ToolbarAction,
    activeTab: BrowserTabState?,
    tabCount: Int,
    isTabDrawerOpen: Boolean,
    buttonCornerRadius: Dp,
    onClick: () -> Unit,
) {
    when (action) {
        ToolbarAction.BACK -> {
            val enabled = activeTab?.canGoBack == true
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "뒤로",
                enabled = enabled,
                tint = if (enabled) PureFrost else FrostSlate.copy(alpha = 0.35f),
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_back"),
            )
        }

        ToolbarAction.FORWARD -> {
            val enabled = activeTab?.canGoForward == true
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "앞으로",
                enabled = enabled,
                tint = if (enabled) PureFrost else FrostSlate.copy(alpha = 0.35f),
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_forward"),
            )
        }

        ToolbarAction.RELOAD_OR_STOP -> {
            val isLoading = activeTab?.isLoading == true
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f))
                },
                label = "reload_stop_morph",
            ) { loading ->
                GlassIconButton(
                    icon = if (loading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    contentDescription = if (loading) "중지" else "새로고침",
                    tint = if (loading) IceCyan else PureFrost,
                    cornerRadius = buttonCornerRadius,
                    onClick = onClick,
                    modifier = Modifier.testTag("toolbar_action_reload_or_stop"),
                )
            }
        }

        ToolbarAction.TABS -> {
            val shape = RoundedCornerShape(buttonCornerRadius)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(42.dp)
                    .clip(shape)
                    .background(
                        if (isTabDrawerOpen) IceCyan.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = IceCyan),
                        onClick = onClick,
                    )
                    .testTag("toolbar_action_tabs"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "탭 목록",
                        tint = if (isTabDrawerOpen) IceCyan else PureFrost,
                        modifier = Modifier.size(20.dp),
                    )
                    val dotCount = tabCount.coerceIn(1, 4)
                    Canvas(modifier = Modifier.size(width = (dotCount * 6).dp, height = 4.dp)) {
                        val radius = 1.6.dp.toPx()
                        val step = size.width / dotCount
                        for (i in 0 until dotCount) {
                            drawCircle(
                                color = if (isTabDrawerOpen) IceCyan else PureFrost.copy(alpha = 0.72f),
                                radius = radius,
                                center = Offset(step * i + step / 2f, size.height / 2f),
                            )
                        }
                    }
                }
            }
        }

        ToolbarAction.NEW_TAB -> {
            GlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "새 탭",
                tint = PureFrost,
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_new_tab"),
            )
        }

        ToolbarAction.HOME -> {
            GlassIconButton(
                icon = Icons.Rounded.Home,
                contentDescription = "홈",
                tint = PureFrost,
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_home"),
            )
        }

        ToolbarAction.DESKTOP_MODE -> {
            val isDesktop = activeTab?.isDesktopMode == true
            GlassIconButton(
                icon = if (isDesktop) Icons.Rounded.Computer else Icons.Rounded.Smartphone,
                contentDescription = "데스크톱 모드",
                tint = if (isDesktop) IceCyan else PureFrost,
                isActive = isDesktop,
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_desktop_mode"),
            )
        }

        ToolbarAction.SHARE -> {
            GlassIconButton(
                icon = Icons.Rounded.Share,
                contentDescription = "공유",
                tint = PureFrost,
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_share"),
            )
        }

        ToolbarAction.PAGE_SUMMARY -> {
            GlassIconButton(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "페이지 요약 및 AI",
                tint = IceCyan,
                cornerRadius = buttonCornerRadius,
                onClick = onClick,
                modifier = Modifier.testTag("toolbar_action_page_summary"),
            )
        }
    }
}

@Composable
private fun IconOnlyTabDock(
    tabs: List<BrowserTabState>,
    activeTabId: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    glassOpacity: Float,
    onSwitchTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateTab: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .testTag("icon_only_tab_dock"),
        shape = RoundedCornerShape(unifiedRadius),
        shadowElevation = 16.dp,
        glassOpacity = glassOpacity,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val chipShape = RoundedCornerShape(innerRadius)
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(chipShape)
                        .background(
                            if (isActive) IceCyan.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isActive) IceCyan.copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.16f),
                            shape = chipShape,
                        )
                        .clickable { onSwitchTab(tab.id) }
                        .padding(start = 10.dp, end = 6.dp)
                        .testTag("tab_chip_${tab.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = when {
                            tab.isIncognito -> Icons.Rounded.PrivacyTip
                            tab.isDesktopMode -> Icons.Rounded.Computer
                            else -> padlockIconFor(tab.securityState)
                        },
                        contentDescription = "탭 전환",
                        tint = if (isActive || tab.isIncognito) IceCyan else padlockTintFor(tab.securityState),
                        modifier = Modifier.size(16.dp),
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape((innerRadius - 4.dp).coerceAtLeast(4.dp)))
                            .clickable { onCloseTab(tab.id) }
                            .testTag("close_tab_${tab.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "탭 닫기",
                            tint = PureFrost.copy(alpha = 0.72f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            val newTabShape = RoundedCornerShape(innerRadius)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(newTabShape)
                    .background(IceCyan.copy(alpha = 0.18f))
                    .border(0.5.dp, IceCyan.copy(alpha = 0.45f), newTabShape)
                    .clickable(onClick = onCreateTab)
                    .testTag("tab_dock_new_tab_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "새 탭 열기",
                    tint = IceCyan,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    cornerRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(42.dp)
            .clip(shape)
            .background(if (isActive) IceCyan.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = IceCyan),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

private fun quickToggleIcon(action: QuickToggleAction, isActive: Boolean): ImageVector =
    when (action) {
        QuickToggleAction.DESKTOP_MODE -> Icons.Rounded.Computer
        QuickToggleAction.FORCE_DARK -> Icons.Rounded.DarkMode
        QuickToggleAction.AD_BLOCK -> Icons.Rounded.Shield
        QuickToggleAction.JAVASCRIPT -> Icons.Rounded.Javascript
        QuickToggleAction.BLOCK_IMAGES -> Icons.Rounded.HideImage
        QuickToggleAction.SCROLL_TO_PILL -> Icons.Rounded.SwipeVertical
        QuickToggleAction.DUAL_PANE -> Icons.Rounded.Splitscreen
        QuickToggleAction.INCOGNITO_TAB -> Icons.Rounded.PrivacyTip
        QuickToggleAction.NEW_TAB -> Icons.Rounded.Add
        QuickToggleAction.BOOKMARK -> if (isActive) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder
        QuickToggleAction.HISTORY -> Icons.Rounded.History
        QuickToggleAction.FIND_IN_PAGE -> Icons.Rounded.FindInPage
        QuickToggleAction.PAGE_SUMMARY -> Icons.Rounded.AutoAwesome
        QuickToggleAction.COPY_AND_SHARE -> Icons.Rounded.Share
    }

fun quickToggleLabel(action: QuickToggleAction): String =
    when (action) {
        QuickToggleAction.DESKTOP_MODE -> "데스크톱 모드"
        QuickToggleAction.FORCE_DARK -> "강제 다크 모드"
        QuickToggleAction.AD_BLOCK -> "광고 및 추적 차단"
        QuickToggleAction.JAVASCRIPT -> "자바스크립트 허용"
        QuickToggleAction.BLOCK_IMAGES -> "이미지 차단 (데이터 절약)"
        QuickToggleAction.SCROLL_TO_PILL -> "스크롤 알약 축소"
        QuickToggleAction.DUAL_PANE -> "듀얼 뷰 분할"
        QuickToggleAction.INCOGNITO_TAB -> "시크릿 모드"
        QuickToggleAction.NEW_TAB -> "새 탭"
        QuickToggleAction.BOOKMARK -> "북마크"
        QuickToggleAction.HISTORY -> "방문 기록"
        QuickToggleAction.FIND_IN_PAGE -> "페이지 내 찾기"
        QuickToggleAction.PAGE_SUMMARY -> "페이지 요약 / AI"
        QuickToggleAction.COPY_AND_SHARE -> "URL 복사 및 공유"
    }

fun quickToggleCompactLabel(action: QuickToggleAction): String =
    when (action) {
        QuickToggleAction.DESKTOP_MODE -> "데스크톱"
        QuickToggleAction.FORCE_DARK -> "다크 모드"
        QuickToggleAction.AD_BLOCK -> "광고 차단"
        QuickToggleAction.JAVASCRIPT -> "자바스크립트"
        QuickToggleAction.BLOCK_IMAGES -> "이미지 차단"
        QuickToggleAction.SCROLL_TO_PILL -> "알약 축소"
        QuickToggleAction.DUAL_PANE -> "듀얼 뷰"
        QuickToggleAction.INCOGNITO_TAB -> "시크릿"
        QuickToggleAction.NEW_TAB -> "새 탭"
        QuickToggleAction.BOOKMARK -> "북마크"
        QuickToggleAction.HISTORY -> "방문 기록"
        QuickToggleAction.FIND_IN_PAGE -> "페이지 찾기"
        QuickToggleAction.PAGE_SUMMARY -> "페이지 요약"
        QuickToggleAction.COPY_AND_SHARE -> "복사/공유"
    }

private fun padlockIconFor(state: SecurityState): ImageVector =
    when (state) {
        SecurityState.SECURE, SecurityState.NEUTRAL -> Icons.Rounded.Lock
        SecurityState.INSECURE -> Icons.Rounded.LockOpen
    }

private fun padlockTintFor(state: SecurityState): Color =
    when (state) {
        SecurityState.SECURE -> EmeraldSecure
        SecurityState.INSECURE -> AmberWarn
        SecurityState.NEUTRAL -> PureFrost.copy(alpha = 0.88f)
    }
