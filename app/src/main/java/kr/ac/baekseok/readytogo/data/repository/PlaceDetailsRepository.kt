package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.Restaurant

/**
 * 즐겨찾기한 장소 하나를 일정에 끼워 넣을 수 있는 모양으로 바꾼다.
 *
 * **왜 따로 받아 와야 하나**: 즐겨찾기는 이름·좌표·종류만 저장한다. 가격과 평점은
 * 시간이 지나면 틀려지는 값이라 일부러 담지 않았다([FavoritePlace] 주석). 그런데
 * 일정에 넣으려면 예산 판정과 점수 표시에 그 값들이 필요하다.
 *
 * **부르는 때는 한 번뿐이다.** 사용자가 "내가 저장한 곳"에서 실제로 하나를 골라
 * 끼워 넣는 순간에만 부른다. 목록을 펼치거나 탭을 여는 것으로는 호출이 나가지 않는다 —
 * 조회마다 과금되기 때문이다.
 */
interface PlaceDetailsRepository {

    /**
     * 식당으로. 조회가 실패하면 즐겨찾기에 있는 이름·좌표만으로 만들고
     * [Restaurant.priceUnknown]을 세운다 — 화면이 "가격 정보 없음"으로 적는다.
     *
     * **실패를 Result로 싸지 않는다.** 실패해도 일정에는 넣을 수 있어야 한다.
     * 가격을 못 받았다고 사용자가 고른 곳을 거절하면 앱이 멈춘 것처럼 보인다.
     */
    suspend fun restaurantFor(favorite: FavoritePlace): Restaurant

    /** 관광지로. 식당과 같은 규칙이지만 관광지는 가격이 없어 실패해도 빠지는 것이 적다. */
    suspend fun attractionFor(favorite: FavoritePlace): Attraction
}
