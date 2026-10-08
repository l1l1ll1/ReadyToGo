package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 설정.
 *
 * 설정은 기기에 남아야 한다(앱을 껐다 켜도 글자 크기가 유지돼야 한다). 서버가 아니라
 * 로컬 저장이므로 2단계에서 Firestore가 아니라 SharedPreferences 구현체를 붙인다.
 *
 * 또한 설정은 **로그인과 무관**하다 — 둘러보기 중에도 글자 크기를 키울 수 있어야 한다.
 */
interface SettingsRepository {

    val settings: StateFlow<AppSettings>

    suspend fun update(settings: AppSettings)
}
