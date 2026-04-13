package com.overklassniy.q25.dialer.call

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.overklassniy.q25.dialer.data.PreferencesManager
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.ui.screens.InCallScreen
import com.overklassniy.q25.dialer.ui.theme.Q25DialerTheme

class CallActivity : ComponentActivity() {

    companion object {
        fun getStartIntent(context: Context): Intent {
            return Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        }

        // Callback invoked by QwertyAccessibilityService for visual DTMF feedback
        @Volatile
        var onDtmfChar: ((Char) -> Unit)? = null
    }

    private var callerName by mutableStateOf("")
    private var callerNumber by mutableStateOf("")
    private var callerPhotoUri by mutableStateOf<String?>(null)
    private var callerFullPhotoUri by mutableStateOf<String?>(null)
    private var isUnknownCaller by mutableStateOf(false)
    private var callStatusText by mutableStateOf("")
    private var isIncoming by mutableStateOf(false)
    private var isMuted by mutableStateOf(false)
    private var isSpeakerOn by mutableStateOf(false)
    private var isOnHold by mutableStateOf(false)
    private var currentAudioRoute by mutableIntStateOf(CallAudioState.ROUTE_EARPIECE)
    private var supportedAudioRoutes by mutableIntStateOf(CallAudioState.ROUTE_EARPIECE)
    private var isBluetoothAvailable by mutableStateOf(false)
    private var callNotes by mutableStateOf("")

    private var heldCallerName by mutableStateOf<String?>(null)
    private var hasHeldCall by mutableStateOf(false)

    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var callDuration by mutableIntStateOf(0)
    private var isCallActive by mutableStateOf(false)
    private var showDialpad by mutableStateOf(false)
    private var dtmfInput by mutableStateOf("")

    private val durationHandler = Handler(Looper.getMainLooper())
    private val durationRunnable = object : Runnable {
        override fun run() {
            callDuration++
            callStatusText = formatDuration(callDuration)
            durationHandler.postDelayed(this, 1000)
        }
    }

    private val contactsRepository by lazy { ContactsRepository(this) }
    private val prefs by lazy { PreferencesManager(this) }

