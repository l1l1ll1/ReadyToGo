package kr.ac.baekseok.readytogo.domain.budget

import kr.ac.baekseok.readytogo.data.model.Lodging

/**
 * 숙소 가격 등급을 1박 예상가로 바꾸는 표.
 *
 * **기본표는 배수 1.0 도시(한 끼 중앙값 2.5만 원, 서울·도쿄 수준) 기준이다.**
 * 다른 도시에는 CityPriceIndex가 구한 배수를 곱해 물가 차이를 반영한다.
 *
 * Places는 숙소 요금을 아예 주지 않는다. 식당은 실제 가격이 있어서 "있으면 실제값, 없으면 추정"이
 * 가능했지만 숙소는 **전부 추정**이다. 그래서 화면에 "예상"을 반드시 붙인다.
 * 유료 예약 API를 붙이기 전까지는 이 한계가 남는다.
 */
object LodgingPriceTable {

    /** 등급별 1박 예상가(원). 배수 1.0 도시 기준. 숫자만 바꿔 조정할 수 있다. */
    val BASE_PRICE_BY_LEVEL = mapOf(
        0 to 60_000L,
        1 to 90_000L,
        2 to 150_000L,
        3 to 260_000L,
        4 to 450_000L,
    )

    /** 등급 정보가 없는 숙소는 중간 등급으로 본다. */
    const val DEFAULT_LEVEL = 2

    fun basePriceWon(level: Int?): Long =
        BASE_PRICE_BY_LEVEL[level] ?: BASE_PRICE_BY_LEVEL.getValue(DEFAULT_LEVEL)
}

/**
 * 도시 물가 배수를 적용한다.
 *
 * Repository는 배수를 모른 채 기본표 값으로 채워 두고(식당 검색을 기다리지 않기 위함),
 * 여기서 한 번에 조정한다. 숙소 쪽이 아는 것은 배수 값 하나뿐이다.
 */
fun Lodging.applyPriceIndex(index: Double): Lodging =
    copy(priceWon = (priceWon * index).toLong())
