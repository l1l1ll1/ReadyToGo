package kr.ac.baekseok.readytogo.data

/**
 * 실제 외부 API를 쓸지 정하는 스위치.
 *
 * **기본값 true (실서비스).** 실제 사용자는 당연히 실제 데이터를 봐야 한다.
 *
 * **false는 테스트 전용이다.** 화면을 다듬는 동안 계획 생성을 수십 번 돌리면 Google Places가
 * 매번 6~10회씩 나가는데 결과는 거의 같아서 돈만 쓴다. 그럴 때만 잠시 내려 두고,
 * 끝나면 반드시 true로 되돌린다.
 *
 * false일 때 쓰이는 Mock 데이터는 **도쿄 기준 가짜 데이터**다. 목적지를 파리로 골라도
 * 도쿄 식당이 나온다 — 화면 모양을 보는 데는 문제가 없지만 추천 결과를 판단할 수는 없다.
 *
 * Mock과 실제 API는 **식당 중복 정책도 다르다**. Mock은 후보가 15곳뿐이라 긴 여행에서 금방
 * 소진되므로 재사용을 허용하고, 실제 API 경로에서는 같은 식당을 두 번 추천하지 않는다
 * (`TripPlanner`의 `allowRestaurantReuse` 참고).
 *
 * 항공(SerpApi)은 실패하면 Mock으로 넘어가지만, 이 값이 false면 아예 호출하지 않는다.
 */
const val USE_REAL_API = true
