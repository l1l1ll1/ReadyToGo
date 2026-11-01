package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AppSettings
import kr.ac.baekseok.readytogo.data.model.AppTheme
import kr.ac.baekseok.readytogo.data.model.TextScale

/**
 * 앱 테마.
 *
 * **다이내믹 컬러와 다크 테마를 쓰지 않는다.** 기기 배경색에 따라 포인트 컬러가 바뀌면
 * "포인트 컬러 하나"라는 디자인 결정이 무너지고, 큰 사진 카드 위의 글자 대비도 기기마다 달라진다.
 * 흰 배경 고정이 이 앱의 분위기다(CLAUDE.md 앱 구조 → 디자인).
 *
 * 설정의 **글자 크기**와 **고대비**가 여기로 들어온다. 화면 코드는 [LocalPalette]와
 * MaterialTheme.typography만 보므로, 설정이 바뀌면 화면을 고치지 않아도 따라 바뀐다.
 */
@Composable
fun ReadytogoTheme(content: @Composable () -> Unit) {
    val settings = appSettings()
    val dark = isAppInDarkTheme()

    // 고대비는 접근성 설정이라 라이트·다크 양쪽에서 유지된다. 그래서 네 벌이 교차한다.
    val palette = when {
        dark && settings.highContrast -> DarkHighContrastPalette
        dark -> DarkPalette
        settings.highContrast -> LightHighContrastPalette
        else -> LightPalette
    }

    // 설정이 바뀔 때만 다시 만든다. 글자 크기를 곱하는 일이 비싸진 않지만
    // 화면을 그릴 때마다 Typography를 통째로 새로 만들 이유는 없다.
    val colorScheme = remember(palette, dark) { colorSchemeOf(palette, dark) }
    val typography = remember(settings.textScale) { Typography.scaledBy(settings.textScale) }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/**
 * 지금 화면을 어둡게 그릴지.
 *
 * **MainActivity도 같은 답이 필요해서 밖으로 꺼냈다.** 상태바 글자 색은 Compose가 아니라
 * 창 설정이라 Activity가 정하는데, 테마와 다른 답을 내면 어두운 배경에 어두운 글자가
 * 남는다. 한 군데서 답을 만들어 둘이 같이 쓴다.
 *
 * 기기 설정은 "시스템 설정 따름"일 때만 본다. 라이트·다크를 직접 고른 사람에게는
 * 기기가 무엇이든 고른 대로 보여야 한다.
 */
@Composable
fun isAppInDarkTheme(): Boolean = when (appSettings().theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
}

/**
 * 지금 설정.
 *
 * 미리보기에서는 AppContainer가 준비되지 않는다. 그때는 기본값으로 그린다 —
 * 한 프로세스 안에서 이 분기가 바뀌지 않아 조건부 호출이 문제되지 않는다.
 */
@Composable
private fun appSettings(): AppSettings = if (AppContainer.isReady) {
    AppContainer.settings.settings.collectAsState().value
} else {
    AppSettings()
}

/**
 * 색 한 벌을 Material이 쓰는 모양으로 옮긴다.
 *
 * 기본값을 그대로 두면 안 되는 자리가 많아 일일이 지정한다 — 아래 주석이 그 이유다.
 *
 * **바탕이 되는 한 벌(light/dark)을 모드에 맞게 고른다.** 우리가 덮지 않은 자리
 * (scrim, surfaceTint, inverseSurface 등)는 그 기본값이 그대로 남는다. 다크에서
 * lightColorScheme을 쓰면 그 자리들만 밝은 색으로 남아 어두운 화면에 흰 띠가 생긴다.
 */
private fun colorSchemeOf(palette: AppPalette, dark: Boolean) = (
    if (dark) darkColorScheme() else lightColorScheme()
    ).copy(
    primary = palette.Point,
    // 버튼 위 글자는 카드색(라이트 흰색). Background는 ⑫부터 민트그레이라 글자가 탁해진다.
    onPrimary = palette.Surface,
    primaryContainer = palette.PointLight,
    onPrimaryContainer = palette.PointDark,

    // secondary는 연노랑 면이다. 그 위 글자는 네이비여야 읽힌다 — 흰 글자는 사라진다.
    secondary = palette.Secondary,
    onSecondary = palette.TextPrimary,
    secondaryContainer = palette.Secondary,
    onSecondaryContainer = palette.TextPrimary,

    // tertiary는 코랄. Material이 "세 번째 강조"로 쓰는 자리에 그대로 맞는다.
    tertiary = palette.Accent,
    onTertiary = palette.Surface,
    tertiaryContainer = palette.Accent,
    onTertiaryContainer = palette.Surface,

    // 스낵바의 동작 버튼("되돌리기")이 inversePrimary를 쓴다. 지정하지 않으면 Material 기본
    // 연보라가 어두운 스낵바 위에 남는다. 어두운 배경에서 읽히는 밝은 청록을 쓴다.
    inversePrimary = palette.PointLight,

    background = palette.Background,
    onBackground = palette.TextPrimary,
    surface = palette.Surface,
    onSurface = palette.TextPrimary,
    surfaceVariant = palette.SurfaceMuted,
    onSurfaceVariant = palette.TextSecondary,

    // Card·AlertDialog·BottomSheet는 surfaceContainer 계열을 기본 배경으로 쓴다.
    // 지정하지 않으면 Material의 기본 보라빛이 그대로 나와 흰 배경 화면에 보라 카드가 올라온다.
    surfaceContainerLowest = palette.Background,
    // ⑫: 카드·시트·대화상자는 흰 카드(Surface)다. 민트그레이 바탕 위에 떠야 하므로.
    surfaceContainerLow = palette.Surface,
    surfaceContainer = palette.Surface,
    surfaceContainerHigh = palette.Surface,
    surfaceContainerHighest = palette.Surface,

    // outline은 **글자 색으로도 쓰인다**(예: "일정 중심에서 1.2km").
    // 선 색인 Divider를 넣었더니 글자가 배경에 묻혀 안 보였다. 읽히는 회색을 따로 둔다.
    outline = palette.TextSecondary,
    outlineVariant = palette.Divider,

    error = palette.Danger,
    onError = palette.Surface,
)

/**
 * 글자 크기 설정을 적용한 Typography.
 *
 * **줄간격(lineHeight)도 같이 곱한다.** 글자만 키우면 줄이 서로 붙어서 오히려 읽기 어렵다.
 *
 * 시스템 글자 크기와 곱해진다는 점에 주의한다. 기기에서 이미 크게 쓰는 사람이 여기서
 * "크게"를 고르면 두 배수가 겹친다 — 그래서 앱 쪽 배수를 1.15까지만 둔다([TextScale]).
 */
private fun androidx.compose.material3.Typography.scaledBy(
    scale: TextScale,
) = if (scale == TextScale.NORMAL) {
    this
} else {
    copy(
        displaySmall = displaySmall.scaledBy(scale.scale),
        titleLarge = titleLarge.scaledBy(scale.scale),
        titleMedium = titleMedium.scaledBy(scale.scale),
        titleSmall = titleSmall.scaledBy(scale.scale),
        bodyLarge = bodyLarge.scaledBy(scale.scale),
        bodyMedium = bodyMedium.scaledBy(scale.scale),
        labelLarge = labelLarge.scaledBy(scale.scale),
        labelMedium = labelMedium.scaledBy(scale.scale),
        labelSmall = labelSmall.scaledBy(scale.scale),
    )
}

private fun androidx.compose.ui.text.TextStyle.scaledBy(factor: Float) = copy(
    fontSize = fontSize * factor,
    lineHeight = lineHeight * factor,
)
