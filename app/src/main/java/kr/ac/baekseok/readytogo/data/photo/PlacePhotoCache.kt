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

        /** 사진이 없는 장소. JSON을 쓰지 않는 이유는 구분만 되면 되기 때문이다. */
        const val NONE_MARKER = "none"
    }
}
