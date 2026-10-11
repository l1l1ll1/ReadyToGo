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
    /** 터쿼이즈. 버튼, 탭 선택, 링크, 진행바. */
    val Point: Color,
    val PointDark: Color,
    val PointLight: Color,

    /**
     * 선샤인 옐로. 칩 배경, 강조 카드 배경, 섹션을 나누는 연한 면.
     *
     * **글자 색으로 쓰지 않는다.** 흰 배경에서 대비가 1.2:1밖에 안 돼 아무것도 읽히지 않는다.
     * 바탕으로만 깔고 그 위에는 [TextPrimary]를 얹는다.
     */
    val Secondary: Color,

    /**
     * 연노랑을 **작은 면으로 칠할 때** 쓰는 진한 쪽.
     *
     * [Secondary]는 칩이나 카드처럼 넓은 바탕에 맞춰 연하게 잡혀 있다. 배분 막대의
     * 한 칸이나 범례 점처럼 면이 작아지면 회색 카드 위에서 거의 안 보인다.
     * 글자를 얹는 자리가 아니므로 대비 대신 **눈에 띄는지**만 보고 고른 값이다.
     */
    val SecondaryBold: Color,

    /**
     * 코랄 오렌지. FAB, 주요 동작 버튼, D-day 배지, 예산 초과 표시.
     *
     * [Point]와 역할이 다르다 — 터쿼이즈는 "지금 고른 것/갈 수 있는 곳"이고,
     * 코랄은 "지금 눌러야 할 것/눈에 걸려야 할 것"이다. 한 화면에 코랄은 하나만 둔다.
     */
    val Accent: Color,

    /**
     * [Accent] 면 위에 얹는 글자·아이콘 색.
     *
     * **[Background]를 쓰면 안 된다.** 라이트에서는 둘 다 흰색이라 같아 보이지만,
     * 다크에서는 Background가 어두워져 코랄 FAB 위 글자가 읽히지 않는다.
     * 코랄은 라이트·다크 어느 쪽에서도 충분히 진해서 이 값은 양쪽 모두 흰색이다.
     */
    val OnAccent: Color,

    val Background: Color,
    val Surface: Color,
    val SurfaceMuted: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Divider: Color,
    val Danger: Color,
    val Warning: Color,
    val Heart: Color,

    /**
     * 큰 면에 까는 그라데이션. 홈의 "다가오는 여행" 카드처럼 한 화면에 한 곳만 쓴다.
     *
     * 작은 요소(버튼, 칩)에 쓰지 않는 이유: 면이 작으면 두 색이 구분되지 않아
     * 그냥 터쿼이즈 한 색이 되고, 포인트 컬러와 겹쳐 보인다.
     */
    val HeroGradient: List<Color>,

    /**
     * 끝난 일정에 까는 회색 그라데이션. 여행 탭의 "지난 여행" 카드가 쓴다.
     *
     * 색을 빼는 이유는 순서가 아니라 무게다 — 지난 여행이 다가오는 여행과 같은 색으로
     * 서 있으면 목록에서 무엇이 먼저인지 읽히지 않는다. 흰 글자가 얹히므로 밝게 두지 않는다.
     */
    val MutedGradient: List<Color>,
)

/**
 * 기본 한 벌.
 *
 * 터쿼이즈(주)·선샤인 옐로(보조)·코랄(강조) 셋으로 간다. 셋 다 흰 배경 위에서 쓰고,
 * 글자는 딥 네이비로 받는다 — 순검정보다 부드럽고 파랑 계열과 같은 가족으로 읽힌다.
 */
