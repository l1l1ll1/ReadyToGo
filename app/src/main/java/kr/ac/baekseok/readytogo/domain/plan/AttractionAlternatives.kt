package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.domain.purpose.IndoorOutdoor
import kr.ac.baekseok.readytogo.domain.purpose.Shelter
import kr.ac.baekseok.readytogo.domain.recommend.attractionScore

/**
 * 날마다 관광지 예비 후보. 관광지 교체(swap_place)가 여기서 고른다.
 *
 * **이미 받은 검색 결과에서 고른다** — 일정에 안 들어간 관광지들이다. Places 호출이 더 나가지 않는다.
 * 점수는 일정을 짤 때와 같은 [attractionScore]이되, 동선 기준을 **그날 관광지**로 둔다.
 * 여행 전체 기준이면 어느 날이든 같은 후보가 뽑혀, 교체하면 그날 동선에서 먼 곳이 들어온다.
 *
 * 저장된 일정도 열 때 계획을 다시 만들므로 여기서 다시 계산된다(저장 형식 변경 없음).
 *
 * @param candidates 관광지 검색 결과 전체
 * @param dailyAttractions 날짜별로 배치된 관광지
 * **실내 후보 하나는 꼭 남긴다.** 상위 [MAX_ALTERNATIVES]곳에 실내가 없으면 점수가 가장 높은 실내를
 * 뒤에 하나 덧붙인다. 비 오는 날 고치기는 실내로만 바꾸는데, 그날 근처가 공원뿐이면 고칠 길이
 * 없었다(삿포로 4일차: 근처 5곳이 전부 공원·전망대, 맥주 박물관은 6위 밖).
 *
 * @return 날짜별 후보. 점수 높은 순, 날마다 [MAX_ALTERNATIVES]곳(+ 실내 1곳)까지
 */
fun alternativesByDay(
    candidates: List<Attraction>,
    dailyAttractions: List<List<Attraction>>,
    purposes: Set<TravelPurpose>,
): List<List<Attraction>> {
    val planned = dailyAttractions.flatten().map { it.id }.toSet()
    val spare = candidates.filter { it.id !in planned }.distinctBy { it.id }

    return dailyAttractions.map { day ->
        // 관광지가 없는 날은 기준점이 없다. 그때는 여행 전체 배치를 기준으로 본다.
        val anchors = (day.ifEmpty { dailyAttractions.flatten() }).map { it.location }
        val ranked = spare.sortedByDescending { attractionScore(it, anchors, purposes) }
        val top = ranked.take(MAX_ALTERNATIVES)
        val indoor = ranked.firstOrNull { IndoorOutdoor.of(it) == Shelter.INDOOR }
        if (indoor == null || indoor in top) top else top + indoor
    }
}

/**
 * 날마다 들고 있을 후보 수. 교체 기준(실내·앞 일정 근처)으로 거르고도 하나쯤 남을 만큼.
 * 검색이 최대 20곳이라 많이 잡아 봐야 날마다 같은 곳이 겹친다.
 */
const val MAX_ALTERNATIVES = 5
