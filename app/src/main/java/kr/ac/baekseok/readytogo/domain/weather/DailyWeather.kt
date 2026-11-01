package kr.ac.baekseok.readytogo.domain.weather

import java.time.LocalDate

/** 날짜 칩에 그릴 하늘. 비·눈을 나누는 이유: 겨울 삿포로가 전부 "비"로 보이면 틀린 정보다. */
enum class Sky { CLEAR, CLOUDY, RAIN, SNOW }

/**
 * 하루 날씨 예보.
 *
 * @param precipitationProbability 그날 최고 강수확률(%). 눈도 강수에 들어간다. 모르면 null.
 */
data class DailyWeather(
    val date: LocalDate,
    val precipitationProbability: Int?,
    val sky: Sky,
)

/**
 * WMO 날씨 코드(Open-Meteo `weather_code`) → 하늘.
 *
 * 0~1 맑음, 2~3·안개(45, 48) 흐림, 이슬비·비·소나기·뇌우 비, 눈·눈 소나기 눈.
 * 표에 없는 코드는 흐림으로 둔다 — 맑음으로 틀리는 것보다 덜 해롭다.
 */
fun skyOf(code: Int): Sky = when (code) {
    0, 1 -> Sky.CLEAR
    in 71..77, 85, 86 -> Sky.SNOW
    in 51..67, in 80..82, in 95..99 -> Sky.RAIN
    else -> Sky.CLOUDY
}

/**
 * 예보를 물어볼 여행 날짜. **오늘부터 [FORECAST_WINDOW_DAYS]일 안의 날만** 남긴다.
 *
 * 그보다 먼 예보는 맞을 확률이 낮아, 일정을 바꾸라고 권할 근거가 못 된다.
 * 하나도 없으면 호출 자체를 하지 않는다(대부분의 일정은 출발 1주 전에 열어 보지 않는다).
 */
fun forecastDates(tripDates: List<LocalDate>, today: LocalDate): List<LocalDate> =
    tripDates.filter { !it.isBefore(today) && !it.isAfter(today.plusDays(FORECAST_WINDOW_DAYS)) }

/** 예보를 믿고 일정을 바꾸라고 할 수 있는 기간(일). 사용자가 정한 값이다(D-7). */
const val FORECAST_WINDOW_DAYS = 7L

/** 이 강수확률 이상이면 실외 관광지를 문제로 본다(%). 사용자가 정한 값이다. */
const val RAINY_PROBABILITY = 60
