package kr.ac.baekseok.readytogo.domain.plan

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.MealTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 끼니 식당을 고를 때 "바로 앞 → 식당 → 바로 뒤"의 앞뒤가 타임라인 배치와 맞는지 본다.
 * 어긋나면 화면에서는 A 뒤에 오는 식당을 B 기준으로 고르게 된다.
 */
class MealNeighboursTest {

    private val hotel = LatLng(35.0, 139.0)
    private val breakfast = LatLng(35.9, 139.9)

    private fun attraction(id: String, lat: Double) =
        Attraction(id = id, name = id, rating = 4.0, location = LatLng(lat, 139.5), stayMinutes = 60, address = "")

    private val a = attraction("a", 35.1)
    private val b = attraction("b", 35.2)
    private val c = attraction("c", 35.3)
    private val d = attraction("d", 35.4)

    private fun neighbours(
        meal: MealTime,
        attractions: List<Attraction>,
        picked: Map<MealTime, LatLng> = emptyMap(),
    ) = mealNeighbours(meal, attractions, picked, dayStart = hotel, dayEnd = hotel)

    @Test
    fun `관광지 넷인 날 - 아침은 숙소와 첫 관광지 사이, 점심은 오전 마지막과 오후 첫, 저녁은 마지막과 숙소 사이`() {
        val day = listOf(a, b, c, d) // 배치: 아침 a b 점심 c d 저녁

        assertEquals(hotel to a.location, neighbours(MealTime.BREAKFAST, day))
        assertEquals(b.location to c.location, neighbours(MealTime.LUNCH, day))
        assertEquals(d.location to hotel, neighbours(MealTime.DINNER, day))
    }

    @Test
    fun `관광지 한 곳인 날 - 아침은 점심을 건너뛰어 관광지를 보고, 점심은 고른 아침에서 출발한다`() {
        val day = listOf(a) // 배치: 아침 점심 a 저녁

        assertEquals(hotel to a.location, neighbours(MealTime.BREAKFAST, day))
        assertEquals(
            breakfast to a.location,
            neighbours(MealTime.LUNCH, day, picked = mapOf(MealTime.BREAKFAST to breakfast)),
        )
        assertEquals(a.location to hotel, neighbours(MealTime.DINNER, day))
    }

    @Test
    fun `앞 끼니를 못 채웠으면 더 앞을 본다`() {
        // 아침 후보가 없어 비었다. 점심의 앞은 하루 시작점이다.
        assertEquals(hotel to a.location, neighbours(MealTime.LUNCH, listOf(a)))
    }

    @Test
    fun `관광지가 없는 날은 숙소에서 나와 숙소로 돌아간다`() {
        assertEquals(hotel to hotel, neighbours(MealTime.BREAKFAST, emptyList()))
        assertEquals(hotel to hotel, neighbours(MealTime.DINNER, emptyList()))
    }
}
