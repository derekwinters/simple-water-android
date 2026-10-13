package com.derekwinters.water.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.derekwinters.water.MainActivity
import com.derekwinters.water.R
import com.derekwinters.water.core.ReminderText
import com.derekwinters.water.core.VolumeUnit

object Notifications {
    const val CHANNEL_ID = "water_reminders"

    /** REM-012: one id, so each reminder replaces the last. */
    const val REMINDER_ID = 1

    private const val REQUEST_OPEN = 1
    private const val REQUEST_LOG = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_description)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** REM-014. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * REM-012: progress text, a "Log <default size>" action, and a tap that opens the app.
     * REM-019: with a [paceText] the collapsed text is the pace sentence and the expanded text adds
     * the totals line beneath it; without one the totals line is the whole text, as before.
     */
    fun showReminder(
        context: Context,
        totalMl: Int,
        goalMl: Int,
        unit: VolumeUnit,
        defaultSizeMl: Int,
        paceText: String? = null,
    ) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val log = PendingIntent.getBroadcast(
            context,
            REQUEST_LOG,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_LOG)
                .putExtra(NotificationActionReceiver.EXTRA_AMOUNT_ML, defaultSizeMl),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val totals = ReminderText.body(totalMl, goalMl, unit)
        val collapsed = paceText ?: totals
        val expanded = if (paceText == null) totals else "$paceText\n$totals"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ReminderText.TITLE)
            .setContentText(collapsed)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, ReminderText.logAction(defaultSizeMl, unit), log)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; nothing else to do.
        }
    }

    /** REM-013. */
    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(REMINDER_ID)
    }
}
