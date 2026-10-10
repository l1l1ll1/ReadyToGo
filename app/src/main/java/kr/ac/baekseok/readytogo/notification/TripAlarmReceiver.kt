package kr.ac.baekseok.readytogo.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import kr.ac.baekseok.readytogo.MainActivity
import kr.ac.baekseok.readytogo.R

/**
 * 예약한 시각이 되면 알림을 띄운다.
 *
 * 알림을 만드는 데 필요한 것(제목, 목적지, 며칠 전인지)을 Intent에 담아 둔다.
 * 여기서 저장소를 다시 읽지 않는 이유: 브로드캐스트는 몇 초 안에 끝나야 하는데
 * Firestore 조회는 네트워크를 타고, 로그아웃 상태면 읽을 수도 없다.
 */
class TripAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 알림 권한이 없으면 notify가 조용히 무시된다. 로그만 남겨 원인을 알 수 있게 한다.
        if (!context.canPostNotifications()) {
            Log.w(TAG, "알림 권한이 없어 띄우지 못했다")
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val destination = intent.getStringExtra(EXTRA_DESTINATION).orEmpty()
        val daysBefore = intent.getIntExtra(EXTRA_DAYS_BEFORE, 1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, title.hashCode())

        val text = context.getString(R.string.notification_body, destination, daysBefore)

        val notification = Notification.Builder(context, TripNotifications.channelId())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            // 누르면 사라지게 한다. 남겨두면 여행 다녀와서도 목록에 붙어 있다.
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    /** 알림을 누르면 앱을 연다. 어느 화면으로 갈지는 홈이 알아서 한다. */
    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_DAYS_BEFORE = "days_before"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        private const val TAG = "TripAlarm"
    }
}

/** 알림을 띄울 수 있는 상태인지. minSdk가 33이라 권한 확인은 항상 필요하다. */
internal fun Context.canPostNotifications(): Boolean =
    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
