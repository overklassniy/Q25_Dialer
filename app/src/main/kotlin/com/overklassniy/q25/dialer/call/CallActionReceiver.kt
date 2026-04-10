package com.overklassniy.q25.dialer.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CallNotificationManager.ACTION_ANSWER -> CallManager.accept()
            CallNotificationManager.ACTION_DECLINE -> CallManager.reject()
            CallNotificationManager.ACTION_HANGUP -> CallManager.hangup()
        }
    }
}