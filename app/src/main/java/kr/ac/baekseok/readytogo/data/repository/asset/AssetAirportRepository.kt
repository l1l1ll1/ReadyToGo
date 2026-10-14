package kr.ac.baekseok.readytogo.data.repository.asset

import android.content.Context
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.repository.AirportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * assets/airports.json을 읽어 공항 좌표를 준다.
 *
 * 표는 tools/geo/build-airports.ps1이 OurAirports 데이터에서 만든다. 목적지 목록에 나오는
 * 공항만 들어 있어(675곳, 16KB) 한 번 읽어 메모리에 들고 있는다.
 */
class AssetAirportRepository(
    private val context: Context,
) : AirportRepository {

    private var cache: Map<String, LatLng>? = null

    override suspend fun locationOf(code: String): LatLng? =
        load()[code.uppercase()]

    private suspend fun load(): Map<String, LatLng> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            root.keys().asSequence().associateWith { code ->
                val pair = root.getJSONArray(code)
                LatLng(pair.getDouble(0), pair.getDouble(1))
            }
        }.also { cache = it }
    }

    private companion object {
        const val FILE_NAME = "airports.json"
    }
}
