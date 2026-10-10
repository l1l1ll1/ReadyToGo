package kr.ac.baekseok.readytogo.data.locale

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import kr.ac.baekseok.readytogo.data.model.AppLanguage

/**
 * 앱 언어를 실제로 바꾼다.
 *
 * **안드로이드 13의 앱별 언어(LocaleManager)를 쓴다.** minSdk가 33이라 프레임워크에 들어 있어서
 * 라이브러리를 더하지 않아도 되고, 시스템 설정의 "앱 언어" 목록에도 우리 앱이 올라간다.
 * (AppCompatDelegate 쪽을 쓰면 appcompat 의존성이 생기는데, 이 앱은 Compose만 쓴다.)
 *
 * 값을 바꾸면 **시스템이 화면을 다시 만든다.** 그래서 설정에서 언어를 고르면 그 자리에서
 * 화면 글자가 바뀐다 — 앱을 껐다 켤 필요가 없다. 대신 같은 값을 다시 넣으면 화면이
 * 괜히 한 번 더 그려지므로 [apply]가 먼저 비교한다.
 */
object AppLocale {

    /** 지금 적용된 언어. 고른 적이 없으면 null(기기 언어를 따르는 중). */
    fun current(context: Context): AppLanguage? {
        val tags = context.localeManager.applicationLocales
        if (tags.isEmpty) return null
        val tag = tags[0].language
        return AppLanguage.entries.firstOrNull { it.tag == tag }
    }

    /**
     * 언어를 적용한다. 이미 그 언어면 아무것도 하지 않는다.
     *
     * 앱이 뜬 뒤에 부르면 화면이 다시 만들어지므로, 시작할 때 맞추는 호출은
     * Activity가 생기기 전(Application.onCreate)에 해야 깜빡이지 않는다.
     */
    fun apply(context: Context, language: AppLanguage) {
        if (current(context) == language) return
        context.localeManager.applicationLocales = LocaleList.forLanguageTags(language.tag)
    }

    private val Context.localeManager: LocaleManager
        get() = getSystemService(LocaleManager::class.java)
}
