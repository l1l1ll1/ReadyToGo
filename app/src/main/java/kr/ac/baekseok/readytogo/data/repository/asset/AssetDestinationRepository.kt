package kr.ac.baekseok.readytogo.data.repository.asset

import android.content.Context
import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.repository.DestinationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * assets/destinations.json을 읽어 목적지 목록을 제공한다.
 *
 * 목록은 OurAirports(여객 공항)와 GeoNames(도시·인구) 데이터로 만들었다.
 * 조건: 정기 여객편이 있는 중대형 공항 반경 200km 이내, 인구 5만 이상, 나라당 40곳.
 * 인구 기준에 걸려 빠지는 유명 관광지(할슈타트·인터라켄 등)는 생성 단계에서 따로 넣었다.
 *
 * 한 번 읽으면 메모리에 들고 있는다. 879개 도시라 1MB가 안 되고, 화면을 오갈 때마다
 * 파일을 다시 파싱할 이유가 없다.
 */
class AssetDestinationRepository(
    private val context: Context,
) : DestinationRepository {

    private var cache: List<Destination>? = null

    override suspend fun countries(): Result<List<String>> = runCatching {
        load().map { it.country }.distinct().sorted()
    }

    override suspend fun cities(country: String): Result<List<Destination>> = runCatching {
        // JSON이 이미 나라별 인구순으로 정렬돼 있어 여기서 다시 정렬하지 않는다.
        load().filter { it.country == country }
    }

    override suspend fun origins(): Result<List<Destination>> = runCatching { KOREAN_AIRPORTS }

    /**
     * 도시명·나라명·공항 코드로 찾는다.
     *
     * **도시명이 검색어로 시작하는 결과를 먼저 보여준다.** 단순히 포함 여부로만 걸러 정렬하지 않으면
     * "항"을 치면 "홍콩"보다 "방콕 근처 어딘가"가 위에 올 수 있다. 공항 코드는 대소문자를 무시한다.
     */
    override suspend fun searchCities(query: String): Result<List<Destination>> = runCatching {
        val keyword = query.trim()
        if (keyword.isEmpty()) return@runCatching emptyList()

        load()
            .filter { it.matches(keyword) }
            .sortedWith(
                // 1) 도시명이 검색어로 시작, 2) 도시명에 포함, 3) 나머지(나라명·공항 코드만 걸린 것)
                compareBy(
                    { if (it.city.startsWith(keyword, ignoreCase = true)) 0 else 1 },
                    { if (it.city.contains(keyword, ignoreCase = true)) 0 else 1 },
                ),
            )
            .take(MAX_SEARCH_RESULTS)
    }

    private fun Destination.matches(keyword: String): Boolean =
        city.contains(keyword, ignoreCase = true) ||
            country.contains(keyword, ignoreCase = true) ||
            airportCodes.any { it.equals(keyword, ignoreCase = true) }

    private suspend fun load(): List<Destination> {
        cache?.let { return it }

        return withContext(Dispatchers.IO) {
            val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            val parsed = ArrayList<Destination>(array.length())

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val codes = item.getJSONArray("airports")
                parsed += Destination(
                    country = item.getString("country"),
                    city = item.getString("city"),
                    location = LatLng(item.getDouble("lat"), item.getDouble("lon")),
                    airportCodes = List(codes.length()) { codes.getString(it) },
                )
            }
            parsed.also { cache = it }
        }
    }

    private companion object {
        const val ASSET_NAME = "destinations.json"

        /**
         * 검색 결과 상한. "a"처럼 짧은 검색어에 수백 개가 걸리면 목록이 쓸모없어지고
         * 스크롤만 길어진다. 원하는 곳이 안 나오면 더 입력하게 하는 쪽이 낫다.
         */
        const val MAX_SEARCH_RESULTS = 50

        /**
         * 출발지는 한국 국제공항뿐이라 데이터셋에서 뽑지 않고 직접 적는다.
         * 일곱 개가 전부고 바뀔 일이 거의 없다.
         */
        val KOREAN_AIRPORTS = listOf(
            Destination("대한민국", "인천", LatLng(37.4602, 126.4407), listOf("ICN")),
            Destination("대한민국", "김포", LatLng(37.5583, 126.7906), listOf("GMP")),
            Destination("대한민국", "부산", LatLng(35.1796, 128.9382), listOf("PUS")),
            Destination("대한민국", "제주", LatLng(33.5104, 126.4914), listOf("CJU")),
            Destination("대한민국", "대구", LatLng(35.8714, 128.6014), listOf("TAE")),
            Destination("대한민국", "청주", LatLng(36.7166, 127.4991), listOf("CJJ")),
            Destination("대한민국", "무안", LatLng(34.9914, 126.3828), listOf("MWX")),
        )
    }
}
