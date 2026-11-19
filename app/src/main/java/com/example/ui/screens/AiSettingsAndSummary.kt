package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.AiCliProfile
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlin.math.roundToInt

@Immutable
data class AiConnectionStatus(
    val profileId: String = "",
    val isChecking: Boolean = false,
    val isConnected: Boolean = false,
    val modelCount: Int = 0,
    val statusMessage: String = "",
    val isError: Boolean = false,
)

@Immutable
data class PageSummaryState(
    val url: String = "",
    val title: String = "",
    val metaDescription: String = "",
    val headings: List<String> = emptyList(),
    val keySentences: List<String> = emptyList(),
    val wordCount: Int = 0,
    val bodyPreviewText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val aiProfileName: String = "",
    val aiModel: String = "",
    val aiResponseText: String = "",
    val aiIsLoading: Boolean = false,
    val aiErrorMessage: String? = null,
)

/**
 * Full-screen edge-to-edge Liquid Glass AI & Page Summary sheet invoked by the "페이지 요약 / AI"
 * quick toggle or toolbar AI action. Uses the active [AiCliProfile] over pure AOSP HttpURLConnection.
 */
@Composable
fun PageSummarySheet(
    summaryState: PageSummaryState,
    unifiedRadius: Dp,
    onDismiss: () -> Unit,
    onAskAiQuestion: (String?) -> Unit,
    onOpenAiSettings: () -> Unit,
) {
    BackHandler(enabled = true) { onDismiss() }

    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val fullScreenBackground = Brush.verticalGradient(
        colors = listOf(ObsidianSurface, ObsidianVoid)
    )
    val cardShape = RoundedCornerShape(unifiedRadius)
    var questionDraft by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fullScreenBackground)
            .testTag("page_summary_sheet"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    IconButton(onClick = onDismiss) {
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
                            text = "페이지 요약 / AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureFrost,
                        )
                        if (summaryState.aiProfileName.isNotBlank()) {
                            Text(
                                text = "${summaryState.aiProfileName} · ${summaryState.aiModel}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = IceCyan,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenAiSettings,
                        modifier = Modifier.testTag("summary_open_ai_settings_button"),
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
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "닫기",
                            tint = FrostSlate,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (summaryState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(ObsidianElevated.copy(alpha = 0.65f))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                color = IceCyan,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "현재 웹 페이지 본문을 추출하고 있습니다...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostSlate,
                            )
                        }
                    }
                } else if (!summaryState.errorMessage.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(ObsidianElevated.copy(alpha = 0.55f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = FrostSlate,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = summaryState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostSlate,
                        )
                    }
                } else {
                    // 1. Live AI CLI Response / Analysis Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(IceCyan.copy(alpha = 0.10f))
                            .border(0.75.dp, IceCyan.copy(alpha = 0.45f), cardShape)
                            .padding(16.dp)
                            .testTag("ai_summary_response_card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = IceCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "AI 분석 결과",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureFrost,
                                )
                            }

                            if (!summaryState.aiIsLoading) {
                                TextButton(onClick = { onAskAiQuestion(null) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = IceCyan,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "다시 분석",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceCyan,
                                    )
                                }
                            }
                        }

                        when {
                            summaryState.aiIsLoading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        color = IceCyan,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                    )
                                    Text(
                                        text = "${summaryState.aiProfileName} (${summaryState.aiModel}) 호출 중...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureFrost,
                                    )
                                }
                            }

                            summaryState.aiResponseText.isNotBlank() -> {
                                Text(
                                    text = summaryState.aiResponseText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureFrost,
                                    modifier = Modifier.testTag("ai_response_body_text"),
                                )
                            }

                            !summaryState.aiErrorMessage.isNullOrBlank() -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = summaryState.aiErrorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AmberWarn,
                                    )
                                    OutlinedButton(
                                        onClick = onOpenAiSettings,
                                        shape = RoundedCornerShape(innerRadius),
                                    ) {
                                        Text(
                                            text = "AI 설정에서 키/엔드포인트 구성",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceCyan,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Ask custom question to active AI CLI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = questionDraft,
                            onValueChange = { questionDraft = it },
                            singleLine = true,
                            shape = RoundedCornerShape(innerRadius),
                            placeholder = {
                                Text(
                                    text = "현재 페이지에 대해 AI에게 질문하기",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrostSlate.copy(alpha = 0.7f),
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (questionDraft.isNotBlank() && !summaryState.aiIsLoading) {
                                        onAskAiQuestion(questionDraft.trim())
                                    }
                                }
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                            colors = aiOutlinedFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_question_input"),
                        )
                        Button(
                            onClick = {
                                if (questionDraft.isNotBlank() && !summaryState.aiIsLoading) {
                                    onAskAiQuestion(questionDraft.trim())
                                }
                            },
                            enabled = questionDraft.isNotBlank() && !summaryState.aiIsLoading,
                            shape = RoundedCornerShape(innerRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IceCyan,
                                contentColor = ObsidianVoid,
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("ai_question_send_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "질문 전송",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // 3. Page metadata & local DOM fallback overview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(ObsidianElevated.copy(alpha = 0.72f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), cardShape)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = summaryState.title.ifBlank { "제목 없음" },
                            style = MaterialTheme.typography.titleMedium,
                            color = PureFrost,
                        )
                        Text(
                            text = summaryState.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyan,
                        )
                        val readMinutes = ((summaryState.wordCount / 220f).coerceAtLeast(1f)).roundToInt()
                        Text(
                            text = "추출 단어 약 ${summaryState.wordCount}개 · 예상 읽기 시간 ${readMinutes}분 (로컬 DOM 추출)",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostSlate,
                        )
                    }

                    if (summaryState.metaDescription.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(ObsidianElevated.copy(alpha = 0.60f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "메타 개요",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan,
                            )
                            Text(
                                text = summaryState.metaDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureFrost,
                            )
                        }
                    }

                    if (summaryState.headings.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(ObsidianElevated.copy(alpha = 0.60f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "주요 섹션 제목",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan,
                            )
                            summaryState.headings.forEach { heading ->
                                Text(
                                    text = "• $heading",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureFrost,
                                )
                            }
                        }
                    }

                    if (summaryState.keySentences.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(ObsidianElevated.copy(alpha = 0.60f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "본문 발췌 요약",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan,
                            )
                            summaryState.keySentences.forEachIndexed { idx, sentence ->
                                Text(
                                    text = "${idx + 1}. $sentence",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureFrost.copy(alpha = 0.92f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Korean "AI 설정" Settings sub-page with Multi-AI CLI selection, custom CLI creation/deletion,
 * and dynamic per-AI model, API key, endpoint URL, temperature, and system prompt controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSettingsPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    connectionStatus: AiConnectionStatus?,
    onSelectAiProfile: (String) -> Unit,
    onTestConnectionAndFetchModels: (AiCliProfile) -> Unit,
    onToggleLongPressAiChat: (Boolean) -> Unit,
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
) {
    val prefs = snapshot.preferences
    val profiles = prefs.aiProfiles
    val activeProfile = prefs.activeAiProfile()
    val innerRadius = unifiedInnerRadius(unifiedRadius)
    val cardShape = RoundedCornerShape(unifiedRadius)

    var showAddForm by remember { mutableStateOf(false) }

    // Gesture integration toggle inside AI Settings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), cardShape)
            .clickable { onToggleLongPressAiChat(!prefs.longPressSwipeUpAiChatEnabled) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("toggle_ai_settings_long_press_chat"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.TouchApp,
            contentDescription = null,
            tint = if (prefs.longPressSwipeUpAiChatEnabled) IceCyan else FrostSlate,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "주소창 롱프레스 후 위로 스와이프 시 AI 대화 실행",
                style = MaterialTheme.typography.bodyLarge,
                color = PureFrost,
            )
            Text(
                text = "주소창을 길게 눌러 진동 후 위로 스와이프하면 현재 URL과 함께 AI 대화창 열기",
                style = MaterialTheme.typography.bodyMedium,
                color = FrostSlate,
            )
        }
        Switch(
            checked = prefs.longPressSwipeUpAiChatEnabled,
            onCheckedChange = onToggleLongPressAiChat,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = IceCyan,
                uncheckedThumbColor = FrostSlate,
                uncheckedTrackColor = ObsidianVoid,
            ),
        )
    }

    // 1. Active AI CLI selector list + "+ AI CLI 추가" button
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "활성 AI CLI 선택 (${profiles.size}개)",
            style = MaterialTheme.typography.titleMedium,
            color = PureFrost,
        )
        OutlinedButton(
            onClick = { showAddForm = !showAddForm },
            shape = RoundedCornerShape(innerRadius),
            modifier = Modifier.testTag("add_custom_ai_cli_button"),
        ) {
            Icon(
                imageVector = if (showAddForm) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (showAddForm) "닫기" else "+ AI CLI 추가",
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }
    }

    // Optional "+ AI CLI 추가" creation card
    AnimatedVisibility(visible = showAddForm) {
        AddCustomAiCliCard(
            unifiedRadius = unifiedRadius,
            innerRadius = innerRadius,
            onCancel = { showAddForm = false },
            onAdd = { name, cliType, endpoint, apiKey, model, temp ->
                onAddCustomAiProfile(name, cliType, endpoint, apiKey, model, temp)
                showAddForm = false
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        profiles.forEach { profile ->
            val isSelected = profile.id == activeProfile.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (isSelected) IceCyan.copy(alpha = 0.16f)
                        else ObsidianElevated.copy(alpha = 0.65f)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        color = if (isSelected) IceCyan else Color.White.copy(alpha = 0.10f),
                        shape = cardShape,
                    )
                    .clickable {
                        onSelectAiProfile(profile.id)
                        onTestConnectionAndFetchModels(profile)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("ai_profile_${profile.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = if (isSelected) IceCyan else FrostSlate,
                    modifier = Modifier.size(20.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureFrost,
                    )
                    val modelLabel = profile.selectedModel.ifBlank { "모델 미선택 (연결 확인 필요)" }
                    Text(
                        text = "모델: $modelLabel · ${cliTypeDisplayName(profile)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) IceCyan else FrostSlate,
                    )
                }

                if (profile.isCustom && profiles.size > 1) {
                    IconButton(
                        onClick = { onDeleteAiProfile(profile.id) },
                        modifier = Modifier.testTag("delete_ai_profile_${profile.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "AI CLI 삭제",
                            tint = CrimsonStop,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    // 2. Dynamic Per-AI Model & Connection Configuration keyed by activeProfile.id
    PerAiModelConfigSection(
        profile = activeProfile,
        connectionStatus = connectionStatus?.takeIf { it.profileId == activeProfile.id },
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onTestConnectionAndFetchModels = onTestConnectionAndFetchModels,
        onUpdateAiProfile = onUpdateAiProfile,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerAiModelConfigSection(
    profile: AiCliProfile,
    connectionStatus: AiConnectionStatus?,
    unifiedRadius: Dp,
    innerRadius: Dp,
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
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    var customModelDraft by remember(profile.id) { mutableStateOf("") }
    var endpointDraft by remember(profile.id, profile.endpointOrCommand) {
        mutableStateOf(profile.endpointOrCommand)
    }
    var apiKeyDraft by remember(profile.id, profile.apiKey) {
        mutableStateOf(profile.apiKey)
    }
    var systemPromptDraft by remember(profile.id, profile.systemPrompt) {
        mutableStateOf(profile.systemPrompt)
    }
    var showApiKey by remember(profile.id) { mutableStateOf(false) }
    var savedBanner by remember(profile.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.72f))
            .border(0.75.dp, IceCyan.copy(alpha = 0.35f), cardShape)
            .padding(14.dp)
            .testTag("per_ai_config_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "${profile.name} 모델 및 연결 구성",
                style = MaterialTheme.typography.titleMedium,
                color = PureFrost,
            )
        }

        // Masked API Key / Token input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = apiKeyDraft,
                onValueChange = {
                    apiKeyDraft = it
                    savedBanner = null
                },
                singleLine = true,
                shape = RoundedCornerShape(innerRadius),
                label = {
                    Text("API 키 / 인증 토큰", style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = FrostSlate,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showApiKey) "키 숨기기" else "키 표시",
                            tint = FrostSlate,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                colors = aiOutlinedFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_api_key_input"),
            )
            OutlinedButton(
                onClick = {
                    val cleanKey = apiKeyDraft.trim()
                    val cleanEndpoint = endpointDraft.trim()
                    onUpdateAiProfile(profile.id, null, cleanEndpoint, cleanKey, null, null, null, null, null)
                    savedBanner = "API 키 저장됨 — 연결 및 모델을 확인합니다."
                    onTestConnectionAndFetchModels(
                        profile.copy(apiKey = cleanKey, endpointOrCommand = cleanEndpoint)
                    )
                },
                shape = RoundedCornerShape(innerRadius),
                modifier = Modifier.testTag("ai_save_key_button"),
            ) {
                Text("저장", style = MaterialTheme.typography.labelMedium, color = IceCyan)
            }
        }

        // Kimi Mode Selector (Kimi API Global / China / Kimi Code / Local Kimi CLI Bridge)
        if (profile.cliType == AiCliProfile.CLI_TYPE_KIMI) {
            val activeKimiSubtype = remember(profile.providerSubtype, endpointDraft) {
                val explicit = profile.providerSubtype.trim().lowercase()
                when {
                    explicit == AiCliProfile.KIMI_SUBTYPE_CODE ||
                        explicit == AiCliProfile.KIMI_SUBTYPE_CHINA ||
                        explicit == AiCliProfile.KIMI_SUBTYPE_LOCAL ||
                        explicit == AiCliProfile.KIMI_SUBTYPE_GLOBAL -> explicit
                    endpointDraft.contains("api.kimi.com/coding", ignoreCase = true) ->
                        AiCliProfile.KIMI_SUBTYPE_CODE
                    endpointDraft.contains("127.0.0.1", ignoreCase = true) ||
                        endpointDraft.contains("localhost", ignoreCase = true) ->
                        AiCliProfile.KIMI_SUBTYPE_LOCAL
                    endpointDraft.contains("moonshot.cn", ignoreCase = true) ->
                        AiCliProfile.KIMI_SUBTYPE_CHINA
                    else -> AiCliProfile.KIMI_SUBTYPE_GLOBAL
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Kimi 모드 및 엔드포인트 선택",
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostSlate,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple(
                            AiCliProfile.KIMI_SUBTYPE_GLOBAL,
                            BrowserPreferences.KIMI_GLOBAL_ENDPOINT,
                            "키미 API (글로벌)",
                        ),
                        Triple(
                            AiCliProfile.KIMI_SUBTYPE_CHINA,
                            BrowserPreferences.KIMI_CHINA_ENDPOINT,
                            "키미 API (중국)",
                        ),
                        Triple(
                            AiCliProfile.KIMI_SUBTYPE_CODE,
                            BrowserPreferences.KIMI_CODE_ENDPOINT,
                            "키미 코드 (Kimi Code)",
                        ),
                        Triple(
                            AiCliProfile.KIMI_SUBTYPE_LOCAL,
                            BrowserPreferences.KIMI_LOCAL_CLI_ENDPOINT,
                            "로컬 Kimi CLI 브릿지",
                        ),
                    ).forEach { (subtype, url, label) ->
                        val activeChip = activeKimiSubtype == subtype
                        val chipShape = RoundedCornerShape(innerRadius)
                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(
                                    if (activeChip) IceCyan.copy(alpha = 0.22f)
                                    else ObsidianVoid.copy(alpha = 0.65f)
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (activeChip) IceCyan else Color.White.copy(alpha = 0.12f),
                                    shape = chipShape,
                                )
                                .clickable {
                                    endpointDraft = url
                                    val cleanKey = apiKeyDraft.trim()
                                    onUpdateAiProfile(
                                        profile.id,
                                        null,
                                        url,
                                        cleanKey,
                                        null,
                                        null,
                                        null,
                                        null,
                                        subtype,
                                    )
                                    savedBanner = "Kimi 모드가 '$label'(으)로 변경되었습니다."
                                    onTestConnectionAndFetchModels(
                                        profile.copy(
                                            providerSubtype = subtype,
                                            endpointOrCommand = url,
                                            apiKey = cleanKey,
                                        )
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("kimi_mode_chip_$subtype"),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeChip) PureFrost else FrostSlate,
                            )
                        }
                    }
                }
                val kimiModeHint = when (activeKimiSubtype) {
                    AiCliProfile.KIMI_SUBTYPE_CODE ->
                        "Kimi Code 모드: User-Agent: KimiCLI/1.0 헤더가 자동 전송되며 api.kimi.com/coding/v1을 사용합니다."
                    AiCliProfile.KIMI_SUBTYPE_LOCAL ->
                        "로컬 CLI 브릿지 모드: API 키 없이 127.0.0.1 로컬 포트 주소로 연결할 수 있습니다."
                    AiCliProfile.KIMI_SUBTYPE_CHINA ->
                        "키미 API (중국): api.moonshot.cn/v1 엔드포인트를 사용합니다."
                    else ->
                        "키미 API (글로벌): api.moonshot.ai/v1 엔드포인트를 사용합니다."
                }
                Text(
                    text = kimiModeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = IceCyan.copy(alpha = 0.85f),
                )
            }
        }

        // Endpoint / CLI Bridge URL input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = {
                    endpointDraft = it
                    savedBanner = null
                },
                singleLine = true,
                shape = RoundedCornerShape(innerRadius),
                label = {
                    Text("엔드포인트 / CLI 브릿지 URL", style = MaterialTheme.typography.labelSmall)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = FrostSlate,
                        modifier = Modifier.size(18.dp),
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                colors = aiOutlinedFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_endpoint_input"),
            )
            OutlinedButton(
                onClick = {
                    val cleanEndpoint = endpointDraft.trim()
                    val cleanKey = apiKeyDraft.trim()
                    onUpdateAiProfile(profile.id, null, cleanEndpoint, cleanKey, null, null, null, null, null)
                    savedBanner = "엔드포인트 저장됨 — 연결 및 모델을 확인합니다."
                    onTestConnectionAndFetchModels(
                        profile.copy(endpointOrCommand = cleanEndpoint, apiKey = cleanKey)
                    )
                },
                shape = RoundedCornerShape(innerRadius),
                modifier = Modifier.testTag("ai_save_endpoint_button"),
            ) {
                Text("저장", style = MaterialTheme.typography.labelMedium, color = IceCyan)
            }
        }

        // Connection Verification & Dynamic Model Fetch Button + Live Status
        Button(
            onClick = {
                val cleanEndpoint = endpointDraft.trim()
                val cleanKey = apiKeyDraft.trim()
                onUpdateAiProfile(profile.id, null, cleanEndpoint, cleanKey, null, null, null, null, null)
                onTestConnectionAndFetchModels(
                    profile.copy(endpointOrCommand = cleanEndpoint, apiKey = cleanKey)
                )
            },
            enabled = connectionStatus?.isChecking != true,
            shape = RoundedCornerShape(innerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = IceCyan.copy(alpha = 0.20f),
                contentColor = IceCyan,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(0.75.dp, IceCyan.copy(alpha = 0.55f), RoundedCornerShape(innerRadius))
                .testTag("ai_fetch_models_button"),
        ) {
            if (connectionStatus?.isChecking == true) {
                CircularProgressIndicator(
                    color = IceCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("연결 확인 및 모델 불러오는 중...", style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(
                    imageVector = Icons.Rounded.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("연결 확인 및 모델 불러오기", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (connectionStatus != null && connectionStatus.statusMessage.isNotBlank()) {
            val statusColor = when {
                connectionStatus.isChecking -> IceCyan
                connectionStatus.isConnected -> EmeraldSecure
                connectionStatus.isError -> AmberWarn
                else -> FrostSlate
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(innerRadius))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(0.5.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(innerRadius))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("ai_connection_status_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = connectionStatus.statusMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Dynamic Model Selection Chips
        Text(
            text = "사용할 모델 선택 (${profile.availableModels.size}개)",
            style = MaterialTheme.typography.labelMedium,
            color = FrostSlate,
        )
        if (profile.availableModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(innerRadius))
                    .background(ObsidianVoid.copy(alpha = 0.55f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(innerRadius))
                    .padding(12.dp)
                    .testTag("ai_models_empty_hint"),
            ) {
                Text(
                    text = "불러온 모델 목록이 없습니다. 상단에서 API 키 또는 로컬 주소를 저장하고 '연결 확인 및 모델 불러오기'를 누르거나 아래에 직접 모델명을 입력하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostSlate,
                )
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                profile.availableModels.forEach { modelName ->
                    val selected = modelName == profile.selectedModel
                    val chipShape = RoundedCornerShape(innerRadius)
                    Row(
                        modifier = Modifier
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
                            .clickable {
                                onUpdateAiProfile(profile.id, null, null, null, modelName, null, null, null, null)
                                savedBanner = "모델이 '$modelName'(으)로 변경되었습니다."
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("ai_model_chip_$modelName"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) PureFrost else FrostSlate,
                        )
                    }
                }
            }
        }

        // Custom model input (fallback / manual override)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = customModelDraft,
                onValueChange = { customModelDraft = it },
                singleLine = true,
                shape = RoundedCornerShape(innerRadius),
                label = {
                    Text("직접 모델명 추가", style = MaterialTheme.typography.labelSmall)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                colors = aiOutlinedFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_custom_model_input"),
            )
            OutlinedButton(
                onClick = {
                    val trimmed = customModelDraft.trim()
                    if (trimmed.isNotEmpty()) {
                        onUpdateAiProfile(profile.id, null, null, null, null, trimmed, null, null, null)
                        customModelDraft = ""
                        savedBanner = "모델 '$trimmed'이(가) 추가 및 선택되었습니다."
                    }
                },
                shape = RoundedCornerShape(innerRadius),
                modifier = Modifier.testTag("ai_add_custom_model_button"),
            ) {
                Text("적용", style = MaterialTheme.typography.labelMedium, color = IceCyan)
            }
        }

        // Temperature slider (0.0f..1.5f)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "창의성 온도 (Temperature)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureFrost,
                )
                Text(
                    text = "${(profile.temperature * 100f).roundToInt() / 100f}",
                    style = MaterialTheme.typography.labelMedium,
                    color = IceCyan,
                )
            }
            Slider(
                value = profile.temperature,
                onValueChange = {
                    onUpdateAiProfile(profile.id, null, null, null, null, null, it, null, null)
                },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = IceCyan,
                    activeTrackColor = IceCyan,
                    inactiveTrackColor = ObsidianVoid,
                ),
                modifier = Modifier.testTag("slider_ai_temperature"),
            )
        }

        // System Prompt customization
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = systemPromptDraft,
                onValueChange = {
                    systemPromptDraft = it
                    savedBanner = null
                },
                maxLines = 3,
                shape = RoundedCornerShape(innerRadius),
                label = {
                    Text("시스템 프롬프트", style = MaterialTheme.typography.labelSmall)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
                colors = aiOutlinedFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_system_prompt_input"),
            )
            OutlinedButton(
                onClick = {
                    onUpdateAiProfile(profile.id, null, null, null, null, null, null, systemPromptDraft, null)
                    savedBanner = "시스템 프롬프트가 저장되었습니다."
                },
                shape = RoundedCornerShape(innerRadius),
            ) {
                Text("저장", style = MaterialTheme.typography.labelMedium, color = IceCyan)
            }
        }

        AnimatedVisibility(visible = savedBanner != null) {
            Text(
                text = savedBanner.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldSecure,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomAiCliCard(
    unifiedRadius: Dp,
    innerRadius: Dp,
    onCancel: () -> Unit,
    onAdd: (
        name: String,
        cliType: String,
        endpointOrCommand: String,
        apiKey: String,
        selectedModel: String,
        temperature: Float,
    ) -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    var name by remember { mutableStateOf("") }
    var cliType by remember { mutableStateOf(AiCliProfile.CLI_TYPE_CUSTOM) }
    var endpoint by remember { mutableStateOf("http://127.0.0.1:8080/v1") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var temperature by remember { mutableFloatStateOf(0.4f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(IceCyan.copy(alpha = 0.08f))
            .border(0.75.dp, IceCyan.copy(alpha = 0.50f), cardShape)
            .padding(14.dp)
            .testTag("add_custom_ai_cli_form"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "새 사용자 지정 AI CLI 등록",
            style = MaterialTheme.typography.titleMedium,
            color = PureFrost,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                AiCliProfile.CLI_TYPE_CUSTOM to "OpenAI 호환 / 브릿지",
                AiCliProfile.CLI_TYPE_KIMI to "Kimi (Moonshot)",
                AiCliProfile.CLI_TYPE_GEMINI to "Gemini",
                AiCliProfile.CLI_TYPE_CLAUDE to "Claude",
                AiCliProfile.CLI_TYPE_OLLAMA to "Ollama",
            ).forEach { (typeId, label) ->
                val selected = cliType == typeId
                val chipShape = RoundedCornerShape(innerRadius)
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (selected) IceCyan.copy(alpha = 0.24f) else ObsidianVoid.copy(alpha = 0.65f))
                        .border(
                            width = 0.75.dp,
                            color = if (selected) IceCyan else Color.White.copy(alpha = 0.12f),
                            shape = chipShape,
                        )
                        .clickable { cliType = typeId }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) PureFrost else FrostSlate,
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            shape = RoundedCornerShape(innerRadius),
            label = { Text("AI CLI 이름 (예: 로컬 vLLM CLI)", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
            colors = aiOutlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_ai_name_input"),
        )

        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            singleLine = true,
            shape = RoundedCornerShape(innerRadius),
            label = { Text("엔드포인트 / CLI 브릿지 URL", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
            colors = aiOutlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_ai_endpoint_input"),
        )

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            singleLine = true,
            shape = RoundedCornerShape(innerRadius),
            label = { Text("기본 모델명 (비워두면 연결 후 자동 조회)", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
            colors = aiOutlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_ai_model_input"),
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            singleLine = true,
            shape = RoundedCornerShape(innerRadius),
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("API 키 / 토큰 (선택)", style = MaterialTheme.typography.labelSmall) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureFrost),
            colors = aiOutlinedFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_ai_key_input"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(innerRadius),
                modifier = Modifier.weight(1f),
            ) {
                Text("취소", style = MaterialTheme.typography.labelMedium, color = FrostSlate)
            }
            Button(
                onClick = {
                    onAdd(
                        name.ifBlank { "사용자 지정 AI CLI" },
                        cliType,
                        endpoint,
                        apiKey,
                        model.trim(),
                        temperature,
                    )
                },
                shape = RoundedCornerShape(innerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IceCyan,
                    contentColor = ObsidianVoid,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("confirm_add_ai_cli_button"),
            ) {
                Text("추가 및 활성화", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun aiOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IceCyan,
    unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
    focusedLabelColor = IceCyan,
    unfocusedLabelColor = FrostSlate,
    cursorColor = IceCyan,
)

private fun cliTypeDisplayName(profile: AiCliProfile): String =
    if (profile.cliType == AiCliProfile.CLI_TYPE_KIMI) {
        when (profile.providerSubtype.lowercase()) {
            AiCliProfile.KIMI_SUBTYPE_CODE -> "키미 코드 (Kimi Code)"
            AiCliProfile.KIMI_SUBTYPE_CHINA -> "키미 API (중국)"
            AiCliProfile.KIMI_SUBTYPE_LOCAL -> "로컬 Kimi CLI 브릿지"
            else -> "키미 API (글로벌)"
        }
    } else {
        cliTypeDisplayName(profile.cliType)
    }

private fun cliTypeDisplayName(cliType: String): String =
    when (cliType) {
        AiCliProfile.CLI_TYPE_GEMINI -> "Gemini API"
        AiCliProfile.CLI_TYPE_CLAUDE -> "Claude API"
        AiCliProfile.CLI_TYPE_OPENAI -> "OpenAI / Codex"
        AiCliProfile.CLI_TYPE_KIMI -> "Kimi (Moonshot AI)"
        AiCliProfile.CLI_TYPE_OLLAMA -> "Ollama 로컬"
        else -> "사용자 지정 REST"
    }
