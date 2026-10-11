package kr.ac.baekseok.readytogo.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.domain.budget.MoneyDisplay
import kr.ac.baekseok.readytogo.domain.plan.TravelLeg
import kr.ac.baekseok.readytogo.domain.plan.splitDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 금액·날짜·기간 표기를 모든 화면이 같이 쓴다.
 *
 * **왜 리소스로 빼는가**: 언어마다 단위 위치와 어순이 다르다. 코드에서 `"${won}원"`처럼
 * 조립하면 영어에서 "339944원"이 그대로 나온다. 패턴을 리소스에 두면 언어 파일만 늘어난다.
 *
 * 전부 `@Composable`인 이유: 글자는 지금 적용된 언어에 따라 달라지므로 리소스를 읽을
 * 자리가 필요하다. 화면 밖(ViewModel, 도메인)에서는 쓰지 않는다 — 그쪽은 숫자를 넘긴다.
 */

/**
 * "339,944원" / "$246"
 *
 * 영어에서는 달러로 환산해 보여준다. 환산은 여기서만 한다 — 계산은 끝까지 원화다
 * ([MoneyDisplay] 주석 참고).
 */
@Composable
fun money(won: Long): String =
    stringResource(R.string.fmt_money, "%,d".format(MoneyDisplay.amountFor(won)))

/**
 * 가격대. "25,441~33,925원" / "$18~$25"
 *
 * **양쪽 다 환산해야 한다.** 한쪽만 바꿨더니 "8,480~$12"처럼 단위가 섞여 나왔다.
 */
@Composable
fun moneyRange(minWon: Long, maxWon: Long): String = stringResource(
    R.string.fmt_money_range,
    "%,d".format(MoneyDisplay.amountFor(minWon)),
    "%,d".format(MoneyDisplay.amountFor(maxWon)),
)

/**
 * "150만원" / "$1,500,000" — 칩이나 슬라이더 눈금처럼 좁은 자리.
 *
 * "만" 단위가 없는 언어에서는 전체 금액을 그대로 적는다. 만 단위로 떨어지지 않는
 * 금액도 마찬가지다 — "15.3만원"은 읽기 더 어렵다.
 */
@Composable
fun moneyShort(won: Long): String =
    if (!MoneyDisplay.isUsd && won >= MAN && won % MAN == 0L) {
        stringResource(R.string.fmt_money_man, "%,d".format(won / MAN))
    } else {
        // 달러는 자릿수가 원화의 1/1000이라 줄일 것이 없다. "$1,600"이 이미 짧다.
        money(won)
    }

/** "약 1시간 30분" / "About 1 hr 30 min" */
@Composable
fun durationText(minutes: Int): String {
    val (hours, rest) = splitDuration(minutes)
    return when {
        hours == 0 -> stringResource(R.string.fmt_duration_m, rest)
        rest == 0 -> stringResource(R.string.fmt_duration_h, hours)
        else -> stringResource(R.string.fmt_duration_hm, hours, rest)
    }
}

/** "도보 12분" / "Walk 12 min" */
@Composable
fun travelLegText(leg: TravelLeg): String =
    stringResource(R.string.fmt_travel_leg, stringResource(leg.mode.labelRes), leg.minutes)

/** "10.24" / "Oct 24" */
@Composable
fun dateShort(date: LocalDate): String = date.format(rememberDateFormat(R.string.fmt_date_md))

/** "10.24(금)" / "Oct 24 (Fri)" */
@Composable
fun dateWithDay(date: LocalDate): String = date.format(rememberDateFormat(R.string.fmt_date_md_e))

/**
 * 수를 세는 말. 영어는 1과 그 외에서 말이 달라져 plurals로 둔다 — 문자열 하나로 두면
 * "1 adults", "1 days"가 그대로 찍힌다(실제로 요약 헤더에 나왔다).
 */
@Composable
fun daysText(days: Int): String = pluralStringResource(R.plurals.days_count, days, days)

@Composable
fun nightsText(nights: Int): String = pluralStringResource(R.plurals.nights_count, nights, nights)

@Composable
fun adultsText(adults: Int): String = pluralStringResource(R.plurals.adults_count, adults, adults)

@Composable
fun roomsText(rooms: Int): String = pluralStringResource(R.plurals.rooms_count, rooms, rooms)

/** "10.24(금) – 10.28(화) · 4박 5일" */
@Composable
fun tripRangeText(departure: LocalDate, returnDate: LocalDate): String {
    val nights = ChronoUnit.DAYS.between(departure, returnDate).toInt()
    return "${dateWithDay(departure)} – ${dateWithDay(returnDate)} · " +
        stringResource(R.string.fmt_nights_days, nightsText(nights), daysText(nights + 1))
}

/**
 * 패턴 문자열로 날짜 포맷터를 만든다.
 *
 * **요일 이름은 패턴이 아니라 Locale이 정한다.** 전에는 `Locale.KOREAN`으로 고정해
 * 뒀는데, 그러면 앱을 영어로 바꿔도 "Oct 24(금)"처럼 요일만 한국어로 남는다.
 * 앱 언어는 시스템 기본 Locale로 반영되므로([AppLocale]) 그대로 따라가면 된다.
 */
@Composable
private fun rememberDateFormat(patternRes: Int): DateTimeFormatter {
    val pattern = stringResource(patternRes)
    return remember(pattern) { DateTimeFormatter.ofPattern(pattern, Locale.getDefault()) }
}

private const val MAN = 10_000L
