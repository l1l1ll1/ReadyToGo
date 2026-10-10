package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AppSettings
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
    // 미리보기에서는 AppContainer가 준비되지 않는다. 그때는 기본 설정으로 그린다.
    // 한 프로세스 안에서 이 값이 바뀌지는 않아 조건부 호출이 문제되지 않는다.
    val settings = if (AppContainer.isReady) {
        AppContainer.settings.settings.collectAsState().value
    } else {
        AppSettings()
    }

    val palette = if (settings.highContrast) HighContrastPalette else LightPalette

    // 설정이 바뀔 때만 다시 만든다. 글자 크기를 곱하는 일이 비싸진 않지만
    // 화면을 그릴 때마다 Typography를 통째로 새로 만들 이유는 없다.
    val colorScheme = remember(palette) { colorSchemeOf(palette) }
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
 * 색 한 벌을 Material이 쓰는 모양으로 옮긴다.
 *
 * 기본값을 그대로 두면 안 되는 자리가 많아 일일이 지정한다 — 아래 주석이 그 이유다.
 */
private fun colorSchemeOf(palette: AppPalette) = lightColorScheme(
    primary = palette.Point,
    onPrimary = palette.Background,
    primaryContainer = palette.PointLight,
    onPrimaryContainer = palette.PointDark,

    // 포인트 컬러 하나로 끌고 가므로 secondary·tertiary도 같은 계열로 맞춘다.
    secondary = palette.PointDark,
    onSecondary = palette.Background,
    secondaryContainer = palette.PointLight,
    onSecondaryContainer = palette.PointDark,
    tertiary = palette.Point,
    onTertiary = palette.Background,

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
    surfaceContainerLow = palette.SurfaceMuted,
    surfaceContainer = palette.SurfaceMuted,
    surfaceContainerHigh = palette.SurfaceMuted,
    surfaceContainerHighest = palette.SurfaceMuted,

    // outline은 **글자 색으로도 쓰인다**(예: "일정 중심에서 1.2km").
    // 선 색인 Divider를 넣었더니 글자가 배경에 묻혀 안 보였다. 읽히는 회색을 따로 둔다.
    outline = palette.TextSecondary,
    outlineVariant = palette.Divider,

    error = palette.Danger,
    onError = palette.Background,
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
