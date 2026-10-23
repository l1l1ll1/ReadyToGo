package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.Restaurant
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm

/**
 * 기준에 맞는 다른 식당. 없으면 null.
 *
 * **"지금보다 나은 것 중 점수 최고"로 고른다.** CLAUDE.md의 가성비 규칙("1번보다 싼 것 중
 * 점수 최고")과 같은 꼴이다. "숙소 2km 안" 같은 고정 거리를 두지 않는 이유: 도시마다
 * 밀도가 달라서 어떤 숫자를 골라도 어디선가는 후보가 0개거나 전부가 된다.
 * 실제 선택은 기존 추천 점수(평점 0.5 + 동선 0.5)가 한다 — 모델은 기준만 정한다.
 *
 * @param previous 바로 앞 일정의 좌표. 하루 첫 끼니면 숙소.
 * @param takenIds 그날 다른 끼니에 이미 나온 식당. 점심과 저녁이 같은 집이 되지 않게 뺀다.
 */
fun pickMealFor(
    basis: MealBasis,
    meal: TimelineItem.Meal,
    previous: LatLng?,
    lodging: LatLng?,
    takenIds: Set<String>,
): Recommendation<Restaurant>? {
    val current = meal.restaurant
    val others = meal.options.filter { it.item.id != current.id && it.item.id !in takenIds }

    val better = when (basis) {
        MealBasis.NEAR_LODGING -> others.closerTo(lodging ?: return null, current)
        MealBasis.NEAR_PREV -> others.closerTo(previous ?: return null, current)
        MealBasis.CHEAPER -> {
            // 지금 식당 가격을 모르면 "더 싼 곳"을 말할 수 없다.
            if (current.priceUnknown) return null
            others.filter { !it.item.priceUnknown && it.item.priceWon < current.priceWon }
        }
        MealBasis.EXCLUDE_SAME_KIND -> others.filterNot {
            it.item.category.trim().equals(current.category.trim(), ignoreCase = true)
        }
    }
    return better.maxByOrNull { it.score }
}

private fun List<Recommendation<Restaurant>>.closerTo(
    target: LatLng,
    current: Restaurant,
): List<Recommendation<Restaurant>> {
    val now = distanceKm(current.location, target)
    return filter { distanceKm(it.item.location, target) < now }
}
