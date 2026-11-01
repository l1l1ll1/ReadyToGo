package kr.ac.baekseok.readytogo.domain.weather

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyWeatherTest {

    @Test
    fun `WMO 코드를 맑음 흐림 비 눈으로 나눈다`() {
        assertEquals(Sky.CLEAR, skyOf(0))
        assertEquals(Sky.CLEAR, skyOf(1))
        assertEquals(Sky.CLOUDY, skyOf(3))
        assertEquals(Sky.CLOUDY, skyOf(45))
        assertEquals(Sky.RAIN, skyOf(51))
        assertEquals(Sky.RAIN, skyOf(61))
        assertEquals(Sky.RAIN, skyOf(81))
        assertEquals(Sky.RAIN, skyOf(95))
        assertEquals(Sky.SNOW, skyOf(71))
        assertEquals(Sky.SNOW, skyOf(85))
        // 표에 없는 코드는 흐림.
        assertEquals(Sky.CLOUDY, skyOf(42))
    }

    @Test
    fun `오늘부터 7일 안의 여행 날짜만 묻는다`() {
        val today = LocalDate.of(2026, 11, 1)
        val trip = (0L..9L).map { today.minusDays(1).plusDays(it) } // 10/31 ~ 11/9

        val dates = forecastDates(trip, today)

        assertEquals(today, dates.first())
        assertEquals(today.plusDays(7), dates.last())
        assertEquals(8, dates.size)
    }

    @Test
    fun `D-7 밖의 여행은 묻지 않는다`() {
        val today = LocalDate.of(2026, 11, 1)
        val trip = listOf(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25))
        assertEquals(emptyList<LocalDate>(), forecastDates(trip, today))
    }
}
