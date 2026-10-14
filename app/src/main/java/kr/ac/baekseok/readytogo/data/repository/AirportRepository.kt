package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.LatLng

/**
 * 공항 코드 → 좌표.
 *
 * 항공편에는 "NRT" 같은 코드만 있어서, 도착일 하루를 공항에서 시작하려면 좌표를 따로 찾아야 한다.
 * 지금은 앱에 포함된 표를 읽지만 출처가 바뀔 수 있어 인터페이스로 둔다(CLAUDE.md).
 */
interface AirportRepository {

    /** 모르는 코드면 null. 호출부는 null이면 공항 대신 숙소를 쓴다. */
    suspend fun locationOf(code: String): LatLng?
}
