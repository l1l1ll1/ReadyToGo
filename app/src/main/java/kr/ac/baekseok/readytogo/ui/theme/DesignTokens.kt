package kr.ac.baekseok.readytogo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 디자인 토큰 — 색·간격·반경·글꼴 크기를 전부 여기에 모은다.
 *
 * 화면 코드에 `16.dp`나 `Color(0xFF...)`를 직접 적지 않는 이유: 분위기를 한 번 바꾸려면
 * 화면 열 몇 개를 다 뒤져야 한다. 여기 숫자만 고치면 앱 전체가 따라 바뀐다.
 */
object Tokens {

    /**
     * 사진 자리를 채우는 그라데이션. 아직 실제 사진을 붙이지 않아 색으로 칸을 채운다.
     * 카드마다 다른 조합이 나오도록 [placeholderGradient]가 골라 쓴다.
     *
     * **브랜드 세 색(터쿼이즈·연노랑·코랄)에서만 조합한다.** 예전에는 청록·파랑·보라가,
     * 그다음에는 주황·자주가 섞여 있었는데, 그때마다 홈의 다가오는 여행 카드와 색이 따로 놀았다.
     * 다섯 가지를 두는 이유는 그대로다 — 목적지마다 달라야 목록이 한 덩어리로 안 보인다.
     *
     * 색 한 벌([AppPalette])과 달리 여기 있는 값은 고대비에서도 바뀌지 않는다 —
     * 사진을 대신하는 자리라 글자를 얹지 않고, 대비를 따질 대상이 아니다.
     */
    val PlaceholderGradients = listOf(
        // 바다 — 브랜드 터쿼이즈에 가장 가깝다.
        listOf(Color(0xFF20B8C4), Color(0xFF5ED3DC)),
        // 해변 — 터쿼이즈에서 모래빛으로.
        listOf(Color(0xFF3FC7C0), Color(0xFFFFE08A)),
        // 노을 — 코랄에서 노랑으로.
        listOf(Color(0xFFFF805E), Color(0xFFFFC978)),
        // 깊은 바다 — 터쿼이즈에서 네이비로.
        listOf(Color(0xFF178F99), Color(0xFF2C5A86)),
        // 산호 — 코랄에서 분홍으로.
        listOf(Color(0xFFFF9A7B), Color(0xFFF2679B)),
    )

    /** 4dp 배수로만 쓴다. 눈대중으로 5, 7, 13을 섞으면 줄이 안 맞는다. */
    object Space {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp

        /** 화면 좌우 기본 여백. 모든 화면이 같은 값을 써야 탭을 옮겨도 흔들리지 않는다. */
        val screenHorizontal = 20.dp
    }

    /** 둥근 모서리 16dp가 기본이다(앱 구조에서 확정). 작은 칩만 더 둥글게 쓴다. */
    object Radius {
        val card = RoundedCornerShape(16.dp)
        val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        val button = RoundedCornerShape(12.dp)
        val chip = RoundedCornerShape(50)
        val image = RoundedCornerShape(16.dp)
    }

    object FontSize {
        val display = 32.sp
        val title = 22.sp
        val section = 18.sp
        val body = 15.sp
        val caption = 13.sp
        val label = 11.sp
    }

    /** 카드 높이 등 반복되는 크기. */
    object Size {
        /** 홈의 큰 사진 카드. 화면 폭에 비해 너무 낮으면 "큰 카드" 느낌이 안 난다. */
        val photoCardHeight = 180.dp

        /** 지난 여행 카드. 끝난 일정이 현재 일정과 같은 자리를 차지하지 않게 낮춘다. */
        val pastTripCardHeight = 110.dp
        val photoThumbHeight = 96.dp
        val avatar = 56.dp
        val icon = 24.dp
        val bottomBarHeight = 64.dp

        /** 버튼 높이. 손가락으로 누르는 최소 크기(48dp)보다 조금 크게 둔다. */
        val button = 52.dp
        val loadingStroke = 2.dp

        /** 스플래시 가운데 심볼과 워드마크. 둘의 비율이 로고 한 덩어리처럼 보이게 맞춘 값이다. */
        val splashSymbol = 120.dp
        val splashWordmark = 240.dp

        /** 로그인 상단 노을 영역의 워드마크. 화면 폭 대비 너무 작으면 브랜드가 아니라 안내문이 된다. */
        val loginWordmark = 260.dp

        /**
         * 홈의 "다가오는 여행" 카드.
         *
         * 일반 사진 카드(180dp)보다 크게 둔다 — 홈에서 가장 먼저 보여야 할 것이
         * "다음에 어디 가는지"이고, 같은 높이면 아래 목록과 구분되지 않는다.
         */
        val heroCardHeight = 220.dp

        /** 빈 목록 자리에 옅게 까는 아이콘 심볼. 글자보다 커지면 주인공이 바뀐다. */
        val emptyStateSymbol = 96.dp

        /**
         * 바닥 시트 안 목록의 최대 높이.
         * 제한하지 않으면 시트가 화면을 다 덮어 "떠 있는 시트"로 보이지 않는다.
         */
        val sheetListMaxHeight = 420.dp

        /**
         * 장소 사진 썸네일. 목록 한 줄 높이에 맞춘다 — 더 키우면 줄이 성큼성큼 길어지고,
         * 더 줄이면 사진에서 무엇인지 알아볼 수 없다.
         */
        val placeThumb = 64.dp

        /**
         * 끌어다 놓아 지우는 자리.
         *
         * **보이는 크기가 곧 판정 범위다.** 72dp로 크게 뒀더니 아이콘 양옆 빈 공간에
         * 놓아도 지워졌다. 아이콘(24dp)에 여유 8dp씩만 더해 눈에 보이는 동그라미와
         * 지워지는 범위를 같게 만든다.
         */
        val trashTarget = 40.dp

        /** 타임라인 왼쪽 세로줄. 점(32dp) 폭에 맞춰야 선과 점의 중심이 어긋나지 않는다. */
        val timelineRailWidth = 32.dp
        val timelineLineWidth = 2.dp

        /** "다음 갈 곳" 점에 두르는 테두리. 선 굵기와 같은 값이지만 쓰임이 달라 따로 둔다. */
        val timelineRing = 2.dp
        val timelineLineHeight = 28.dp
    }

    /**
     * 카드 띄우는 정도. 그림자를 세게 주면 흰 배경이 탁해 보여서 0~2dp만 쓴다.
     * 구분은 그림자보다 [Palette.SurfaceMuted] 배경으로 한다.
     */
    object Elevation {
        val none = 0.dp
        val card = 1.dp
        val raised = 2.dp
    }

    /**
     * 사진 자리 그라데이션을 고른다. 같은 [key]는 항상 같은 색을 받는다 —
     * 스크롤할 때마다 색이 바뀌면 어지럽다.
     *
     * **key로는 도시 이름처럼 짧고 서로 다른 값을 넘긴다.** 저장된 일정 id처럼
     * "도쿄-2026-10-14-3000000-[...]" 같은 긴 문자열을 넘겼더니 세 일정이 모두 같은 색을 받았다.
     * 글자 코드를 더해 섞는 이유도 같다 — String.hashCode는 비슷한 문자열에서 나머지가 겹친다.
     */
    fun placeholderGradient(key: String): List<Color> {
        val mixed = key.foldIndexed(0) { index, acc, char -> acc + char.code * (index + 1) }
        val index = mixed.mod(PlaceholderGradients.size)
        return PlaceholderGradients[index]
    }
}
