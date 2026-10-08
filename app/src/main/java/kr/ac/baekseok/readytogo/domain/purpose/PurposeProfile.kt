package kr.ac.baekseok.readytogo.domain.purpose

import kr.ac.baekseok.readytogo.data.model.TravelPurpose
import kotlin.math.roundToInt

/**
 * 여행 목적에 따라 달라지는 값들.
 *
 * 목적을 고르게 해놓고 결과가 똑같으면 고를 이유가 없다.
 * 맛집을 고르면 식비 비중이 올라가고, 휴양을 고르면 하루 일정이 느슨해져야 한다.
 */
data class PurposeProfile(
    /** 항공료를 뺀 나머지 예산에서 숙박이 가져갈 몫. */
    val lodgingShare: Double,
    val foodShare: Double,
    /** 현지 교통·입장료·예비비. */
    val otherShare: Double,
    /** 온전히 쓸 수 있는 날에 넣을 관광지 수. 도착일·출국일은 이 값과 무관하게 1곳이다. */
    val attractionsPerDay: Int,
)

object PurposeProfiles {

    /** 목적을 하나도 고르지 않았을 때. */
    val DEFAULT = PurposeProfile(
        lodgingShare = 0.45,
        foodShare = 0.30,
        otherShare = 0.25,
        attractionsPerDay = 3,
    )

    /**
     * 목적별 값.
     *
     * 숫자만 바꿔 조정할 수 있게 표로 둔다. 각 행의 비율 합은 1이어야 한다.
     * - 휴양: 숙소에 오래 머무니 숙박 비중이 높고 일정은 느슨하다.
     * - 관광: 입장료가 많이 들어 기타 비중이 높고 하루에 여러 곳을 돈다.
     * - 맛집: 식비가 여행의 목적이므로 식비 비중이 가장 높다.
     * - 액티비티: 체험 비용이 기타에 들어간다.
     */
    private val BY_PURPOSE: Map<TravelPurpose, PurposeProfile> = mapOf(
        TravelPurpose.REST to PurposeProfile(0.55, 0.25, 0.20, 2),
        TravelPurpose.SIGHTSEEING to PurposeProfile(0.40, 0.25, 0.35, 4),
        TravelPurpose.FOOD to PurposeProfile(0.35, 0.45, 0.20, 3),
        TravelPurpose.ACTIVITY to PurposeProfile(0.35, 0.25, 0.40, 3),
    )

    /**
     * 고른 목적들의 값을 평균낸다.
     *
     * 예) 관광 + 휴양 → 숙박 0.475, 식비 0.25, 기타 0.275, 하루 3곳.
     *
     * 비율은 평균 뒤 합이 1이 되게 정규화한다. 위 표의 각 행이 이미 합 1이라 평균도 1이지만,
     * 나중에 표를 고칠 때 합이 어긋나도 예산이 새거나 남지 않도록 해둔다.
     */
    fun of(purposes: Set<TravelPurpose>): PurposeProfile {
        val profiles = purposes.mapNotNull { BY_PURPOSE[it] }
        if (profiles.isEmpty()) return DEFAULT

        val lodging = profiles.sumOf { it.lodgingShare } / profiles.size
        val food = profiles.sumOf { it.foodShare } / profiles.size
        val other = profiles.sumOf { it.otherShare } / profiles.size
        val total = lodging + food + other

        return PurposeProfile(
            lodgingShare = lodging / total,
            foodShare = food / total,
            otherShare = other / total,
            attractionsPerDay = (profiles.sumOf { it.attractionsPerDay }.toDouble() / profiles.size)
                .roundToInt(),
        )
    }
}
