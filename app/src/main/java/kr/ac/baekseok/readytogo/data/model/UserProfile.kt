package kr.ac.baekseok.readytogo.data.model

/**
 * 로그인한 사용자.
 *
 * 2단계에서 Firebase Auth의 `FirebaseUser`를 이 모델로 옮겨 담는다. 화면이 Firebase 타입을
 * 직접 알게 하면 로그인 수단을 바꿀 때 화면까지 고쳐야 한다.
 *
 * @param photoUrl Google 계정 프로필 사진. 1단계에서는 비어 있고 이니셜을 대신 그린다.
 */
data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
)

/**
 * 로그인 상태.
 *
 * **[Guest]가 따로 있는 이유**: 비로그인 사용자도 "둘러보기"로 일정을 만들 수 있어야 한다
 * (CLAUDE.md 앱 구조 → 로그인). 저장만 막으면 되므로 "로그인 안 됨"과 "둘러보기 중"은 다른 상태다.
 */
sealed interface AuthState {
    /** 아직 확인 중. 스플래시가 이 상태를 기다린다. */
    data object Unknown : AuthState

    /** 로그인하지 않았고 둘러보기도 선택하지 않음 → 로그인 화면을 보여준다. */
    data object SignedOut : AuthState

    /** 둘러보기. 일정 생성은 되고 저장은 막힌다. */
    data object Guest : AuthState

    data class SignedIn(val user: UserProfile) : AuthState
}

/** 저장 기능을 쓸 수 있는 상태인지. 화면마다 `is SignedIn`을 적지 않도록 여기 둔다. */
val AuthState.canSave: Boolean
    get() = this is AuthState.SignedIn
