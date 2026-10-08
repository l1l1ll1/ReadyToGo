package kr.ac.baekseok.readytogo.domain.recommend

import kr.ac.baekseok.readytogo.data.model.AlternativeKind
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Recommendation
import kr.ac.baekseok.readytogo.data.model.RecommendationPair
import kr.ac.baekseok.readytogo.data.model.ScoredCandidate

/**
 * "품질 우선" 항목이 예산 상한의 몇 배까지 허용되는지.
 *
 * 상한을 두는 이유: 이 값이 없으면 1끼 상한 24,640원짜리 여행에 95,000원 오마카세가 추천된다.
 * 예산의 4배짜리 추천은 사용자가 실제로 고를 수 없어서 선택지 한 칸이 장식이 된다.
 * 2배로 잡으면 "조금 더 쓰면 이 정도 / 예산 맞추면 이 정도"라는 진짜 선택이 된다.
 *
 * 숫자만 바꿔 조정할 수 있게 상수로 뒀다.
 */
const val QUALITY_PRICE_MULTIPLIER = 2.0

/**
 * 후보들 중에서 "품질 우선"과 "예산 내" 두 가지를 고른다(CLAUDE.md 추천 로직).
 *
 * @param anchors 동선 점수의 기준 장소들. 숙소는 여행 전체 관광지, 식당은 그날 관광지를 넘긴다.
 * @param maxPriceWon **이 항목 하나에 쓸 수 있는 상한(원)**. 여행 총예산이 아니다.
 *        숙소 1박 가격을 총예산과 비교하면 거의 모든 후보가 "예산 내"가 되어 버리므로,
 *        총예산을 항공·숙박·식비로 나누는 일은 호출부(3~4단계)가 하고 여기는 상한만 받는다.
 *        null이면 예산 제한 없이 고른다.
 * @return 후보가 하나도 없으면 null.
 */
fun <T : ScoredCandidate> recommendPair(
    candidates: List<T>,
    anchors: List<LatLng>,
    maxPriceWon: Long?,
): RecommendationPair<T>? {
    if (candidates.isEmpty()) return null

    // 점수 내림차순. 점수가 같으면 싼 쪽이 위로 온다.
    val ranked = candidates
        .map { it to totalScore(it, anchors) }
        .sortedWith(
            compareByDescending<Pair<T, Double>> { it.second }.thenBy { it.first.priceWon }
        )

    // 품질 우선도 무제한은 아니다. 상한의 QUALITY_PRICE_MULTIPLIER배까지만 후보로 본다.
    // 그 안에 아무것도 없으면(전부 너무 비싸면) 제한 없이 점수 1등을 쓴다 — 한 칸을 비울 수는 없다.
    val qualityCap = maxPriceWon?.let { (it * QUALITY_PRICE_MULTIPLIER).toLong() }
    val best = ranked.firstOrNull { qualityCap == null || it.first.priceWon <= qualityCap }
        ?: ranked.first()

    val others = ranked.filter { it.first !== best.first }
    val bestOverBudget = maxPriceWon != null && best.first.priceWon > maxPriceWon

    // 두 번째 선택지를 고르는 규칙.
    //
    // 1등이 예산을 넘겼다면 사용자에게 필요한 건 "그럼 예산 안에서는 뭐가 있나"다.
    // 반대로 1등이 이미 예산 안이라면 같은 값을 또 보여줄 이유가 없고,
    // "더 싸게 가면 뭐가 있나"가 실제로 도움이 되는 질문이다.
    // 숙소는 가격이 등급에서 나오므로 "더 싸다"가 곧 "한 등급 아래"와 같다.
    val (runnerUp, kind) = when {
        // 후보가 하나뿐이면 대안 칸을 비운다. 같은 곳을 두 번 보여주면 선택지가 있는 것처럼 보인다.
        others.isEmpty() -> null to AlternativeKind.NONE

        bestOverBudget -> {
            val affordable = others.filter { maxPriceWon == null || it.first.priceWon <= maxPriceWon }
            // 예산 내가 하나도 없으면 그나마 가장 싼 것을 보여준다. 칸을 비울 수는 없다.
            val pick = affordable.firstOrNull() ?: others.minBy { it.first.priceWon }
            pick to AlternativeKind.WITHIN_BUDGET
        }

        else -> {
            val cheaper = others.filter { it.first.priceWon < best.first.priceWon }
            if (cheaper.isNotEmpty()) {
                cheaper.first() to AlternativeKind.CHEAPER
            } else {
                // 더 싼 후보가 없다(숙소는 같은 등급이면 값이 같아 흔하다).
                // 가격으로 못 가르면 동선으로 가른다.
                others.maxBy { routeScore(it.first.location, anchors) } to AlternativeKind.CLOSEST
            }
        }
    }

    return RecommendationPair(
        qualityFirst = best.toRecommendation(maxPriceWon, anchors),
        withinBudget = runnerUp?.toRecommendation(maxPriceWon, anchors),
        alternativeKind = kind,
    )
}

private fun <T : ScoredCandidate> Pair<T, Double>.toRecommendation(
    maxPriceWon: Long?,
    anchors: List<LatLng>,
): Recommendation<T> = Recommendation(
    item = first,
    score = second,
    overBudget = maxPriceWon != null && first.priceWon > maxPriceWon,
    distanceFromCenterKm = medianCenter(anchors)?.let { distanceKm(first.location, it) },
)