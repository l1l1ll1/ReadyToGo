package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.domain.weather.DailyWeather
import java.time.LocalDate

/**
 * 일별 날씨 예보. 지금 구현은 Open-Meteo(무료, 키 없음)를 앱이 직접 부른다.
 *
 * 함수를 거치지 않는 이유: 숨길 키가 없고, 거치면 지연과 함수 실행 비용만 는다.
 */
interface WeatherRepository {
    /**
     * 여행 날짜 [dates] 중 오늘부터 7일 안의 날 예보. 거르는 것은 구현이 한다.
     * 받지 못한 날은 결과에 없다. 실패해도 계획은 그대로 보여야 하므로 예외를 던지지 않고
     * 빈 결과를 돌려준다 — 날씨는 덧붙이는 정보다.
     */
    suspend fun dailyForecast(location: LatLng, dates: List<LocalDate>): Map<LocalDate, DailyWeather>
}
