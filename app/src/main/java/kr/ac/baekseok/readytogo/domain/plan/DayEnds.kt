package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TripPlan

/** 하루가 시작하는 곳과 끝나는 곳. 모르면 null. */
data class DayEnds(val start: LatLng?, val end: LatLng?)

/**
 * 하루의 시작·끝 지점.
 *
 * 보통은 숙소에서 나와 숙소로 돌아온다. 도착일은 공항에서 시작하고 출국일은 공항에서 끝난다 —
 * 그날 첫 이동은 공항에서 시내로, 마지막 이동은 시내에서 공항으로 가기 때문이다.
 * 공항을 모르면 지금처럼 숙소를 쓴다. 하루짜리 여행이면 공항에서 시작해 공항에서 끝난다.
 *
 * 끼니 식당 선택과 지도 경로선이 함께 쓴다. 두 곳이 따로 정하면 지도 선은 공항에서 시작하는데
 * 아침 식당은 숙소 기준으로 골라지는 식으로 어긋난다.
 */
fun dayEnds(dayNumber: Int, dayCount: Int, home: LatLng?, airport: LatLng?): DayEnds = DayEnds(
    start = if (dayNumber == 1) airport ?: home else home,
    end = if (dayNumber == dayCount) airport ?: home else home,
)

/** 지도처럼 완성된 계획에서 하루의 시작·끝이 필요할 때. 숙소는 추천(1번) 쪽을 기준으로 한다. */
fun TripPlan.endsOf(day: DayPlan): DayEnds = dayEnds(
    dayNumber = day.dayNumber,
    dayCount = dailyPlans.size,
    home = lodging?.qualityFirst?.item?.location,
    airport = airport?.location,
)