    private val callCallback = object : CallManagerListener {
        override fun onStateChanged() {
            runOnUiThread { updateCallState() }
        }

        override fun onPrimaryCallChanged(call: Call) {
            runOnUiThread {
                updateCallInfo(call)
                updateCallState()
            }
        }

        override fun onAudioStateChanged(audioState: CallAudioState) {
            runOnUiThread {
                isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER
                isMuted = audioState.isMuted
                currentAudioRoute = audioState.route
                supportedAudioRoutes = audioState.supportedRouteMask
                isBluetoothAvailable = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        addLockScreenFlags()
        super.onCreate(savedInstanceState)

        if (CallManager.getPhoneState() == NoCall) {
            finish()
            return
        }

        try {
            val audioManager = getSystemService(AudioManager::class.java)
            audioManager?.mode = AudioManager.MODE_IN_CALL
        } catch (_: Exception) { }

        // Acquire proximity wake lock so screen turns off when near ear
        acquireProximityWakeLock()

        CallManager.addListener(callCallback)

        val primaryCall = CallManager.getPrimaryCall()
        if (primaryCall != null) {
            updateCallInfo(primaryCall)
            updateCallState()
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = remember(prefs.themeMode) {
                when (prefs.themeMode) {
                    PreferencesManager.THEME_DARK -> true
                    PreferencesManager.THEME_LIGHT -> false
                    else -> systemDark
                }
            }
            Q25DialerTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                InCallScreen(
                    isDarkTheme = isDarkTheme,
                    callerName = callerName,
                    callerNumber = callerNumber,
                    callerPhotoUri = callerPhotoUri,
                    callerFullPhotoUri = callerFullPhotoUri,
                    isUnknownCaller = isUnknownCaller,
                    statusText = callStatusText,
                    isIncoming = isIncoming,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isOnHold = isOnHold,
                    onToggleMute = {
                        isMuted = !isMuted
                        CallManager.setMuted(isMuted)
                    },
                    onToggleSpeaker = {
                        isSpeakerOn = !isSpeakerOn
                        CallManager.setSpeaker(isSpeakerOn)
                    },
                    currentAudioRoute = currentAudioRoute,
                    isBluetoothAvailable = isBluetoothAvailable,
                    onSetAudioRoute = { route ->
                        CallManager.setAudioRoute(route)
                    },
                    callNotes = callNotes,
                    onCallNotesChanged = { notes ->
                        callNotes = notes
                        if (callerNumber.isNotEmpty()) {
                            prefs.setCallNote(callerNumber, notes)
                        }
                    },
                    showFullscreenAvatar = prefs.fullscreenAvatar,
                    onAddCall = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    hasHeldCall = hasHeldCall,
                    heldCallerName = heldCallerName,
                    onSwapCall = { CallManager.swap() },
                    onMergeCall = { CallManager.merge() },
                    onToggleHold = {
                        if (isOnHold) CallManager.unhold() else CallManager.hold()
                        isOnHold = !isOnHold
                    },
                    onEndCall = { CallManager.hangup() },
                    onAcceptCall = { CallManager.accept() },
                    onDeclineCall = { CallManager.reject() },
                    onMessageClick = {
                        val number = callerNumber
                        CallManager.reject()
                        if (number.isNotEmpty()) {
                            try {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                                smsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(smsIntent)
                            } catch (_: Exception) { }
                        }
                    },
                    showDialpad = showDialpad,
                    dtmfInput = dtmfInput,
                    onDtmf = { char ->
                        CallManager.playDtmf(char)
                        dtmfInput += char
                        showDialpad = true
                    },
                    onToggleDialpad = { showDialpad = !showDialpad },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        onDtmfChar = { char ->
            dtmfInput += char
            showDialpad = true
        }
    }

    override fun onPause() {
        super.onPause()
        onDtmfChar = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val dialpadChar = mapKeyToDialpad(keyCode)
        if (dialpadChar != null && CallManager.getPhoneState() !is NoCall) {
            CallManager.playDtmf(dialpadChar)
            dtmfInput += dialpadChar
            showDialpad = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun mapKeyToDialpad(keyCode: Int): Char? {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> '#'
            KeyEvent.KEYCODE_A -> '*'
            KeyEvent.KEYCODE_W -> '1'
            KeyEvent.KEYCODE_E -> '2'
            KeyEvent.KEYCODE_R -> '3'
            KeyEvent.KEYCODE_S -> '4'
            KeyEvent.KEYCODE_D -> '5'
            KeyEvent.KEYCODE_F -> '6'
            KeyEvent.KEYCODE_Z -> '7'
            KeyEvent.KEYCODE_X -> '8'
            KeyEvent.KEYCODE_C -> '9'
            KeyEvent.KEYCODE_O -> '+'
            KeyEvent.KEYCODE_0 -> '0'
            KeyEvent.KEYCODE_1 -> '1'
            KeyEvent.KEYCODE_2 -> '2'
            KeyEvent.KEYCODE_3 -> '3'
            KeyEvent.KEYCODE_4 -> '4'
            KeyEvent.KEYCODE_5 -> '5'
            KeyEvent.KEYCODE_6 -> '6'
            KeyEvent.KEYCODE_7 -> '7'
            KeyEvent.KEYCODE_8 -> '8'
            KeyEvent.KEYCODE_9 -> '9'
            KeyEvent.KEYCODE_STAR -> '*'
            KeyEvent.KEYCODE_POUND -> '#'
            KeyEvent.KEYCODE_PLUS -> '+'
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CallManager.removeListener(callCallback)
        durationHandler.removeCallbacks(durationRunnable)
        releaseProximityWakeLock()
    }

    @Suppress("DEPRECATION")
    private fun acquireProximityWakeLock() {
        try {
            val pm = getSystemService(PowerManager::class.java) ?: return
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = pm.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "Q25Dialer:ProximityWakeLock"
                ).apply { acquire() }
            }
        } catch (_: Exception) { }
    }

    private fun releaseProximityWakeLock() {
        try {
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock?.release()
            }
        } catch (_: Exception) { }
        proximityWakeLock = null
    }

    private fun updateCallInfo(call: Call) {
        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        callerNumber = number

        // Try to find contact by number
        val contact = try {
            contactsRepository.getContactByNumber(number)
        } catch (_: Exception) { null }

        if (contact != null) {
            callerName = contact.getDisplayName()
            callerPhotoUri = contact.photoUri
            callerFullPhotoUri = contact.photoFullUri ?: contact.photoUri
            isUnknownCaller = false
        } else {
            callerName = number.ifEmpty { getString(com.overklassniy.q25.dialer.R.string.unknown_caller) }
            callerPhotoUri = null
            callerFullPhotoUri = null
            isUnknownCaller = true
        }

        // Load saved notes for this number
        if (number.isNotEmpty()) {
            callNotes = prefs.getCallNote(number)
        }
    }

    private fun updateCallState() {
        val state = CallManager.getPhoneState()
        when (state) {
            is NoCall -> {
                durationHandler.removeCallbacks(durationRunnable)
                callStatusText = getString(com.overklassniy.q25.dialer.R.string.call_ended)
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
            }
            is SingleCall -> {
                hasHeldCall = false
                heldCallerName = null
                updateCallInfo(state.call)
                val callState = state.call.state
                isIncoming = callState == Call.STATE_RINGING
                isOnHold = callState == Call.STATE_HOLDING

                callStatusText = when (callState) {
                    Call.STATE_RINGING -> getString(com.overklassniy.q25.dialer.R.string.ringing)
                    Call.STATE_DIALING, Call.STATE_CONNECTING -> getString(com.overklassniy.q25.dialer.R.string.dialing)
                    Call.STATE_ACTIVE -> {
                        if (!isCallActive) {
                            isCallActive = true
                            callDuration = 0
                            durationHandler.post(durationRunnable)
                        }
                        formatDuration(callDuration)
                    }
                    Call.STATE_HOLDING -> getString(com.overklassniy.q25.dialer.R.string.call_on_hold)
                    Call.STATE_DISCONNECTING -> getString(com.overklassniy.q25.dialer.R.string.call_ended)
                    else -> ""
                }
            }
            is TwoCalls -> {
                isIncoming = false
                hasHeldCall = true

                // Update active call info
                val activeCall = state.active
                updateCallInfo(activeCall)

                // Get held call contact name
                val heldHandle = state.held.details?.handle
                val heldNumber = heldHandle?.schemeSpecificPart ?: ""
                val heldContact = try {
                    contactsRepository.getContactByNumber(heldNumber)
                } catch (_: Exception) { null }
                heldCallerName = heldContact?.getDisplayName() ?: heldNumber.ifEmpty {
                    getString(com.overklassniy.q25.dialer.R.string.unknown_caller)
                }

                val activeState = activeCall.state
                callStatusText = when (activeState) {
                    Call.STATE_ACTIVE -> {
                        if (!isCallActive) {
                            isCallActive = true
                            callDuration = 0
                            durationHandler.post(durationRunnable)
                        }
                        formatDuration(callDuration)
                    }
                    Call.STATE_DIALING, Call.STATE_CONNECTING -> getString(com.overklassniy.q25.dialer.R.string.dialing)
                    else -> ""
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun addLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}