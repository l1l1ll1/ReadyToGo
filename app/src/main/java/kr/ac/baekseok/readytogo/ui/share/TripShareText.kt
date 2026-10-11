package kr.ac.baekseok.readytogo.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.PickedPlaces
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.TripPlan
import kr.ac.baekseok.readytogo.domain.plan.TimelineEntry
import kr.ac.baekseok.readytogo.domain.plan.TimelineItem
import kr.ac.baekseok.readytogo.domain.plan.toTimeline
import kr.ac.baekseok.readytogo.ui.format.adultsText
import kr.ac.baekseok.readytogo.ui.format.dateWithDay
import kr.ac.baekseok.readytogo.ui.format.money
import kr.ac.baekseok.readytogo.ui.format.travelLegText
import kr.ac.baekseok.readytogo.ui.format.tripRangeText
import java.time.format.DateTimeFormatter

/**
 * 공유할 일정 글.
 *
 * **@Composable인 이유**: 금액·날짜·소요시간을 [money]·[dateWithDay] 같은 AppFormat
 * 함수에 맡겨야 언어 설정과 달러 환산을 그대로 따라간다. 그 함수들이 리소스를 읽으므로
 * composition 안에서만 부를 수 있다. 여기서 직접 포맷하면 공유 글만 늘 한국어·원화로
 * 나가서, 영어로 쓰는 사람이 자기 화면과 다른 글을 보내게 된다.
 *
 * **쓰는 쪽은 공유를 누른 순간에만 부른다.** 매 recomposition마다 글을 만들 이유가 없다.
 *
 * **화면에 보이는 그대로를 담는다.** 계획 원본이 아니라 편집(교체·삭제·순서)을 적용한
 * 타임라인을 쓴다 — 보낸 글과 내 화면이 다르면 공유가 거짓말이 된다.
 *
 * **장소마다 지도 링크를 넣지 않는다.** 하루 여섯 줄 × 닷새면 링크만 서른 개라 글이
 * 읽히지 않는다. 링크는 장소 하나를 공유할 때만 붙인다([placeShareText]).
 */
@Composable
fun tripShareText(
    plan: TripPlan,
    edits: TimelineEdits,
    pickedPlaces: PickedPlaces,
): String {
    val lines = mutableListOf<String>()

    // 첫 줄에는 이모지를 붙이지 않는다. 바로 아래 항공 줄이 ✈️를 쓰는데 둘이 나란히
    // 서면 같은 뜻처럼 읽힌다. 맨 윗줄은 표식이 없어도 제목으로 읽힌다.
    plan.condition.destination?.let { destination ->
        lines += destination.shortName
    }
    val departure = plan.condition.departureDate
    val returnDate = plan.condition.returnDate
    if (departure != null && returnDate != null) {
        lines += "${tripRangeText(departure, returnDate)} · ${adultsText(plan.condition.adults)}"
    }

    plan.flights.firstOrNull()?.let { flight ->
        lines += ""
        // 예시 데이터면 그 사실을 같이 보낸다. 받는 쪽은 화면의 "예시 데이터" 표시를
        // 볼 수 없어서, 적지 않으면 이 가격을 실제 요금으로 믿는다.
        val sample = if (flight.isSample) " (${stringResource(R.string.plan_sample_title)})" else ""
        lines += "✈️ ${flight.airline} ${flight.flightNumber} · ${money(flight.priceWon)}$sample"
        lines += "   " + stringResource(
            R.string.plan_flight_route,
            flight.departureAirport,
            flight.departureAt.format(TIME),
            flight.arrivalAirport,
            flight.arrivalAt.format(TIME),
        ) + " · " + if (flight.stops == 0) {
            stringResource(R.string.plan_flight_direct)
        } else {
            pluralStringResource(R.plurals.stops_count, flight.stops, flight.stops)
        }
    }

    plan.lodging?.qualityFirst?.item?.let { lodging ->
        lines += ""
        // **"1박"을 반드시 붙인다.** 금액만 적으면 닷새치 총액으로 읽힌다.
        // 화면의 숙소 카드와 같은 문구를 쓴다 — 보낸 글과 내 화면이 같아야 한다.
        val price = stringResource(
            if (lodging.priceEstimated) {
                R.string.plan_lodging_price_estimated
            } else {
                R.string.plan_lodging_price
            },
            money(lodging.priceWon),
        )
        lines += "🏨 ${lodging.name} · $price"
        lines += "   ${lodging.address}"
    }

    plan.dailyPlans.forEach { day ->
        val entries = day.toTimeline(
            deletedKeys = edits.deletedIn(day.dayNumber),
            pickedMeals = edits.pickedIn(day.dayNumber),
            customOrder = edits.orderIn(day.dayNumber),
            pickedAttractions = edits.pickedAttractionsIn(day.dayNumber),
            picked = pickedPlaces,
        )
        if (entries.isEmpty()) return@forEach

        lines += ""
        lines += "[${stringResource(R.string.plan_day_tab, day.dayNumber)} ${dateWithDay(day.date)}]"
        entries.forEach { entry -> lines += entryLines(entry) }
    }

    lines += ""
    lines += stringResource(R.string.share_footer)

    return lines.joinToString("\n")
}

/** 타임라인 한 줄 — 앞에 오는 이동, 그다음 장소. 화면의 배치를 글로 옮긴 것이다. */
@Composable
private fun entryLines(entry: TimelineEntry): List<String> {
    val lines = mutableListOf<String>()

    entry.travelFromPrevious?.let { leg ->
        lines += "   ↓ ${travelLegText(leg)}"
    }

    when (val item = entry.item) {
        is TimelineItem.Meal -> {
            val restaurant = item.restaurant
            lines += "${stringResource(item.meal.labelRes)} · ${restaurant.name}"
            lines += "   ${restaurant.address}"
        }

        is TimelineItem.Visit -> {
            lines += "📍 ${item.attraction.name}"
            lines += "   ${item.attraction.address}"
        }

        // 추천을 못 찾은 끼니도 자리를 남긴다. 빼 버리면 "아침 다음 저녁"이 되어
        // 점심을 잊은 것처럼 보인다 — 화면과 같은 이유다.
        is TimelineItem.NoMeal -> {
            lines += "${stringResource(item.meal.labelRes)} · " +
                stringResource(R.string.plan_sheet_no_restaurant)
        }
    }

    return lines
}

/**
 * 장소 하나를 공유하는 글.
 *
 * 여기에는 지도 링크를 붙인다. 한 곳만 보내는 글이라 링크가 묻히지 않고, 받는 쪽이
 * 가장 먼저 하는 일이 그 장소를 지도에서 여는 것이다.
 */
@Composable
fun placeShareText(item: TimelineItem): String {
    val lines = mutableListOf<String>()

    when (item) {
        is TimelineItem.Meal -> {
            val restaurant = item.restaurant
            lines += restaurant.name
            lines += restaurant.category
            lines += restaurant.address
            lines += restaurant.mapUrl
        }

        is TimelineItem.Visit -> {
            val attraction = item.attraction
            lines += attraction.name
            lines += attraction.address
            lines += attraction.mapUrl
        }

        is TimelineItem.NoMeal -> lines += stringResource(R.string.plan_sheet_no_restaurant)
    }

    return lines.filter { it.isNotBlank() }.joinToString("\n")
}

/** 공유 글의 시각. 화면의 항공 카드와 같은 모양을 쓴다. */
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
