package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import android.util.Log
import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.Lodging
import kr.ac.baekseok.readytogo.data.model.MealTime
import kr.ac.baekseok.readytogo.data.model.Restaurant
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Places 검색 결과를 기기에 저장해 같은 검색을 두 번 보내지 않는다.
 *
 * **왜 필요한가**: 계획을 한 번 만들 때 Nearby Search가 6~10회 나간다. 화면을 다듬으며
 * 같은 조건으로 수십 번 돌리면 결과는 거의 같은데 호출만 쌓인다. Places는 호출마다 과금된다.
 *
 * 항공권 캐시([kr.ac.baekseok.readytogo.data.repository.flight.FlightSearchCache])와 같은 방식이다 —
 * SharedPreferences에 JSON으로 넣고 유효기간을 둔다. 장소·평점·가격대는 하루 사이에 바뀌지
 * 않으므로 [CACHE_TTL_HOURS]를 길게 잡아도 된다.
 *
 * 직렬화를 손으로 쓰는 이유: kotlinx.serialization을 넣으면 플러그인과 빌드 설정이 늘어나는데,
 * 캐시할 모델이 세 개뿐이라 얻는 게 없다(CLAUDE.md — 과도하게 설계하지 않는다).
 */
class PlaceSearchCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 캐시 키.
     *
     * 좌표를 소수점 셋째 자리까지만 쓴다(약 100m). 그보다 세밀하게 넣으면 일정이 조금만 달라져도
     * 중심점이 미세하게 움직여 캐시가 전부 빗나간다 — 100m 차이로 식당 목록이 달라지지는 않는다.
     */
    fun keyOf(kind: String, center: LatLng, radiusMeters: Double, types: List<String>): String {
        val lat = "%.3f".format(center.latitude)
        val lon = "%.3f".format(center.longitude)
        return "$kind|$lat|$lon|${radiusMeters.toInt()}|${types.sorted().joinToString(",")}"
    }

    fun readRestaurants(key: String): List<Restaurant>? =
        read(key) { it.toRestaurant() }

    fun readAttractions(key: String): List<Attraction>? =
        read(key) { it.toAttraction() }

    fun readLodgings(key: String): List<Lodging>? =
        read(key) { it.toLodging() }

    fun writeRestaurants(key: String, items: List<Restaurant>) =
        write(key, items) { it.toJson() }

    fun writeAttractions(key: String, items: List<Attraction>) =
        write(key, items) { it.toJson() }

    fun writeLodgings(key: String, items: List<Lodging>) =
        write(key, items) { it.toJson() }

    /**
     * 유효기간이 지났으면 null을 돌려주고 지운다.
     *
     * 파싱이 실패해도 null이다 — 모델에 필드를 추가하면 예전 캐시를 읽을 수 없는데,
     * 그때 예외를 던지면 앱이 죽는다. 버리고 다시 받는 쪽이 맞다.
     */
    private fun <T> read(key: String, parse: (JSONObject) -> T?): List<T>? {
        val raw = prefs.getString(key, null) ?: return null

        return runCatching {
            val wrapper = JSONObject(raw)
            val savedAt = wrapper.getLong(FIELD_SAVED_AT)

            if (System.currentTimeMillis() - savedAt > TTL_MILLIS) {
                prefs.edit().remove(key).apply()
                Log.d(TAG, "캐시 만료: $key")
                return null
            }

            val array = wrapper.getJSONArray(FIELD_ITEMS)
            val items = (0 until array.length()).mapNotNull { parse(array.getJSONObject(it)) }
            Log.d(TAG, "캐시 사용: $key (${items.size}건, 호출 0회)")
            items
        }.getOrElse {
            Log.w(TAG, "캐시 파싱 실패, 버림: $key")
            prefs.edit().remove(key).apply()
            null
        }
    }

    private fun <T> write(key: String, items: List<T>, toJson: (T) -> JSONObject) {
        val array = JSONArray().apply { items.forEach { put(toJson(it)) } }
        val wrapper = JSONObject().apply {
            put(FIELD_SAVED_AT, System.currentTimeMillis())
            put(FIELD_ITEMS, array)
        }
        prefs.edit().putString(key, wrapper.toString()).apply()
    }

    /** 개발 중 캐시를 비우고 싶을 때. 화면에서 부를 곳은 아직 없다. */
    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val PREFS_NAME = "place_search_cache"
        const val TAG = "PlaceCache"

        const val FIELD_SAVED_AT = "savedAt"
        const val FIELD_ITEMS = "items"

        /**
         * 유효기간. 장소 목록과 평점은 하루 사이에 바뀌지 않는다.
         * 일주일로 둬서 개발 중 반복 확인이 호출 0회가 되게 한다.
         */
        const val CACHE_TTL_HOURS = 24L * 7
        val TTL_MILLIS = TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS)
    }
}

