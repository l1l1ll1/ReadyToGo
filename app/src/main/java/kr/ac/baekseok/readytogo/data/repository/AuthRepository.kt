package kr.ac.baekseok.readytogo.data.repository

import kr.ac.baekseok.readytogo.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * 로그인.
 *
 * 상태를 [StateFlow]로 내보내는 이유: 로그인은 어느 화면에서든 풀릴 수 있다(토큰 만료, 탈퇴).
 * 함수로 "지금 로그인됐나?"를 묻는 구조면 화면마다 다시 물어봐야 하고, 한 화면은 묻는 걸 잊는다.
 *
 * 2단계에서 `FirebaseAuthRepository`가 이 인터페이스를 구현한다. 화면 코드는 바뀌지 않는다.
 */
interface AuthRepository {

    val authState: StateFlow<AuthState>

    /** Google 계정으로 로그인한다. */
    suspend fun signInWithGoogle(): Result<Unit>

    /** 로그인하지 않고 둘러본다. 일정 생성은 되고 저장은 막힌다. */
    fun continueAsGuest()

    suspend fun signOut()

    /** 계정 탈퇴. 저장된 일정과 즐겨찾기까지 지운다. */
    suspend fun deleteAccount(): Result<Unit>
}
