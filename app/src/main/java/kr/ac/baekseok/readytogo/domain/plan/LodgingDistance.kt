package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kr.ac.baekseok.readytogo.domain.recommend.medianCenter

/**
 * 숙소가 날짜별 일정에서 얼마나 떨어져 있는지.
 *
 * **추천 점수와 묻는 것이 다르다.** 숙소 동선 점수는 *여행 전체* 관광지의 중앙값 하나까지의
 * 거리라, 1일차는 북쪽 2일차는 남쪽인 일정에서 한가운데 숙소가 높은 점수를 받고도 두 날
 * 모두 멀 수 있다. 그 경우를 잡으려고 **날짜마다 따로** 잰다.
 *
 * 그래서 이 결과를 점수에 되먹이지 않는다. 더하면 같은 거리를 두 번 세게 된다.
 */
data class DayLodgingDistance(
    val dayNumber: Int,
    val travel: TravelLeg,
) {
    val isFar: Boolean get() = travel.distanceKm > LODGING_FAR_KM
}

/**
 * 어느 날에도 덜 먼 후보.
 *
 * @param worstDistanceKm 이 후보의 **가장 먼 날** 거리. 평균이 아니라 최댓값을 보여준다 —
 *        평균이 좋아도 하루가 아주 멀면 그 하루를 고스란히 겪는다.
 */
data class BetterLodging(
    val lodging: Lodging,
    val worstDistanceKm: Double,
)

/**
 * 숙소 거리 검사 결과.
 *
 * @param better 대부분의 날이 멀 때 대신 제안할 후보. 없으면 null.
 */
data class LodgingDistanceCheck(
    val days: List<DayLodgingDistance>,
    val better: BetterLodging? = null,
) {
    val farDays: List<DayLodgingDistance> get() = days.filter { it.isFar }

    /** 검사한 날의 과반이 멀면 숙소 자체를 다시 보라는 뜻이다. */
    val mostlyFar: Boolean get() = days.isNotEmpty() && farDays.size * 2 > days.size
}

/**
 * 날짜별 일정의 중심점.
 *
 * **편집을 반영한다.** 계획을 만들 때의 관광지가 아니라 지금 타임라인에 남아 있는 관광지로
 * 잰다 — 멀어서 지운 곳이 계속 거리 계산에 들어가면 경고가 사라지지 않는다.
 *
 * 중심은 위·경도의 중앙값이다(숙소·식당 추천과 같은 기준). 화면마다 다른 중심을 쓰면
 * 같은 장소가 어디서는 멀고 어디서는 가까운 곳이 된다.
 */
fun TripPlan.dayCenters(
    edits: TimelineEdits = TimelineEdits(),
    picked: PickedPlaces = PickedPlaces(),
): Map<Int, LatLng> = dailyPlans.mapNotNull { day ->
    val locations = day
        .toTimeline(
            deletedKeys = edits.deletedIn(day.dayNumber),
            pickedMeals = edits.pickedIn(day.dayNumber),
            customOrder = edits.orderIn(day.dayNumber),
            pickedAttractions = edits.pickedAttractionsIn(day.dayNumber),
            picked = picked,
        )
        .mapNotNull { (it.item as? TimelineItem.Visit)?.attraction?.location }

    // 관광지가 하나도 안 남은 날은 중심을 구할 수 없다. 검사에서 뺀다.
    medianCenter(locations)?.let { day.dayNumber to it }
}.toMap()

/**
 * 숙소와 날짜별 중심 사이의 거리를 잰다.
 *
 * 거리는 [estimateTravel]로 이동 시간까지 같이 구한다. "5km"는 감이 안 오지만
 * "대중교통 약 24분"은 온다. 경로 API는 부르지 않는다 — 날짜 × 후보만큼 과금된다.
 */
fun TripPlan.checkLodgingDistance(
    edits: TimelineEdits = TimelineEdits(),
    picked: PickedPlaces = PickedPlaces(),
): LodgingDistanceCheck? {
    val pair = lodging ?: return null
    val centers = dayCenters(edits, picked)
    if (centers.isEmpty()) return null

    val days = centers.map { (dayNumber, center) ->
        DayLodgingDistance(dayNumber, estimateTravel(pair.qualityFirst.item.location, center))
    }.sortedBy { it.dayNumber }

    val check = LodgingDistanceCheck(days)
    if (!check.mostlyFar) return check

    // **점수가 아니라 "가장 먼 날"로 다시 줄 세운다.** 점수로 다시 뽑으면 이미 1등인
    // 지금 숙소가 또 1등이라 아무것도 달라지지 않는다. 묻는 것은 "어느 날도 많이
    // 멀지 않은 곳이 있나"이므로 날짜별 거리의 최댓값이 가장 작은 후보를 고른다.
    val current = pair.qualityFirst.item
    val better = (listOfNotNull(pair.withinBudget) + pair.alternatives)
        .map { it.item }
        .filter { it.id != current.id }
        .mapNotNull { candidate ->
            val distances = centers.values.map { distanceKm(candidate.location, it) }

            // **과반의 날에서 가까운 후보만 제안한다.** 지금보다 조금이라도 가까우면
            // 제안하게 했더니 4.8km에 4.6km를 내놓았다 — 바꿔도 똑같이 멀어서
            // "더 나은 곳"이라 부를 수 없다. 그런 후보가 없으면 경고만 띄운다.
            val nearDays = distances.count { it <= LODGING_FAR_KM }
            if (nearDays * 2 <= distances.size) return@mapNotNull null

            BetterLodging(lodging = candidate, worstDistanceKm = distances.max())
        }
        .minByOrNull { it.worstDistanceKm }

    return check.copy(better = better)
}

/**
 * 숙소가 "멀다"고 보는 거리(km).
 *
 * 5km는 도보로는 한 시간, 대중교통으로는 환승을 감안해 20분 안팎이다. 하루 두 번(나갈 때,
 * 돌아올 때) 겪는 거리라 이쯤부터는 숙소를 고른 값을 다시 볼 만하다.
 */
const val LODGING_FAR_KM = 5.0
