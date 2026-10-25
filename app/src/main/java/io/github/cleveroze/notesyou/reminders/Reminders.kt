package io.github.cleveroze.notesyou.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.cleveroze.notesyou.MainActivity
import io.github.cleveroze.notesyou.R
import io.github.cleveroze.notesyou.data.Note
import io.github.cleveroze.notesyou.data.NotesStore

object Reminders {
    private const val CHANNEL = "reminders"
    const val EXTRA_NOTE_ID = "note_id"

    private fun pending(context: Context, note: Note): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_NOTE_ID, note.id)
            .putExtra("title", note.title.ifBlank { "Напоминание" })
            .putExtra("text", note.plain().removePrefix(note.title).trim().take(200))
        return PendingIntent.getBroadcast(
            context, (note.id % Int.MAX_VALUE).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, note: Note) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = note.reminderAt
        if (at == null || at <= System.currentTimeMillis() || note.isTrashed) {
            am.cancel(pending(context, note))
            return
        }
        // Inexact-but-doze-friendly alarm; no special permission needed.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, note))
    }

    fun cancel(context: Context, note: Note) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, note))
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Напоминания о заметках"
                }
            )
        }
    }

    fun notify(context: Context, id: Long, title: String, text: String) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, (id % Int.MAX_VALUE).toInt(),
            Intent(context, MainActivity::class.java).putExtra(EXTRA_NOTE_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text.ifBlank { "Откройте заметку" })
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) nm.notify((id % Int.MAX_VALUE).toInt(), n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_NOTE_ID, 0)
        Reminders.notify(context, id, intent.getStringExtra("title") ?: "Напоминание", intent.getStringExtra("text") ?: "")
    }
}

/** Alarms are cleared on reboot — schedule them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val store = NotesStore(context.applicationContext)
        store.notes.value.filter { it.reminderAt != null }.forEach { Reminders.schedule(context, it) }
    }
}
