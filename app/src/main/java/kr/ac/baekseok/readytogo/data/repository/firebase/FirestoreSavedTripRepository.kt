package kr.ac.baekseok.readytogo.data.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.TimelineEdits
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import kr.ac.baekseok.readytogo.data.repository.SavedTripRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate

/**
 * 저장된 일정을 Firestore에 둔다. `users/{uid}/trips/{tripId}`
 *
 * **uid를 경로에 넣는 이유**: 보안 규칙이 문서를 읽지 않고 경로만으로 소유자를 판단할 수 있다.
 * 문서 안에 `ownerId` 필드를 두고 비교하는 방식은 규칙이 문서를 먼저 읽어야 해서
 * 읽기 횟수가 늘고(무료 한도를 더 빨리 쓴다), 규칙도 복잡해진다.
 *
 * 로그인하지 않았으면 빈 목록을 준다 — 경로를 만들 uid가 없다. Fake 구현과 같은 규칙이다.
 */
class FirestoreSavedTripRepository(
    private val authState: StateFlow<AuthState>,
) : SavedTripRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * 로그인 상태가 바뀌면 **구독 대상을 갈아탄다.**
     *
     * flatMapLatest가 핵심이다 — 계정을 바꿀 때 이전 계정의 구독을 끊지 않으면
     * 남의 일정이 화면에 남는다. 실제로 그게 이 기능의 격리 요건이다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<SavedTrip>> =
        authState.flatMapLatest { state ->
            val uid = (state as? AuthState.SignedIn)?.user?.id
            if (uid == null) flowOf(emptyList()) else tripsOf(uid)
        }

    /**
     * Firestore 실시간 구독.
     *
     * 한 번 읽고 끝내지 않는 이유: 결과 화면에서 저장을 누르면 "여행" 탭이 바로 바뀌어야 한다.
     * 다시 읽어오는 구조면 저장했는데 목록에 없는 순간이 생긴다.
     */
    private fun tripsOf(uid: String): Flow<List<SavedTrip>> = callbackFlow {
        val registration = db.tripsCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 예외를 흘리면 화면이 비어버린다. 로그만 남기고 마지막 목록을 유지한다.
                    Log.w(TAG, "일정 구독 실패", error)
                    return@addSnapshotListener
                }
                val trips = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.toSavedTrip(document.id) }
                    .orEmpty()
                trySend(trips)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun save(trip: SavedTrip): Result<Unit> = runCatching {
        val uid = requireUid()
        db.tripsCollection(uid).document(trip.id).set(trip.toMap()).await()
        Unit
    }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        val uid = requireUid()
        db.tripsCollection(uid).document(id).delete().await()
        Unit
    }

    override suspend fun replace(oldId: String, trip: SavedTrip): Result<Unit> = runCatching {
        val uid = requireUid()
        val trips = db.tripsCollection(uid)
        // 일괄 쓰기 — 둘 다 되거나 둘 다 안 된다.
        db.batch()
            .delete(trips.document(oldId))
            .set(trips.document(trip.id), trip.toMap())
            .commit()
            .await()
        Unit
    }

    override suspend fun rename(id: String, title: String): Result<Unit> = runCatching {
        val uid = requireUid()
        // 제목만 바꾼다. set을 쓰면 나머지 필드가 지워진다.
        db.tripsCollection(uid).document(id).update("title", title).await()
        Unit
    }

    /**
     * 이 계정의 일정을 전부 지운다. 탈퇴 전에 부른다.
     *
     * 계정을 먼저 지우면 uid로 문서에 접근할 권한이 사라져 영원히 남는 데이터가 된다.
     * 보안 규칙이 `request.auth.uid`를 보기 때문이다.
     */
    override suspend fun deleteAll(): Result<Unit> = runCatching {
        val uid = requireUid()
        val documents = db.tripsCollection(uid).get().await().documents
        // 문서가 많지 않아(저장된 일정 수십 개) 하나씩 지워도 된다.
        documents.forEach { it.reference.delete().await() }
        Unit
    }

    private fun requireUid(): String =
        (authState.value as? AuthState.SignedIn)?.user?.id
            ?: error(AppContainer.string(R.string.error_sign_in_to_save_trip))

    private fun FirebaseFirestore.tripsCollection(uid: String) =
        collection(USERS).document(uid).collection(TRIPS)

    private companion object {
        const val USERS = "users"
        const val TRIPS = "trips"
        const val TAG = "FirestoreTrips"
    }
}

private fun SavedTrip.toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "destinationName" to destinationName,
    "departureDate" to departureDate.toString(),
    "returnDate" to returnDate.toString(),
    "savedAt" to savedAt.toString(),
    "condition" to condition.toMap(),
    // 편집이 없으면 필드를 만들지 않는다. 빈 맵 셋이 모든 문서에 붙을 이유가 없다.
    "edits" to edits.takeUnless { it.isEmpty }?.toMap(),
    // 카드 사진에 쓸 장소 id 하나. 없는 일정도 있어 null이 그대로 들어간다.
    "coverPlaceId" to coverPlaceId,
)

/**
 * 문서를 모델로. 필수 값이 없으면 null을 돌려 그 문서만 건너뛴다.
 *
 * @param id 문서 id가 곧 일정 id다. 본문에 중복으로 넣지 않는다 — 두 값이 어긋날 수 있다.
 */
private fun Map<String, Any?>.toSavedTrip(id: String): SavedTrip? {
    val condition = childMap("condition")?.toTripCondition() ?: return null
    val departure = localDate("departureDate") ?: return null
    val returnDate = localDate("returnDate") ?: return null

    return SavedTrip(
        id = id,
        title = this["title"] as? String ?: destinationFallbackTitle(condition.destination?.city),
        destinationName = this["destinationName"] as? String
            ?: condition.destination?.shortName.orEmpty(),
        departureDate = departure,
        returnDate = returnDate,
        condition = condition,
        // 저장 시각이 없으면 오늘로 본다. 목록 정렬에만 쓰여서 틀려도 피해가 작다.
        savedAt = localDate("savedAt") ?: LocalDate.now(),
        edits = childMap("edits")?.toTimelineEdits() ?: TimelineEdits(),
        // 이 필드가 생기기 전에 저장된 일정에는 없다. 그때는 카드가 그라데이션으로 남는다.
        coverPlaceId = this["coverPlaceId"] as? String,
    )
}

private fun destinationFallbackTitle(city: String?) = city
    ?.let { AppContainer.string(R.string.saved_trip_fallback_title, it) }
    ?: AppContainer.string(R.string.saved_trip_fallback_title_unknown)
