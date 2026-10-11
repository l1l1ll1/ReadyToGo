package kr.ac.baekseok.readytogo.data.photo

import android.content.Context
import java.time.YearMonth

/**
 * 이번 달에 받은 사진 수를 세고, 상한을 넘으면 더 받지 않게 막는다.
 *
 * **왜 필요한가**: Places 사진은 검색과 별개로 과금되는 SKU다. 무료 한도가 작아서
 * (제3자 자료 기준 월 1,000건) 시연 도중 한도를 넘기면 요금이 나가기 시작한다.
 * 상한에 닿으면 사진 없이 그라데이션으로 떨어지는 쪽이, 모르는 사이에 돈이 나가는 것보다 낫다.
 *
 * **세는 단위는 "장"이다.** 사진 한 장을 얻으려면 실제로는 요청이 두 번 나가지만
 * (사진 정보 → 실제 주소), 사람이 가늠하기 쉬운 쪽은 장수라 장수로 센다.
 * 실제 과금 건수는 이 값의 두 배로 보면 된다.
 *
 * 캐시에서 꺼낸 사진은 요청을 보내지 않으므로 세지 않는다.
 */
class PhotoBudget(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 더 받아도 되는지. */
    fun canFetch(): Boolean = usedThisMonth() < MONTHLY_PHOTO_LIMIT

    /** 한 장을 받았다고 기록한다. */
    fun recordFetch() {
        val month = currentMonth()
        preferences.edit()
            .putString(KEY_MONTH, month)
            .putInt(KEY_COUNT, usedThisMonth() + 1)
            .apply()
    }

    /**
     * 이번 달에 받은 장수.
     *
     * 달이 바뀌면 0부터 다시 센다. 지난달 값을 지우지 않고 달 이름을 같이 저장해 두었다가
     * 다르면 0으로 보는 방식이다 — 자정에 돌릴 작업을 따로 두지 않아도 된다.
     */
    fun usedThisMonth(): Int {
        if (preferences.getString(KEY_MONTH, null) != currentMonth()) return 0
        return preferences.getInt(KEY_COUNT, 0)
    }

    private fun currentMonth(): String = YearMonth.now().toString()

    companion object {
        /**
         * 한 달에 받을 사진 수의 상한.
         *
         * 무료 한도는 월 1,000장이다(Google Maps Platform 가격 페이지 확인). 딱 맞춰 두면
         * 한도를 재는 시점 차이나 다른 경로의 호출 때문에 넘길 수 있어 200장을 남겨 둔다.
         * 숫자만 바꿔 조정한다.
         */
        const val MONTHLY_PHOTO_LIMIT = 800

        private const val FILE = "photo_budget"
        private const val KEY_MONTH = "month"
        private const val KEY_COUNT = "count"
    }
}
