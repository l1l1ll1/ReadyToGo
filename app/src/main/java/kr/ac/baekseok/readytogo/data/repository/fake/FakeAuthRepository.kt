package kr.ac.baekseok.readytogo.data.repository.fake

import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.UserProfile
import kr.ac.baekseok.readytogo.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그인 Mock — 버튼을 누르면 성공한 것처럼 상태만 바꾼다.
 *
 * 8단계 2번에서 `FirebaseAuthRepository`로 교체한다. 지금 가짜로 두는 이유는
 * Firebase 프로젝트를 만들고 SHA-1을 등록하지 않아도 화면 흐름 전체를 눌러볼 수 있어야 하기 때문이다.
 */
class FakeAuthRepository : AuthRepository {

    // 처음에는 Unknown이다. 스플래시가 이 상태를 보고 "확인 중"을 유지한다.
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** 스플래시가 부른다. 실제 Firebase도 저장된 세션을 확인하는 데 시간이 걸린다. */
    suspend fun restoreSession() {
        delay(SESSION_CHECK_DELAY_MS)
        // 가짜 단계에서는 저장된 세션이 없다고 본다. 매번 로그인 화면을 볼 수 있어야 확인이 쉽다.
        if (_authState.value is AuthState.Unknown) {
            _authState.value = AuthState.SignedOut
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> {
        delay(SIGN_IN_DELAY_MS)
        _authState.value = AuthState.SignedIn(SAMPLE_USER)
        return Result.success(Unit)
    }

    override fun continueAsGuest() {
        _authState.value = AuthState.Guest
    }

    override suspend fun signOut() {
        _authState.value = AuthState.SignedOut
    }

    override suspend fun deleteAccount(): Result<Unit> {
        delay(SIGN_IN_DELAY_MS)
        _authState.value = AuthState.SignedOut
        return Result.success(Unit)
    }

    private companion object {
        /** 실제 로그인도 즉시 끝나지 않는다. 로딩 표시가 제대로 뜨는지 보려면 지연이 있어야 한다. */
        const val SIGN_IN_DELAY_MS = 800L
        const val SESSION_CHECK_DELAY_MS = 600L

        val SAMPLE_USER = UserProfile(
            id = "sample-user",
            email = "traveler@example.com",
            displayName = AppContainer.string(R.string.default_display_name),
        )
    }
}
