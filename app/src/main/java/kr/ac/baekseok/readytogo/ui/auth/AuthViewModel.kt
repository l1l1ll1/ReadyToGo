package kr.ac.baekseok.readytogo.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 로그인 상태 관리.
 *
 * Repository를 [AppContainer]에서 가져오는 이유: 로그인은 앱 전체가 공유하는 상태다.
 * ViewModel이 Repository를 새로 만들면 화면을 옮길 때마다 로그아웃된 것으로 보인다.
 *
 * 생성자에 인자를 받지 않는 이유: 인자가 있으면 기본 ViewModel 팩토리가 만들지 못해
 * "Cannot create an instance" 예외가 난다. 이 프로젝트에서 이미 한 번 겪은 문제다.
 */
class AuthViewModel : ViewModel() {

    private val repository = AppContainer.auth

    val authState: StateFlow<AuthState> = repository.authState

    /** 로그인 버튼을 누른 직후. 버튼에 진행 표시를 띄우는 데 쓴다. */
    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 스플래시가 부른다. 저장된 로그인 세션이 있는지 확인한다. */
    fun restoreSession() {
        viewModelScope.launch { repository.restoreSession() }
    }

    /**
     * Google 계정으로 로그인한다.
     *
     * @param context 계정 선택창을 띄울 **화면의** Context.
     *        Credential Manager는 이 Context로 창을 올리기 때문에 Application Context로는
     *        아무것도 보이지 않는다. 그래서 ViewModel이 들고 있지 않고 화면에서 받는다.
     */
    fun signInWithGoogle(context: Context) {
        if (_signingIn.value) return
        _signingIn.value = true
        _error.value = null

        viewModelScope.launch {
            repository.signInWithGoogle(context)
                .onFailure { _error.value = it.message ?: "로그인에 실패했습니다." }
            _signingIn.value = false
        }
    }

    fun continueAsGuest() = repository.continueAsGuest()

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    /**
     * 탈퇴.
     *
     * **저장한 데이터를 먼저 지우고 계정을 지운다.** 순서를 바꾸면 안 된다 — 계정이 사라지는
     * 순간 보안 규칙이 보는 uid가 없어져서, 그 계정 경로의 일정·즐겨찾기를 아무도 지울 수 없게 된다.
     *
     * 데이터 삭제가 실패하면 계정을 지우지 않는다. 지워버리면 영원히 남는 데이터가 되므로,
     * 탈퇴를 실패로 끝내고 다시 시도하게 하는 쪽이 낫다.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            val dataDeleted = AppContainer.savedTrips.deleteAll()
                .mapCatching { AppContainer.favorites.deleteAll().getOrThrow() }

            dataDeleted
                .onFailure {
                    _error.value = "저장된 데이터를 지우지 못해 탈퇴를 멈췄습니다. 다시 시도해 주세요."
                    return@launch
                }

            repository.deleteAccount()
                .onFailure { _error.value = it.message ?: "탈퇴에 실패했습니다." }
        }
    }

    fun clearError() {
        _error.value = null
    }
}