val LightPalette = AppPalette(
    Point = Color(0xFF20B8C4),
    PointDark = Color(0xFF178F99),
    PointLight = Color(0xFFD6F3F5),

    Secondary = Color(0xFFFFF0B3),
    SecondaryBold = Color(0xFFFFD75E),
    Accent = Color(0xFFFF805E),
    OnAccent = Color(0xFFFFFFFF),

    Background = Color(0xFFFFFFFF),
    Surface = Color(0xFFFFFFFF),
    // 카드는 아주 옅은 회색으로 띄워 경계선 없이 구분한다.
    SurfaceMuted = Color(0xFFF4F6F8),

    TextPrimary = Color(0xFF233B5D),
    // 보조 글자는 같은 네이비의 60%다. 별도 회색을 쓰면 두 색 계열이 섞여 탁해진다.
    TextSecondary = Color(0xFF233B5D).copy(alpha = 0.6f),

    // 선은 거의 안 보일 정도로만. 큰 사진 카드가 주인공이라 선이 끼면 지저분해진다.
    Divider = Color(0xFFE4E8EC),

    // 삭제·탈퇴처럼 되돌릴 수 없는 일에만 쓴다. 예산 초과 같은 "주의"는 Accent 쪽이다.
    Danger = Color(0xFFD03A2C),
    Warning = Color(0xFFB26B00),

    // 즐겨찾기 ♥ 채운 상태. 코랄과 구분돼야 눌린 게 보인다.
    Heart = Color(0xFFE0245E),

    // 포인트색 한 가지의 밝기 차이로만 만든다(Point → PointDark).
    // 밝은 하늘색으로 번지게 두면 카드가 흰 배경에 녹아 면이 안 보였다.
    HeroGradient = listOf(
        Color(0xFF20B8C4),
        Color(0xFF178F99),
    ),

    MutedGradient = listOf(
        Color(0xFF97A3B0),
        Color(0xFF6E7B8A),
    ),
)

/**
 * 라이트 + 고대비 한 벌.
 *
 * **흰 배경은 그대로 둔다.** 배경까지 검게 뒤집으면 큰 카드 위 글자 대비가 다시 무너지고,
 * 이 앱의 분위기가 아니게 된다(CLAUDE.md 디자인). 대신 **흐린 것들을 진하게** 만든다:
 * 보조 글자, 선, 포인트·강조 컬러가 대상이다.
 *
 * 특히 보조 글자(네이비 60%)가 문제였다. 흰 배경 위 대비가 작은 글씨(13sp)에서 모자란다.
 * 알파를 빼고 진한 네이비를 그대로 쓴다.
 */
val LightHighContrastPalette = LightPalette.copy(
    Point = Color(0xFF0E6E76),
    PointDark = Color(0xFF0A5056),
    PointLight = Color(0xFFBFE9ED),

    // 연노랑은 바탕으로만 쓰이므로 글자(네이비)와의 대비를 올리는 쪽으로 진하게 간다.
    Secondary = Color(0xFFFFE38A),
    // 고대비에서는 Secondary가 이미 진해서 더 올리지 않는다. 같은 값을 쓴다.
    SecondaryBold = Color(0xFFFFE38A),
    Accent = Color(0xFFC9472A),

    SurfaceMuted = Color(0xFFEAEEF2),

    TextPrimary = Color(0xFF0F1F33),
    TextSecondary = Color(0xFF0F1F33),

    // 선이 보여야 섹션 경계가 읽힌다. 평소에는 일부러 안 보이게 두는 값이다.
    Divider = Color(0xFF8D96A0),

    Danger = Color(0xFF9B1C12),
    Warning = Color(0xFF6E4200),
    Heart = Color(0xFFB0124C),

    HeroGradient = listOf(
        Color(0xFF0E6E76),
        Color(0xFF0A5056),
    ),

    // 고대비에서는 더 어둡게. 회색 위 흰 글자가 가장 먼저 무너지는 조합이다.
    MutedGradient = listOf(
        Color(0xFF5A6773),
        Color(0xFF33404D),
    ),
)

