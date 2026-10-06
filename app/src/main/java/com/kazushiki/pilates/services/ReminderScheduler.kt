package com.kazushiki.pilates.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kazushiki.pilates.MainActivity
import com.kazushiki.pilates.R
import com.kazushiki.pilates.data.ProfileStore
import com.kazushiki.pilates.data.SharedPrefsStore
import com.kazushiki.pilates.model.UserProfile
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Schedules (or clears) workout reminders: one a week for each of the user's workout days,
 * at the time she picked. Each weekday has its own alarm; when it fires, [ReminderReceiver]
 * posts the notification and books the same weekday for next week.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "workout_reminders"
    const val ACTION_REMIND = "com.kazushiki.pilates.action.WORKOUT_REMINDER"
    const val EXTRA_WEEKDAY = "kp.weekday"
    private const val NOTIFICATION_ID = 4100
    private const val PERMISSION_REQUEST_CODE = 4101

    fun createChannel(context: Context) {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.description = context.getString(R.string.reminder_channel_description)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    fun apply(context: Context, profile: UserProfile) {
        try {
            for (weekday in 1..7) cancel(context, weekday)
            if (!profile.remindersOn) return
            requestPermissionIfNeeded(context)
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            for (weekday in profile.effectiveTrainingDays.sorted()) {
                schedule(context, profile, weekday, now)
            }
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    /** The next time on [weekday] (1 = Sunday … 7 = Saturday) at hour:minute, strictly after [from]. */
    fun nextTrigger(from: ZonedDateTime, weekday: Int, hour: Int, minute: Int): ZonedDateTime {
        val day = DayOfWeek.of((weekday + 5) % 7 + 1)
        var candidate = from
            .with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!candidate.isAfter(from)) candidate = candidate.plusWeeks(1)
        return candidate
    }

    internal fun schedule(context: Context, profile: UserProfile, weekday: Int, from: ZonedDateTime) {
        try {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val trigger = nextTrigger(from, weekday, profile.reminderHour, profile.reminderMinute)
            val pending = PendingIntent.getBroadcast(
                context,
                weekday,
                reminderIntent(context, weekday),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), pending)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    private fun cancel(context: Context, weekday: Int) {
        try {
            val pending = PendingIntent.getBroadcast(
                context,
                weekday,
                reminderIntent(context, weekday),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            ) ?: return
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarms?.cancel(pending)
            pending.cancel()
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    private fun reminderIntent(context: Context, weekday: Int): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .putExtra(EXTRA_WEEKDAY, weekday)

    /** Like iPhone asking for notification permission when reminders are turned on (Android 13+). */
    private fun requestPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission(context)) return
        val activity = context.findActivity() ?: return
        try {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current != null) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext
        }
        return null
    }

    /** Posts "Today's a Pilates day" if notifications are allowed. */
    @SuppressLint("MissingPermission")
    internal fun showReminder(context: Context, profile: UserProfile) {
        try {
            if (!hasNotificationPermission(context)) return
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            createChannel(context)

            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                open,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setColor(0xFF8A4F7D.toInt())
                .setContentTitle("Today's a Pilates day")
                .setContentText("Your ${profile.minutesPerDay}-minute session is ready when you are.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }

    internal fun loadProfileStore(context: Context): ProfileStore = ProfileStore(SharedPrefsStore(context))
}

/** Fires on a workout day: shows the reminder, then books the same weekday for next week. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val weekday = intent.getIntExtra(ReminderScheduler.EXTRA_WEEKDAY, 0)
            val profile = ReminderScheduler.loadProfileStore(context).profile
            if (!profile.remindersOn || weekday !in profile.effectiveTrainingDays) return
            ReminderScheduler.showReminder(context, profile)
            // A minute's margin so a slightly early alarm can't book today again.
            val from = ZonedDateTime.now(ZoneId.systemDefault()).plusMinutes(1)
            ReminderScheduler.schedule(context, profile, weekday, from)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }
}

/** Alarms don't survive a reboot (or an app update / time-zone change), so book them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val profiles = ReminderScheduler.loadProfileStore(context)
            if (!profiles.hasCompletedOnboarding) return
            ReminderScheduler.apply(context, profiles.profile)
        } catch (e: Exception) {
            // Never crash over reminders.
        }
    }
}
