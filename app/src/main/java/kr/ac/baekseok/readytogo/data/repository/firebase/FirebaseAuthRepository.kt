package kr.ac.baekseok.readytogo.data.repository.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.UserProfile
import kr.ac.baekseok.readytogo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.ac.baekseok.readytogo.data.repository.await

/**
 * Google 계정 로그인(Firebase Auth + Credential Manager).
 *
 * 흐름이 두 단계다 — 헷갈리기 쉬워 적어 둔다.
 * 1. **Credential Manager**가 계정 선택창을 띄우고 Google ID 토큰을 준다.
 * 2. 그 토큰을 **Firebase Auth**에 넘겨 Firebase 계정으로 로그인한다.
 *
 * 1번만으로는 Firestore를 쓸 수 없다. Firestore 보안 규칙이 보는 `request.auth.uid`는
 * Firebase 계정의 uid이고, Google 토큰만으로는 그게 생기지 않는다.
 *
 * 예전 `GoogleSignInClient`(play-services-auth)를 쓰지 않는 이유: deprecated다.
 *
 * @param context 계정 선택창을 띄울 Activity의 Context가 필요하다.
 *        Application Context로는 창을 띄울 수 없어 [signInWithGoogle]에서 받는다.
 */
class FirebaseAuthRepository(
    private val appContext: Context,
) : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Firebase가 로그인 상태를 바꿀 때마다 따라간다.
     *
     * 리스너로 받는 이유: 토큰 만료나 다른 기기에서의 탈퇴로 로그인이 **스스로 풀릴 수 있다.**
     * 로그인·로그아웃 함수에서만 상태를 바꾸면 그런 경우를 놓친다.
     */
    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _authState.value = when {
            user != null -> AuthState.SignedIn(user.toProfile())
            // 둘러보기 중이었다면 그 상태를 유지한다. Firebase는 "둘러보기"를 모르므로
            // 로그인 안 된 것으로 보고하는데, 그걸 그대로 쓰면 둘러보기가 풀려 로그인 화면으로 튄다.
            _authState.value is AuthState.Guest -> AuthState.Guest
            else -> AuthState.SignedOut
        }
    }

    init {
        auth.addAuthStateListener(listener)
    }

    /**
     * 저장된 로그인 세션을 확인한다. 스플래시가 부른다.
     *
     * Firebase는 토큰을 기기에 저장하므로 앱을 껐다 켜도 로그인이 유지된다.
     * 리스너가 즉시 현재 상태를 알려주므로 여기서 따로 할 일은 없지만,
     * 상태가 아직 Unknown이면 로그인 안 된 것으로 확정한다 —
     * 그러지 않으면 스플래시가 영원히 기다린다.
     */
    suspend fun restoreSession() {
        if (_authState.value is AuthState.Unknown) {
            val user = auth.currentUser
            _authState.value = if (user != null) {
                AuthState.SignedIn(user.toProfile())
            } else {
                AuthState.SignedOut
            }
        }
    }

    /**
     * 인터페이스를 지키기 위한 구현. 계정 선택창을 띄울 수 없어 실제로는 실패한다.
     *
     * 창을 띄우려면 Activity Context가 필요한데 이 Repository는 Application Context만 들고 있다.
     * 화면은 [signInWithGoogle]에 Context를 넘기는 쪽을 쓴다.
     */
    override suspend fun signInWithGoogle(): Result<Unit> =
        signInWithGoogle(appContext)

    /**
     * Google 계정으로 로그인한다.
     *
     * @param activityContext 계정 선택창을 띄울 Activity의 Context.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = runCatching {
        // google-services.json의 client_type 3(웹 클라이언트) ID.
        // 플러그인이 이 문자열 리소스를 자동으로 만들어 준다 — 소스에 적지 않는다.
        val serverClientId = appContext.getString(R.string.default_web_client_id)

        // **GetGoogleIdOption이 아니라 GetSignInWithGoogleOption을 쓴다.**
        //
        // GetGoogleIdOption은 기기에 이미 있는 계정만 목록에 올린다. 계정이 하나뿐이면
        // 그 하나만 뜨고 "다른 계정 사용"이 없어서, 계정을 바꾸려는 사용자가 앱 안에서
        // 갈 길이 막힌다(설정에서 계정을 추가하고 돌아와야 한다).
        //
        // 이쪽은 Google 로그인 창이라 기존 계정 목록과 계정 추가가 같이 나온다.
        // 계정이 하나뿐인 사용자도 창을 한 번 거치지만, 막히는 것보다는 낫다.
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)

        val googleIdToken = GoogleIdTokenCredential
            .createFrom(response.credential.data)
            .idToken

        // 2단계 — Google 토큰을 Firebase 계정으로 바꾼다.
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(credential).await()
        Unit
    }.recoverCatching { error ->
        // 사용자가 창을 닫은 것은 실패가 아니다. 화면에 오류를 띄우면 안 된다.
        if (error is GetCredentialCancellationException) {
            Log.d(TAG, "사용자가 로그인 창을 닫았다")
            return@recoverCatching Unit
        }
        throw IllegalStateException(error.userMessage(), error)
    }

    override fun continueAsGuest() {
        _authState.value = AuthState.Guest
    }

    override suspend fun signOut() {
        auth.signOut()
        _authState.value = AuthState.SignedOut
    }

    /**
     * 계정 탈퇴.
     *
     * **Firestore에 저장한 데이터는 여기서 지우지 않는다.** 계정을 지우는 순간 uid로
     * 문서에 접근할 권한이 사라져서, 삭제를 먼저 하지 않으면 영원히 남는 데이터가 된다.
     * 그래서 호출부가 데이터를 먼저 지우고 이 함수를 부른다(AuthViewModel 참고).
     */
    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("로그인 상태가 아닙니다.")
        user.delete().await()
        _authState.value = AuthState.SignedOut
        Unit
    }

    private fun FirebaseUser.toProfile() = UserProfile(
        id = uid,
        email = email.orEmpty(),
        // Google 계정에 표시 이름이 없을 수 있다. 그때는 이메일 앞부분을 쓴다 —
        // 화면이 "님"을 붙이는데 빈 문자열이면 " 님"이 된다.
        displayName = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: "여행자",
        photoUrl = photoUrl?.toString(),
    )

    /**
     * 사용자에게 보여줄 문구.
     *
     * 계정이 없는 경우를 따로 안내하지 않는 이유: Google 로그인 창이 계정 추가까지 품고 있어서
     * "계정이 없다"로 끝나는 길이 없다. 여기까지 왔다면 창 안에서 실패한 것이다.
     */
    private fun Throwable.userMessage(): String = when (this) {
        is GetCredentialException ->
            "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요."
        else -> message ?: "로그인에 실패했습니다."
    }

    private companion object {
        const val TAG = "FirebaseAuth"
    }
}
