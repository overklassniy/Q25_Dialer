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
        private val listeners = CopyOnWriteArraySet<CallManagerListener>()
        var isSpeakerOn: Boolean = false

        fun onCallAdded(call: Call) {
            calls.add(call)
            for (listener in listeners) {
                listener.onPrimaryCallChanged(call)
            }
            call.registerCallback(object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    updateState()
                }
                override fun onDetailsChanged(call: Call, details: Call.Details) {
                    updateState()
                }
            })
        }

        fun onCallRemoved(call: Call) {
            calls.remove(call)
            updateState()
        }

        fun onAudioStateChanged(audioState: CallAudioState) {
            for (listener in listeners) {
                listener.onAudioStateChanged(audioState)
            }
        }

        fun getPhoneState(): PhoneState {
            return when (calls.size) {
                0 -> NoCall
                1 -> SingleCall(calls.first())
                else -> {
                    val active = calls.find { it.state == Call.STATE_ACTIVE }
                    val other = calls.find { it != active }
                    if (active != null && other != null) {
                        TwoCalls(active, other)
                    } else {
                        TwoCalls(calls[0], calls[1])
                    }
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
                if (state.active.state == Call.STATE_ACTIVE) {
                    state.active.hold()
                } else {
                    state.held.hold()
                }
            }
        }

        fun merge() {
            val state = getPhoneState()
            if (state is TwoCalls) {
                state.active.conference(state.held)
            }
        }

        fun setMuted(muted: Boolean) {
            inCallService?.setMuted(muted)
        }

        fun setSpeaker(on: Boolean) {
            isSpeakerOn = on
            val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_WIRED_OR_EARPIECE
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