package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kotlin.math.roundToInt

/**
 * 함수에 보낼 하루 일정 요약. **그날 일정과 사용자 문장 말고는 보내지 않는다.**
 *
 * 별칭(a1, m_lunch)을 쓰는 이유: Google 장소 id는 길어서 토큰을 많이 먹고, 모델이 옮겨
 * 적다가 한 글자만 틀려도 엉뚱한 항목을 가리킨다. 짧은 별칭은 틀리면 그냥 "없는 id"가 된다.
 */
data class DaySummary(
    val items: List<SummaryItem>,
    /** 별칭 → 타임라인 키("visit-…", "meal-LUNCH"). */
    val keyByAlias: Map<String, String>,
)

data class SummaryItem(
    val id: String,
    val isMeal: Boolean,
    val name: String,
    /** 식당 종류(라멘, 카페…) 또는 관광지 타입 몇 개. "라멘 말고"를 알아들으려면 필요하다. */
    val category: String,
    val stayMinutes: Int,
    val moveMinutes: Int,
    /** 숙소까지 직선거리(km, 소수 첫째 자리). 숙소가 없으면 null. */
    val lodgingKm: Double?,
)

/**
 * 화면에 보이는 타임라인 그대로 요약한다. 지운 항목은 이미 빠져 있다.
 *
 * 추천을 못 찾은 끼니([TimelineItem.NoMeal])는 넣지 않는다 — 바꿀 식당이 없으니
 * 모델에게 보여줘 봐야 할 수 있는 일이 없다.
 */
fun summarizeDay(entries: List<TimelineEntry>, lodging: LatLng?): DaySummary {
    val items = mutableListOf<SummaryItem>()
    val keyByAlias = mutableMapOf<String, String>()
    var visitNumber = 0

    for (entry in entries) {
        val item = entry.item
        val alias: String
        val name: String
        val category: String
        when (item) {
            is TimelineItem.Visit -> {
                visitNumber += 1
                alias = "a$visitNumber"
                name = item.attraction.name
                category = item.attraction.types
                    .filterNot { it in GENERIC_TYPES }
                    .take(MAX_TYPES)
                    .joinToString(",")
            }
            is TimelineItem.Meal -> {
                alias = "m_${item.meal.name.lowercase()}"
                name = item.restaurant.name
                category = item.restaurant.category
            }
            is TimelineItem.NoMeal -> continue
        }

        keyByAlias[alias] = item.key
        items += SummaryItem(
            id = alias,
            isMeal = item is TimelineItem.Meal,
            name = name,
            category = category,
            stayMinutes = item.stayMinutes,
            moveMinutes = entry.travelFromPrevious?.minutes ?: 0,
            lodgingKm = lodging?.let { home ->
                item.location?.let { (distanceKm(home, it) * 10).roundToInt() / 10.0 }
            },
        )
    }
    return DaySummary(items, keyByAlias)
}

/** 거의 모든 장소에 붙어 있어 아무것도 알려주지 않는 타입. 토큰만 먹는다. */
private val GENERIC_TYPES = setOf("point_of_interest", "establishment", "tourist_attraction")

/** 관광지 타입은 앞의 몇 개만. 뒤로 갈수록 일반적인 것이라 판단에 도움이 안 된다. */
private const val MAX_TYPES = 3
