package kr.ac.baekseok.readytogo.data.repository.flight

import android.content.Context
import kr.ac.baekseok.readytogo.data.model.Flight
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 항공권 검색 결과를 기기에 저장해 재사용한다.
 *
 * 무료 한도가 월 250회뿐이라 캐시가 선택이 아니라 필수다. 같은 조건을 다시 조회하면
 * 호출을 아예 내보내지 않는다. 앱을 껐다 켜도 남아야 할당량이 실제로 절약되므로
 * 메모리가 아니라 SharedPreferences에 둔다.
 *
 * 항공 요금은 하루 사이에 크게 변하지 않아 24시간이면 충분하다고 본다.
 */
class FlightSearchCache(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 캐시 키. 이 네 가지가 같으면 같은 검색이다. */
    fun keyOf(origin: String, destination: String, departureDate: String, returnDate: String) =
        "$origin-$destination-$departureDate-$returnDate"

    /** 유효기간이 지나지 않은 결과만 돌려준다. 없으면 null. */
    fun read(key: String): List<Flight>? {
        val raw = preferences.getString(key, null) ?: return null

        return runCatching {
            val wrapper = JSONObject(raw)
            val savedAt = wrapper.getLong(FIELD_SAVED_AT)
            if (System.currentTimeMillis() - savedAt > CACHE_TTL_MS) {
                preferences.edit().remove(key).apply()
                return null
            }
            wrapper.getJSONArray(FIELD_FLIGHTS).toFlights()
        }.getOrNull()
    }

    fun write(key: String, flights: List<Flight>) {
        val wrapper = JSONObject()
            .put(FIELD_SAVED_AT, System.currentTimeMillis())
            .put(FIELD_FLIGHTS, flights.toJsonArray())

        preferences.edit().putString(key, wrapper.toString()).apply()
    }

    private fun List<Flight>.toJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { flight ->
            array.put(
                JSONObject()
                    .put("id", flight.id)
                    .put("airline", flight.airline)
                    .put("flightNumber", flight.flightNumber)
                    .put("departureAirport", flight.departureAirport)
                    .put("arrivalAirport", flight.arrivalAirport)
                    .put("departureAt", flight.departureAt.toString())
                    .put("arrivalAt", flight.arrivalAt.toString())
                    .put("stops", flight.stops)
                    .put("priceWon", flight.priceWon)
                    .put("bookingUrl", flight.bookingUrl)
            )
        }
        return array
    }

    private fun JSONArray.toFlights(): List<Flight> = (0 until length()).map { index ->
        val item = getJSONObject(index)
        Flight(
            id = item.getString("id"),
            airline = item.getString("airline"),
            flightNumber = item.getString("flightNumber"),
            departureAirport = item.getString("departureAirport"),
            arrivalAirport = item.getString("arrivalAirport"),
            departureAt = LocalDateTime.parse(item.getString("departureAt")),
            arrivalAt = LocalDateTime.parse(item.getString("arrivalAt")),
            stops = item.getInt("stops"),
            priceWon = item.getLong("priceWon"),
            bookingUrl = item.getString("bookingUrl"),
        )
    }

    companion object {
        /** 캐시 유효기간. 숫자만 바꿔 조정할 수 있게 상수로 둔다. */
        const val CACHE_TTL_HOURS = 24L
        val CACHE_TTL_MS: Long = TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS)

        private const val PREFS_NAME = "flight_search_cache"
        private const val FIELD_SAVED_AT = "savedAt"
        private const val FIELD_FLIGHTS = "flights"
    }
}