/**
 * 다크 한 벌.
 *
 * **순검정을 쓰지 않는다.** 글자가 네이비 가족이라 순검정 배경에서는 색이 따로 놀고,
 * OLED에서 스크롤할 때 잔상이 남는다. 같은 네이비를 어둡게 내린 값으로 깐다.
 *
 * **[SurfaceMuted]가 [Background]보다 밝다.** 라이트에서는 카드를 옅은 회색으로 "가라앉혀"
 * 구분했는데, 어두운 화면에서는 반대로 **떠올려야** 면이 보인다. 라이트 값을 그냥 뒤집으면
 * 카드가 배경에 묻힌다.
 *
 * **[PointDark]와 [PointLight]는 값이 서로 뒤바뀐다.** 두 이름은 밝기가 아니라 역할이다 —
 * PointLight는 칩·배지의 *면*, PointDark는 그 면 위의 *글자*다. 면이 어두워지면 글자는
 * 밝아져야 한다. 이름을 Container/OnContainer로 바꾸는 편이 정확하지만 호출부가 많아
 * 이름은 두고 여기에 적어 둔다.
 */
val DarkPalette = LightPalette.copy(
    // 브랜드색은 그대로 둔다. 모드에 따라 포인트 컬러가 바뀌면 같은 앱으로 안 보인다.
    Point = Color(0xFF20B8C4),
    PointDark = Color(0xFF8FE4EB),
    PointLight = Color(0xFF0E3B40),

    // 연노랑은 어두운 화면에서 형광처럼 뜬다. 바탕은 깊게, 작은 면은 살짝 낮춘 금색으로.
    Secondary = Color(0xFF3E3A22),
    SecondaryBold = Color(0xFFE6C45C),
    Accent = Color(0xFFFF9470),

    Background = Color(0xFF121820),
    Surface = Color(0xFF1A2230),
    SurfaceMuted = Color(0xFF222C3C),

    TextPrimary = Color(0xFFE8EDF5),
    TextSecondary = Color(0xFFE8EDF5).copy(alpha = 0.65f),

    Divider = Color(0xFF2E3A4C),

    // 어두운 바탕에서는 진한 빨강이 바탕에 먹힌다. 밝혀야 "주의"로 읽힌다.
    Danger = Color(0xFFFF6B5C),
    Warning = Color(0xFFE8A33D),
    Heart = Color(0xFFFF4D7D),

    // 어두운 화면에서 원래 터쿼이즈는 너무 밝게 튄다. 한 단계 깊게.
    HeroGradient = listOf(
        Color(0xFF1BA3AE),
        Color(0xFF136C75),
    ),

    MutedGradient = listOf(
        Color(0xFF3A4553),
        Color(0xFF28313D),
    ),
)

/**
 * 다크 + 고대비 한 벌.
 *
 * 고대비는 접근성 설정이라 다크에서도 유지된다. 라이트 고대비가 "흐린 것을 진하게"였다면
 * 여기서는 반대로 **흐린 것을 밝게** 만든다 — 보조 글자, 선, 포인트 컬러가 대상이다.
 */
val DarkHighContrastPalette = DarkPalette.copy(
    Point = Color(0xFF4FD3DD),
    PointDark = Color(0xFFB8F0F5),
    PointLight = Color(0xFF14525A),

    SecondaryBold = Color(0xFFFFD98A),
    Accent = Color(0xFFFFA98C),

    Background = Color(0xFF0A0E14),
    Surface = Color(0xFF141C28),
    SurfaceMuted = Color(0xFF1E2836),

    TextPrimary = Color(0xFFFFFFFF),
    // 알파를 빼고 흰색을 그대로 쓴다. 라이트 고대비가 네이비 60%를 버린 것과 같은 이유다.
    TextSecondary = Color(0xFFD6DEE9),

    // 선이 보여야 섹션 경계가 읽힌다.
    Divider = Color(0xFF5C6B7E),

    Danger = Color(0xFFFF8A7D),
    Warning = Color(0xFFFFBE5E),
    Heart = Color(0xFFFF7098),
)

/**
 * 지금 화면이 쓰는 색 한 벌.
 *
 * `staticCompositionLocalOf`를 쓰는 이유: 색이 바뀌는 일은 설정에서 고대비를 켤 때뿐이라
 * 드물다. 그때는 화면 전체를 다시 그리는 편이 싸고, 읽을 때마다 구독을 거는 비용이 없다.
 */
val LocalPalette = staticCompositionLocalOf { LightPalette }
