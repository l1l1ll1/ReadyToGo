package kr.ac.baekseok.readytogo.data.model

/**
 * 장소 사진 한 장.
 *
 * @param url 그릴 이미지 주소. 비어 있으면 사진이 없다는 뜻이다([NONE]).
 * @param attribution 사진을 올린 사람. **Places 약관상 화면에 반드시 적어야 한다.**
 *   비어 있을 수 있어서 화면이 먼저 확인하고 그린다.
 */
data class PlacePhoto(
    val url: String,
    val attribution: String,
) {
    val hasPhoto: Boolean get() = url.isNotBlank()

    companion object {
        /** 물어봤지만 사진이 없던 장소. "아직 안 물어봄"(null)과 구분한다. */
        val NONE = PlacePhoto(url = "", attribution = "")
    }
}
