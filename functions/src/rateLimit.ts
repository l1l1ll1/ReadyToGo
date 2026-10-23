/**
 * 사용자당 분당 호출 수 제한.
 *
 * **Firestore에 센다.** 인스턴스가 최대 2개라 메모리 카운터로는 요청이 어느 쪽으로 가느냐에
 * 따라 한도가 두 배가 된다. 트랜잭션으로 읽고 써야 동시에 두 번 눌러도 한 번씩 센다.
 *
 * 경로 rateLimits/{uid}는 보안 규칙의 "나머지는 전부 거부"에 걸려 앱이 직접 건드릴 수 없다.
 * 함수는 Admin SDK라 규칙을 거치지 않는다.
 */
import {getFirestore} from "firebase-admin/firestore";
import {HttpsError} from "firebase-functions/https";

export const WINDOW_MS = 60_000;
export const MAX_CALLS_PER_WINDOW = 5;

/**
 * 한도 안이면 한 번을 세고 돌아온다. 넘었으면 resource-exhausted를 던진다.
 * @param {string} uid 로그인한 사용자
 */
export async function consumeQuota(uid: string): Promise<void> {
  const db = getFirestore();
  const ref = db.collection("rateLimits").doc(uid);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const now = Date.now();
    const windowStart = snap.get("windowStart") as number | undefined;
    const count = (snap.get("count") as number | undefined) ?? 0;

    // 고정 창(1분)으로 센다. 미끄러지는 창보다 정확도는 떨어지지만 문서 하나로 끝난다.
    if (windowStart === undefined || now - windowStart >= WINDOW_MS) {
      tx.set(ref, {windowStart: now, count: 1});
      return;
    }
    if (count >= MAX_CALLS_PER_WINDOW) {
      throw new HttpsError("resource-exhausted", "잠시 후 다시 시도해 주세요.");
    }
    tx.update(ref, {count: count + 1});
  });
}
