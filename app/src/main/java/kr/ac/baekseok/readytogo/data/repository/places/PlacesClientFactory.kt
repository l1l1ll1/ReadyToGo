package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kr.ac.baekseok.readytogo.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Places 클라이언트를 만든다. 식당·관광지와 숙소가 같은 SDK를 쓰므로 초기화를 한곳에 모은다.
 *
 * 언어는 검색 요청이 아니라 초기화에서 정해진다. 한국어 이름이 있는 곳은 한글로,
 * 없는 곳은 원어 그대로 온다("에펠탑" / "Le Procope").
 * 한 번 호출에 한 언어만 오므로 한글·영문 병기는 호출이 두 배가 되어 하지 않는다.
 */
internal object PlacesClientFactory {

    fun create(context: Context): PlacesClient {
        val app = context.applicationContext
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(app, BuildConfig.MAPS_API_KEY, Locale.KOREAN)
        }
        return Places.createClient(app)
    }
}

/** Places SDK는 Task를 돌려준다. 코루틴에서 기다릴 수 있게 감싼다. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
