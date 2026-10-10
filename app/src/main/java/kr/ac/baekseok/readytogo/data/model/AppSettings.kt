package kr.ac.baekseok.readytogo.data.model

import androidx.annotation.StringRes
import kr.ac.baekseok.readytogo.R

/**
 * 앱 설정.
 *
 * 기기에 저장되고(PreferencesSettingsRepository) 각 값이 실제로 동작한다 —
 * 언어는 시스템 앱별 언어로, 글자 크기·고대비는 테마로, 알림은 예약으로 이어진다.
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

/**
 * 앱 언어.
 *
 * @param tag 시스템에 넘길 BCP-47 태그. `values-en` 같은 리소스 폴더 이름과 맞아야 한다.
 */
enum class AppLanguage(@StringRes val labelRes: Int, val tag: String) {
    KOREAN(R.string.language_korean, "ko"),
    ENGLISH(R.string.language_english, "en"),
}

/**
 * 글자 크기.
 *
 * 시스템 글자 크기 설정과 별개로 앱 안에서만 조절할 수 있게 한다 —
 * 여행 중에 작은 글씨가 안 보여도 기기 설정까지 들어가게 하고 싶지 않다.
 *
 * **1.3배까지 두지 않는 이유**: 결과 화면의 카드가 1.15배에서 이미 두 줄로 넘어간다.
 * 그 이상이 필요하면 기기 설정의 글자 크기가 더 크게 올라가고, 그쪽은 화면 배치까지
 * 안드로이드가 같이 조정해 준다.
 *
 * @param scale 기본 크기에 곱할 배수.
 */
enum class TextScale(@StringRes val labelRes: Int, val scale: Float) {
    SMALL(R.string.text_scale_small, 0.9f),
    NORMAL(R.string.text_scale_normal, 1.0f),
    LARGE(R.string.text_scale_large, 1.15f),
}
