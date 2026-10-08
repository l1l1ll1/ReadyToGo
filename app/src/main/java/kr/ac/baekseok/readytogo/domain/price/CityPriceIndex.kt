package kr.ac.baekseok.readytogo.domain.price

/**
 * 도시 물가 배수.
 *
 * 숙소는 Places가 요금을 주지 않아 등급(1~4)으로 추정할 수밖에 없는데, 등급을 고정 금액표로
 * 환산하면 물가가 다른 나라에서 크게 빗나간다. 같은 도시 식당의 **실제** 가격이 이미
 * 손에 있으므로, 그 값에서 물가 수준을 끌어내 숙소 추정에 곱한다.
 *
 * 식당 Repository를 직접 참조하지 않는다. 가격 목록만 받아 배수 하나를 돌려주는 순수 계산이라,
 * 숙소 코드는 이 배수 값 하나만 알면 된다.
 *
 * **가정**: 표본이 평점순 상위 식당이라 그 도시의 일반 식당보다 비쌀 수 있다.
 * 다만 이 편향은 모든 도시에 똑같이 적용되므로, 도시 간 **비율**을 구하는 배수에는
 * 영향이 크지 않다고 본다. 절대 금액이 아니라 상대 비교에 쓰는 값이라는 점이 중요하다.
 */
object CityPriceIndex {

    /** 이보다 표본이 적으면 중앙값을 믿을 수 없어 배수를 적용하지 않는다. */
    const val MIN_SAMPLE_SIZE = 10

    /** 배수 하한. 지나치게 낮은 추정으로 숙소가 전부 "예산 내"가 되는 것을 막는다. */
    const val MIN_MULTIPLIER = 0.5

    /** 배수 상한. 표본이 고급 식당에 쏠렸을 때 숙소가 전부 "예산 초과"가 되는 것을 막는다. */
    const val MAX_MULTIPLIER = 2.0

    /**
     * 배수 1.0의 기준점. 한 끼 중앙값이 이 금액인 도시를 기준으로 삼는다(서울·도쿄 수준).
     *
     * 데이터에서 끌어낼 수 없는 앵커라 직접 정한 값이다. 이 값이 틀리면 모든 도시의
     * 숙소 추정가가 같은 방향으로 밀리므로, 조정이 필요하면 여기만 바꾸면 된다.
     */
    const val BASELINE_MEAL_MEDIAN_WON = 25_000L

    /**
     * @param mealPricesWon 그 도시 식당들의 1인 1끼 가격(원).
     * @return 0.5 ~ 2.0 사이의 배수. 표본이 부족하면 1.0(=조정하지 않음).
     */
    fun of(mealPricesWon: List<Long>): Double {
        if (mealPricesWon.size < MIN_SAMPLE_SIZE) return 1.0

        val median = mealPricesWon.sorted()[mealPricesWon.size / 2]
        if (median <= 0) return 1.0

        return (median.toDouble() / BASELINE_MEAL_MEDIAN_WON)
            .coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)
    }
}
