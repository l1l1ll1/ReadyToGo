package kr.ac.baekseok.readytogo.data.model

import java.time.LocalDate

/** 추천된 항목 하나와 그 근거. */
data class Recommendation<T>(
    val item: T,
    /** 0.0 ~ 1.0. 평점 점수와 동선 점수를 가중 합산한 값. */
    val score: Double,
    /** 예산을 넘겼으면 true. 화면에 "예산 초과" 표시를 붙인다(CLAUDE.md). */
    val overBudget: Boolean,
    /**
     * 이번 일정의 중심점까지의 거리(km). 일정에 장소가 없으면 null.
     * 점수만 보여주면 왜 이게 추천됐는지 알 수 없어, 근거가 되는 거리를 같이 내보낸다.
     */
    val distanceFromCenterKm: Double? = null,
)

/** 두 번째 선택지가 어떤 기준으로 뽑혔는지. 화면 이름표가 여기서 나온다. */
enum class AlternativeKind(val label: String) {
    /** 1등이 예산을 넘겨서, 예산 안에서 점수가 가장 높은 것을 골랐다. */
    WITHIN_BUDGET("예산 내"),

    /** 1등이 이미 예산 안이라, 더 싼 것 중 점수가 가장 높은 것을 골랐다. */
    CHEAPER("가성비"),

    /** 더 싼 후보가 없어서, 동선이 가장 좋은 것을 골랐다. */
    CLOSEST("동선 우선"),

    /** 후보가 하나뿐이라 대안을 만들 수 없었다. */
    NONE("대안 없음"),
}

/**
 * 숙소·식당은 항상 두 가지 선택지를 함께 보여준다(CLAUDE.md).
 * 화면에서 하나를 빠뜨릴 수 없도록 타입으로 묶어둔다.
 */
data class RecommendationPair<T>(
    /** 점수가 가장 높은 항목. 예산을 넘길 수도 있다. */
    val qualityFirst: Recommendation<T>,
    /**
     * 두 번째 선택지. 어떤 기준으로 뽑혔는지는 alternativeKind가 말해준다.
     *
     * 후보가 하나뿐이면 null이다. 예전에는 같은 곳을 두 칸에 넣었는데,
     * 같은 가게가 "추천"과 "대안"에 나란히 서면 선택지가 있는 것처럼 보여 오해를 준다.
     */
    val withinBudget: Recommendation<T>?,
    val alternativeKind: AlternativeKind,
)

/** 끼니. 화면에서 MealTime.entries 순서대로 돌리면 아침→점심→저녁이 된다. */
enum class MealTime(val label: String) {
    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁"),
}

/**
 * 하루치 일정.
 *
 * meals를 Map으로 둔 이유: 도착일 아침이나 출국일 저녁처럼 끼니가 비는 날이 생긴다.
 * 필드 세 개를 각각 nullable로 두는 것보다, 없는 끼니는 키 자체가 없는 편이 화면 코드가 단순하다.
 */
data class DayPlan(
    val date: LocalDate,
    /** 1일차, 2일차… 화면 표시용. */
    val dayNumber: Int,
    /** 방문 순서대로 정렬된 관광지. 그날 식당의 동선 점수 기준점이기도 하다. */
    val attractions: List<Attraction>,
    val meals: Map<MealTime, RecommendationPair<Restaurant>>,
)

/**
 * 결과 화면(4단계)에 넘길 완성된 여행 계획.
 *
 * 입력 조건을 함께 들고 다니는 이유는, 화면에서 "예산 150만원 / 9.24~9.28" 같은 요약을
 * 다시 보여줘야 하고 예산 초과 여부를 판단할 기준도 필요하기 때문이다.
 */
data class TripPlan(
    val condition: TripCondition,
    /** 이 계획을 만들 때 쓴 예산 배분. 결과 화면에서 "기타 25만원" 같은 안내에 쓴다. */
    val budget: BudgetAllocation,
    /** 최저가 오름차순(CLAUDE.md 항공권 기준). */
    val flights: List<Flight>,
    /** 숙소는 여행 전체에 하나. 후보가 없으면 null. */
    val lodging: RecommendationPair<Lodging>?,
    val dailyPlans: List<DayPlan>,
)