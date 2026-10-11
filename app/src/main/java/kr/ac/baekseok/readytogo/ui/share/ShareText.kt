package kr.ac.baekseok.readytogo.ui.share

import android.content.Context
import android.content.Intent

/**
 * 글을 다른 앱으로 보낸다. 카톡·메모·메일 중 무엇으로 보낼지는 안드로이드가 묻는다.
 *
 * **이미지가 아니라 글인 이유**: 공유하는 내용이 가게 이름과 주소라, 받는 쪽이 가장 먼저
 * 하는 일이 그것을 복사해 지도에 넣는 것이다. 이미지로는 복사가 안 된다. 닷새치 일정이
 * 서른 줄을 넘어 한 장에 담기지도 않는다.
 *
 * **예외를 삼킨다.** 공유할 앱이 하나도 없는 기기(에뮬레이터 등)에서 던지는데,
 * 일정을 보려던 사용자의 앱이 그걸로 죽으면 안 된다.
 */
fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { startActivity(Intent.createChooser(intent, null)) }
}
