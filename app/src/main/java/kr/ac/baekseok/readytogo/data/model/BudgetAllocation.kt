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
    /** 실제 검색된 항공권 최저가. **인원 전체의 총액**이다(SerpApi에 인원을 넘겨 받는다). */
    val flightWon: Long?,
    /** 총예산에서 항공료를 뺀 금액. 항공료가 예산을 넘겼으면 0. */
    val remainingWon: Long?,
    /**
     * 숙소 **1박 1실** 상한. recommendPair의 maxPriceWon으로 그대로 넘긴다.
     *
     * 숙소 가격이 1실 기준으로 들어오므로 상한도 1실 기준이어야 비교가 맞는다.
     * 방이 여러 개면 전체 숙박비는 이 값 × [roomCount] × 박수다.
     */
    val lodgingPerNightWon: Long?,
    /** 식당 **1인 1끼** 상한. 식당 가격이 1인 기준이라 상한도 1인 기준이다. */
    val mealWon: Long?,
    /** 현지 교통·입장료·예비비 몫. 추천에는 쓰지 않고 화면에 안내용으로 보여준다. */
    val otherWon: Long?,
    /** 항공료만으로 총예산을 넘긴 경우. 화면에서 경고를 띄울 근거다. */
    val flightAloneExceedsBudget: Boolean,
    /**
     * 이 배분이 몇 명 기준인지. 화면이 "성인 2명 · 2인 1실 2방"을 적으려면 알아야 한다.
     * 계산 결과를 화면이 다시 구하면 규칙이 두 곳에 생겨 어긋난다.
     */
    val adults: Int = 1,
    val roomCount: Int = 1,
)