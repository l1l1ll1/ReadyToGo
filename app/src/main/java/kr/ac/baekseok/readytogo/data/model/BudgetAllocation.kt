package kr.ac.baekseok.readytogo.data.model

/**
 * 예산 배분 결과. 계산은 domain/budget의 allocateBudget이 한다.
 *
 * 계산 함수가 아니라 결과 자료형만 모델에 두는 이유: TripPlan이 이 값을 들고 결과 화면까지
 * 내려가야 하는데, data/model이 domain을 import하면 계층 방향이 뒤집히기 때문이다.
 *
 * 상한 값이 null이면 "예산을 입력하지 않아 제한이 없다"는 뜻이다.
 * 0은 "쓸 수 있는 돈이 없다"는 뜻이므로 서로 다르다.
 */
data class BudgetAllocation(
    val totalWon: Long?,
    /** 실제 검색된 항공권 최저가. 검색 결과가 없으면 null. */
    val flightWon: Long?,
    /** 총예산에서 항공료를 뺀 금액. 항공료가 예산을 넘겼으면 0. */
    val remainingWon: Long?,
    /** 숙소 1박 상한. recommendPair의 maxPriceWon으로 그대로 넘긴다. */
    val lodgingPerNightWon: Long?,
    /** 식당 1인 1끼 상한. */
    val mealWon: Long?,
    /** 현지 교통·입장료·예비비 몫. 추천에는 쓰지 않고 화면에 안내용으로 보여준다. */
    val otherWon: Long?,
    /** 항공료만으로 총예산을 넘긴 경우. 화면에서 경고를 띄울 근거다. */
    val flightAloneExceedsBudget: Boolean,
)