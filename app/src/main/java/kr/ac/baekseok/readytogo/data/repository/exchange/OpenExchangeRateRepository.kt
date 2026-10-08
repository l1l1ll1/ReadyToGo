package kr.ac.baekseok.readytogo.data.repository.exchange

import kr.ac.baekseok.readytogo.data.repository.ExchangeRateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 공개 환율 API에서 받아온다. 키가 필요 없고 무료라 Google API와 달리 호출 비용이 없다.
 *
 * 한 번 받으면 모든 통화가 들어 있어 메모리에 들고 쓴다. 여행 예산 추정 용도라
 * 분 단위 정확도가 필요 없고, 앱을 켜 둔 동안 다시 받을 이유도 없다.
 *
 * 받지 못하면 내장 기본값으로 넘어간다 — 환율을 못 받았다고 계획 전체가 실패하면 안 된다.
 * HttpURLConnection을 쓰는 이유는 이 호출 하나 때문에 HTTP 라이브러리를 더 넣지 않기 위함이다.
 */
class OpenExchangeRateRepository : ExchangeRateRepository {

    private var cachedRates: Map<String, Double>? = null

    /** 한 번 받으면 모든 통화가 들어오므로, 어느 통화가 필요한지 몰라도 미리 받아둘 수 있다. */
    override suspend fun prepare() {
        if (cachedRates == null) cachedRates = fetchRates()
    }

    override suspend fun rateToKrw(currencyCode: String): Result<Double> {
        val code = currencyCode.uppercase()
        if (code == "KRW") return Result.success(1.0)

        val rates = cachedRates ?: fetchRates()?.also { cachedRates = it }

        val rate = rates?.get(code) ?: FALLBACK_RATES[code]
        return if (rate != null) {
            Result.success(rate)
        } else {
            Result.failure(IllegalArgumentException("환율을 모르는 통화입니다: $code"))
        }
    }

    /**
     * 원화 기준 환율을 받아 "통화 1단위 = 몇 원"으로 뒤집는다.
     * API는 "1원 = 0.00069 EUR" 형태로 주므로 역수를 취한다.
     */
    private suspend fun fetchRates(): Map<String, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            val body = connection.use { it.inputStream.bufferedReader().readText() }

            val rates = JSONObject(body).getJSONObject("rates")
            buildMap {
                rates.keys().forEach { code ->
                    val perKrw = rates.getDouble(code)
                    if (perKrw > 0) put(code, 1.0 / perKrw)
                }
            }
        }.getOrNull()
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private companion object {
        const val API_URL = "https://open.er-api.com/v6/latest/KRW"
        const val TIMEOUT_MS = 5_000

        /**
         * API를 못 받았을 때만 쓰는 값. 정확하지 않아도 계획이 만들어지는 편이 낫다.
         * 낡은 값이므로 API가 살아 있으면 절대 쓰이지 않는다.
         */
        val FALLBACK_RATES = mapOf(
            "USD" to 1_350.0, "EUR" to 1_450.0, "JPY" to 9.0, "GBP" to 1_700.0,
            "CNY" to 190.0, "HKD" to 173.0, "TWD" to 42.0, "SGD" to 1_000.0,
            "THB" to 40.0, "VND" to 0.055, "PHP" to 24.0, "MYR" to 300.0,
            "IDR" to 0.085, "AUD" to 890.0, "NZD" to 820.0, "CAD" to 990.0,
            "CHF" to 1_530.0, "CZK" to 58.0, "TRY" to 40.0, "AED" to 368.0,
            "INR" to 16.0, "KHR" to 0.33, "LAK" to 0.062, "MNT" to 0.39,
            "MOP" to 168.0,
        )
    }
}