// ---- 직렬화 ----
// 필드 이름은 모델 속성명과 같게 둔다. 다르게 적으면 어느 쪽이 맞는지 매번 대조해야 한다.

private fun Restaurant.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("category", category)
    put("rating", rating)
    put("reviewCount", reviewCount)
    put("lat", location.latitude)
    put("lon", location.longitude)
    put("priceWon", priceWon)
    minPriceWon?.let { put("minPriceWon", it) }
    localPriceText?.let { put("localPriceText", it) }
    put("priceEstimated", priceEstimated)
    put("priceIsUpperBound", priceIsUpperBound)
    put("address", address)
    put("mapUrl", mapUrl)
    put("suitableMeals", JSONArray().apply { suitableMeals.forEach { put(it.name) } })
}

private fun JSONObject.toRestaurant() = Restaurant(
    id = getString("id"),
    name = getString("name"),
    category = getString("category"),
    rating = getDouble("rating"),
    reviewCount = getInt("reviewCount"),
    location = LatLng(getDouble("lat"), getDouble("lon")),
    priceWon = getLong("priceWon"),
    minPriceWon = if (has("minPriceWon")) getLong("minPriceWon") else null,
    localPriceText = if (has("localPriceText")) getString("localPriceText") else null,
    priceEstimated = getBoolean("priceEstimated"),
    priceIsUpperBound = getBoolean("priceIsUpperBound"),
    address = getString("address"),
    mapUrl = getString("mapUrl"),
    suitableMeals = getJSONArray("suitableMeals").let { array ->
        (0 until array.length()).map { MealTime.valueOf(array.getString(it)) }.toSet()
    },
)

private fun Attraction.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("rating", rating)
    put("lat", location.latitude)
    put("lon", location.longitude)
    put("stayMinutes", stayMinutes)
    put("address", address)
    put("description", description)
    put("reviewCount", reviewCount)
    put("types", JSONArray().apply { types.forEach { put(it) } })
}

private fun JSONObject.toAttraction() = Attraction(
    id = getString("id"),
    name = getString("name"),
    rating = getDouble("rating"),
    location = LatLng(getDouble("lat"), getDouble("lon")),
    stayMinutes = getInt("stayMinutes"),
    address = getString("address"),
    description = getString("description"),
    reviewCount = getInt("reviewCount"),
    types = getJSONArray("types").let { array ->
        (0 until array.length()).map { array.getString(it) }
    },
)

private fun Lodging.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("rating", rating)
    put("reviewCount", reviewCount)
    put("lat", location.latitude)
    put("lon", location.longitude)
    put("priceWon", priceWon)
    put("address", address)
    put("bookingUrl", bookingUrl)
    imageUrl?.let { put("imageUrl", it) }
    priceLevel?.let { put("priceLevel", it) }
    put("priceEstimated", priceEstimated)
}

private fun JSONObject.toLodging() = Lodging(
    id = getString("id"),
    name = getString("name"),
    rating = getDouble("rating"),
    reviewCount = getInt("reviewCount"),
    location = LatLng(getDouble("lat"), getDouble("lon")),
    priceWon = getLong("priceWon"),
    address = getString("address"),
    bookingUrl = getString("bookingUrl"),
    imageUrl = if (has("imageUrl")) getString("imageUrl") else null,
    priceLevel = if (has("priceLevel")) getInt("priceLevel") else null,
    priceEstimated = getBoolean("priceEstimated"),
)
