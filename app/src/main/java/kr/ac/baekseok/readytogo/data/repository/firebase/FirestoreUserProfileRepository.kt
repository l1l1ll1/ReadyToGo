package kr.ac.baekseok.readytogo.data.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.repository.UserProfileRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 닉네임을 `users/{uid}` 문서에 둔다.
 *
 * **하위 컬렉션이 아니라 그 문서 자체에 넣는 이유**: 값 하나를 위해 컬렉션을 만들 이유가 없고,
 * 지금까지 `users/{uid}`는 `trips`·`favorites`를 담기만 하는 빈 자리였다.
 *
 * 보안 규칙은 그대로 둬도 된다 — `match /users/{uid}/{document=**}`의 `=**`가
 * 0개 세그먼트도 매치해서 이 문서를 이미 덮는다.
 */
class FirestoreUserProfileRepository(
    private val authState: StateFlow<AuthState>,
) : UserProfileRepository {

    private val db = FirebaseFirestore.getInstance()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNickname(): Flow<String?> =
        authState.flatMapLatest { state ->
            val uid = (state as? AuthState.SignedIn)?.user?.id
            if (uid == null) flowOf(null) else nicknameOf(uid)
        }

    private fun nicknameOf(uid: String): Flow<String?> = callbackFlow {
        val registration = db.userDocument(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 예외를 흘리면 홈 화면이 통째로 비어버린다. 닉네임만 포기한다.
                Log.w(TAG, "닉네임 구독 실패", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.getString(FIELD_NICKNAME)?.takeIf { it.isNotBlank() })
        }

        awaitClose { registration.remove() }
    }

    override suspend fun setNickname(nickname: String): Result<Unit> = runCatching {
        val uid = requireUid()
        // merge를 쓰는 이유: 나중에 이 문서에 다른 항목이 생겨도 닉네임만 바꿀 때
        // 나머지가 지워지지 않는다. set(덮어쓰기)이면 같이 날아간다.
        db.userDocument(uid)
            .set(mapOf(FIELD_NICKNAME to nickname.trim()), SetOptions.merge())
            .await()
        Unit
    }

    override suspend fun delete(): Result<Unit> = runCatching {
        val uid = requireUid()
        db.userDocument(uid).delete().await()
        Unit
    }

    private fun requireUid(): String =
        (authState.value as? AuthState.SignedIn)?.user?.id
            ?: error("로그인해야 닉네임을 정할 수 있습니다.")

    private fun FirebaseFirestore.userDocument(uid: String) =
        collection(USERS).document(uid)

    private companion object {
        const val USERS = "users"
        const val FIELD_NICKNAME = "nickname"
        const val TAG = "FirestoreUserProfile"
    }
}
