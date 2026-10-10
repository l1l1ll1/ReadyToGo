package kr.ac.baekseok.readytogo.data.repository

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google SDK들은 코루틴이 아니라 [Task]를 돌려준다. 코루틴에서 기다릴 수 있게 감싼다.
 *
 * Places·Firebase Auth·Firestore가 모두 이 형태라 한곳에 둔다.
 *
 * `kotlinx-coroutines-play-services`의 `await()`를 쓰지 않는 이유: 라이브러리 하나를
 * 더 넣어야 하는데, 하는 일이 이 다섯 줄과 같다.
 *
 * 취소도 넘겨준다 — 화면을 벗어나 코루틴이 취소될 때 Task 결과를 기다리며 매달려 있으면
 * 그만큼 메모리를 붙잡는다.
 */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
