package kr.ac.baekseok.readytogo.domain.budget

/**
 * 예산 슬라이더가 다룰 범위. **인원에 비례한다.**
 *
 * 왜 비례해야 하는가: 범위를 1인 기준으로 고정하면 3명 여행에서 슬라이더를 끝까지 올려도
 * 1인당 몫이 너무 적다. 실제로 3명·100만원으로 도쿄 7일을 돌렸더니 항공료가 99만원이라
 * 남은 돈이 3천원이 되고, 끼니 21칸 전부가 예산 초과로 찍혔다.
 *
 * 화면과 ViewModel이 같은 범위를 봐야 한다 — 화면은 슬라이더를 그리고 ViewModel은 인원이
 * 바뀔 때 현재 금액을 범위 안으로 되돌린다. 규칙이 두 곳에 있으면 어긋난다.
 */
object BudgetRange {

    /**
     * 1인당 하한·상한.
     *
     * 30만원 아래로는 해외여행 항공권조차 어렵고, 500만원을 넘기면 슬라이더 한 칸이 너무 커져
     * 조절이 안 된다. 그보다 큰 금액은 직접 입력으로 받는다(직접 입력에는 상한이 없다).
     */
    private const val MIN_PER_ADULT = 300_000L
    private const val MAX_PER_ADULT = 5_000_000L

    /**
     * 달러로 보여줄 때 쓰는 1인당 범위와 눈금.
     *
     * 원화 값을 그대로 환산하면 "$217 ~ $3,623", 칩은 "$362"처럼 어정쩡한 수가 나온다.
     * 보여줄 돈이 달러면 **달러 기준으로 떨어지는 수**를 먼저 정하고 원화로 되돌린다.
     */
    private const val MIN_PER_ADULT_USD = 200L
    private const val MAX_PER_ADULT_USD = 3_600L
    private const val STEP_USD = 100L
    private val QUICK_PER_ADULT_USD = listOf(400L, 800L, 1_200L, 1_600L)

    /**
     * 슬라이더가 끊는 단위. 1원 단위로 움직이면 "1,237,419원" 같은 값이 나온다.
     * 달러로 보여줄 때는 $100씩 움직여야 눈금도 떨어진다.
     */
    val stepWon: Long
        get() = if (MoneyDisplay.isUsd) MoneyDisplay.wonOfUsd(STEP_USD) else STEP_WON

    private const val STEP_WON = 100_000L

    /** 빠른 선택 칩의 1인당 금액. */
    private val QUICK_PER_ADULT = listOf(500_000L, 1_000_000L, 1_500_000L, 2_000_000L)

    fun minFor(adults: Int): Long = perAdultMin() * adults

    fun maxFor(adults: Int): Long = perAdultMax() * adults

    /** 빠른 선택 칩 금액. 인원을 곱해 "3명이면 300만원" 같은 실제 총액이 나온다. */
    fun quickOptionsFor(adults: Int): List<Long> = quickPerAdult().map { it * adults }

    private fun perAdultMin(): Long =
        if (MoneyDisplay.isUsd) MoneyDisplay.wonOfUsd(MIN_PER_ADULT_USD) else MIN_PER_ADULT

    private fun perAdultMax(): Long =
        if (MoneyDisplay.isUsd) MoneyDisplay.wonOfUsd(MAX_PER_ADULT_USD) else MAX_PER_ADULT

    private fun quickPerAdult(): List<Long> =
        if (MoneyDisplay.isUsd) QUICK_PER_ADULT_USD.map { MoneyDisplay.wonOfUsd(it) } else QUICK_PER_ADULT

    /**
     * 금액을 범위 안으로 되돌린다.
     *
     * 인원을 바꿀 때 쓴다. 사용자가 정한 금액은 **그대로 두는 것이 원칙**이고,
     * 범위를 벗어날 때만 가장 가까운 끝으로 옮긴다 — 인원을 한 명 늘렸다고
     * 애써 맞춰둔 예산이 기본값으로 돌아가면 당황스럽다.
     */
    fun coerce(won: Long, adults: Int): Long = won.coerceIn(minFor(adults), maxFor(adults))
}
