package kr.ac.baekseok.readytogo.data.repository.weather

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.repository.WeatherRepository
import kr.ac.baekseok.readytogo.domain.weather.DailyWeather
import kr.ac.baekseok.readytogo.domain.weather.forecastDates
import kr.ac.baekseok.readytogo.domain.weather.skyOf
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo 일별 예보. 무료·키 없음(비상업 이용, CC BY 4.0 — 화면에 출처를 적는다).
 *
 * - 여행 날짜 중 오늘부터 7일 안의 날만 묻는다([forecastDates]). 없으면 호출하지 않는다.
 * - 같은 좌표·기간은 [CACHE_TTL_HOURS]시간 동안 기기에 저장해 다시 묻지 않는다.
 *   예보는 몇 시간마다 갱신되므로 하루씩 묵히면 낡는다.
 * - 실패하면 빈 결과. 날씨는 덧붙이는 정보라 계획 화면을 막지 않는다.
 */
class OpenMeteoWeatherRepository(context: Context) : WeatherRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun dailyForecast(location: LatLng, dates: List<LocalDate>): Map<LocalDate, DailyWeather> {
        // 디버그 빌드에서만 0이 아닐 수 있다(WeatherDebugShift 참고).
        val shift = WeatherDebugShift.days()
        val today = LocalDate.now().plusDays(shift)
        val wanted = forecastDates(dates, today)
        if (wanted.isEmpty()) return emptyMap()

        // 실제로 물어보는 날짜. 디버그 이동이 있으면 그만큼 당겨 "지금 받을 수 있는 예보"를 묻는다.
        val start = wanted.min().minusDays(shift)
        val end = wanted.max().minusDays(shift)
        val body = cached(location, start, end) ?: fetch(location, start, end) ?: return emptyMap()

        return runCatching { parse(body) }
            .getOrElse {
                Log.w(TAG, "날씨 응답을 읽지 못했다", it)
                emptyMap()
            }
            .mapKeys { (date, _) -> date.plusDays(shift) }
            .mapValues { (date, weather) -> weather.copy(date = date) }
            .filterKeys { it in wanted }
    }

    private suspend fun fetch(location: LatLng, start: LocalDate, end: LocalDate): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = String.format(
                    Locale.US,
                    "%s?latitude=%.4f&longitude=%.4f&daily=precipitation_probability_max,weather_code" +
                        "&timezone=auto&start_date=%s&end_date=%s",
                    API_URL, location.latitude, location.longitude, start, end,
                )
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
                val body = try {
                    connection.inputStream.bufferedReader().readText()
                } finally {
                    connection.disconnect()
                }
                Log.d(TAG, "날씨 호출 $start~$end")
                prefs.edit()
                    .putString(keyOf(location, start, end), body)
                    .putLong(keyOf(location, start, end) + SAVED_AT_SUFFIX, System.currentTimeMillis())
                    .apply()
                body
            }
                .onFailure { Log.w(TAG, "날씨를 받지 못했다", it) }
                .getOrNull()
        }

    private fun cached(location: LatLng, start: LocalDate, end: LocalDate): String? {
        val key = keyOf(location, start, end)
        val savedAt = prefs.getLong(key + SAVED_AT_SUFFIX, 0L)
        if (System.currentTimeMillis() - savedAt > TTL_MILLIS) return null
        return prefs.getString(key, null)?.also { Log.d(TAG, "날씨 캐시 사용 $start~$end") }
    }

    /** 좌표는 소수 둘째 자리(약 1km). 도시 날씨라 그보다 촘촘할 이유가 없다. */
    private fun keyOf(location: LatLng, start: LocalDate, end: LocalDate) =
        String.format(Locale.US, "%.2f|%.2f|%s|%s", location.latitude, location.longitude, start, end)

    /** `daily.time`·`precipitation_probability_max`·`weather_code`는 같은 순서의 배열이다. */
    private fun parse(body: String): Map<LocalDate, DailyWeather> {
        val daily = JSONObject(body).getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val probabilities = daily.getJSONArray("precipitation_probability_max")
        val codes = daily.getJSONArray("weather_code")
        return (0 until times.length()).associate { i ->
            val date = LocalDate.parse(times.getString(i))
            val probability = if (probabilities.isNull(i)) null else probabilities.getInt(i)
            date to DailyWeather(date, probability, skyOf(codes.optInt(i)))
        }
    }

    private companion object {
        const val TAG = "Weather"
        const val PREFS_NAME = "weather_cache"
        const val SAVED_AT_SUFFIX = "|savedAt"
        const val API_URL = "https://api.open-meteo.com/v1/forecast"
        const val TIMEOUT_MS = 5_000

        /** 사용자가 정한 캐시 유효기간(6시간). */
        const val CACHE_TTL_HOURS = 6L
        val TTL_MILLIS = TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS)
    }
}
