package kr.ac.baekseok.readytogo.data.model

import androidx.annotation.StringRes
import kr.ac.baekseok.readytogo.R
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
    /**
     * 앞뒤 일정 사이에 들렀다 갈 때 더 가는 거리(km). 끼니 식당에만 채워진다.
     * 식당은 중심 거리가 아니라 이 값으로 동선 점수를 매기므로, 근거도 이 값을 보여준다.
     */
    val detourKm: Double? = null,
)

/**
 * 두 번째 선택지가 어떤 기준으로 뽑혔는지. 화면 이름표가 여기서 나온다.
 *
 * 글자가 아니라 리소스 id를 드는 이유는 [TravelPurpose] 주석 참고.
 */
enum class AlternativeKind(@StringRes val labelRes: Int) {
    /** 1등이 예산을 넘겨서, 예산 안에서 점수가 가장 높은 것을 골랐다. */
    WITHIN_BUDGET(R.string.alternative_within_budget),

    /** 1등이 이미 예산 안이라, 더 싼 것 중 점수가 가장 높은 것을 골랐다. */
    CHEAPER(R.string.alternative_cheaper),

    /** 더 싼 후보가 없어서, 동선이 가장 좋은 것을 골랐다. */
    CLOSEST(R.string.alternative_closest),

    /** 후보가 하나뿐이라 대안을 만들 수 없었다. */
    NONE(R.string.alternative_none),
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
    /**
     * 사용자가 직접 고를 수 있는 다른 후보들. 점수 높은 순.
     *
     * 위 두 칸에 이미 나온 곳은 들어 있지 않다. 비어 있을 수 있다 — 후보가 둘뿐이면
     * 추천과 대안으로 다 쓰여서 남는 것이 없다.
     *
     * 기본값을 둔 이유: 이 목록이 생기기 전에 저장된 일정을 읽을 때 필요하다.
     */
    val alternatives: List<Recommendation<T>> = emptyList(),
)

/** 끼니. 화면에서 MealTime.entries 순서대로 돌리면 아침→점심→저녁이 된다. */
enum class MealTime(@StringRes val labelRes: Int) {
    BREAKFAST(R.string.meal_breakfast),
    LUNCH(R.string.meal_lunch),
    DINNER(R.string.meal_dinner),
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
    /**
     * 도착일에 내리고 출국일에 떠나는 공항. 모르면 null이고, 그때는 그 두 날도 숙소에서 시작·끝난다.
     *
     * 예시 항공편(Flight.isSample)이면 null이다 — 예시의 공항은 지어낸 값이라 동선 기준으로 쓰면
     * 엉뚱한 공항 쪽으로 식당이 끌려간다.
     */
    val airport: AirportStop? = null,
)

/**
 * 공항 하나. 지도 마커 제목에 코드가 필요해서 좌표와 같이 든다.
 *
 * 귀국편도 같은 공항에서 떠난다고 본다. 항공 검색 1차 응답이 가는 편만 주기 때문이다.
 */
data class AirportStop(
    val code: String,
    val location: LatLng,
)