package kr.ac.baekseok.readytogo.ui.plan

import kr.ac.baekseok.readytogo.ui.theme.pressClickable
import kr.ac.baekseok.readytogo.ui.theme.pressScale
import kr.ac.baekseok.readytogo.ui.theme.motionTween
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import kr.ac.baekseok.readytogo.ui.component.FitText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.domain.weather.DailyWeather
import kr.ac.baekseok.readytogo.domain.weather.Sky
import kr.ac.baekseok.readytogo.ui.theme.LocalPalette
import kr.ac.baekseok.readytogo.ui.theme.Tokens

/**
 * 결과 화면 맨 위(design/mockup/timeline·summary). 왼쪽 둥근 뒤로, 가운데 여행 제목과 기간,
 * 오른쪽 둥근 공유·저장. Material TopAppBar 대신 직접 그리는 이유: 시안의 흰 원 버튼과 두 줄 제목을
 * TopAppBar 슬롯으로 맞추면 높이·정렬을 매번 덮어써야 한다.
 *
 * @param actionsVisible 계획이 완성되기 전에는 공유할 내용도 저장할 내용도 없다.
 */
@Composable
fun PlanTopBar(
    title: String,
    subtitle: String?,
    saved: Boolean,
    actionsVisible: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleSaved: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Tokens.Space.screenHorizontal, vertical = Tokens.Space.md),
    ) {
        RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.common_back), onBack)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            FitText(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalPalette.current.TextSecondary,
                    maxLines = 1,
                )
            }
        }
        if (actionsVisible) {
            RoundIconButton(Icons.Outlined.Share, stringResource(R.string.share_trip), onShare)
            RoundIconButton(
                icon = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                // 눌렀을 때 일어날 일을 읽어 준다(저장됨 → "저장 해제").
                description = stringResource(if (saved) R.string.plan_unsave else R.string.plan_save),
                onClick = onToggleSaved,
            )
        } else {
            // 버튼이 없어도 제목이 가운데에 오도록 자리를 비워 둔다.
            Box(Modifier.width(Tokens.Size.roundButton * 2 + Tokens.Space.sm))
        }
    }
}

/** 흰 원 안의 아이콘 버튼(시안의 뒤로·공유·저장, 시트 닫기). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Tokens.Size.roundButton)
            .clip(CircleShape)
            .background(LocalPalette.current.Surface)
            .pressClickable(role = Role.Button, onClickLabel = description, onClick = onClick),
    ) {
        Icon(icon, contentDescription = description, tint = LocalPalette.current.TextPrimary)
    }
}

/**
 * 결과 화면 탭(design/mockup/summary) — 흰 pill 묶음 안에서 고른 칸만 짙은 남색으로 채운다.
 * 다섯 칸이 한 줄에 들어가도록 칸 폭을 똑같이 나눈다.
 */
@Composable
fun PlanSegmentedTabs(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val palette = LocalPalette.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Tokens.Space.screenHorizontal)
            .clip(Tokens.Radius.chip)
            .background(palette.Surface)
            .padding(Tokens.Space.xs),
    ) {
        val slot = maxWidth / labels.size
        // 남색 칸이 탭 사이를 미끄러진다(⑫). 하단 탭 바와 같은 방식 — 칸 하나를 옮긴다.
        val pillOffset by animateDpAsState(
            targetValue = slot * selectedIndex,
            animationSpec = motionTween(Tokens.Motion.mediumMs),
            label = "segmentPill",
        )
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(slot)
                .height(Tokens.Size.segmentHeight)
                .clip(Tokens.Radius.chip)
                .background(palette.TextPrimary),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                // 글자 색도 칸이 지나가는 동안 함께 바뀐다. 즉시 바꾸면 남색이 도착하기 전에 흰 글자가 흰 바탕에 묻힌다.
                val textColor by animateColorAsState(
                    targetValue = if (selected) palette.Surface else palette.TextSecondary,
                    animationSpec = motionTween(Tokens.Motion.mediumMs),
                    label = "segmentText",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(Tokens.Size.segmentHeight)
                        .clip(Tokens.Radius.chip)
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Tab) {
                            onSelect(index)
                        },
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1)
                }
            }
        }
    }
}

/**
 * 날짜 칩 줄(design/mockup/timeline). 둥근 네모 칩에 "N일"과 그날 날씨 아이콘, 고른 날은 남색.
 * 날짜가 많으면 가로로 밀린다.
 */
@Composable
fun DayChipRow(
    dayNumbers: List<Int>,
    selectedDay: Int?,
    weatherOf: (Int) -> DailyWeather?,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sm),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Tokens.Space.screenHorizontal),
    ) {
        dayNumbers.forEach { dayNumber ->
            val selected = dayNumber == selectedDay
            val weather = weatherOf(dayNumber)
            val tint = if (selected) palette.Surface else palette.TextSecondary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.xs, Alignment.CenterVertically),
                modifier = Modifier
                    .width(Tokens.Size.dayChipWidth)
                    .height(Tokens.Size.dayChipHeight)
                    .clip(Tokens.Radius.card)
                    .background(if (selected) palette.TextPrimary else palette.Surface)
                    .pressClickable(role = Role.Tab) { onSelect(dayNumber) },
            ) {
                Text(
                    text = stringResource(R.string.plan_day_chip, dayNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
                if (weather != null) {
                    val label = stringResource(weather.sky.labelRes())
                    Icon(
                        imageVector = weather.sky.icon(),
                        contentDescription = weather.precipitationProbability?.let { "$label $it%" } ?: label,
                        tint = tint,
                        modifier = Modifier.size(Tokens.Size.inlineIcon),
                    )
                }
            }
        }
    }
}

internal fun Sky.icon(): ImageVector = when (this) {
    Sky.CLEAR -> Icons.Outlined.WbSunny
    Sky.CLOUDY -> Icons.Outlined.Cloud
    Sky.RAIN -> Icons.Outlined.WaterDrop
    Sky.SNOW -> Icons.Outlined.AcUnit
}

internal fun Sky.labelRes(): Int = when (this) {
    Sky.CLEAR -> R.string.weather_clear
    Sky.CLOUDY -> R.string.weather_cloudy
    Sky.RAIN -> R.string.weather_rain
    Sky.SNOW -> R.string.weather_snow
}
