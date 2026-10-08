package kr.ac.baekseok.readytogo.data.model

/**
 * 앱 설정.
 *
 * 1단계에서는 값을 들고만 있고 실제로 동작하지는 않는다(언어를 바꿔도 화면은 그대로다).
 * 8단계 3번에서 각 값을 실제 동작에 연결한다. 그래도 모델과 화면을 먼저 만드는 이유는
 * 설정 화면이 "무엇을 설정할 수 있는 앱인지"를 보여주는 자리이기 때문이다.
 */
data class AppSettings(
    val language: AppLanguage = AppLanguage.KOREAN,
    /** 출발 7일 전 알림. */
    val notifyBeforeWeek: Boolean = true,
    /** 출발 1일 전 알림. */
    val notifyBeforeDay: Boolean = true,
    val textScale: TextScale = TextScale.NORMAL,
    /** 고대비. 켜면 글자와 배경 대비를 높인다. */
    val highContrast: Boolean = false,
)

enum class AppLanguage(val label: String, val tag: String) {
    KOREAN("한국어", "ko"),
    ENGLISH("English", "en"),
}

/**
 * 글자 크기.
 *
 * 시스템 글자 크기 설정과 별개로 앱 안에서만 조절할 수 있게 한다 —
 * 여행 중에 작은 글씨가 안 보여도 기기 설정까지 들어가게 하고 싶지 않다.
 *
 * @param scale 기본 크기에 곱할 배수.
 */
enum class TextScale(val label: String, val scale: Float) {
    NORMAL("보통", 1.0f),
    LARGE("크게", 1.15f),
    LARGEST("아주 크게", 1.3f),
}
