package kr.ac.baekseok.readytogo.domain.purpose

import kr.ac.baekseok.readytogo.data.model.Attraction

/** 관광지가 비를 피할 수 있는 곳인지. [UNKNOWN]은 판정을 미룬 것이다. */
enum class Shelter { INDOOR, OUTDOOR, UNKNOWN }

/**
 * Places 타입으로 실내·실외를 가른다. 추가 호출 없이 이미 받은 타입만 본다.
 *
 * **애매하면 [Shelter.UNKNOWN]이다** — 사찰·신사·`tourist_attraction`·전망대·시장처럼
 * 건물과 마당이 섞인 곳. 비 규칙에 걸지 않고, "실내로 교체" 후보로도 뽑지 않는다.
 * 실외를 실내로 잘못 보내는 것보다 경고 하나를 놓치는 편이 낫다.
 *
 * 본업 타입([Attraction.primaryType])으로 판정한다. 본업이 표에 없는 구체적 타입(buddhist_temple 등)이면
 * 그대로 보류다 — 절에 딸린 정원 때문에 `types`에 park가 있어도 절을 실외로 보지 않는다.
 * 본업이 없거나(예전 캐시) 범용 타입([GENERIC])일 때만 [Attraction.types]에서 처음 걸리는 것으로 본다.
 * 타입 이름은 Places place type 표에 있는 것만 쓴다.
 */
object IndoorOutdoor {

    // 본업 타입은 상위 타입(park)이 아니라 세부 타입(city_park)으로 오는 일이 많다. 세부 타입을 빠뜨리면
    // 본업이 "표에 없는 구체적 타입"이 되어 판정이 보류된다(삿포로: 나카지마 공원 = city_park,
    // 찜질방 = public_bath). 새 데이터에서 실제로 나온 본업 타입을 보고 채운다.
    private val INDOOR = setOf(
        "museum", "art_gallery", "aquarium", "shopping_mall", "department_store", "discount_store",
        "movie_theater", "performing_arts_theater", "bowling_alley", "amusement_center", "casino", "spa",
        "public_bath", "planetarium", "library",
    )

    private val OUTDOOR = setOf(
        "park", "city_park", "state_park", "national_park", "dog_park", "zoo", "beach", "hiking_area",
        "garden", "botanical_garden", "amusement_park", "water_park", "ski_resort", "marina",
    )

    /** 아무것도 알려주지 않는 타입. 본업이 이것이면 부가 타입을 본다. */
    private val GENERIC = setOf("tourist_attraction", "point_of_interest", "establishment")

    fun of(attraction: Attraction): Shelter {
        val primary = attraction.primaryType
        if (primary != null && primary !in GENERIC) return shelterOf(primary) ?: Shelter.UNKNOWN
        return attraction.types.firstNotNullOfOrNull(::shelterOf) ?: Shelter.UNKNOWN
    }

    private fun shelterOf(type: String): Shelter? = when (type) {
        in INDOOR -> Shelter.INDOOR
        in OUTDOOR -> Shelter.OUTDOOR
        else -> null
    }
}
