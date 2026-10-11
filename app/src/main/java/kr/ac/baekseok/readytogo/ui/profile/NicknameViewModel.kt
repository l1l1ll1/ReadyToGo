package kr.ac.baekseok.readytogo.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 닉네임 입력 화면의 상태. */
data class NicknameUiState(
    val input: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    /** 공백만 남으면 빈 이름이다. 길이는 다듬은 뒤에 센다. */
    private val trimmed: String get() = input.trim()

    val tooLong: Boolean get() = trimmed.length > NicknameViewModel.MAX_LENGTH

    val canSave: Boolean get() = trimmed.isNotEmpty() && !tooLong && !saving
}

/**
 * 닉네임 입력 상태 관리.
 *
 * 처음 열릴 때 **이미 정한 닉네임이 있으면 그것을, 없으면 Google 계정 이름을** 채운다.
 * 빈 칸으로 두면 첫 로그인에서 아무거나 지어내야 해서 거기서 멈추는 사람이 생긴다.
 */
class NicknameViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NicknameUiState())
    val uiState: StateFlow<NicknameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // first()로 한 번만 읽는다. 계속 구독하면 사용자가 고치는 중에 덮어쓴다.
            val current = AppContainer.userProfile.observeNickname().first()
            val fallback = (AppContainer.auth.authState.value as? AuthState.SignedIn)
                ?.user
                ?.displayName
                .orEmpty()

            _uiState.update { it.copy(input = current ?: fallback) }
        }
    }

    fun onInputChange(value: String) {
        // 줄바꿈은 붙여넣기로 들어올 수 있다. 한 줄짜리 이름이라 그 자리에서 지운다.
        _uiState.update { it.copy(input = value.replace("\n", ""), error = null) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        _uiState.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            AppContainer.userProfile.setNickname(state.input)
                .onSuccess { _uiState.update { s -> s.copy(saving = false, saved = true) } }
                .onFailure { error ->
                    _uiState.update { s ->
                        s.copy(
                            saving = false,
                            error = error.message ?: string(R.string.common_save_failed),
                        )
                    }
                }
        }
    }

    companion object {
        /** 홈 인사말 "(닉네임)님, 어디로 갈까요?"가 한 줄에 들어가는 길이. */
        const val MAX_LENGTH = 12
    }

    /** 화면에 띄울 문구. ViewModel에는 stringResource가 없어 Application Context로 읽는다. */
    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
