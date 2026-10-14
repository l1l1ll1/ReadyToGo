package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

/** 하루의 시작·끝 규칙. 끼니 식당 선택과 지도 선이 이 규칙 하나를 같이 쓴다. */
class DayEndsTest {

    private val hotel = LatLng(35.68, 139.76)
    private val airport = LatLng(35.77, 140.39)

    @Test
    fun `도착일은 공항에서 시작해 숙소에서 끝난다`() {
        assertEquals(DayEnds(airport, hotel), dayEnds(1, 5, hotel, airport))
    }

    @Test
    fun `중간 날은 숙소에서 나와 숙소로 돌아온다`() {
        assertEquals(DayEnds(hotel, hotel), dayEnds(3, 5, hotel, airport))
    }

    @Test
    fun `출국일은 숙소에서 나와 공항에서 끝난다`() {
        assertEquals(DayEnds(hotel, airport), dayEnds(5, 5, hotel, airport))
    }

    @Test
    fun `하루짜리 여행은 공항에서 시작해 공항에서 끝난다`() {
        assertEquals(DayEnds(airport, airport), dayEnds(1, 1, hotel, airport))
    }

    @Test
    fun `공항을 모르면 도착일과 출국일도 숙소 기준이다`() {
        assertEquals(DayEnds(hotel, hotel), dayEnds(1, 5, hotel, null))
        assertEquals(DayEnds(hotel, hotel), dayEnds(5, 5, hotel, null))
    }
}
