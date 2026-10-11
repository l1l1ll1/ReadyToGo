package kr.ac.baekseok.readytogo.data.repository.flight

import android.content.Context
import android.util.Log
import kr.ac.baekseok.readytogo.BuildConfig
import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.FlightRepository
import kr.ac.baekseok.readytogo.data.repository.mock.MockFlightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SerpApi의 Google Flights로 항공권을 찾는다.
 *
 * 무료 한도가 월 250회라 두 가지 장치를 둔다.
 * - **캐시**: 같은 조건이면 24시간 동안 호출을 내보내지 않는다(FlightSearchCache).
 * - **폴백**: 키가 없거나 한도 초과·네트워크 실패면 Mock 결과를 쓰고 `isSample`을 세운다.
 *   항공권을 못 받았다고 계획 전체를 실패시키면 숙소·식당·일정까지 못 보게 된다.
 *
 * 폴백으로 넘어갔을 때 화면이 그 사실을 반드시 드러내야 한다. 예시 가격을 실제로
 * 오해하면 사용자가 잘못된 예산을 세우게 된다.
 */
class SerpApiFlightRepository(
    context: Context,
    private val cache: FlightSearchCache = FlightSearchCache(context),
    private val fallback: FlightRepository = MockFlightRepository(),
    /**
     * 결과를 받을 언어. 항공사 이름이 이 언어로 돌아온다("에어부산" vs "Air Busan").
     *
     * 설정값을 직접 읽지 않고 함수로 받는 이유: 사용자가 언어를 바꾸면 다음 검색부터
     * 바로 따라가야 하는데, 생성 시점의 값을 들고 있으면 앱을 다시 켤 때까지 안 바뀐다.
     */
    private val language: () -> String =
        { context.resources.configuration.locales[0].language },
) : FlightRepository {

    override suspend fun search(condition: TripCondition): Result<List<Flight>> {
        // 도시의 공항을 하나만 보내면 안 된다. 목록이 "가까운 순"이라 도심에 가까운
        // 국내선 공항(자카르타 HLP, 오사카 ITM, 파리 ORY…)이 먼저 잡혀 검색이 빈다.
        // 전부 넘기면 어느 쪽이 국제선인지 판단할 필요 없이 Google이 알아서 고른다.
        val origin = condition.origin?.airportCodes?.joinToString(",")?.ifBlank { null }
        val destination = condition.destination?.airportCodes?.joinToString(",")?.ifBlank { null }
        val departureDate = condition.departureDate
        val returnDate = condition.returnDate

        if (origin == null || destination == null || departureDate == null || returnDate == null) {
            return sampleResult(condition, "출발지·목적지·날짜가 아직 정해지지 않았다")
        }
        if (BuildConfig.SERPAPI_KEY.isBlank()) {
            return sampleResult(condition, "SERPAPI_KEY가 없다")
        }

        val key = cache.keyOf(
            origin,
            destination,
            departureDate.toString(),
            returnDate.toString(),
            condition.adults,
            language(),
        )
        cache.read(key)?.let {
            Log.d(TAG, "캐시 사용: $key (호출 0회)")
            return Result.success(it)
        }

        val flights = runCatching {
            fetch(origin, destination, departureDate, returnDate, condition.adults)
        }.getOrElse { error ->
            return sampleResult(condition, "검색 실패: ${error.message}")
        }

        if (flights.isEmpty()) {
            return sampleResult(condition, "검색 결과가 비었다")
        }

        cache.write(key, flights)
        Log.d(TAG, "실제 검색 ${flights.size}건 (호출 1회)")
        return Result.success(flights)
    }

    /**
     * 실패했을 때 Mock으로 채운다. 어떤 이유로 넘어갔는지 로그에 남겨 원인을 추적할 수 있게 한다.
     * 무료 한도를 다 쓴 건지 네트워크 문제인지는 나중에 봐야 알 수 있다.
     */
    private suspend fun sampleResult(condition: TripCondition, reason: String): Result<List<Flight>> {
        Log.w(TAG, "예시 데이터로 대체 — $reason")
        return fallback.search(condition).map { flights ->
            flights.map { it.copy(isSample = true) }
        }
    }

    private suspend fun fetch(
        origin: String,
        destination: String,
        departureDate: LocalDate,
        returnDate: LocalDate,
        adults: Int,
    ): List<Flight> = withContext(Dispatchers.IO) {
        val url = buildUrl(origin, destination, departureDate, returnDate, adults, language())

        val body = (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            try {
                inputStream.bufferedReader().readText()
            } finally {
                disconnect()
            }
        }

        val json = JSONObject(body)
        // 한도 초과나 잘못된 키는 200이 아니라 error 필드로 오기도 한다.
        json.optString("error").takeIf { it.isNotBlank() }?.let { error(it) }

        parseFlights(json, origin, destination, departureDate)
            // 최저가 우선(CLAUDE.md 항공권 기준). 정렬을 구현체가 책임진다.
            .sortedBy { it.priceWon }
    }

    private fun buildUrl(
        origin: String,
        destination: String,
        departureDate: LocalDate,
        returnDate: LocalDate,
        adults: Int,
        language: String,
    ): String {
        val params = mapOf(
            "engine" to "google_flights",
            "departure_id" to origin,
            "arrival_id" to destination,
            "outbound_date" to departureDate.format(DATE_FORMAT),
            "return_date" to returnDate.format(DATE_FORMAT),
            // 인원을 넘기면 가격이 1인가가 아니라 **인원 전체 총액**으로 돌아온다.
            "adults" to adults.toString(),
            // 원화로 직접 받아 환율 변환 단계를 없앤다.
            "currency" to "KRW",
            "hl" to language,
            "gl" to "kr",
            "api_key" to BuildConfig.SERPAPI_KEY,
        )
        val query = params.entries.joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "$BASE_URL?$query"
    }

    /**
     * 응답에서 항공편을 뽑는다.
     *
     * best_flights와 other_flights 두 묶음으로 오고, 각 항목 안에 구간(flights) 배열이 있다.
     * 필드가 빠져 있어도 죽지 않게 방어적으로 읽는다 — 응답 구조가 바뀌어도 앱이 멈추는 것보다
     * 일부만 읽히는 편이 낫다.
     */
    private fun parseFlights(
        json: JSONObject,
        origin: String,
        destination: String,
        departureDate: LocalDate,
    ): List<Flight> {
        val groups = listOf("best_flights", "other_flights")
            .mapNotNull { json.optJSONArray(it) }

        val result = mutableListOf<Flight>()
        groups.forEach { array ->
            for (index in 0 until array.length()) {
                val option = array.optJSONObject(index) ?: continue
                result += option.toFlight(origin, destination, departureDate, result.size) ?: continue
            }
        }

        // 같은 편(항공사+편명+출발시각)이 best_flights와 other_flights에 모두 들어 있고,
        // 좌석 등급이나 수하물 조건만 다른 항목이 여러 번 오기도 한다. 그대로 보여주면
        // 같은 비행기가 목록에 세 줄씩 뜬다. **최저가 하나만** 남긴다.
        return result
            .groupBy { "${it.airline}-${it.flightNumber}-${it.departureAt}" }
            .values
            .map { sameFlight -> sameFlight.minBy { it.priceWon } }
            .sortedBy { it.priceWon }
    }

    private fun JSONObject.toFlight(
        origin: String,
        destination: String,
        departureDate: LocalDate,
        index: Int,
    ): Flight? {
        val price = optLong("price").takeIf { it > 0 } ?: return null
        val segments = optJSONArray("flights") ?: return null
        if (segments.length() == 0) return null

        val first = segments.optJSONObject(0) ?: return null
        val last = segments.optJSONObject(segments.length() - 1) ?: return null

        val departure = first.optJSONObject("departure_airport")
        val arrival = last.optJSONObject("arrival_airport")

        return Flight(
            id = "serpapi-$index-${optString("departure_token", price.toString())}",
            airline = first.optString("airline").ifBlank { "항공사 미상" },
            flightNumber = first.optString("flight_number"),
            departureAirport = departure?.optString("id").orEmpty().ifBlank { origin },
            arrivalAirport = arrival?.optString("id").orEmpty().ifBlank { destination },
            departureAt = parseDateTime(departure?.optString("time"), departureDate),
            arrivalAt = parseDateTime(arrival?.optString("time"), departureDate),
            // 구간이 둘이면 경유 1회. layovers 배열이 있으면 그것을 우선한다.
            stops = optJSONArray("layovers")?.length() ?: (segments.length() - 1),
            priceWon = price,
            bookingUrl = GOOGLE_FLIGHTS_URL,
        )
    }

    /** SerpApi는 "2026-09-29 09:05" 형태로 준다. 못 읽으면 출발일 자정으로 둔다. */
    private fun parseDateTime(raw: String?, fallbackDate: LocalDate): LocalDateTime {
        if (raw.isNullOrBlank()) return fallbackDate.atStartOfDay()
        return runCatching { LocalDateTime.parse(raw, TIME_FORMAT) }
            .getOrElse { fallbackDate.atStartOfDay() }
    }

    private companion object {
        const val TAG = "FlightSearch"
        const val BASE_URL = "https://serpapi.com/search.json"
        const val TIMEOUT_MS = 15_000

        /** 예약은 하지 않는다(CLAUDE.md 범위). 검색 결과를 볼 수 있는 링크만 건다. */
        const val GOOGLE_FLIGHTS_URL = "https://www.google.com/travel/flights"

        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
