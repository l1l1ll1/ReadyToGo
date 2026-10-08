package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.ScoredCandidate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 점수 가중치.
 *
 * CLAUDE.md가 "숫자만 바꾸면 되게" 분리하라고 한 지점이다.
 * 합이 1.0이어야 총점이 0~1 범위를 유지한다.
 */
object ScoreWeights {
    const val RATING = 0.5
    const val ROUTE = 0.5
}

/** Google 평점 만점. 평점을 0~1로 정규화할 때 나눈다. */
private const val MAX_RATING = 5.0

/**
 * 평점을 그대로 믿기 시작하는 리뷰 수.
 *
 * 리뷰가 이 값보다 적으면 그만큼 전체 평균 쪽으로 끌어당긴다.
 * 50으로 잡은 이유: 리뷰 50개면 한두 명의 극단적인 평가로 평균이 흔들리지 않는다.
 * Mock 데이터로 순위를 확인해보고 표본이 적은 곳이 여전히 상위에 오면 이 값을 올린다.
 */
const val REVIEW_CONFIDENCE_THRESHOLD = 50.0

/**
 * 리뷰가 적을 때 끌어당길 기준 평점.
 *
 * Google 평점은 4점대에 몰려 있어서 전체 평균을 4.0으로 잡았다.
 * 7단계에서 실제 검색 결과의 평균을 계산해 넘기도록 바꿀 수 있다.
 */
const val PRIOR_RATING = 4.0

/**
 * 동선 점수가 0이 되는 평균 거리(km).
 *
 * 해외 도시 여행에서 기준 장소들로부터 평균 10km를 넘으면 사실상 동선 밖이라 보고 잡았다.
 * 실제 Mock 데이터를 넣어보고 점수 분포가 한쪽으로 쏠리면 이 숫자를 조정한다.
 */
const val ROUTE_ZERO_DISTANCE_KM = 10.0

/** 지구 반지름(km). 하버사인 공식에 쓴다. */
private const val EARTH_RADIUS_KM = 6371.0

/**
 * 표본 수를 감안해 보정한 평점. 베이지안 평균(축소 추정).
 *
 *     보정평점 = (v / (v+m)) × 별점 + (m / (v+m)) × 전체평균
 *     v = 리뷰 수, m = REVIEW_CONFIDENCE_THRESHOLD
 *
 * 리뷰가 많을수록 자기 별점을 그대로 쓰고, 적을수록 전체 평균으로 끌려간다.
 * 이 보정이 없으면 리뷰 4개짜리 5.0점 식당이 리뷰 1900개짜리 4.8점 노포를 이겨버린다.
 */
fun adjustedRating(rating: Double, reviewCount: Int): Double {
    val reviews = reviewCount.coerceAtLeast(0).toDouble()
    val weight = reviews / (reviews + REVIEW_CONFIDENCE_THRESHOLD)
    return weight * rating + (1 - weight) * PRIOR_RATING
}

/**
 * 평점 점수: 보정 평점을 0~1로 정규화한다.
 *
 * 별점을 그대로 쓰지 않고 리뷰 수까지 반영하는 이유는 adjustedRating 주석 참고.
 */
fun ratingScore(rating: Double, reviewCount: Int): Double =
    (adjustedRating(rating, reviewCount) / MAX_RATING).coerceIn(0.0, 1.0)

/**
 * 일정의 중심점. 위도와 경도의 **중앙값**을 각각 취한다.
 *
 * 평균을 쓰면 외딴 한 곳이 중심을 끌고 간다 — 도쿄 일정에 하코네가 하나 끼면
 * 중심이 남서쪽으로 밀려 정작 도심 숙소가 손해를 본다. 중앙값은 그런 이상치에 흔들리지 않는다.
 */
fun medianCenter(locations: List<LatLng>): LatLng? {
    if (locations.isEmpty()) return null
    val latitudes = locations.map { it.latitude }.sorted()
    val longitudes = locations.map { it.longitude }.sorted()
    return LatLng(latitudes[latitudes.size / 2], longitudes[longitudes.size / 2])
}

/**
 * 동선 점수: 일정의 중심에 가까울수록 1에 가깝다.
 *
 * 각 장소까지의 평균 거리가 아니라 **중심점 하나까지의 거리**를 쓴다.
 * 평균 거리는 일정이 넓게 퍼진 도시에서 어느 후보든 비슷한 값이 되어 변별력을 잃는다.
 *
 * @param anchors 이번 일정에 **실제 포함된** 장소들. 가지도 않을 곳은 넣지 않는다.
 *                비어 있으면 동선을 따질 수 없으므로 0.0을 준다
 *                (모르는 값을 만점으로 쳐서 순위를 왜곡하지 않기 위함).
 */
fun routeScore(target: LatLng, anchors: List<LatLng>): Double {
    val center = medianCenter(anchors) ?: return 0.0
    return (1.0 - distanceKm(target, center) / ROUTE_ZERO_DISTANCE_KM).coerceIn(0.0, 1.0)
}

/** 총점 = 평점 점수 × 가중치 + 동선 점수 × 가중치. 0.0 ~ 1.0 */
fun totalScore(candidate: ScoredCandidate, anchors: List<LatLng>): Double =
    ScoreWeights.RATING * ratingScore(candidate.rating, candidate.reviewCount) +
        ScoreWeights.ROUTE * routeScore(candidate.location, anchors)

/**
 * 두 좌표 사이의 거리(km). 하버사인 공식으로 지구를 구로 근사한다.
 *
 * 도보·대중교통 실제 이동거리가 아니라 직선거리다. 추천은 상대 비교만 하면 되므로
 * 이 정도 근사로 충분하고, 외부 경로 API를 부르지 않아도 된다.
 */
fun distanceKm(a: LatLng, b: LatLng): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val deltaLat = Math.toRadians(b.latitude - a.latitude)
    val deltaLon = Math.toRadians(b.longitude - a.longitude)

    val h = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(h).coerceAtMost(1.0))
}