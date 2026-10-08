package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.Destination

/**
 * 고를 수 있는 나라·도시 목록.
 *
 * 지금은 앱에 포함된 JSON을 읽지만, 나중에 서버에서 받아오도록 바꿀 수 있게 인터페이스로 둔다.
 * (CLAUDE.md: 모든 데이터 출처는 Repository 인터페이스로 분리)
 */
interface DestinationRepository {

    /** 목적지로 고를 수 있는 나라. 가나다순. */
    suspend fun countries(): Result<List<String>>

    /** 해당 나라의 도시. 인구가 많은 순으로 정렬돼 있어 유명한 곳이 위에 온다. */
    suspend fun cities(country: String): Result<List<Destination>>

    /** 출발 공항. 한국 국제공항 목록이라 나라를 고를 필요가 없다. */
    suspend fun origins(): Result<List<Destination>>

    /**
     * 전 세계에서 도시를 찾는다. 도시명·나라명·공항 코드가 걸린다.
     *
     * 나라를 먼저 고르게 하는 [cities]와 따로 두는 이유: 사용자는 보통 가고 싶은 도시를
     * 이미 알고 있다. "도쿄"를 찾으려고 일본을 먼저 골라야 하면 단계가 하나 더 늘어난다.
     * 나라별로 둘러보고 싶을 때를 위해 [cities]도 남겨 둔다.
     */
    suspend fun searchCities(query: String): Result<List<Destination>>
}
