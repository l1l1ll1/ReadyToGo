package kr.ac.baekseok.readytogo.ui.input

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 날짜·금액 표기를 입력 화면과 시트가 같이 쓴다.
 *
 * 시트 안과 카드에 같은 기간이 다른 모양으로 적히면 같은 값인지 확신할 수 없다.
 */

/** 요일이 기기 언어를 따라가면 한국어 화면에 "10.24(Fri)"가 섞인다. 한국어로 고정한다. */
private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M.d(E)", Locale.KOREAN)

/**
 * "10.24(금) – 10.28(화) · 4박 5일"
 *
 * 박과 일을 같이 적는 이유: 숙소를 며칠 잡아야 하는지는 "박"이고,
 * 일정이 며칠인지는 "일"이다. 둘 중 하나만 적으면 매번 헷갈린다.
 */
fun formatTripRange(departure: LocalDate, returnDate: LocalDate): String {
    val nights = ChronoUnit.DAYS.between(departure, returnDate)
    val days = nights + 1
    return "${departure.format(DATE_FORMAT)} – ${returnDate.format(DATE_FORMAT)} · ${nights}박 ${days}일"
}

/** "1,500,000원" */
fun formatWon(won: Long): String = "%,d원".format(won)

/**
 * "150만원" — 칩이나 슬라이더 눈금처럼 좁은 자리에 쓴다.
 * 만 단위로 끊기지 않으면 원 단위로 적는다.
 */
fun formatWonShort(won: Long): String = when {
    won >= 10_000 && won % 10_000 == 0L -> "${won / 10_000}만원"
    else -> formatWon(won)
}
