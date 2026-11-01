package kr.ac.baekseok.readytogo.data.repository.weather

import kr.ac.baekseok.readytogo.BuildConfig

/**
 * **디버그 빌드 전용.** 날씨를 확인할 때 "오늘"을 며칠 뒤로 민다.
 *
 * 왜 필요한가: 날씨는 D-7 안의 날만 묻는데, 확인용 일정(12월 삿포로)은 50일도 더 남았다.
 * Open-Meteo는 16일 밖의 날짜를 거절하므로 오늘만 바꿔서는 예보를 받을 수 없다.
 * 그래서 오늘을 N일 미는 동시에, 요청 날짜는 N일 당겨 **지금 받을 수 있는 실제 예보를 여행 날짜에 옮겨 붙인다.**
 *
 * 켜는 법: `adb shell setprop debug.readytogo.weather_shift 53` (끄려면 0).
 * 릴리스 빌드에서는 항상 0이라 실제 날짜만 쓴다.
 */
object WeatherDebugShift {

    private const val PROPERTY = "debug.readytogo.weather_shift"

    fun days(): Long {
        if (!BuildConfig.DEBUG) return 0
        // 시스템 속성을 읽는 공개 API가 없어 getprop을 부른다. 디버그 빌드에서만 도는 길이다.
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", PROPERTY))
            process.inputStream.bufferedReader().readText().trim().toLongOrNull() ?: 0
        }.getOrDefault(0)
    }
}
