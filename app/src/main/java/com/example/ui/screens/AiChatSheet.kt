package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.core.AiChatTurn
import com.example.core.AiCliProfile
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Immutable
data class AiChatUiState(
    val sessionSeed: Int = 0,
    val prefillUrl: String = "",
    val pageTitle: String = "",
    val pageBodyText: String = "",
    val messages: List<AiChatTurn> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Bottom-anchored Liquid Glass AI Chat modal expanding directly upward from the bottom address bar
 * when triggered via Address Bar Swipe / Long-Press + Swipe Up.
 * - Compact icon-only AI Settings trigger (`Icons.Rounded.Settings`, `RoundedCornerShape(unifiedRadius)`)
 * - Quick-switch CLI dropdown and Model dropdown right inside the modal header toolbar
 * - Multi-line [BasicTextField] with [ImeAction.Default] so Enter always inserts a new paragraph (`\n`)
 *   and messages are sent exclusively via the dedicated Send button
 * - Pre-fills the active tab URL with the cursor at the end without auto-sending
 */
@Composable
fun AiChatSheet(
    chatState: AiChatUiState,
    activeProfile: AiCliProfile,
    allProfiles: List<AiCliProfile>,
    connectionStatus: AiConnectionStatus? = null,
    unifiedRadius: Dp,
    glassOpacity: Float = 0.24f,
    topSafeAreaPaddingDp: Dp = 0.dp,
    bottomSafeAreaPaddingDp: Dp = 0.dp,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onSelectAiProfile: (String) -> Unit,
    onSelectAiModel: (profileId: String, modelName: String) -> Unit,
    onRefreshModels: (AiCliProfile) -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    BackHandler(enabled = true) { onDismiss() }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val cardShape = RoundedCornerShape(unifiedRadius)
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var isInputFocused by remember { mutableStateOf(false) }
    var isCliMenuExpanded by remember { mutableStateOf(false) }
    var isModelMenuExpanded by remember { mutableStateOf(false) }
    var cliTriggerWidthPx by remember { mutableIntStateOf(0) }
    var modelTriggerWidthPx by remember { mutableIntStateOf(0) }
    var modalHeightPx by remember { mutableIntStateOf(0) }
    var isDismissingByDrag by remember(chatState.sessionSeed) { mutableStateOf(false) }
    val dragOffsetY = remember(chatState.sessionSeed) { Animatable(0f) }
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val quickFlickThresholdPx = with(density) { 40.dp.toPx() }

    // Synchronize modal corner transition from bottom capsule radius into settled modal corners
    var hasExpandedCorners by remember(chatState.sessionSeed) { mutableStateOf(false) }
    val animatedBottomRadius by animateDpAsState(
        targetValue = if (hasExpandedCorners) unifiedRadius else (unifiedRadius + 10.dp).coerceAtMost(40.dp),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ai_modal_corner_morph",
    )

    var inputFieldValue by remember(chatState.sessionSeed) {
        val seedText = chatState.prefillUrl
        mutableStateOf(
            TextFieldValue(
                text = seedText,
                selection = TextRange(seedText.length),
            )
        )
    }

    LaunchedEffect(chatState.sessionSeed) {
        hasExpandedCorners = true
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(chatState.messages.size, chatState.isLoading) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    val isRefreshingModels = connectionStatus?.isChecking == true &&
        connectionStatus.profileId == activeProfile.id

    val modalShape = RoundedCornerShape(
        topStart = unifiedRadius,
        topEnd = unifiedRadius,
        bottomStart = animatedBottomRadius,
        bottomEnd = animatedBottomRadius,
    )
    val modalBackground = Brush.verticalGradient(
        colors = listOf(
            ObsidianSurface.copy(alpha = 0.96f),
            ObsidianVoid.copy(alpha = 0.98f),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = topSafeAreaPaddingDp,
                bottom = 8.dp + bottomSafeAreaPaddingDp,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .onSizeChanged { modalHeightPx = it.height }
                .graphicsLayer {
                    val clampedDragY = dragOffsetY.value.coerceAtLeast(0f)
                    val dismissProgress = if (modalHeightPx > 0) {
                        (clampedDragY / modalHeightPx.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationY = clampedDragY
                    scaleX = 1f - dismissProgress * 0.08f
                    scaleY = 1f - dismissProgress * 0.05f
                }
                .shadow(
                    elevation = 28.dp,
                    shape = modalShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.60f),
                    spotColor = Color.Black.copy(alpha = 0.78f),
                )
                .clip(modalShape)
                .background(modalBackground)
                .border(
                    width = 0.85.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            IceCyan.copy(alpha = 0.36f),
                            Color.White.copy(alpha = 0.14f),
                        )
                    ),
                    shape = modalShape,
                )
                .pointerInput(Unit) {
                    detectTapGestures { /* consume taps inside modal card */ }
                }
                .testTag("ai_chat_sheet"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = (glassOpacity * 0.12f).coerceIn(0.01f, 0.06f)))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Swipe-down dismiss zone covering top drag handle and header bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(chatState.sessionSeed, dismissThresholdPx, quickFlickThresholdPx) {
                            val velocityTracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = {
                                    velocityTracker.resetTracking()
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    if (isDismissingByDrag) return@detectVerticalDragGestures
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    val nextY = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    coroutineScope.launch {
                                        dragOffsetY.snapTo(nextY)
                                    }
                                },
                                onDragEnd = {
                                    if (isDismissingByDrag) return@detectVerticalDragGestures
                                    val vy = runCatching { velocityTracker.calculateVelocity().y }.getOrDefault(0f)
                                    val currentY = dragOffsetY.value
                                    val shouldDismiss = currentY > dismissThresholdPx ||
                                        (currentY > quickFlickThresholdPx && vy > 650f)
                                    if (shouldDismiss) {
                                        isDismissingByDrag = true
                                        coroutineScope.launch {
                                            val collapseDistance = (modalHeightPx.takeIf { it > 0 }?.toFloat() ?: 900f) * 0.65f
                                            dragOffsetY.animateTo(
                                                targetValue = collapseDistance,
                                                initialVelocity = vy.coerceAtLeast(0f),
                                                animationSpec = spring(
                                                    dampingRatio = 0.84f,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                            onDismiss()
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(
                                                targetValue = 0f,
                                                initialVelocity = vy,
                                                animationSpec = spring(
                                                    dampingRatio = 0.80f,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    if (!isDismissingByDrag) {
                                        coroutineScope.launch {
                                            dragOffsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.80f,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                        .testTag("ai_chat_drag_header"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Bottom-sheet drag handle bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 2.dp, bottom = 2.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                            .testTag("ai_chat_drag_handle")
                    )

                    // Top header bar (subtle icon-only buttons; dismissed exclusively via Back or swipe-down)
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
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "AI 대화",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureFrost,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (chatState.messages.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearChat,
                                    modifier = Modifier.testTag("ai_chat_clear_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = "대화 초기화",
                                        tint = FrostSlate,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenAiSettings,
                                modifier = Modifier.testTag("ai_chat_open_settings_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "AI 설정",
                                    tint = FrostSlate,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                // Quick-switch CLI & Model Dropdown Toolbar (Liquid Glass + RoundedCornerShape(unifiedRadius))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_chat_quick_selectors_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 1. CLI Dropdown
                    Box(modifier = Modifier.weight(0.46f)) {
                        val cliChevronRotation by animateFloatAsState(
                            targetValue = if (isCliMenuExpanded) 180f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "cli_chevron_rotation",
                        )
                        val cliMenuMinWidth = with(density) { cliTriggerWidthPx.toDp() }.coerceAtLeast(156.dp)
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { cliTriggerWidthPx = it.width },
                            shape = cardShape,
                            glassOpacity = (glassOpacity * 0.85f).coerceIn(0.10f, 0.34f),
                            shadowElevation = 4.dp,
                            isHighlighted = isCliMenuExpanded,
                            accentColor = IceCyan,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(cardShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = IceCyan),
                                        onClick = { isCliMenuExpanded = true },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("ai_chat_cli_dropdown"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = "CLI 선택",
                                    tint = IceCyan,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = activeProfile.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PureFrost,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = FrostSlate,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = cliChevronRotation },
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isCliMenuExpanded,
                            onDismissRequest = { isCliMenuExpanded = false },
                            offset = DpOffset(0.dp, 6.dp),
                            shape = cardShape,
                            containerColor = ObsidianSurface,
                            shadowElevation = 12.dp,
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .widthIn(min = cliMenuMinWidth, max = 280.dp)
                                .testTag("ai_chat_cli_dropdown_menu"),
                        ) {
                            allProfiles.forEach { profile ->
                                val isSelected = profile.id == activeProfile.id
                                AiDropdownMenuRow(
                                    label = profile.name,
                                    icon = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Terminal,
                                    isSelected = isSelected,
                                    innerRadius = innerRadius,
                                    testTag = "ai_chat_cli_item_${profile.id}",
                                    onClick = {
                                        isCliMenuExpanded = false
                                        onSelectAiProfile(profile.id)
                                        if (profile.availableModels.isEmpty()) {
                                            onRefreshModels(profile)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // 2. Model Dropdown
                    Box(modifier = Modifier.weight(0.54f)) {
                        val modelChevronRotation by animateFloatAsState(
                            targetValue = if (isModelMenuExpanded) 180f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "model_chevron_rotation",
                        )
                        val modelMenuMinWidth = with(density) { modelTriggerWidthPx.toDp() }.coerceAtLeast(180.dp)
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { modelTriggerWidthPx = it.width },
                            shape = cardShape,
                            glassOpacity = (glassOpacity * 0.85f).coerceIn(0.10f, 0.34f),
                            shadowElevation = 4.dp,
                            isHighlighted = isModelMenuExpanded || isRefreshingModels,
                            accentColor = IceCyan,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(cardShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = IceCyan),
                                        onClick = { isModelMenuExpanded = true },
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("ai_chat_model_dropdown"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (isRefreshingModels) {
                                    CircularProgressIndicator(
                                        color = IceCyan,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "모델 선택",
                                        tint = IceCyan,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                val modelDisplay = when {
                                    isRefreshingModels -> "모델 불러오는 중..."
                                    activeProfile.selectedModel.isNotBlank() -> activeProfile.selectedModel
                                    else -> "모델 선택"
                                }
                                Text(
                                    text = modelDisplay,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (activeProfile.selectedModel.isNotBlank()) PureFrost else FrostSlate,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = FrostSlate,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = modelChevronRotation },
                                )
                            }
                        }

                        val dropdownModels = remember(
                            activeProfile.id,
                            activeProfile.pinnedModels,
                            activeProfile.selectedModel,
                        ) {
                            activeProfile.dropdownModels()
                        }

                        DropdownMenu(
                            expanded = isModelMenuExpanded,
                            onDismissRequest = { isModelMenuExpanded = false },
                            offset = DpOffset(0.dp, 6.dp),
                            shape = cardShape,
                            containerColor = ObsidianSurface,
                            shadowElevation = 12.dp,
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier
                                .widthIn(min = modelMenuMinWidth, max = 300.dp)
                                .testTag("ai_chat_model_dropdown_menu"),
                        ) {
                            if (dropdownModels.isEmpty()) {
                                AiDropdownMenuRow(
                                    label = "불러온 모델 없음",
                                    icon = Icons.Rounded.AutoAwesome,
                                    isSelected = false,
                                    enabled = false,
                                    innerRadius = innerRadius,
                                    onClick = {},
                                )
                            } else {
                                dropdownModels.forEach { modelName ->
                                    val isSelected = modelName == activeProfile.selectedModel
                                    AiDropdownMenuRow(
                                        label = modelName,
                                        icon = if (isSelected) Icons.Rounded.Check else Icons.Rounded.AutoAwesome,
                                        isSelected = isSelected,
                                        innerRadius = innerRadius,
                                        testTag = "ai_chat_model_item_$modelName",
                                        onClick = {
                                            isModelMenuExpanded = false
                                            onSelectAiModel(activeProfile.id, modelName)
                                        },
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )

                            AiDropdownMenuRow(
                                label = if (isRefreshingModels) "모델 불러오는 중..." else "모델 새로고침",
                                icon = Icons.Rounded.Refresh,
                                isSelected = false,
                                enabled = !isRefreshingModels,
                                forceAccentLabel = true,
                                innerRadius = innerRadius,
                                testTag = "ai_chat_refresh_models_item",
                                onClick = {
                                    isModelMenuExpanded = false
                                    onRefreshModels(activeProfile)
                                },
                            )
                        }
                    }
                }

                // Active Tab Context Pill
                if (chatState.prefillUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(innerRadius))
                            .background(ObsidianElevated.copy(alpha = 0.65f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(innerRadius))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(15.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            if (chatState.pageTitle.isNotBlank()) {
                                Text(
                                    text = chatState.pageTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PureFrost,
                                )
                            }
                            Text(
                                text = chatState.prefillUrl,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostSlate,
                            )
                        }
                    }
                }

                // Conversation list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = chatState.messages,
                        key = { idx, msg -> "${idx}_${msg.isUser}_${msg.text.hashCode()}" },
                    ) { _, msg ->
                        val isUser = msg.isUser
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .clip(cardShape)
                                    .background(
                                        if (isUser) IceCyan.copy(alpha = 0.18f)
                                        else ObsidianElevated.copy(alpha = 0.80f)
                                    )
                                    .border(
                                        width = 0.75.dp,
                                        color = if (isUser) IceCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                                        shape = cardShape,
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (isUser) "나" else activeProfile.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) IceCyan else FrostSlate,
                                )
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureFrost,
                                )
                            }
                        }
                    }

                    if (chatState.isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(ObsidianElevated.copy(alpha = 0.72f))
                                    .border(0.75.dp, IceCyan.copy(alpha = 0.35f), cardShape)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    color = IceCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "${activeProfile.name} (${activeProfile.selectedModel.ifBlank { "모델" }}) 응답 생성 중...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureFrost,
                                )
                            }
                        }
                    }

                    if (!chatState.errorMessage.isNullOrBlank()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(AmberWarn.copy(alpha = 0.12f))
                                    .border(0.75.dp, AmberWarn.copy(alpha = 0.45f), cardShape)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = chatState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberWarn,
                                )
                                OutlinedButton(
                                    onClick = onOpenAiSettings,
                                    shape = RoundedCornerShape(innerRadius),
                                ) {
                                    Text(
                                        text = "AI 설정에서 연결 및 모델 확인",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceCyan,
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick prompt append chips (never auto-send; append to user draft cleanly)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "핵심 요약" to " 이 페이지의 핵심 내용을 한국어로 5줄 요약해 주세요.",
                        "장단점 비교" to " 이 문서의 주요 장점과 한계점을 정리해 주세요.",
                        "초보자 설명" to " 이 내용을 비전공자도 이해하기 쉽게 설명해 주세요.",
                    ).forEach { (chipLabel, promptSuffix) ->
                        val chipShape = RoundedCornerShape(innerRadius)
                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(ObsidianElevated.copy(alpha = 0.70f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.14f), chipShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = IceCyan),
                                ) {
                                    val base = inputFieldValue.text.trimEnd()
                                    val cleanSuffix = promptSuffix.trim()
                                    val updated = when {
                                        base.isEmpty() -> cleanSuffix
                                        base.contains(cleanSuffix) -> base
                                        else -> "$base\n$cleanSuffix"
                                    }
                                    inputFieldValue = TextFieldValue(
                                        text = updated,
                                        selection = TextRange(updated.length),
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = "+ $chipLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan,
                            )
                        }
                    }
                }

                // Bottom Multi-Line Prompt Input Box (BasicTextField + ImeAction.Default: Enter inserts \n, Send button submits)
                val inputShape = RoundedCornerShape(innerRadius)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp, max = 148.dp)
                            .clip(inputShape)
                            .background(ObsidianElevated.copy(alpha = 0.78f))
                            .border(
                                width = if (isInputFocused) 1.1.dp else 0.75.dp,
                                color = if (isInputFocused) IceCyan else Color.White.copy(alpha = 0.20f),
                                shape = inputShape,
                            )
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (inputFieldValue.text.isEmpty()) {
                            Text(
                                text = "URL 또는 AI에게 보낼 프롬프트 입력 (Enter: 줄바꿈)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostSlate.copy(alpha = 0.72f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = inputFieldValue,
                            onValueChange = { inputFieldValue = it },
                            singleLine = false,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                            cursorBrush = SolidColor(IceCyan),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Default,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isInputFocused = it.isFocused }
                                .testTag("ai_chat_input"),
                        )
                    }

                    Button(
                        onClick = {
                            val clean = inputFieldValue.text.trim()
                            if (clean.isNotEmpty() && !chatState.isLoading) {
                                onSendMessage(clean)
                                inputFieldValue = TextFieldValue("")
                            }
                        },
                        enabled = inputFieldValue.text.isNotBlank() && !chatState.isLoading,
                        shape = RoundedCornerShape(innerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyan,
                            contentColor = ObsidianVoid,
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("ai_chat_send_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "전송",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dropdown menu row clipped strictly inside `RoundedCornerShape(innerRadius)` so both the
 * active selection pill and Material press ripple match the exact row bounds with zero bleed.
 */
@Composable
private fun AiDropdownMenuRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    innerRadius: Dp,
    enabled: Boolean = true,
    forceAccentLabel: Boolean = false,
    accentColor: Color = IceCyan,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(innerRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .defaultMinSize(minHeight = 48.dp)
            .clip(rowShape)
            .background(
                if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent
            )
            .then(
                if (isSelected) {
                    Modifier.border(0.75.dp, accentColor.copy(alpha = 0.42f), rowShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                !enabled -> FrostSlate.copy(alpha = 0.55f)
                isSelected || forceAccentLabel -> accentColor
                else -> FrostSlate
            },
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (forceAccentLabel) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = when {
                !enabled -> FrostSlate.copy(alpha = 0.65f)
                isSelected || forceAccentLabel -> accentColor
                else -> PureFrost
            },
            modifier = Modifier.weight(1f),
        )
    }
}
