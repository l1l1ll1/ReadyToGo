package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kotlin.math.roundToInt

/** 두 지점 사이를 어떻게 이동하는지. 화면에 "도보 12분"처럼 적는다. */
enum class TravelMode(val label: String) {
    WALK("도보"),
    TRANSIT("대중교통"),
}

/**
 * 한 항목에서 다음 항목으로 가는 이동.
 *
 * @param minutes 분. 0이 되지 않게 최소 1분으로 올린다 — "도보 0분"은 정보가 아니다.
 */
data class TravelLeg(
    val mode: TravelMode,
    val minutes: Int,
    val distanceKm: Double,
) {
    /** "도보 12분" */
    val label: String get() = "${mode.label} ${minutes}분"
}

/**
 * 거리로 이동 시간을 추정한다.
 *
 * **실제 경로가 아니라 직선거리 기반 추정이다.** Google Directions API를 쓰면 정확하지만
 * 호출마다 과금되고, 하루 일정에 구간이 대여섯 개씩이라 일정 하나에 수십 번 호출된다.
 * 사용자는 "걸어갈 만한가, 지하철을 타야 하나"를 알고 싶을 뿐이므로 추정으로 충분하다.
 *
 * 실제 길은 직선보다 돌아가므로 [DETOUR_FACTOR]로 늘려 잡는다 — 추정이 실제보다 짧으면
 * 일정이 빡빡해 보이는 것보다 나쁘다(가능한 줄 알고 갔다가 못 간다).
 */
fun estimateTravel(from: LatLng, to: LatLng): TravelLeg {
    val straight = distanceKm(from, to)
    val routed = straight * DETOUR_FACTOR

    val mode = if (routed <= WALK_MAX_KM) TravelMode.WALK else TravelMode.TRANSIT
    val speed = if (mode == TravelMode.WALK) WALK_SPEED_KMH else TRANSIT_SPEED_KMH

    val minutes = (routed / speed * MINUTES_PER_HOUR).roundToInt().coerceAtLeast(1)
    return TravelLeg(mode = mode, minutes = minutes, distanceKm = straight)
}

/**
 * 분을 사람이 읽는 말로. "약 1시간 30분", "약 45분"
 *
 * "약"을 붙이는 이유: 추정값인데 "1시간 30분"이라고 단언하면 정확한 값으로 읽힌다.
 */
fun formatDuration(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> "약 ${rest}분"
        rest == 0 -> "약 ${hours}시간"
        else -> "약 ${hours}시간 ${rest}분"
    }
}

/**
 * 이동 추정 상수. 숫자만 바꿔 조정할 수 있게 한곳에 모았다.
 *
 * 도보 4km/h는 성인 평균 보행 속도보다 약간 느리게 잡은 값이다 — 여행 중에는 짐을 들고
 * 길을 찾으며 걷는다. 대중교통 20km/h는 환승·대기를 포함한 도시 평균이다.
 */
private const val WALK_MAX_KM = 2.0
private const val WALK_SPEED_KMH = 4.0
private const val TRANSIT_SPEED_KMH = 20.0

/** 직선거리를 실제 경로 길이로 늘리는 배수. 도시에서는 보통 1.2~1.4배다. */
private const val DETOUR_FACTOR = 1.3

private const val MINUTES_PER_HOUR = 60
