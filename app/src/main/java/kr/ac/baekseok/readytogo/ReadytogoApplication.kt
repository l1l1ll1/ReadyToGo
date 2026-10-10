package kr.ac.baekseok.readytogo

import android.app.Application
import kr.ac.baekseok.readytogo.data.AppContainer

/**
 * 앱이 시작될 때 가장 먼저 실행되는 곳.
 *
 * **왜 필요한가**: [AppContainer]의 Firebase Repository들이 Application Context를 쓴다.
 * Activity에서 넘기면 화면이 생기기 전에 Repository를 건드리는 경로(스플래시의 세션 확인)에서
 * 초기화 전 접근이 생긴다. Application.onCreate는 어떤 화면보다 먼저 한 번만 돌아서 안전하다.
 *
 * `FirebaseApp` 자체는 google-services 플러그인이 넣어주는 ContentProvider가 이보다도 먼저
 * 초기화하므로 여기서 부르지 않는다.
 */
class ReadytogoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
