package kr.ac.baekseok.readytogo.data.repository.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.AuthState
import kr.ac.baekseok.readytogo.data.model.FavoriteKind
import kr.ac.baekseok.readytogo.data.model.FavoritePlace
import kr.ac.baekseok.readytogo.data.model.LatLng
import kr.ac.baekseok.readytogo.data.repository.FavoriteRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 즐겨찾기한 장소를 Firestore에 둔다. `users/{uid}/favorites/{placeId}`
 *
 * **문서 id를 Places의 placeId로 쓰는 이유**: 같은 장소를 두 번 담는 것을 Firestore가
 * 막아 준다. 자동 생성 id를 쓰면 중복을 코드로 검사해야 하고, ♥를 연타하면 둘이 생긴다.
 */
class FirestoreFavoriteRepository(
    private val authState: StateFlow<AuthState>,
) : FavoriteRepository {

    private val db = FirebaseFirestore.getInstance()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val favorites: Flow<List<FavoritePlace>> =
        authState.flatMapLatest { state ->
            val uid = (state as? AuthState.SignedIn)?.user?.id
            if (uid == null) flowOf(emptyList()) else favoritesOf(uid)
        }

    override fun observeAll(): Flow<List<FavoritePlace>> = favorites

    override fun observeIds(): Flow<Set<String>> =
        favorites.map { list -> list.map { it.placeId }.toSet() }

    private fun favoritesOf(uid: String): Flow<List<FavoritePlace>> = callbackFlow {
        val registration = db.favoritesCollection(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "즐겨찾기 구독 실패", error)
                    return@addSnapshotListener
                }
                val places = snapshot?.documents
                    ?.mapNotNull { document -> document.data?.toFavoritePlace(document.id) }
                    .orEmpty()
                trySend(places)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun add(place: FavoritePlace): Result<Unit> = runCatching {
        val uid = requireUid()
        db.favoritesCollection(uid).document(place.placeId).set(place.toMap()).await()
        Unit
    }

    override suspend fun remove(placeId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        db.favoritesCollection(uid).document(placeId).delete().await()
        Unit
    }

    /** 이 계정의 즐겨찾기를 전부 지운다. 탈퇴 전에 부른다. */
    override suspend fun deleteAll(): Result<Unit> = runCatching {
        val uid = requireUid()
        db.favoritesCollection(uid).get().await().documents
            .forEach { it.reference.delete().await() }
        Unit
    }

    private fun requireUid(): String =
        (authState.value as? AuthState.SignedIn)?.user?.id
            ?: error(AppContainer.string(R.string.error_sign_in_to_save_place))

    private fun FirebaseFirestore.favoritesCollection(uid: String) =
        collection(USERS).document(uid).collection(FAVORITES)

    private companion object {
        const val USERS = "users"
        const val FAVORITES = "favorites"
        const val TAG = "FirestoreFavorites"
    }
}

private fun FavoritePlace.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "category" to category,
    "kind" to kind.name,
    "lat" to location.latitude,
    "lon" to location.longitude,
    "cityName" to cityName,
    "mapUrl" to mapUrl,
)

private fun Map<String, Any?>.toFavoritePlace(placeId: String): FavoritePlace? {
    val name = this["name"] as? String ?: return null
    val lat = (this["lat"] as? Number)?.toDouble() ?: return null
    val lon = (this["lon"] as? Number)?.toDouble() ?: return null

    return FavoritePlace(
        placeId = placeId,
        name = name,
        category = this["category"] as? String ?: "",
        // 저장된 뒤 종류 이름이 바뀌었으면 관광지로 본다. 목록 필터에만 쓰여서 피해가 작다.
        kind = runCatching { FavoriteKind.valueOf(this["kind"] as String) }
            .getOrDefault(FavoriteKind.ATTRACTION),
        location = LatLng(lat, lon),
        cityName = this["cityName"] as? String ?: "",
        mapUrl = this["mapUrl"] as? String ?: "",
    )
}
