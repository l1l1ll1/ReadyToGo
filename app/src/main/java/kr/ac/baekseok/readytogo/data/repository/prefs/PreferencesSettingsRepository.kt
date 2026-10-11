package kr.ac.baekseok.readytogo.data.repository.prefs

import android.content.Context
import kr.ac.baekseok.readytogo.data.model.AppLanguage
import kr.ac.baekseok.readytogo.data.model.AppSettings
import kr.ac.baekseok.readytogo.data.model.TextScale
import kr.ac.baekseok.readytogo.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 설정을 기기에 저장한다.
 *
 * **Firestore를 쓰지 않는 이유**: 설정은 계정이 아니라 기기에 딸린 것이다. 둘러보기 중에도
 * 글자 크기를 키울 수 있어야 하는데(SettingsRepository 주석), Firestore는 uid가 없으면 쓸 수 없다.
 *
 * **DataStore가 아니라 SharedPreferences인 이유**: 값이 다섯 개뿐이고, 앱이 뜨기 전에
 * **동기로** 한 번 읽어야 한다 — 언어는 첫 화면이 그려지기 전에 정해져야 깜빡임이 없다.
 * DataStore는 suspend라 그 자리에서 읽을 수 없고, 라이브러리도 하나 늘어난다.
 */
class PreferencesSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun update(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.language.name)
            .putBoolean(KEY_NOTIFY_WEEK, settings.notifyBeforeWeek)
            .putBoolean(KEY_NOTIFY_DAY, settings.notifyBeforeDay)
            .putString(KEY_TEXT_SCALE, settings.textScale.name)
            .putBoolean(KEY_HIGH_CONTRAST, settings.highContrast)
            .apply()

        _settings.value = settings
    }

    /**
     * 온보딩 3장을 본 적 있는지.
     *
     * **[AppSettings]에 넣지 않는 이유**: 설정 화면에서 사용자가 바꾸는 값이 아니다.
     * 거기 섞으면 설정을 한 번 저장할 때마다 이 값도 같이 쓰여서, 나중에 "설정 초기화"를
     * 만들면 온보딩이 딸려 나온다.
     *
     * 같은 파일에 두는 이유는 성격이 같아서다 — 계정이 아니라 기기에 딸린 값이다.
     */
    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, value).apply()

    /**
     * 저장된 값을 읽는다. 없거나 알아볼 수 없으면 기본값을 쓴다.
     *
     * enum 이름을 그대로 저장하므로, 나중에 enum에서 항목을 빼면 예전 값이 읽히지 않는다.
     * 그때 예외를 던지면 설정 화면이 열리지 않으므로 조용히 기본값으로 돌린다.
     */
    private fun load(): AppSettings {
        val default = AppSettings()
        return AppSettings(
            language = prefs.enumOrNull(KEY_LANGUAGE, AppLanguage.entries) ?: deviceLanguage(),
            notifyBeforeWeek = prefs.getBoolean(KEY_NOTIFY_WEEK, default.notifyBeforeWeek),
            notifyBeforeDay = prefs.getBoolean(KEY_NOTIFY_DAY, default.notifyBeforeDay),
            textScale = prefs.enumOrNull(KEY_TEXT_SCALE, TextScale.entries) ?: default.textScale,
            highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, default.highContrast),
        )
    }

    /**
     * 처음 실행했을 때 쓸 언어. 기기 언어를 따른다.
     *
     * **고정값(한국어)을 쓰면 첫 화면이 깜빡인다.** 앱 언어를 적용하는 시스템 호출은
     * 다음 실행부터 반영되는데, 영어 기기에서 첫 화면은 영어로 그려지고 그다음부터
     * 한국어가 된다. 기기 언어에서 시작하면 그 어긋남이 생기지 않는다.
     *
     * 목록에 없는 언어(예: 프랑스어)는 한국어로 본다 — 기본 리소스가 한국어라
     * 어차피 그 글자가 나온다.
     */
    private fun deviceLanguage(): AppLanguage {
        val tag = Locale.getDefault().language
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.KOREAN
    }

    private fun <T : Enum<T>> android.content.SharedPreferences.enumOrNull(
        key: String,
        values: List<T>,
    ): T? {
        val name = getString(key, null) ?: return null
        return values.firstOrNull { it.name == name }
    }

    private companion object {
        const val FILE = "settings"
        const val KEY_LANGUAGE = "language"
        const val KEY_NOTIFY_WEEK = "notify_before_week"
        const val KEY_NOTIFY_DAY = "notify_before_day"
        const val KEY_TEXT_SCALE = "text_scale"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    }
}
