package kr.ac.baekseok.readytogo.domain.budget

import java.util.Locale
import kotlin.math.roundToLong

/**
 * 금액을 어떤 돈으로 **보여줄지**.
 *
 * **계산은 끝까지 원화로 한다.** 환산은 화면에 찍기 직전에만 한다. 중간 계산을 달러로
 * 바꾸면 예산 배분·점수 판정이 반올림 오차를 타고, 항공·숙소 가격이 원화로 들어오는
 * 것과도 단위가 어긋난다.
 *
 * **고정 환율을 쓰는 이유**: 예산을 가늠하는 자리라 소수점 둘째 자리까지 맞을 필요가 없고,
 * 환율 API를 한 번 더 붙이면 네트워크 실패 때 금액이 아예 안 보이는 경우가 생긴다.
 * 숫자가 실제와 벌어지면 [USD_KRW_RATE] 하나만 고친다.
 */
object MoneyDisplay {

    /** 1달러가 몇 원인지. 환율이 크게 움직이면 이 값만 바꾼다. */
    const val USD_KRW_RATE = 1380L

    /**
     * 지금 달러로 보여줄 때인지.
     *
     * 앱 언어는 LocaleManager가 프로세스 기본 Locale에 반영한다(AppLocale 주석 참고).
     * 값을 들고 있지 않고 쓸 때마다 읽는 이유는 언어가 실행 중에 바뀌기 때문이다.
     */
    val isUsd: Boolean get() = Locale.getDefault().language == "en"

    /** 원 → 달러. 1달러 미만은 올려 0달러가 되지 않게 한다. */
    fun usdOf(won: Long): Long =
        (won.toDouble() / USD_KRW_RATE).roundToLong().coerceAtLeast(if (won > 0) 1 else 0)

    /** 달러 → 원. 슬라이더 눈금처럼 달러 기준으로 정한 값을 실제 금액으로 되돌릴 때. */
    fun wonOfUsd(usd: Long): Long = usd * USD_KRW_RATE

    /** 화면에 찍을 숫자. 달러로 보여줄 때만 환산한다. */
    fun amountFor(won: Long): Long = if (isUsd) usdOf(won) else won
}
