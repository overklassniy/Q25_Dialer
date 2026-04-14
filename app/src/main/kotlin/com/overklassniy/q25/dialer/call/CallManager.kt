package com.overklassniy.q25.dialer.call

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import java.util.concurrent.CopyOnWriteArraySet

sealed class PhoneState
object NoCall : PhoneState()
data class SingleCall(val call: Call) : PhoneState()
data class TwoCalls(val active: Call, val held: Call) : PhoneState()

interface CallManagerListener {
    fun onStateChanged() {}
    fun onPrimaryCallChanged(call: Call) {}
    fun onAudioStateChanged(audioState: CallAudioState) {}
}

class CallManager {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var inCallService: InCallService? = null
        private val calls = mutableListOf<Call>()
        private val callCallbacks = mutableMapOf<Call, Call.Callback>()
        private val listeners = CopyOnWriteArraySet<CallManagerListener>()
        var isSpeakerOn: Boolean = false

        fun onCallAdded(call: Call) {
            calls.add(call)
            for (listener in listeners) {
                listener.onPrimaryCallChanged(call)
            }
            val callback = object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    if (state == Call.STATE_DISCONNECTED) {
                        // Auto-cleanup disconnected calls
                        removeCallInternal(call)
                    }
                    updateState()
                }
                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateState()
                }
            }
            callCallbacks[call] = callback
            call.registerCallback(callback)
        }

        fun onCallRemoved(call: Call) {
            removeCallInternal(call)
            updateState()
        }

        private fun removeCallInternal(call: Call) {
            calls.remove(call)
            callCallbacks.remove(call)?.let { cb ->
                try { call.unregisterCallback(cb) } catch (_: Exception) { }
            }
            // Auto-unhold the remaining call so the line is not stuck on hold
            val remaining = calls.filter { it.details.state != Call.STATE_DISCONNECTED }
            if (remaining.size == 1 && remaining[0].details.state == Call.STATE_HOLDING) {
                try { remaining[0].unhold() } catch (_: Exception) { }
            }
        }

        fun onAudioStateChanged(audioState: CallAudioState) {
            for (listener in listeners) {
                listener.onAudioStateChanged(audioState)
            }
        }

        fun getPhoneState(): PhoneState {
            // Filter out any lingering disconnected calls
            val liveCalls = calls.filter { it.details.state != Call.STATE_DISCONNECTED }
            return when (liveCalls.size) {
                0 -> NoCall
                1 -> SingleCall(liveCalls.first())
                else -> {
                    val active = liveCalls.find { it.details.state == Call.STATE_ACTIVE }
                        ?: liveCalls.find { it.details.state == Call.STATE_DIALING || it.details.state == Call.STATE_CONNECTING }
                        ?: liveCalls[0]
                    val held = liveCalls.first { it != active }
                    TwoCalls(active, held)
                }
            }
        }

        fun getPrimaryCall(): Call? {
            return when (val state = getPhoneState()) {
                is SingleCall -> state.call
                is TwoCalls -> state.active
                NoCall -> null
            }
        }

        fun accept() {
            getPrimaryCall()?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun reject() {
            getPrimaryCall()?.reject(false, null)
        }

        fun hangup() {
            getPrimaryCall()?.disconnect()
        }

        fun hold() {
            getPrimaryCall()?.hold()
        }

        fun unhold() {
            getPrimaryCall()?.unhold()
        }

        fun swap() {
            val state = getPhoneState()
            if (state is TwoCalls) {
                state.active.hold()
            }
        }

        fun setMuted(muted: Boolean) {
            inCallService?.setMuted(muted)
        }

        @Suppress("DEPRECATION")
        fun setSpeaker(on: Boolean) {
            isSpeakerOn = on
            val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
            inCallService?.setAudioRoute(route)
        }

        @Suppress("DEPRECATION")
        fun setAudioRoute(route: Int) {
            isSpeakerOn = route == CallAudioState.ROUTE_SPEAKER
            inCallService?.setAudioRoute(route)
        }

        fun playDtmf(char: Char) {
            getPrimaryCall()?.playDtmfTone(char)
            getPrimaryCall()?.stopDtmfTone()
        }

        fun addListener(listener: CallManagerListener) {
            listeners.add(listener)
        }

        fun removeListener(listener: CallManagerListener) {
            listeners.remove(listener)
        }

        private fun updateState() {
            for (listener in listeners) {
                listener.onStateChanged()
            }
        }
    }
}