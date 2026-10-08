package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.domain.purpose.PurposeAttractionTypes

/**
 * 고른 목적과 맞는 장소에 더해 주는 점수.
 *
 * 평점·동선만으로 고르면 "유명하고 가까운 곳"만 남아서, 휴양을 골랐는데 박물관이 가득한
 * 일정이 나올 수 있다. 목적에 맞는 곳을 조금 밀어 올린다.
 *
 * 0.15인 이유 — 평점 한 단계(★0.5 차이)가 점수로 약 0.05다. 가산점이 그보다 크면 목적이
 * 평점을 이기고, 너무 작으면 아무 영향이 없다. 0.15면 비슷한 평점끼리는 목적이 이기고,
 * 평점이 크게 벌어지면 평점이 이긴다.
 */
const val PURPOSE_MATCH_BONUS = 0.15

/**
 * 관광지 점수. 숙소·식당과 달리 가격이 없어 전용 함수를 둔다.
 *
 *     점수 = 평점 점수 × 0.5 + 동선 점수 × 0.5 (+ 목적 일치 0.15)
 *
 * 가산점은 가중 합산이 끝난 뒤에 더한다. 그래서 최대 점수가 1.15까지 올라갈 수 있는데,
 * 순위를 매기는 용도라 절대값이 1을 넘어도 문제가 없다.
 *
 * @param anchors 동선 기준. 후보 관광지들이 몰려 있는 쪽에 가까울수록 높은 점수를 준다.
 *                외딴 곳 하나 때문에 하루가 통째로 무너지는 것을 막는다.
 */
fun attractionScore(
    attraction: Attraction,
    anchors: List<LatLng>,
    purposes: Set<TravelPurpose>,
): Double {
    val base = ScoreWeights.RATING * ratingScore(attraction.rating, attraction.reviewCount) +
        ScoreWeights.ROUTE * routeScore(attraction.location, anchors)

    val bonus = if (PurposeAttractionTypes.matches(attraction.types, purposes)) {
        PURPOSE_MATCH_BONUS
    } else {
        0.0
    }
    return base + bonus
}
