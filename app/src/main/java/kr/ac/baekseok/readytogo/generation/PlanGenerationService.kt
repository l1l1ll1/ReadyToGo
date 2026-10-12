package kr.ac.baekseok.readytogo.generation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import kr.ac.baekseok.readytogo.MainActivity
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.AppContainer
import kr.ac.baekseok.readytogo.data.model.TripCondition
import kr.ac.baekseok.readytogo.notification.canPostNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 일정을 만드는 동안 프로세스를 붙잡아 두고, 알림으로 진행·완료를 알린다.
 *
 * **생성 자체는 여기서 하지 않는다.** [PlanGenerator]가 돌리고, 이 서비스는 그 상태만 본다.
 * 서비스에 로직을 넣으면 화면이 결과를 받으려고 서비스에 붙어야(bind) 해서 구조가 무거워진다.
 *
 * 서비스 타입은 dataSync다 — 네트워크로 항공·숙소·장소를 받아 오는 일이다.
 * shortService는 3분 제한이 있어 실제 API가 느린 날 끊길 수 있다.
 */
class PlanGenerationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val generator = AppContainer.planGenerator
        val condition = when (val state = generator.state.value) {
            is PlanGeneration.Running -> state.condition
            is PlanGeneration.Done -> state.condition
            PlanGeneration.Idle -> null
        }

        // startForegroundService로 불렸으면 상태와 무관하게 먼저 포그라운드로 올라가야 한다.
        // 빠뜨리면 시스템이 앱을 죽인다. Mock은 금방 끝나 이미 Done일 수도 있다.
        startForeground(
            NOTIFICATION_ID,
            progressNotification(condition),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        if (watching == null) {
            watching = scope.launch {
                generator.state.collect { state ->
                    when (state) {
                        // 다른 조건으로 새로 시작했으면 알림 문구만 바꾼다.
                        is PlanGeneration.Running -> notify(progressNotification(state.condition))
                        is PlanGeneration.Done -> finish(state)
                        PlanGeneration.Idle -> Unit
                    }
                }
            }
        }

        // 프로세스가 죽었다 살아나도 다시 돌릴 생성이 메모리에 없다. 되살릴 이유가 없다.
        return START_NOT_STICKY
    }

    /**
     * 끝났다. 앱을 보고 있으면 알림을 지우고, 나가 있으면 결과 알림으로 바꿔 남긴다.
     *
     * 보고 있는데 "일정 완성"을 남기면 이미 본 결과를 또 알리는 셈이다.
     */
    private fun finish(state: PlanGeneration.Done) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (!AppContainer.isAppVisible && canPostNotifications()) {
            // 진행 알림 자리를 덮어쓰지 않고 새 id로 띄운다. 덮어쓰면 조용한 진행 채널의
            // 순위를 물려받아 알림창 "무음" 칸에 묻혔다(에뮬레이터에서 확인).
            getSystemService(NotificationManager::class.java)
                .notify(DONE_NOTIFICATION_ID, doneNotification(state))
        }
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun progressNotification(condition: TripCondition?): Notification =
        Notification.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.plan_notification_running_title))
            .setContentText(getString(R.string.plan_notification_running_body, condition.placeName()))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setContentIntent(openPlanIntent(condition))
            .build()

    private fun doneNotification(state: PlanGeneration.Done): Notification {
        val (title, body) = if (state.result.isSuccess) {
            getString(R.string.plan_notification_done_title) to
                getString(R.string.plan_notification_done_body, state.condition.placeName())
        } else {
            getString(R.string.plan_notification_failed_title) to
                getString(R.string.plan_notification_failed_body)
        }
        return Notification.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openPlanIntent(state.condition))
            .setAutoCancel(true)
            .build()
    }

    /**
     * 누르면 그 조건의 결과 화면을 연다. 실패했어도 같다 — 결과 화면의 "다시 시도"로 이어진다.
     *
     * 조건을 통째로 담는다. 생성기가 결과를 들고 있으면 바로 뜨고, 그사이 프로세스가 내려갔으면
     * 결과 화면이 같은 조건으로 다시 만든다.
     */
    private fun openPlanIntent(condition: TripCondition?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (condition != null) intent.putExtra(MainActivity.EXTRA_OPEN_PLAN, condition)
        return PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notify(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun TripCondition?.placeName(): String = this?.destination?.shortName.orEmpty()

    companion object {
        private const val TAG = "PlanGenerationService"

        /** 진행 알림. 끝나면 지우고 [DONE_NOTIFICATION_ID]로 결과를 알린다. */
        private const val NOTIFICATION_ID = 2001
        private const val DONE_NOTIFICATION_ID = 2002

        /** 진행은 조용히, 완료는 소리를 낸다. 채널을 나눠야 사용자가 따로 끌 수 있다. */
        private const val CHANNEL_PROGRESS = "plan_progress"
        private const val CHANNEL_DONE = "plan_done"

        fun ensureChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    context.getString(R.string.plan_channel_progress_name),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DONE,
                    context.getString(R.string.plan_channel_done_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }

        /**
         * 생성이 시작될 때 [PlanGenerator]가 부른다.
         *
         * 시작하지 못해도(백그라운드 시작 제한 등) 생성은 그대로 돈다. 앱을 보고 있는 동안에는
         * 서비스 없이도 끝까지 가므로, 서비스 때문에 계획을 실패시키지 않는다.
         */
        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, PlanGenerationService::class.java))
            }.onFailure { Log.w(TAG, "포그라운드 서비스를 시작하지 못했다", it) }
        }
    }
}
