package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 화면이 쓰는 색 한 벌.
 *
 * **상수가 아니라 값으로 들고 있는 이유**: 고대비 설정을 켜면 같은 자리에 다른 색이 들어가야
 * 한다. 전역 상수를 바꿔치기하면 Compose가 바뀐 줄 모르고 화면을 다시 그리지 않는다.
 * 한 벌을 통째로 [LocalPalette]로 내려보내면 바뀔 때 알아서 다시 그려진다.
 *
 * 다크 모드도 같은 구조로 한 벌 더 만들면 된다 — 화면 코드는 건드릴 것이 없다.
 */
data class AppPalette(
    val Point: Color,
    val PointDark: Color,
    val PointLight: Color,
    val Background: Color,
    val Surface: Color,
    val SurfaceMuted: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Divider: Color,
    val Danger: Color,
    val Warning: Color,
    val Heart: Color,
)

/**
 * 기본 한 벌.
 *
 * 포인트 컬러 하나로만 끌고 간다. 여행 앱 느낌의 청록이고, 흰 배경에서 대비가 충분하다.
 * 에어비앤비류 분위기를 참고하되 그쪽 코랄 계열은 쓰지 않는다 — 브랜드 색 모방은 금지다.
 */
val LightPalette = AppPalette(
    Point = Color(0xFF0F766E),
    PointDark = Color(0xFF0B5A54),
    PointLight = Color(0xFFCCFBF1),

    // 배경은 흰색. 카드는 아주 옅은 회색으로 띄워 경계선 없이 구분한다.
    Background = Color(0xFFFFFFFF),
    Surface = Color(0xFFFFFFFF),
    SurfaceMuted = Color(0xFFF4F5F7),

    TextPrimary = Color(0xFF1A1C1E),
    TextSecondary = Color(0xFF5F6368),

    // 선은 거의 안 보일 정도로만. 큰 사진 카드가 주인공이라 선이 끼면 지저분해진다.
    Divider = Color(0xFFE8EAED),

    Danger = Color(0xFFB3261E),
    Warning = Color(0xFFB26B00),

    // 즐겨찾기 ♥ 채운 상태. 포인트 컬러와 구분돼야 눌린 게 보인다.
    Heart = Color(0xFFE0245E),
)

/**
 * 고대비 한 벌.
 *
 * **흰 배경은 그대로 둔다.** 배경까지 검게 뒤집으면 큰 사진 카드 위 글자 대비가 다시 무너지고,
 * 이 앱의 분위기가 아니게 된다(CLAUDE.md 디자인). 대신 **흐린 것들을 진하게** 만든다:
 * 보조 글자, 선, 포인트 컬러가 대상이다.
 *
 * 특히 보조 글자(#5F6368)가 문제였다. 흰 배경 위 대비가 약 5.9:1로 본문 기준(4.5:1)은
 * 넘지만 작은 글씨(13sp)에서 읽기 어렵다. 거의 검정에 가깝게 내려 11:1 이상으로 올린다.
 */
val HighContrastPalette = LightPalette.copy(
    Point = Color(0xFF0A4F49),
    PointDark = Color(0xFF063B36),
    PointLight = Color(0xFFB7F0E4),

    SurfaceMuted = Color(0xFFE9ECEF),

    TextPrimary = Color(0xFF000000),
    TextSecondary = Color(0xFF26292C),

    // 선이 보여야 섹션 경계가 읽힌다. 평소에는 일부러 안 보이게 두는 값이다.
    Divider = Color(0xFF9AA0A6),

    Danger = Color(0xFF8C1D18),
    Warning = Color(0xFF6E4200),
    Heart = Color(0xFFB0124C),
)

/**
 * 지금 화면이 쓰는 색 한 벌.
 *
 * `staticCompositionLocalOf`를 쓰는 이유: 색이 바뀌는 일은 설정에서 고대비를 켤 때뿐이라
 * 드물다. 그때는 화면 전체를 다시 그리는 편이 싸고, 읽을 때마다 구독을 거는 비용이 없다.
 */
val LocalPalette = staticCompositionLocalOf { LightPalette }
