package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.PlacePhoto

/**
 * 장소 사진을 가져온다.
 *
 * 인터페이스로 둔 이유는 다른 데이터 출처와 같다(CLAUDE.md 개발 전략) — 화면은 사진이
 * Places에서 오는지 가짜인지 알 필요가 없다. 키가 없으면 [NoPlacePhotoRepository]가 대신 선다.
 */
interface PlacePhotoRepository {

    /**
     * 사진 한 장. 없거나 더 받을 수 없으면 [PlacePhoto.NONE].
     *
     * **실패를 Result로 싸지 않는다.** 사진은 없어도 화면이 성립하고(그라데이션으로 떨어진다),
     * 호출부마다 실패를 처리하게 하면 카드 코드가 지저분해진다.
     */
    suspend fun photoFor(placeId: String): PlacePhoto
}

/** 키가 없을 때. 항상 사진이 없다고 답한다. */
object NoPlacePhotoRepository : PlacePhotoRepository {
    override suspend fun photoFor(placeId: String): PlacePhoto = PlacePhoto.NONE
}
