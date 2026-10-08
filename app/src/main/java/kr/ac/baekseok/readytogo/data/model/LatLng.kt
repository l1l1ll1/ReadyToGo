package kr.ac.baekseok.readytogo.data.model

/**
 * 위경도 좌표.
 *
 * 6단계에서 Google Maps SDK에 그대로 넘길 수 있도록 필드 이름을 Maps의 LatLng과 맞췄다.
 * Maps 타입을 직접 쓰지 않는 이유는 data/model이 안드로이드 라이브러리에 의존하지 않게 하기 위함이다
 * (단위 테스트에서 에뮬레이터 없이 점수 계산을 검증할 수 있어야 한다).
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
)