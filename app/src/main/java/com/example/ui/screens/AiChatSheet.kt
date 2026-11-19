package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * when triggered via Address Bar Long-Press + Swipe Up.
 * Pre-fills the active tab URL with the cursor at the end without auto-sending.
 */
@Composable
fun AiChatSheet(
    chatState: AiChatUiState,
    activeProfile: AiCliProfile,
    unifiedRadius: Dp,
    glassOpacity: Float = 0.24f,
    topSafeAreaPaddingDp: Dp = 0.dp,
    bottomSafeAreaPaddingDp: Dp = 0.dp,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    BackHandler(enabled = true) { onDismiss() }

    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val cardShape = RoundedCornerShape(unifiedRadius)
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

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
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(chatState.messages.size, chatState.isLoading) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    val modalShape = RoundedCornerShape(unifiedRadius)
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
                top = 12.dp + topSafeAreaPaddingDp,
                bottom = 8.dp + bottomSafeAreaPaddingDp,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
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
            // Bottom-sheet drag handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 2.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
            )

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("ai_chat_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로",
                            tint = PureFrost,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "AI 대화",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureFrost,
                        )
                        val modelLabel = activeProfile.selectedModel.ifBlank { "모델 미선택" }
                        Text(
                            text = "${activeProfile.name} · $modelLabel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyan,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    TextButton(
                        onClick = onOpenAiSettings,
                        modifier = Modifier.testTag("ai_chat_open_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "AI 설정",
                            style = MaterialTheme.typography.labelMedium,
                            color = IceCyan,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("ai_chat_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "닫기",
                            tint = FrostSlate,
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
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = IceCyan,
                        modifier = Modifier.size(16.dp),
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
                if (chatState.messages.isEmpty() && !chatState.isLoading && chatState.errorMessage.isNullOrBlank()) {
                    item {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = cardShape,
                            glassOpacity = 0.14f,
                            shadowElevation = 8.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "현재 페이지 URL이 입력창에 준비되었습니다",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureFrost,
                                )
                                Text(
                                    text = "URL 뒤에 원하는 질문이나 프롬프트를 입력한 뒤 전송하거나 바로 전송하여 ${activeProfile.name}와 대화를 시작하세요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrostSlate,
                                )
                            }
                        }
                    }
                }

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
                            .clickable {
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

            // Bottom Input Box (pre-filled with currentUrl, cursor at end)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputFieldValue,
                    onValueChange = { inputFieldValue = it },
                    maxLines = 4,
                    shape = RoundedCornerShape(innerRadius),
                    placeholder = {
                        Text(
                            text = "URL 또는 AI에게 보낼 프롬프트 입력",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostSlate.copy(alpha = 0.72f),
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val clean = inputFieldValue.text.trim()
                            if (clean.isNotEmpty() && !chatState.isLoading) {
                                onSendMessage(clean)
                                inputFieldValue = TextFieldValue("")
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                        focusedLabelColor = IceCyan,
                        unfocusedLabelColor = FrostSlate,
                        cursorColor = IceCyan,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("ai_chat_input"),
                )

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
                        .height(52.dp)
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
