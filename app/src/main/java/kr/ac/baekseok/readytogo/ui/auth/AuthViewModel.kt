package kr.ac.baekseok.readytogo.ui.auth

import android.content.Context
import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kr.ac.baekseok.readytogo.R
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
class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {

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
                .onFailure { _error.value = it.message ?: string(R.string.auth_sign_in_failed) }
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
            // 지울 uid를 남긴다. 계정이 사라진 뒤에는 어느 경로의 데이터였는지 알 방법이 없어서,
            // 데이터가 남았다는 신고가 들어오면 이 줄이 유일한 단서다.
            val uid = (authState.value as? AuthState.SignedIn)?.user?.id
            Log.d(TAG, "탈퇴 시작: users/$uid")

            val dataDeleted = AppContainer.savedTrips.deleteAll()
                .mapCatching { AppContainer.favorites.deleteAll().getOrThrow() }
                .mapCatching { AppContainer.userProfile.delete().getOrThrow() }

            dataDeleted
                .onFailure {
                    Log.w(TAG, "데이터 삭제 실패 — 계정은 그대로 둔다", it)
                    _error.value = string(R.string.auth_delete_data_failed)
                    return@launch
                }
            Log.d(TAG, "데이터 삭제 완료: users/$uid")

            repository.deleteAccount()
                .onSuccess { Log.d(TAG, "계정 삭제 완료") }
                .onFailure { _error.value = it.message ?: string(R.string.auth_delete_failed) }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private companion object {
        const val TAG = "Auth"
    }

    /** 화면에 띄울 문구. ViewModel에는 stringResource가 없어 Application Context로 읽는다. */
    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
