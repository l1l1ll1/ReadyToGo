package kr.ac.baekseok.readytogo.data.repository.fake

import kr.ac.baekseok.readytogo.data.model.AppSettings
import kr.ac.baekseok.readytogo.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 설정 Mock — 메모리에만 둔다. 앱을 껐다 켜면 기본값으로 돌아간다.
 *
 * 8단계 3번에서 SharedPreferences 구현체로 교체하고, 그때 각 값을 실제 동작에 연결한다.
 * 지금은 토글이 눌리고 값이 바뀌는 것까지만 확인할 수 있다.
 */
class FakeSettingsRepository : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun update(settings: AppSettings) {
        _settings.value = settings
    }
}
