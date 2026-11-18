package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.BrowserCoreSnapshot
import com.example.core.BrowserPreferences
import com.example.ui.theme.FrostSlate
import com.example.ui.theme.IceCyan
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PureFrost
import com.example.ui.theme.unifiedInnerRadius
import kotlin.math.roundToInt

/**
 * Dedicated Korean Settings sub-page for multi-directional Address Bar Gestures.
 */
@Composable
fun GesturesSettingsPage(
    snapshot: BrowserCoreSnapshot,
    unifiedRadius: Dp,
    onUpdateGestureSettings: (
        gesturesEnabled: Boolean?,
        swipeUpAction: Int?,
        swipeDownAction: Int?,
        swipeLeftRightTabSwitch: Boolean?,
        doubleTapAction: Int?,
        gestureSensitivityDp: Float?,
    ) -> Unit,
    onResetGestureSettings: () -> Unit,
) {
    val prefs = snapshot.preferences
    val innerRadius = unifiedInnerRadius(unifiedRadius)

    GestureSwitchCard(
        icon = Icons.Rounded.Gesture,
        title = "주소창 제스처 사용",
        subtitle = "잠금 상태의 중앙 주소창 캡슐에서 스와이프 및 더블 탭 제스처 인식",
        checked = prefs.gesturesEnabled,
        unifiedRadius = unifiedRadius,
        testTag = "toggle_gestures_enabled",
        onCheckedChange = { onUpdateGestureSettings(it, null, null, null, null, null) },
    )

    GestureSwitchCard(
        icon = Icons.Rounded.Swipe,
        title = "좌우 스와이프로 탭 전환",
        subtitle = "주소창을 좌우로 드래그하여 이전/다음 열린 탭으로 빠르게 전환",
        checked = prefs.swipeLeftRightTabSwitch,
        unifiedRadius = unifiedRadius,
        testTag = "toggle_swipe_lr_tabs",
        onCheckedChange = { onUpdateGestureSettings(null, null, null, it, null, null) },
    )

    GestureActionSelectorCard(
        icon = Icons.Rounded.TouchApp,
        title = "위로 스와이프 동작",
        subtitle = "주소창을 위로 밀어 올렸을 때 실행할 동작",
        selectedAction = prefs.swipeUpAction,
        options = listOf(
            BrowserPreferences.GESTURE_OPEN_TABS to "탭 창 열기",
            BrowserPreferences.GESTURE_NEW_TAB to "새 탭",
            BrowserPreferences.GESTURE_RELOAD to "새로고침",
            BrowserPreferences.GESTURE_NONE to "없음",
        ),
        tagPrefix = "gesture_up",
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onSelectAction = { action ->
            onUpdateGestureSettings(null, action, null, null, null, null)
        },
    )

    GestureActionSelectorCard(
        icon = Icons.Rounded.TouchApp,
        title = "아래로 스와이프 동작",
        subtitle = "주소창을 아래로 내렸을 때 실행할 동작",
        selectedAction = prefs.swipeDownAction,
        options = listOf(
            BrowserPreferences.GESTURE_RELOAD to "새로고침",
            BrowserPreferences.GESTURE_NEW_TAB to "새 탭",
            BrowserPreferences.GESTURE_QUICK_MENU to "퀵토글 메뉴",
            BrowserPreferences.GESTURE_NONE to "없음",
        ),
        tagPrefix = "gesture_down",
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onSelectAction = { action ->
            onUpdateGestureSettings(null, null, action, null, null, null)
        },
    )

    GestureActionSelectorCard(
        icon = Icons.Rounded.TouchApp,
        title = "더블 탭 동작",
        subtitle = "잠금 상태의 주소창을 두 번 빠르게 탭했을 때 실행할 동작",
        selectedAction = prefs.doubleTapAction,
        options = listOf(
            BrowserPreferences.GESTURE_SCROLL_TO_TOP to "페이지 최상단 이동",
            BrowserPreferences.GESTURE_BOOKMARK to "북마크",
            BrowserPreferences.GESTURE_RELOAD to "새로고침",
            BrowserPreferences.GESTURE_NONE to "없음",
        ),
        tagPrefix = "gesture_double_tap",
        unifiedRadius = unifiedRadius,
        innerRadius = innerRadius,
        onSelectAction = { action ->
            onUpdateGestureSettings(null, null, null, null, action, null)
        },
    )

    // Swipe sensitivity slider card (20dp..64dp)
    val sliderShape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(sliderShape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), sliderShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "제스처 스와이프 감도",
                style = MaterialTheme.typography.bodyLarge,
                color = PureFrost,
            )
            Text(
                text = "${prefs.gestureSensitivityDp.roundToInt()}dp",
                style = MaterialTheme.typography.labelMedium,
                color = IceCyan,
            )
        }
        Text(
            text = "값이 작을수록 짧은 스와이프에도 제스처가 민감하게 반응합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = FrostSlate,
        )
        Slider(
            value = prefs.gestureSensitivityDp,
            onValueChange = { onUpdateGestureSettings(null, null, null, null, null, it) },
            valueRange = 20f..64f,
            colors = SliderDefaults.colors(
                thumbColor = IceCyan,
                activeTrackColor = IceCyan,
                inactiveTrackColor = ObsidianVoid,
            ),
            modifier = Modifier.testTag("slider_gesture_sensitivity"),
        )
    }

    OutlinedButton(
        onClick = onResetGestureSettings,
        shape = RoundedCornerShape(unifiedRadius),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("reset_gestures_bottom_button"),
    ) {
        Icon(
            imageVector = Icons.Rounded.RestartAlt,
            contentDescription = null,
            tint = IceCyan,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "제스처 설정 기본값 복원",
            style = MaterialTheme.typography.titleMedium,
            color = IceCyan,
        )
    }
}

@Composable
private fun GestureSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    unifiedRadius: Dp,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(unifiedRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ObsidianElevated.copy(alpha = 0.62f))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GestureActionSelectorCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selectedAction: Int,
    options: List<Pair<Int, String>>,
    tagPrefix: String,
    unifiedRadius: Dp,
    innerRadius: Dp,
    onSelectAction: (Int) -> Unit,
) {
    val cardShape = RoundedCornerShape(unifiedRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(ObsidianElevated.copy(alpha = 0.65f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IceCyan,
                modifier = Modifier.size(19.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureFrost,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostSlate,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (actionCode, label) ->
                val selected = selectedAction == actionCode
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
                        .clickable { onSelectAction(actionCode) }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("${tagPrefix}_$actionCode"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PureFrost else FrostSlate,
                    )
                }
            }
        }
    }
}
