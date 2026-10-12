package kr.ac.baekseok.readytogo.data.model

import java.io.Serializable

/**
 * 위경도 좌표.
 *
 * 6단계에서 Google Maps SDK에 그대로 넘길 수 있도록 필드 이름을 Maps의 LatLng과 맞췄다.
 * Maps 타입을 직접 쓰지 않는 이유는 data/model이 안드로이드 라이브러리에 의존하지 않게 하기 위함이다
 * (단위 테스트에서 에뮬레이터 없이 점수 계산을 검증할 수 있어야 한다).
 *
 * Serializable인 이유: [Destination]이 이 좌표를 품고 있어서, 여행 조건을 Bundle·Intent에
 * 담을 때 여기가 빠지면 통째로 실패한다. 알림에 조건을 담다가 앱이 죽어서 드러났다.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
) : Serializable
