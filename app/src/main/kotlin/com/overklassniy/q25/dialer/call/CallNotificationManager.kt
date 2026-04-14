package com.overklassniy.q25.dialer.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.repository.ContactsRepository

class CallNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "active_calls"
        const val NOTIFICATION_ID = 42
        const val ACTION_ANSWER = "com.overklassniy.q25.dialer.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.overklassniy.q25.dialer.ACTION_DECLINE"
        const val ACTION_HANGUP = "com.overklassniy.q25.dialer.ACTION_HANGUP"
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val contactsRepository by lazy { ContactsRepository(context) }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.call_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun setupNotification(lowPriority: Boolean = false) {
        val call = CallManager.getPrimaryCall() ?: return
        val callerInfo = getCallerInfo(call)

        val callActivityIntent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, callActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val isIncoming = call.state == Call.STATE_RINGING

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(callerInfo)
            .setContentText(
                if (isIncoming) context.getString(R.string.incoming_call)
                else context.getString(R.string.ongoing_call)
            )
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (!lowPriority) {
            builder.setFullScreenIntent(contentIntent, true)
            builder.priority = NotificationCompat.PRIORITY_MAX
        } else {
            builder.priority = NotificationCompat.PRIORITY_DEFAULT
        }

        if (isIncoming) {
            val answerIntent = PendingIntent.getBroadcast(
                context, 1,
                Intent(ACTION_ANSWER).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val declineIntent = PendingIntent.getBroadcast(
                context, 2,
                Intent(ACTION_DECLINE).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.decline), declineIntent)
            builder.addAction(0, context.getString(R.string.answer), answerIntent)
        } else {
            val hangupIntent = PendingIntent.getBroadcast(
                context, 3,
                Intent(ACTION_HANGUP).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, context.getString(R.string.end_call), hangupIntent)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun getCallerInfo(call: Call): String {
        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart
        if (number.isNullOrEmpty()) {
            return context.getString(R.string.unknown_caller)
        }

        // Try to find contact by number
        val contact = try {
            contactsRepository.getContactByNumber(number)
        } catch (_: Exception) { null }

        return contact?.name?.takeIf { it.isNotBlank() } ?: number
    }
}