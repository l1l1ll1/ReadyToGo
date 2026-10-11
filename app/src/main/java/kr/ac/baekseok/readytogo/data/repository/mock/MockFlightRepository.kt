package kr.ac.baekseok.readytogo.data.repository.mock

import kr.ac.baekseok.readytogo.data.model.Flight
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.data.repository.FlightRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * 항공권 Mock.
 *
 * 출발일에 맞춰 시각을 만들어 주므로, 사용자가 고른 날짜가 결과에 그대로 반영된다.
 * 가격은 고정 표를 쓰되 항상 오름차순으로 정렬해 내보낸다(인터페이스 계약).
 */
class MockFlightRepository : FlightRepository {

    override suspend fun search(condition: TripCondition): Result<List<Flight>> {
        delay(NETWORK_DELAY_MS) // 실제 API처럼 잠깐 걸리게 해서 5단계 로딩 UI를 검증할 수 있게 한다
        val departureDate = condition.departureDate ?: LocalDate.now()

        // 고른 도시의 실제 공항 코드를 쓴다. 목록에서 골랐기 때문에 코드가 이미 조건에 들어 있다.
        val originCode = condition.origin?.airportCodes?.firstOrNull() ?: "ICN"
        val arrivalCodes = condition.destination?.airportCodes.orEmpty()

        val flights = TEMPLATES.mapIndexed { index, template ->
            Flight(
                id = template.id,
                airline = template.airlineForLanguage(),
                flightNumber = template.flightNumber,
                departureAirport = originCode,
                // 공항이 둘 이상인 도시(도쿄=HND/NRT)는 편마다 번갈아 배정해 그럴듯하게 만든다.
                arrivalAirport = arrivalCodes.getOrNull(index % arrivalCodes.size.coerceAtLeast(1))
                    ?: template.arrivalAirport,
                departureAt = departureDate.atTime(template.departureTime),
                arrivalAt = departureDate.atTime(template.departureTime)
                    .plusMinutes(template.durationMinutes),
                stops = template.stops,
                priceWon = template.priceWon,
                bookingUrl = "https://example.com/flight/${template.id}",
            )
        }.sortedBy { it.priceWon }

        return Result.success(flights)
    }

    /**
     * 예시 항공편 한 줄.
     *
     * 항공사 이름을 두 언어로 들고 있는 이유: 실제 검색이 실패하면 이 값이 그대로 화면에
     * 올라간다. 영어 화면에 "티웨이항공"이 남으면 다른 데이터까지 못 믿게 된다.
     */
    private data class Template(
        val id: String,
        val airline: String,
        val airlineEn: String,
        val flightNumber: String,
        val arrivalAirport: String,
        val departureTime: LocalTime,
        val durationMinutes: Long,
        val stops: Int,
        val priceWon: Long,
    ) {
        fun airlineForLanguage(): String =
            if (Locale.getDefault().language == "en") airlineEn else airline
    }

    private companion object {
        const val NETWORK_DELAY_MS = 600L

        /** 가격은 왕복 1인 기준. 직항이 비싸고 경유가 싼 실제 경향을 흉내 냈다. */
        val TEMPLATES = listOf(
            Template("f1", "티웨이항공", "T'way Air", "TW301", "NRT", LocalTime.of(7, 30), 140, 0, 312_000),
            Template("f2", "제주항공", "Jeju Air", "7C1102", "NRT", LocalTime.of(9, 5), 145, 0, 348_000),
            Template("f3", "에어부산", "Air Busan", "BX122", "NRT", LocalTime.of(11, 40), 140, 0, 365_000),
            Template("f4", "대한항공", "Korean Air", "KE703", "HND", LocalTime.of(8, 20), 135, 0, 520_000),
            Template("f5", "아시아나항공", "Asiana Airlines", "OZ174", "HND", LocalTime.of(10, 15), 135, 0, 498_000),
            Template("f6", "전일본공수", "ANA", "NH862", "HND", LocalTime.of(13, 0), 140, 0, 545_000),
            Template("f7", "중국동방항공", "China Eastern", "MU5042", "NRT", LocalTime.of(6, 10), 430, 1, 268_000),
            Template("f8", "일본항공", "Japan Airlines", "JL954", "NRT", LocalTime.of(15, 45), 140, 0, 480_000),
        )
    }
}