package kr.ac.baekseok.readytogo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 재부팅 후 알람을 다시 깐다.
 *
 * AlarmManager에 넣은 예약은 기기를 껐다 켜면 전부 사라진다. 이 리시버가 없으면
 * 재부팅한 사용자만 조용히 알림을 못 받게 되는데, 그건 알아차리기도 어렵다.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // 채널은 앱 프로세스가 새로 떴을 수 있으니 여기서도 확인한다.
        TripNotifications.ensureChannel(context)
        TripNotifications.rescheduleFromCache(context)
    }
}
