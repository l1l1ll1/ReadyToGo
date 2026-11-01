package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.isClosedOn
import kr.ac.baekseok.readytogo.domain.purpose.IndoorOutdoor
import kr.ac.baekseok.readytogo.domain.purpose.Shelter
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import java.time.DayOfWeek

/**
 * 기준에 맞는 다른 관광지. 없으면 null.
 *
 * [alternatives]는 이미 점수 순이다([kr.ac.baekseok.readytogo.domain.plan.alternativesByDay]).
 * 그래서 기준으로 거른 뒤 **맨 앞**이 "기준에 맞는 것 중 점수 최고"다 — 끼니 교체([pickMealFor])와 같은 꼴.
 *
 * @param current 지금 그 자리의 관광지
 * @param previous 바로 앞 일정의 좌표. 하루 첫 항목이면 숙소.
 * @param takenIds 여행 전체에서 이미 화면에 있는 관광지. 같은 곳을 두 번 가지 않게 뺀다.
 * @param visitDay 방문 요일. 그날 쉬는 곳은 뺀다(모르면 남긴다).
 */
fun pickPlaceFor(
    basis: PlaceBasis,
    current: Attraction,
    alternatives: List<Attraction>,
    previous: LatLng?,
    takenIds: Set<String>,
    visitDay: DayOfWeek?,
): Attraction? {
    val others = alternatives.filter { option ->
        option.id != current.id && option.id !in takenIds &&
            (visitDay == null || !option.openDays.isClosedOn(visitDay))
    }

    return when (basis) {
        // 판정을 미룬 곳(사찰 등)은 실내로 치지 않는다. 비를 피하려고 바꿨는데 마당이면 소용없다.
        PlaceBasis.INDOOR -> others.firstOrNull { IndoorOutdoor.of(it) == Shelter.INDOOR }
        PlaceBasis.NEAR_PREV -> {
            val target = previous ?: return null
            val now = distanceKm(current.location, target)
            others.firstOrNull { distanceKm(it.location, target) < now }
        }
    }
}
