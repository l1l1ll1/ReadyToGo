package kr.ac.baekseok.readytogo.data.repository.firebase

import kr.ac.baekseok.readytogo.data.model.Destination
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kr.ac.baekseok.readytogo.data.model.TripCondition
import java.time.LocalDate

/**
 * Firestore에 넣고 꺼내기 위한 변환.
 *
 * Firestore는 Map·List·기본형만 저장한다. [LocalDate]나 [TravelPurpose] 같은 타입은
 * 직접 넣을 수 없어서 손으로 바꾼다.
 *
 * **모델에 어노테이션을 붙이지 않는 이유**: `data/model`은 Firestore를 몰라야 한다.
 * 나중에 저장 위치를 자체 서버로 바꿀 때 모델까지 고치게 되면 Repository 인터페이스로
 * 분리한 의미가 없다(CLAUDE.md 개발 전략).
 *
 * 날짜는 ISO 문자열("2026-10-16")로 넣는다. Firestore Timestamp를 쓰면 시간대가 끼어들어
 * 기기 설정에 따라 하루가 밀린다 — 여행 날짜에 시각은 의미가 없다.
 *
 * 읽을 때 필드가 없거나 형식이 달라도 예외를 던지지 않는다. 모델에 필드를 추가하면
 * 예전에 저장한 문서에는 그 값이 없는데, 그때 앱이 죽으면 저장된 일정 전체를 못 보게 된다.
 */

internal fun TripCondition.toMap(): Map<String, Any?> = mapOf(
    "origin" to origin?.toMap(),
    "destination" to destination?.toMap(),
    "departureDate" to departureDate?.toString(),
    "returnDate" to returnDate?.toString(),
    "budgetWon" to budgetWon,
    "purposes" to purposes.map { it.name },
    "adults" to adults,
)

internal fun Map<String, Any?>.toTripCondition() = TripCondition(
    origin = childMap("origin")?.toDestination(),
    destination = childMap("destination")?.toDestination(),
    departureDate = localDate("departureDate"),
    returnDate = localDate("returnDate"),
    budgetWon = (this["budgetWon"] as? Number)?.toLong(),
    purposes = stringList("purposes").mapNotNull { name ->
        // 저장된 뒤 enum 이름이 바뀌었거나 사라졌으면 그 항목만 버린다.
        runCatching { TravelPurpose.valueOf(name) }.getOrNull()
    }.toSet(),
    // adults가 없는 문서는 인원 기능 전에 저장된 것이다. 1명으로 본다.
    adults = (this["adults"] as? Number)?.toInt() ?: 1,
)

private fun Destination.toMap(): Map<String, Any?> = mapOf(
    "country" to country,
    "city" to city,
    // 영문 이름도 같이 저장한다. 없으면 영어로 바꿨을 때 저장된 일정만 한글로 남는다 —
    // 목록을 다시 뒤져 이름을 맞추느니 조건에 같이 들고 다니는 쪽이 싸다.
    "cityEn" to cityEn,
    "countryEn" to countryEn,
    "lat" to location.latitude,
    "lon" to location.longitude,
    "airportCodes" to airportCodes,
)

private fun Map<String, Any?>.toDestination(): Destination? {
    val country = this["country"] as? String ?: return null
    val city = this["city"] as? String ?: return null
    val lat = (this["lat"] as? Number)?.toDouble() ?: return null
    val lon = (this["lon"] as? Number)?.toDouble() ?: return null

    return Destination(
        country = country,
        city = city,
        location = LatLng(lat, lon),
        airportCodes = stringList("airportCodes"),
        // 이 열이 생기기 전에 저장된 문서에는 없다. 비면 한글 이름을 그대로 쓴다.
        cityEn = this["cityEn"] as? String ?: "",
        countryEn = this["countryEn"] as? String ?: "",
    )
}

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.childMap(key: String): Map<String, Any?>? =
    this[key] as? Map<String, Any?>

internal fun Map<String, Any?>.stringList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

internal fun Map<String, Any?>.localDate(key: String): LocalDate? {
    val text = this[key] as? String ?: return null
    return runCatching { LocalDate.parse(text) }.getOrNull()
}
