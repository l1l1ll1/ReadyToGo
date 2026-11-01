package kr.ac.baekseok.readytogo.domain.edit

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.DayPlan
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.alternativesByDay
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.domain.purpose.IndoorOutdoor
import kr.ac.baekseok.readytogo.domain.purpose.Shelter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** 관광지 교체: 실내/실외 판정, 예비 후보 고르기, 교체 기준, 실행과 타임라인 반영. */
class PlaceSwapTest {

    private fun place(
        id: String,
        lat: Double,
        primary: String? = null,
        types: List<String> = emptyList(),
        rating: Double = 4.0,
        openDays: Set<DayOfWeek>? = null,
    ) = Attraction(
        id = id, name = id, rating = rating, location = LatLng(lat, 139.5), stayMinutes = 90,
        address = "", types = types, primaryType = primary, openDays = openDays, reviewCount = 500,
    )

    // ---- 실내/실외 ----

    @Test
    fun `본업 타입으로 실내 실외를 가른다`() {
        assertEquals(Shelter.INDOOR, IndoorOutdoor.of(place("m", 0.0, "museum")))
        assertEquals(Shelter.OUTDOOR, IndoorOutdoor.of(place("p", 0.0, "park")))
    }

    @Test
    fun `구체적인 본업이 표에 없으면 부가 타입이 있어도 보류`() {
        // 절에 딸린 정원 때문에 park가 붙어 있어도 절은 실외로 보지 않는다.
        assertEquals(Shelter.UNKNOWN, IndoorOutdoor.of(place("t", 0.0, "buddhist_temple", listOf("park"))))
    }

    @Test
    fun `본업이 없거나 범용이면 부가 타입을 본다`() {
        assertEquals(Shelter.INDOOR, IndoorOutdoor.of(place("a", 0.0, null, listOf("point_of_interest", "aquarium"))))
        assertEquals(Shelter.OUTDOOR, IndoorOutdoor.of(place("z", 0.0, "tourist_attraction", listOf("zoo"))))
        assertEquals(Shelter.UNKNOWN, IndoorOutdoor.of(place("x", 0.0, "tourist_attraction")))
    }

    // ---- 예비 후보 ----

    @Test
    fun `예비 후보는 일정에 안 든 곳만, 날마다 그날 근처가 위로, 최대 5곳`() {
        val day1 = listOf(place("d1", 35.0))
        val day2 = listOf(place("d2", 36.0))
        val spare = (1..7).map { place("s$it", 35.0 + it * 0.01) } + place("far", 36.01)

        val result = alternativesByDay(day1 + day2 + spare, listOf(day1, day2), emptySet())

        assertEquals(5, result[0].size)
        assertTrue(result.flatten().none { it.id == "d1" || it.id == "d2" })
        assertEquals("far", result[1].first().id)
    }

    @Test
    fun `상위 5곳에 실내가 없으면 가장 나은 실내 하나를 덧붙인다`() {
        val day1 = listOf(place("d1", 35.0))
        val parks = (1..6).map { place("park$it", 35.0 + it * 0.01, "park") }
        val museum = place("museum", 36.0, "museum")

        val result = alternativesByDay(day1 + parks + museum, listOf(day1), emptySet()).single()

        assertEquals(6, result.size)
        assertEquals("museum", result.last().id)
    }

    @Test
    fun `상위 5곳에 실내가 이미 있으면 덧붙이지 않는다`() {
        val day1 = listOf(place("d1", 35.0))
        val spare = listOf(place("museum", 35.01, "museum")) + (1..6).map { place("park$it", 35.0 + it * 0.02, "park") }

        assertEquals(5, alternativesByDay(day1 + spare, listOf(day1), emptySet()).single().size)
    }

    // ---- 교체 기준 ----

    private val current = place("cur", 35.5, "park")
    private val previous = LatLng(35.0, 139.5)

    @Test
    fun `실내 - 점수 순 후보 중 첫 실내`() {
        val alts = listOf(place("park2", 35.1, "park"), place("museum", 35.6, "museum"), place("mall", 35.2, "shopping_mall"))
        assertEquals("museum", pickPlaceFor(PlaceBasis.INDOOR, current, alts, previous, emptySet(), null)?.id)
    }

    @Test
    fun `실내 - 보류 타입은 실내로 치지 않는다`() {
        val alts = listOf(place("temple", 35.1, "buddhist_temple"))
        assertNull(pickPlaceFor(PlaceBasis.INDOOR, current, alts, previous, emptySet(), null))
    }

    @Test
    fun `앞 일정 근처 - 지금보다 가까운 것 중 첫 번째`() {
        val alts = listOf(place("farther", 35.9), place("closer", 35.2), place("closest", 35.05))
        assertEquals("closer", pickPlaceFor(PlaceBasis.NEAR_PREV, current, alts, previous, emptySet(), null)?.id)
    }

    @Test
    fun `이미 일정에 있는 곳과 그날 쉬는 곳은 뺀다`() {
        val closedMonday = DayOfWeek.entries.toSet() - DayOfWeek.MONDAY
        val alts = listOf(
            place("taken", 35.1, "museum"),
            place("closed", 35.1, "museum", openDays = closedMonday),
            place("ok", 35.1, "museum"),
        )
        val chosen = pickPlaceFor(PlaceBasis.INDOOR, current, alts, previous, setOf("taken"), DayOfWeek.MONDAY)
        assertEquals("ok", chosen?.id)
    }

    // ---- 실행 ----

    private val day = DayPlan(
        date = LocalDate.of(2026, 11, 2), // 월요일
        dayNumber = 2,
        attractions = listOf(place("a", 35.1, "park"), place("b", 35.2, "park")),
        meals = emptyMap(),
        alternatives = listOf(place("onOtherDay", 35.15, "museum"), place("museum", 35.15, "museum")),
    )

    @Test
    fun `교체하면 자리 키에 후보 id가 기록되고 타임라인에 그 장소가 들어간다`() {
        val summary = summarizeDay(day.toTimeline(), null)
        val alias = summary.keyByAlias.entries.first { it.value == "visit-a" }.key

        val result = applyCommands(
            day, TimelineEdits(), listOf(EditCommand.SwapPlace(alias, PlaceBasis.INDOOR)),
            summary.keyByAlias, null, otherDaysAttractionIds = setOf("onOtherDay"),
        )

        assertEquals(1, result.appliedCount)
        assertEquals(mapOf("visit-a" to "museum"), result.edits.pickedAttractionsIn(2))
        val names = day.toTimeline(pickedAttractions = result.edits.pickedAttractionsIn(2))
            .mapNotNull { (it.item as? TimelineItem.Visit)?.attraction?.id }
        assertEquals(listOf("museum", "b"), names)
    }

    @Test
    fun `후보가 없으면 거부한다`() {
        val empty = day.copy(alternatives = emptyList())
        val summary = summarizeDay(empty.toTimeline(), null)
        val alias = summary.keyByAlias.entries.first { it.value == "visit-a" }.key

        val result = applyCommands(empty, TimelineEdits(), listOf(EditCommand.SwapPlace(alias, PlaceBasis.INDOOR)), summary.keyByAlias, null)

        assertEquals(RejectReason.NO_CANDIDATE, result.rejections.single().reason)
    }

    @Test
    fun `캐시가 바뀌어 기록된 후보가 사라지면 원래 관광지로 보인다`() {
        val edits = mapOf("visit-a" to "gone")
        val ids = day.toTimeline(pickedAttractions = edits).mapNotNull { (it.item as? TimelineItem.Visit)?.attraction?.id }
        assertEquals(listOf("a", "b"), ids)
    }
}
