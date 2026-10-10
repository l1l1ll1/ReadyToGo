package kr.ac.baekseok.readytogo.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import kr.ac.baekseok.readytogo.R
import kr.ac.baekseok.readytogo.data.model.AppSettings
import kr.ac.baekseok.readytogo.data.model.SavedTrip
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId

/** 어느 시점의 알림인지. 출발 며칠 전인지를 값으로 들고 있는다. */
enum class TripReminder(val daysBefore: Long) {
    WEEK(7),
    DAY(1),
}

/** 깔아둔 알람 하나. 재부팅 뒤 그대로 다시 깔 수 있을 만큼만 담는다. */
private data class AlarmEntry(
    val key: String,
    val title: String,
    val destination: String,
    val daysBefore: Int,
    val triggerAt: Long,
)

/**
 * 출발 전 알림 예약.
 *
 * **WorkManager가 아니라 AlarmManager를 쓰는 이유**: 라이브러리를 하나 더 들이지 않기 위해서다.
 * 대신 재부팅하면 알람이 사라지므로 [BootReceiver]가 다시 깐다.
 *
 * **정확 알람(setExactAndAllowWhileIdle)을 쓰지 않는 이유**: 안드로이드 12부터
 * `SCHEDULE_EXACT_ALARM` 권한을 따로 받아야 하는데, "출발 일주일 전" 알림이 9시 정각에
 * 올 필요는 없다. [AlarmManager.setWindow]로 한 시간 안에 오게 두면 권한 없이 된다.
 */
object TripNotifications {

    /** 알림을 보낼 시각(24시간). 아침에 받아야 그날 준비할 수 있다. */
    const val NOTIFY_HOUR = 9

    /** 알람이 떠도 되는 폭. 정확 알람이 아니라 이 범위 안 아무 때나 온다. */
    private const val WINDOW_MILLIS = 60L * 60 * 1000

    private const val CHANNEL_ID = "trip_reminder"
    private const val TAG = "TripNotifications"

