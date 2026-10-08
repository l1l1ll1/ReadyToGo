package kr.ac.baekseok.readytogo.data.model

import java.io.Serializable

/**
 * 고를 수 있는 도시 하나.
 *
 * 자유 입력 대신 목록에서 고르게 만든 이유:
 * - 좌표를 미리 들고 있어서 "파리" 같은 글자를 좌표로 바꾸는 Geocoding 호출이 필요 없다(비용 0).
 * - 오타나 모호함("강남"이 어느 강남인지)이 생기지 않는다.
 * - 공항 코드를 같이 들고 있어서 항공권 API를 붙일 때 "도쿄 → NRT" 변환이 따로 필요 없다.
 *
 * 수록 기준은 **정기 여객편이 있는 국제공항 반경 200km 이내**다(차·기차로 2~3시간 근사).
 * 비행기로 가는 해외여행 앱이라 공항에서 닿지 않는 도시는 넣어봐야 쓸 수 없다.
 */
data class Destination(
    val country: String,
    val city: String,
    val location: LatLng,
    /** 이 도시로 갈 때 쓰는 공항 코드. 가까운 순. 도쿄처럼 공항이 둘 이상인 곳도 있다. */
    val airportCodes: List<String>,
) : Serializable {

    /** 목록에서 한 줄로 보여줄 때. 예: "도쿄 (HND)" */
    val displayName: String
        get() = if (airportCodes.isEmpty()) city else "$city (${airportCodes.first()})"
}
