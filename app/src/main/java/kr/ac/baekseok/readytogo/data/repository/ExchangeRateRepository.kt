package kr.ac.baekseok.readytogo.data.repository

/**
 * 환율.
 *
 * Places가 주는 가격은 현지 통화(EUR, JPY…)인데 예산은 원화로 입력받는다.
 * 비교하려면 환산이 필요하고, 환율은 코드에 적어두면 금방 낡기 때문에 데이터 출처로 분리한다.
 */
interface ExchangeRateRepository {
    /**
     * 해당 통화 1단위가 몇 원인지. 예: "EUR" → 1450.0
     *
     * 실패하면 Result.failure를 주지만, 구현체는 마지막 수단으로 내장 기본값을 쓰는 편이 낫다.
     * 환율을 못 받았다고 계획 전체를 실패시킬 이유는 없다.
     */
    suspend fun rateToKrw(currencyCode: String): Result<Double>

    /**
     * 환율을 미리 받아둔다.
     *
     * 부르지 않아도 동작하지만, 그러면 첫 환산이 일어나는 순간에야 네트워크를 타서
     * 그만큼 기다리게 된다. 다른 검색과 함께 미리 시작하면 그 대기가 사라진다.
     */
    suspend fun prepare() = Unit
}
