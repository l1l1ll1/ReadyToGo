package kr.ac.baekseok.readytogo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AppLanguage
import kr.ac.baekseok.readytogo.data.model.AppSettings
import kr.ac.baekseok.readytogo.data.model.TextScale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 설정 상태 관리.
 *
 * 항목마다 함수를 따로 둔 이유: 화면이 `AppSettings`를 통째로 만들어 넘기게 하면
 * 토글 하나를 바꿀 때 나머지 값을 복사해 오는 코드가 화면마다 생기고, 한 군데서 빠뜨린다.
 */
class SettingsViewModel : ViewModel() {

    private val repository = AppContainer.settings

    val settings: StateFlow<AppSettings> = repository.settings

    fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }

    fun setNotifyBeforeWeek(enabled: Boolean) = update { it.copy(notifyBeforeWeek = enabled) }

    fun setNotifyBeforeDay(enabled: Boolean) = update { it.copy(notifyBeforeDay = enabled) }

    fun setTextScale(scale: TextScale) = update { it.copy(textScale = scale) }

    fun setHighContrast(enabled: Boolean) = update { it.copy(highContrast = enabled) }

    fun setShowPlacePhotos(enabled: Boolean) = update { it.copy(showPlacePhotos = enabled) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform(repository.settings.value)) }
    }
}