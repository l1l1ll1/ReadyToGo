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
    /**
     * 출발 7일 전 알림.
     *
     * **기본값이 꺼짐인 이유**: 안드로이드 13부터 알림에는 사용자 허락이 따로 필요하다.
     * 켜진 상태로 시작하면, 허락하지 않은 사용자에게 "알림을 보내는 중"이라고 보여주면서
     * 실제로는 아무것도 가지 않는다. 켤 때 허락을 묻는 쪽이 약속과 동작이 맞는다.
     */
    val notifyBeforeWeek: Boolean = false,
    /** 출발 1일 전 알림. [notifyBeforeWeek]와 같은 이유로 기본값이 꺼짐이다. */
    val notifyBeforeDay: Boolean = false,
    val textScale: TextScale = TextScale.NORMAL,
    /** 고대비. 켜면 글자와 배경 대비를 높인다. */
    val highContrast: Boolean = false,
    /** 밝게 볼지 어둡게 볼지. 기본은 기기 설정을 따른다. */
    val theme: AppTheme = AppTheme.SYSTEM,
    /**
     * 장소 사진을 받아 보여줄지.
     *
     * **기본값이 꺼짐인 이유**: 사진은 요청마다 과금되고 데이터도 쓴다. 일정 하나를
     * 펼치면 수십 장이 오가는데, 그걸 사용자가 모르는 채로 시작하면 안 된다.
     * 켜고 싶은 사람만 켠다.
     */
    val showPlacePhotos: Boolean = false,
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
 * 화면을 밝게 볼지 어둡게 볼지.
 *
 * **고대비와 별개다.** 고대비는 접근성 설정이라 라이트·다크 어느 쪽에서도 유지된다 —
 * 둘을 합치면 "어둡게 보고 싶지만 대비도 높이고 싶다"를 고를 수 없다.
 *
 * [SYSTEM]이 기본인 이유: 기기를 어둡게 쓰는 사람은 앱도 어둡기를 기대한다. 앱마다
 * 따로 켜게 하면 설정을 찾아 들어가야 한다.
 */
enum class AppTheme(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
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
