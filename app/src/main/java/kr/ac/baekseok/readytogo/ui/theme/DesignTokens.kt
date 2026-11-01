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
        val none = 0.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp

        /** 화면 좌우 기본 여백. 모든 화면이 같은 값을 써야 탭을 옮겨도 흔들리지 않는다. */
        val screenHorizontal = 20.dp
    }

    /**
     * 둥근 모서리. ⑫에서 16dp → 24dp로 키웠다 — 그림자를 없앤 흰 카드는 모서리가 커야
     * 민트그레이 바탕 위에서 "떠 있는 면"으로 읽힌다(design/mockup). 버튼과 칩은 pill.
     */
    object Radius {
        val card = RoundedCornerShape(24.dp)

        /** 홈의 다가오는 여행처럼 화면에서 가장 큰 카드. 일반 카드보다 한 단계 둥글다. */
        val hero = RoundedCornerShape(28.dp)
        val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

        /**
         * 목록 줄을 이어 붙여 카드 한 장처럼 보이게 할 때 위·가운데·아래 조각(일정표).
         * 목록이 LazyColumn이라 카드 하나로 감쌀 수 없어 줄마다 조각을 칠한다.
         */
        val cardTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        val cardMiddle = RoundedCornerShape(0.dp)
        val cardBottom = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        val button = RoundedCornerShape(50)
        val chip = RoundedCornerShape(50)

        /** 카드 안의 사진·썸네일. 카드(24)보다 작아야 안쪽 여백과 곡선이 겹쳐 보이지 않는다. */
        val image = RoundedCornerShape(20.dp)
    }

    /**
     * 글자 크기(⑫부터 design/mockup 기준). 시안 780px ≈ 360dp(2.17배)로 재서 맞췄다.
     * - display: 히어로 도시명·큰 금액. headline: 화면 큰 제목("다음 여행"). sheetTitle: 시트 제목.
     * - title: 상단 바 제목. section: 카드 제목·섹션 머리. body·caption·label: 본문·보조·칩.
     */
    object FontSize {
        val display = 34.sp
        val headline = 28.sp
        val sheetTitle = 24.sp
        val title = 20.sp
        val section = 17.sp
        val body = 16.sp
        val caption = 14.sp
        val label = 13.sp
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

        /** 글줄 옆에 붙는 작은 아이콘(요약 머리의 기간 연필). 본문 글자 높이에 맞춘다. */
        val inlineIcon = 18.dp
        val bottomBarHeight = 64.dp

        /** 떠 있는 탭 바(⑫). 아이콘 + 글자 두 줄이 pill 안에서 답답하지 않은 높이. */
        val tabBarHeight = 72.dp

        /** 결과 화면의 "말로 고치기" 원형 버튼. 탭 바보다 조금 작게 — 주인공은 일정표다. */
        val aiButton = 64.dp

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
        val heroCardHeight = 260.dp

        /** 히어로 카드 오른쪽 아래 화살표 버튼(⑫). 손가락에 넉넉한 정사각형. */
        val heroArrow = 56.dp

        /** 홈 최근 일정 2열 카드 위쪽 색 블록(사진 자리). */
        val recentBlockHeight = 72.dp

        /** 홈 검토 카드 왼쪽 아이콘 원. */
        val reviewIcon = 48.dp

        /** 결과 화면 위 흰 원 버튼(뒤로·공유·저장)과 시트 닫기 버튼. */
        val roundButton = 44.dp

        /** 결과 화면 탭 묶음 한 칸 높이. */
        val segmentHeight = 40.dp

        /** 일정표 날짜 칩. 글자 + 날씨 아이콘 두 줄이 들어가는 크기(design/mockup/timeline). */
        val dayChipWidth = 64.dp
        val dayChipHeight = 56.dp

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
        val timelineRailWidth = 44.dp
        val timelineLineWidth = 2.dp

        /** "다음 갈 곳" 점에 두르는 테두리. 선 굵기와 같은 값이지만 쓰임이 달라 따로 둔다. */
        val timelineRing = 2.dp
        val timelineLineHeight = 28.dp

        /** 점선 구분선 굵기와 점 하나·틈 하나의 길이. */
        val divider = 1.dp
        val dividerDash = 4.dp
    }

    /**
     * 카드 띄우는 정도. ⑫부터 **그림자를 쓰지 않는다** — 구분은 민트그레이 바탕과 흰 카드의
     * 색 차이로 한다. 그림자를 얹으면 둥근 흰 카드가 탁한 회색 테두리를 두른다.
     */
    object Elevation {
        val none = 0.dp
        val card = 0.dp
        val raised = 0.dp
    }

    /**
     * 모션. Compose 내장 애니메이션만 쓰고, 길이는 150~300ms 안에서 고른다.
     * 시스템 "애니메이션 제거"를 켜면 직접 만든 효과(눌림·펄스·하이라이트)는 꺼진다(LocalReduceMotion).
     */
    object Motion {
        /** 눌림, 색 바뀜처럼 손가락 바로 아래의 반응. */
        const val shortMs = 150

        /** 펼침, 탭 선택 표시 이동. */
        const val mediumMs = 220

        /** 화면 전환, 시트 뒤 화면 축소. */
        const val longMs = 300

        /** 말로 고치기로 바뀐 항목을 잠깐 칠해 두는 시간. 너무 짧으면 무엇이 바뀌었는지 못 본다. */
        const val highlightMs = 600

        /** AI 응답을 기다리는 동안 테두리가 한 번 밝아졌다 어두워지는 주기. */
        const val pulseMs = 900

        const val pressScale = 0.96f
        const val dragLiftScale = 1.03f

        /** 시트가 올라왔을 때 뒤 화면을 살짝 줄여 시트가 앞에 있다는 것을 보여준다. */
        const val sheetBackdropScale = 0.97f
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
