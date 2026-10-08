package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Attraction
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.ItineraryRepository
import kr.ac.baekseok.readytogo.domain.purpose.PurposeProfiles
import kr.ac.baekseok.readytogo.domain.recommend.attractionScore
import kr.ac.baekseok.readytogo.domain.recommend.distanceKm
import kotlinx.coroutines.delay
import java.time.temporal.ChronoUnit

/**
 * 일정 생성 Mock.
 *
 * 가까운 곳끼리 묶어 하루로 만든다. 가장 평점 높은 곳에서 출발해 매번 제일 가까운 다음 장소로
 * 이어붙이는 방식(최근접 이웃)이다. 최적해는 아니지만 "하루에 도쿄 동쪽과 서쪽을 오가는" 엉터리 동선은
 * 확실히 피할 수 있고, 규칙이 단순해서 결과를 눈으로 검증할 수 있다.
 *
 * 나중에 서버나 AI가 이 자리를 대신하면 훨씬 나은 순서를 낼 수 있다.
 */
class MockItineraryRepository : ItineraryRepository {

    override suspend fun planDailyAttractions(
        condition: TripCondition,
        candidates: List<Attraction>,
    ): Result<List<List<Attraction>>> {
        delay(NETWORK_DELAY_MS)

        val dayCount = dayCountOf(condition)
        if (candidates.isEmpty()) {
            return Result.success(List(dayCount) { emptyList() })
        }

        // 하루에 몇 곳을 넣을지는 여행 목적에 따라 다르다. 휴양이면 느슨하게, 관광이면 빽빽하게.
        val perDay = PurposeProfiles.of(condition.purposes).attractionsPerDay

        // 검색된 순서대로 자르면 어디가 일정에 들어갈지가 사실상 운이다.
        // 평점·동선·목적 일치로 점수를 매겨 필요한 만큼만 먼저 뽑는다.
        val needed = (0 until dayCount).sumOf { capacityOf(it, dayCount, perDay) }
        val anchors = candidates.map { it.location }
        val selected = candidates
            .sortedByDescending { attractionScore(it, anchors, condition.purposes) }
            .take(needed)

        // 뽑고 나서 동선을 엮는다. 순서를 반대로 하면 점수가 낮은 곳이 사슬에 끼어 들어온다.
        val ordered = orderByNearestNeighbour(selected)
        return Result.success(splitIntoDays(ordered, dayCount, perDay))
    }

    /** 평점이 가장 높은 곳에서 시작해, 매번 아직 안 간 곳 중 제일 가까운 곳으로 이어붙인다. */
    private fun orderByNearestNeighbour(candidates: List<Attraction>): List<Attraction> {
        val remaining = candidates.toMutableList()
        var current = remaining.maxBy { it.rating }
        remaining.remove(current)

        val ordered = mutableListOf(current)
        while (remaining.isNotEmpty()) {
            val next = remaining.minBy { distanceKm(current.location, it.location) }
            remaining.remove(next)
            ordered.add(next)
            current = next
        }
        return ordered
    }

    /**
     * 날짜별로 몇 곳씩 갈지 정한 뒤, 순서를 유지한 채 이어서 잘라 담는다.
     *
     * 앞에서부터 정원껏 채우면 안 된다 — 관광지 15곳에 10일 여행이면 앞 5일이 꽉 차고
     * **뒤 5일이 통째로 빈다**. 여행 목적을 하나만 고르면 후보가 4곳이라 5일 여행에서도 터진다.
     * 그래서 하루에 한 곳씩 돌아가며 배분해, 부족분이 특정 날짜에 몰리지 않게 한다.
     *
     * 자르는 것은 연속 구간으로 한다. 순서가 최근접 이웃으로 이어져 있으므로,
     * 번갈아 나눠 담으면 가까운 장소들이 서로 다른 날로 흩어져 동선이 망가진다.
     */
    private fun splitIntoDays(
        ordered: List<Attraction>,
        dayCount: Int,
        fullDayCapacity: Int,
    ): List<List<Attraction>> {
        val counts = IntArray(dayCount)
        var remaining = ordered.size

        // 하루에 한 곳씩 돌아가며 얹는다. 정원이 찬 날은 건너뛴다.
        while (remaining > 0) {
            var placedThisRound = false
            for (day in 0 until dayCount) {
                if (remaining == 0) break
                if (counts[day] >= capacityOf(day, dayCount, fullDayCapacity)) continue
                counts[day]++
                remaining--
                placedThisRound = true
            }
            if (!placedThisRound) break // 모든 날이 정원까지 찼다. 남은 장소는 이번 여행에서 못 간다.
        }

        var index = 0
        return counts.map { count ->
            ordered.subList(index, index + count).toList().also { index += count }
        }
    }

    /**
     * 첫날은 도착, 마지막 날은 출국이라 쓸 수 있는 시간이 적다.
     * 여행 목적과 무관하게 이 두 날은 한 곳만 넣는다.
     */
    private fun capacityOf(dayIndex: Int, dayCount: Int, fullDayCapacity: Int): Int = when {
        dayCount == 1 -> fullDayCapacity
        dayIndex == 0 || dayIndex == dayCount - 1 -> TRAVEL_DAY_CAPACITY
        else -> fullDayCapacity
    }

    /** 여행 일수. 4박이면 5일. 날짜가 없으면 하루로 본다. */
    private fun dayCountOf(condition: TripCondition): Int {
        val departure = condition.departureDate ?: return 1
        val returning = condition.returnDate ?: return 1
        return (ChronoUnit.DAYS.between(departure, returning) + 1).coerceAtLeast(1).toInt()
    }

    private companion object {
        const val NETWORK_DELAY_MS = 400L
        /** 도착일·출국일처럼 이동에 시간을 쓰는 날. */
        const val TRAVEL_DAY_CAPACITY = 1
    }
}