package kr.ac.baekseok.readytogo.data.repository.places

import com.google.android.libraries.places.api.model.Place
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.repository.ExchangeRateRepository

/**
 * Places가 준 가격대를 원화로 바꾼다.
 *
 * **검색과 상세 조회가 같은 규칙을 써야 한다.** 즐겨찾기를 일정에 끼워 넣을 때
 * 장소 하나만 따로 받아 오는데(GooglePlaceDetailsRepository), 거기서 값을 다르게
 * 계산하면 같은 식당이 자리에 따라 다른 가격으로 보인다. 그래서 두 곳이 함께 쓰도록 뺐다.
 */
internal data class ResolvedPrice(
    val maxWon: Long,
    val minWon: Long?,
    val localText: String?,
    val estimated: Boolean,
    /** 하한이 사실상 없어 "이 금액 이하"로만 말할 수 있는 경우. */
    val upperBoundOnly: Boolean = false,
)

/**
 * 가격을 원화로 환산한다.
 *
 * Places가 실제 가격대(예: €20~€40)를 주면 그것을 쓰고, **예산 비교에는 상한**을 쓴다.
 * 하한으로 비교하면 "예산 내"로 추천해놓고 현지에서 모자랄 수 있다.
 *
 * 주의: 상한은 **보장된 최대 금액이 아니다.** Google이 사용자 데이터로 추정한
 * "1인당 통상 지출 범위의 위쪽"이라, 비싼 메뉴나 술을 곁들이면 넘을 수 있다.
 * 상한이 아예 없는 집(예: "$100 이상")은 endPrice가 비어 있어 null이 된다.
 *
 * 가격대가 없으면 null이다. 검색에서는 등급 추정으로 넘어가고, 상세 조회에서는
 * "가격 정보 없음"이 된다 — 한 곳만 보고는 그 도시의 시세를 알 수 없기 때문이다.
 */
internal suspend fun Place.realPrice(exchangeRates: ExchangeRateRepository): ResolvedPrice? {
    val range = priceRange ?: return null
    val start = range.startPrice
    val end = range.endPrice
    val currency = end?.currencyCode ?: start?.currencyCode

    if (end?.units == null || currency == null) return null

    // 환율을 못 구하면 포기한다. 계획 전체를 실패시킬 일은 아니다.
    val rate = exchangeRates.rateToKrw(currency).getOrElse { return null }

    // Google은 최저가 구간을 "¥1~¥1,000"처럼 1단위부터로 표기한다.
    // 그대로 환산하면 "8원~8,614원"이 되어 8원짜리 아침처럼 보인다.
    // 사실상 "1,000엔 이하"라는 뜻이므로 상한만 말한다.
    val startUnits = start?.units?.takeIf { it > MEANINGFUL_MIN_UNITS }
    val upperBoundOnly = start?.units != null && startUnits == null

    return ResolvedPrice(
        maxWon = (end.units!! * rate).toLong(),
        minWon = startUnits?.let { (it * rate).toLong() },
        localText = formatLocalPrice(currency, startUnits, end.units!!, upperBoundOnly),
        estimated = false,
        upperBoundOnly = upperBoundOnly,
    )
}

/** "¥1,000~¥2,000" / "¥2,000 이하" — 메뉴판과 바로 대조하라고 현지 통화로도 적는다. */
internal fun formatLocalPrice(
    currency: String,
    start: Long?,
    end: Long,
    upperBoundOnly: Boolean,
): String {
    val symbol = CURRENCY_SYMBOLS[currency] ?: "$currency "
    val endText = "$symbol${"%,d".format(end)}"
    return when {
        upperBoundOnly -> AppContainer.string(R.string.place_price_upper_bound, endText)
        start != null && start != end -> "$symbol${"%,d".format(start)}~$endText"
        else -> endText
    }
}

/** 이 값 이하의 하한은 "사실상 없음"으로 본다. 위 주석의 "¥1부터" 표기 때문이다. */
internal const val MEANINGFUL_MIN_UNITS = 1L

/** 관광지는 Places가 머무는 시간을 주지 않는다. 한 곳에 한 시간 반으로 잡는다. */
internal const val DEFAULT_STAY_MINUTES = 90

/** 평점이 없는 관광지. 빼지 않고 중간값을 줘서 유명하지 않은 곳도 후보로 남긴다. */
internal const val DEFAULT_ATTRACTION_RATING = 4.0

internal val CURRENCY_SYMBOLS = mapOf(
    "USD" to "$", "EUR" to "€", "JPY" to "¥", "GBP" to "£",
    "CNY" to "¥", "KRW" to "₩", "THB" to "฿", "VND" to "₫",
    "PHP" to "₱", "INR" to "₹", "TRY" to "₺",
)
