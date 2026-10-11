package kr.ac.baekseok.readytogo.data.repository.places

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kr.ac.baekseok.readytogo.BuildConfig
import java.util.Locale

/**
 * Places 클라이언트를 만든다. 식당·관광지와 숙소가 같은 SDK를 쓰므로 초기화를 한곳에 모은다.
 *
 * 언어는 검색 요청이 아니라 초기화에서 정해진다. 그 언어 이름이 있는 곳은 그 언어로,
 * 없는 곳은 원어 그대로 온다("에펠탑" / "Le Procope").
 * 한 번 호출에 한 언어만 오므로 한글·영문 병기는 호출이 두 배가 되어 하지 않는다.
 *
 * **앱 언어를 따라간다.** 한국어로 고정해 뒀더니 영어 화면에 "일본 음식점" 같은 종류
 * 이름이 그대로 올라왔다. 다만 Places는 한 번만 초기화되므로, 설정에서 언어를 바꾸면
 * 그 뒤의 검색은 **앱을 다시 켤 때까지** 이전 언어로 나온다. 계획을 새로 만들 때마다
 * 다시 초기화할 수는 없어(SDK가 재초기화를 지원하지 않는다) 이 정도로 둔다.
 */
internal object PlacesClientFactory {

    fun create(context: Context): PlacesClient {
        val app = context.applicationContext
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                app,
                BuildConfig.MAPS_API_KEY,
                Locale.getDefault(),
            )
        }
        return Places.createClient(app)
    }
}