    /**
     * 알림 채널을 만든다. 앱이 시작될 때 한 번 부른다.
     *
     * 같은 id로 다시 만들어도 덮어쓰지 않으므로 여러 번 불러도 안전하다.
     */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_body)
        }
        context.notificationManager.createNotificationChannel(channel)
    }

    /**
     * 저장된 일정 전체를 보고 알람을 다시 깐다.
     *
     * **깔려 있던 것을 지우고 새로 까는 방식이다.** 일정이 지워졌는지 날짜가 바뀌었는지를
     * 하나씩 따라가는 것보다, 매번 전부 다시 까는 쪽이 어긋날 구석이 없다.
     * 일정이 수십 개 수준이라 비용도 문제되지 않는다.
     */
    fun reschedule(context: Context, trips: List<SavedTrip>, settings: AppSettings) {
        val store = ScheduledAlarms(context)
        store.entries.forEach { cancel(context, it.key) }

        val enabled = TripReminder.entries.filter { reminder ->
            when (reminder) {
                TripReminder.WEEK -> settings.notifyBeforeWeek
                TripReminder.DAY -> settings.notifyBeforeDay
            }
        }

        val now = System.currentTimeMillis()
        val entries = trips.flatMap { trip ->
            enabled.mapNotNull { reminder ->
                val triggerAt = trip.departureDate
                    .minusDays(reminder.daysBefore)
                    .atTime(NOTIFY_HOUR, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                // 이미 지난 시각은 예약해봐야 즉시 울리거나 무시된다.
                if (triggerAt <= now) return@mapNotNull null

                AlarmEntry(
                    key = "${trip.id}|${reminder.name}",
                    title = trip.title,
                    destination = trip.destinationName,
                    daysBefore = reminder.daysBefore.toInt(),
                    triggerAt = triggerAt,
                )
            }
        }

        entries.forEach { schedule(context, it) }
        store.entries = entries
        Log.d(TAG, "알림 ${entries.size}건 예약 (일정 ${trips.size}개)")
    }

    /**
     * 저장해둔 목록 그대로 다시 깐다. 재부팅 뒤에 쓴다.
     *
     * 저장소를 다시 읽지 않는 이유: 부팅 직후에는 로그인 세션이 아직 복구되지 않았고,
     * 브로드캐스트는 몇 초 안에 끝나야 해서 Firestore 조회를 기다릴 수 없다.
     */
    fun rescheduleFromCache(context: Context) {
        val now = System.currentTimeMillis()
        val entries = ScheduledAlarms(context).entries.filter { it.triggerAt > now }
        entries.forEach { schedule(context, it) }
        Log.d(TAG, "재부팅 후 알림 ${entries.size}건 복구")
    }

    /** 알림을 전부 끈다. 두 토글이 모두 꺼졌을 때와 같은 결과다. */
    fun cancelAll(context: Context) {
        val store = ScheduledAlarms(context)
        store.entries.forEach { cancel(context, it.key) }
        store.entries = emptyList()
    }

    private fun schedule(context: Context, entry: AlarmEntry) {
        context.alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            entry.triggerAt,
            WINDOW_MILLIS,
            pendingIntent(context, entry),
        )
    }

    private fun cancel(context: Context, key: String) {
        // 취소에는 같은 requestCode와 같은 Intent 동작이면 된다. 담긴 값은 보지 않는다.
        val pending = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            Intent(context, TripAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        pending?.let {
            context.alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(context: Context, entry: AlarmEntry): PendingIntent {
        val intent = Intent(context, TripAlarmReceiver::class.java).apply {
            putExtra(TripAlarmReceiver.EXTRA_TITLE, entry.title)
            putExtra(TripAlarmReceiver.EXTRA_DESTINATION, entry.destination)
            putExtra(TripAlarmReceiver.EXTRA_DAYS_BEFORE, entry.daysBefore)
            // 알림 id로도 쓴다. 같은 일정의 D-7과 D-1이 서로를 덮어쓰면 안 된다.
            putExtra(TripAlarmReceiver.EXTRA_NOTIFICATION_ID, entry.key.hashCode())
        }

        return PendingIntent.getBroadcast(
            context,
            entry.key.hashCode(),
            intent,
            // IMMUTABLE은 안드로이드 12부터 필수다. UPDATE_CURRENT로 제목이 바뀌면 갱신한다.
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    internal fun channelId() = CHANNEL_ID

    private val Context.alarmManager: AlarmManager
        get() = getSystemService(AlarmManager::class.java)

    private val Context.notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)
}

/**
 * 지금 깔려 있는 알람 목록.
 *
 * 앱이 꺼져 있는 동안에도 알람은 시스템에 남아 있다. 무엇을 깔았는지 기억해두지 않으면
 * 지운 일정의 알림이 그대로 울리고, 재부팅 뒤에 되살릴 수도 없다.
 */
private class ScheduledAlarms(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var entries: List<AlarmEntry>
        get() = parse(prefs.getString(KEY, null))
        set(value) = prefs.edit().putString(KEY, serialize(value)).apply()

    private fun serialize(entries: List<AlarmEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("key", entry.key)
                    .put("title", entry.title)
                    .put("destination", entry.destination)
                    .put("daysBefore", entry.daysBefore)
                    .put("triggerAt", entry.triggerAt),
            )
        }
        return array.toString()
    }

    /** 읽다 깨지면 빈 목록으로 본다 — 알림을 못 받는 것이 앱이 죽는 것보다 낫다. */
    private fun parse(text: String?): List<AlarmEntry> {
        if (text == null) return emptyList()
        return runCatching {
            val array = JSONArray(text)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                AlarmEntry(
                    key = item.getString("key"),
                    title = item.optString("title"),
                    destination = item.optString("destination"),
                    daysBefore = item.optInt("daysBefore", 1),
                    triggerAt = item.getLong("triggerAt"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val FILE = "alarms"
        const val KEY = "scheduled"
    }
}
