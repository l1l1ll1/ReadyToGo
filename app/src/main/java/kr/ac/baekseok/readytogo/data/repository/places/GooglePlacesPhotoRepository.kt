package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kr.ac.baekseok.readytogo.data.model.PlacePhoto
import kr.ac.baekseok.readytogo.data.photo.PhotoBudget
import kr.ac.baekseok.readytogo.data.photo.PlacePhotoCache
import kr.ac.baekseok.readytogo.data.repository.PlacePhotoRepository
import kr.ac.baekseok.readytogo.data.repository.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Places에서 장소 사진을 한 장 받아 온다.
 *
 * **검색 필드 마스크를 건드리지 않는다.** `PHOTO_METADATAS`를 Nearby Search에 넣으면
 * 일정 하나당 열 번 넘게 나가는 검색이 전부 상위 SKU로 올라간다. 대신 사진을 실제로
 * 보여줄 장소만 id로 따로 묻는다 — 비싸지는 범위가 "검색 전부"에서 "화면에 뜨는 몇 곳"으로
 * 고정된다.
 *
 * 사진 한 장에 요청이 두 번 나간다. 사진 정보를 받고([FetchPlaceRequest]),
 * 그걸로 실제 주소를 받는다([FetchResolvedPhotoUriRequest]). 둘 다 과금된다.
 *
 * 과금을 막는 장치가 셋이다.
 * - **캐시**: 한 번 받은 장소는 다시 묻지 않는다. 사진이 없던 장소도 기록한다.
 * - **월 상한**: [PhotoBudget]이 막는다. 넘으면 조용히 사진 없음으로 답한다.
 * - **중복 요청 차단**: 같은 장소를 동시에 여러 카드가 물어도 한 번만 나간다.
 */
class GooglePlacesPhotoRepository(
    context: Context,
    private val client: PlacesClient = PlacesClientFactory.create(context),
    private val cache: PlacePhotoCache = PlacePhotoCache(context),
    private val budget: PhotoBudget = PhotoBudget(context),
) : PlacePhotoRepository {

    /**
     * 같은 장소를 동시에 묻는 것을 막는다.
     *
     * 요약 탭이 열리면 카드 다섯 장이 한꺼번에 사진을 찾는다. 자물쇠가 없으면 같은 장소에
     * 요청이 두 번 나가 그대로 두 배로 과금된다.
     */
    private val lock = Mutex()

    override suspend fun photoFor(placeId: String): PlacePhoto {
        if (placeId.isBlank()) return PlacePhoto.NONE

        cache.read(placeId)?.let { return it }

        return lock.withLock {
            // 기다리는 동안 다른 쪽이 받아 뒀을 수 있다. 자물쇠를 잡고 한 번 더 본다.
            cache.read(placeId) ?: fetchAndCache(placeId)
        }
    }

    private suspend fun fetchAndCache(placeId: String): PlacePhoto {
        if (!budget.canFetch()) {
            // 상한에 닿았다. 화면은 그라데이션으로 떨어진다. **캐시에 적지 않는다** —
            // 적어 두면 다음 달에도 "사진 없음"으로 굳는다.
            Log.w(TAG, "이번 달 사진 상한(${PhotoBudget.MONTHLY_PHOTO_LIMIT}장)에 닿아 건너뜀")
            return PlacePhoto.NONE
        }

        // **실패와 "사진 없음"을 가른다.** 둘을 같이 캐시했더니, 목록을 빠르게 스크롤해서
        // 요청이 취소된 장소가 "사진 없음"으로 영구히 굳었다(캐시에 유효기간이 없다).
        // 실제로 저장 탭에서 6곳이 그렇게 굳어 있었다.
        val photo = try {
            fetch(placeId)
        } catch (e: CancellationException) {
            // 화면에서 사라져 취소된 것이지 사진이 없는 것이 아니다. 다시 그릴 때 또 묻는다.
            // 요청이 실제로 나갔는지는 알 수 없지만, 굳히는 쪽보다 한 번 더 묻는 쪽이 싸다.
            throw e
        } catch (e: Exception) {
            // 네트워크나 키 문제. **캐시에 적지 않는다** — 적으면 그 장소는 영원히 그라데이션이다.
            Log.w(TAG, "사진을 받지 못했다(캐시하지 않음): $placeId", e)
            return PlacePhoto.NONE
        }

        // 여기까지 왔으면 Places가 제대로 답한 것이다. 사진이 없다는 답도 기록할 값이다.
        cache.write(placeId, photo)
        if (photo.hasPhoto) budget.recordFetch()
        return photo
    }

    private suspend fun fetch(placeId: String): PlacePhoto = withContext(Dispatchers.IO) {
        val request = FetchPlaceRequest.builder(placeId, PHOTO_FIELDS).build()
        val metadata = client.fetchPlace(request).await().place
            .photoMetadatas
            ?.firstOrNull()
            ?: return@withContext PlacePhoto.NONE

        val uriRequest = FetchResolvedPhotoUriRequest.builder(metadata)
            // 카드 폭이 화면 폭을 넘지 않는다. 더 크게 받아도 화면에서 줄어들 뿐이고
            // 받는 양만 늘어난다.
            .setMaxWidth(MAX_PHOTO_WIDTH)
            .build()
        val uri = client.fetchResolvedPhotoUri(uriRequest).await().uri
            ?: return@withContext PlacePhoto.NONE

        PlacePhoto(
            url = uri.toString(),
            // Places 약관상 화면에 반드시 적어야 한다. HTML 태그가 섞여 올 수 있어 걷어낸다.
            attribution = metadata.attributions.orEmpty().stripHtml(),
        )
    }

    /** `<a href=...>이름</a>` 같은 태그를 걷어내고 글자만 남긴다. */
    private fun String.stripHtml(): String = replace(HTML_TAG, "").trim()

    private companion object {
        const val TAG = "PlacePhoto"

        /** 사진 정보만 받는다. 다른 필드를 같이 받으면 더 비싼 SKU가 된다. */
        val PHOTO_FIELDS = listOf(Place.Field.PHOTO_METADATAS)

        /** 받을 사진의 최대 가로 픽셀. */
        const val MAX_PHOTO_WIDTH = 400

        val HTML_TAG = Regex("<[^>]*>")
    }
}
