package com.overklassniy.q25.dialer.service

import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.overklassniy.q25.dialer.call.CallActivity
import com.overklassniy.q25.dialer.call.CallManager
import com.overklassniy.q25.dialer.call.CallNotificationManager
import com.overklassniy.q25.dialer.call.NoCall

class DialerCallService : InCallService() {

    private val callNotificationManager by lazy { CallNotificationManager(this) }

    private val callListener = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                callNotificationManager.cancelNotification()
            } else {
                callNotificationManager.setupNotification()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.onCallAdded(call)
        CallManager.inCallService = this
        call.registerCallback(callListener)

        val isOutgoing = call.details.state == Call.STATE_CONNECTING || call.details.state == Call.STATE_DIALING
        val isDeviceLocked = !(getSystemService(PowerManager::class.java)?.isInteractive ?: true)
        val lowPriority = !isDeviceLocked && isOutgoing

        if (lowPriority) {
            try {
                startActivity(CallActivity.getStartIntent(this))
            } catch (_: Exception) { }
        }
        callNotificationManager.setupNotification(lowPriority)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callListener)
        callNotificationManager.cancelNotification()
        CallManager.onCallRemoved(call)
        if (CallManager.getPhoneState() == NoCall) {
            CallManager.inCallService = null
        } else {
            callNotificationManager.setupNotification()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        if (audioState != null) {
            CallManager.onAudioStateChanged(audioState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callNotificationManager.cancelNotification()
    }
}