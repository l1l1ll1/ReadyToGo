package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 앱 테마.
 *
 * **다이내믹 컬러와 다크 테마를 쓰지 않는다.** 기기 배경색에 따라 포인트 컬러가 바뀌면
 * "포인트 컬러 하나"라는 디자인 결정이 무너지고, 큰 사진 카드 위의 글자 대비도 기기마다 달라진다.
 * 흰 배경 고정이 이 앱의 분위기다(CLAUDE.md 앱 구조 → 디자인).
 *
 * 고대비 모드는 8단계 3번에서 접근성 설정으로 따로 넣는다.
 */
private val LightColors = lightColorScheme(
    primary = Tokens.Palette.Point,
    onPrimary = Tokens.Palette.Background,
    primaryContainer = Tokens.Palette.PointLight,
    onPrimaryContainer = Tokens.Palette.PointDark,

    // 포인트 컬러 하나로 끌고 가므로 secondary·tertiary도 같은 계열로 맞춘다.
    secondary = Tokens.Palette.PointDark,
    onSecondary = Tokens.Palette.Background,
    secondaryContainer = Tokens.Palette.PointLight,
    onSecondaryContainer = Tokens.Palette.PointDark,
    tertiary = Tokens.Palette.Point,
    onTertiary = Tokens.Palette.Background,

    // 스낵바의 동작 버튼("되돌리기")이 inversePrimary를 쓴다. 지정하지 않으면 Material 기본
    // 연보라가 어두운 스낵바 위에 남는다. 어두운 배경에서 읽히는 밝은 청록을 쓴다.
    inversePrimary = Tokens.Palette.PointLight,

    background = Tokens.Palette.Background,
    onBackground = Tokens.Palette.TextPrimary,
    surface = Tokens.Palette.Surface,
    onSurface = Tokens.Palette.TextPrimary,
    surfaceVariant = Tokens.Palette.SurfaceMuted,
    onSurfaceVariant = Tokens.Palette.TextSecondary,

    // Card·AlertDialog·BottomSheet는 surfaceContainer 계열을 기본 배경으로 쓴다.
    // 지정하지 않으면 Material의 기본 보라빛이 그대로 나와 흰 배경 화면에 보라 카드가 올라온다.
    surfaceContainerLowest = Tokens.Palette.Background,
    surfaceContainerLow = Tokens.Palette.SurfaceMuted,
    surfaceContainer = Tokens.Palette.SurfaceMuted,
    surfaceContainerHigh = Tokens.Palette.SurfaceMuted,
    surfaceContainerHighest = Tokens.Palette.SurfaceMuted,

    // outline은 **글자 색으로도 쓰인다**(예: "일정 중심에서 1.2km").
    // 선 색인 Divider를 넣었더니 글자가 배경에 묻혀 안 보였다. 읽히는 회색을 따로 둔다.
    outline = Tokens.Palette.TextSecondary,
    outlineVariant = Tokens.Palette.Divider,

    error = Tokens.Palette.Danger,
    onError = Tokens.Palette.Background,
)

@Composable
fun ReadytogoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
