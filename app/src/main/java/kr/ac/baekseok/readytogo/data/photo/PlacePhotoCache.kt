package kr.ac.baekseok.readytogo.data.photo

import android.content.Context
import kr.ac.baekseok.readytogo.data.model.PlacePhoto
import org.json.JSONObject

/**
 * 받아 둔 사진 주소를 기기에 저장한다.
 *
 * **왜 캐시가 필수인가**: 사진은 요청마다 과금된다. 같은 장소를 다시 볼 때마다 또 받으면
 * 요약 탭을 두 번 열었다는 이유로 돈이 나간다. 한 번 받은 장소는 다시 묻지 않는다.
 *
 * **유효기간을 두지 않는다.** 장소 검색 캐시(PlaceSearchCache)와 달리 사진은 잘 바뀌지 않고,
 * 만료시켜 봐야 다시 과금될 뿐이다. 주소가 죽으면 화면이 그라데이션으로 떨어진다.
 *
 * 사진이 **없는** 장소도 기록한다([NONE]). 그러지 않으면 사진 없는 곳을 볼 때마다
 * 매번 다시 물어보게 된다 — 그것도 과금이다.
 */
class PlacePhotoCache(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    init {
        purgeFailuresOnce()
    }

    /**
     * 실패까지 "사진 없음"으로 적어 두던 시절의 기록을 한 번만 지운다.
     *
     * 예전에는 요청이 취소되거나 실패해도 결과를 [NONE_MARKER]로 적었다. 캐시에
     * 유효기간이 없어서 그렇게 굳은 장소는 영영 그라데이션으로 남는다 — 실제로
     * 저장 탭 38곳 중 6곳이 그 상태였다.
     *
     * **진짜로 사진이 없는 곳과 구분할 방법이 없으므로** 없음 기록을 통째로 지운다.
     * 그 6곳은 다음에 화면에 뜰 때 한 번 더 물어보게 되는데, 장소당 한 번뿐이고
     * 사진이 정말 없으면 다시 없음으로 적힌다. 월 상한(800장)에 견주면 무시할 양이다.
     *
     * 받아 둔 **사진 주소는 건드리지 않는다.** 그것까지 지우면 멀쩡한 32곳을 다시 받느라
     * 돈이 나간다.
     */
    private fun purgeFailuresOnce() {
        if (preferences.getInt(KEY_VERSION, 0) >= VERSION) return

        val editor = preferences.edit()
        preferences.all.forEach { (key, value) ->
            if (value == NONE_MARKER) editor.remove(key)
        }
        editor.putInt(KEY_VERSION, VERSION).apply()
    }

    /**
     * 저장된 값. 아직 물어본 적 없으면 null.
     *
     * 물어봤지만 사진이 없었던 장소는 [PlacePhoto.NONE]을 돌려준다 — null과 구분해야
     * "안 물어봄"과 "없음"이 섞이지 않는다.
     */
    fun read(placeId: String): PlacePhoto? {
        val raw = preferences.getString(placeId, null) ?: return null
        if (raw == NONE_MARKER) return PlacePhoto.NONE

        return runCatching {
            val json = JSONObject(raw)
            PlacePhoto(
                url = json.getString(FIELD_URL),
                attribution = json.optString(FIELD_ATTRIBUTION),
            )
        }.getOrNull()
    }

    fun write(placeId: String, photo: PlacePhoto) {
        val raw = if (photo.url.isBlank()) {
            NONE_MARKER
        } else {
            JSONObject()
                .put(FIELD_URL, photo.url)
                .put(FIELD_ATTRIBUTION, photo.attribution)
                .toString()
        }
        preferences.edit().putString(placeId, raw).apply()
    }

    private companion object {
        const val FILE = "place_photos"
        const val FIELD_URL = "url"
        const val FIELD_ATTRIBUTION = "attribution"

        /**
         * 캐시 모양이 바뀐 횟수. 올리면 [purgeFailuresOnce]가 한 번 더 돈다.
         *
         * 1 = 실패를 "사진 없음"으로 적던 기록을 지우는 판.
         */
        const val VERSION = 1

        /** placeId와 겹치지 않게 밑줄을 붙인다. Google place id는 ChIJ...로 시작한다. */
        const val KEY_VERSION = "__version"

        /** 사진이 없는 장소. JSON을 쓰지 않는 이유는 구분만 되면 되기 때문이다. */
        const val NONE_MARKER = "none"
    }
}
