package com.overklassniy.q25.dialer.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.overklassniy.q25.dialer.MainActivity
import com.overklassniy.q25.dialer.call.CallActivity
import com.overklassniy.q25.dialer.call.CallManager
import com.overklassniy.q25.dialer.call.NoCall
import com.overklassniy.q25.dialer.call.SingleCall
import com.overklassniy.q25.dialer.data.PreferencesManager

@SuppressLint("AccessibilityPolicy")
class QwertyAccessibilityService : AccessibilityService() {

    private val prefs by lazy { PreferencesManager(this) }

    // Long-press detection for speed dial
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressKeyCode: Int = -1
    private var longPressConsumed = false
    private val LONG_PRESS_TIMEOUT = 500L // ms

    // Held-backspace continuous deletion
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceRunnable: Runnable? = null
    private var backspaceKeyDown = false
    private val BACKSPACE_INITIAL_DELAY = 500L
    private val BACKSPACE_REPEAT_INTERVAL = 50L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used – we only need key event filtering
    }

    override fun onInterrupt() {
        // Not used
    }

    @Suppress("DEPRECATION")
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Handle ACTION_UP: cancel timers if key released
        if (event.action == KeyEvent.ACTION_UP) {
            val keyCode = event.keyCode
            if (keyCode == longPressKeyCode && !longPressConsumed) {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                longPressRunnable = null
                longPressKeyCode = -1
            }
            if (keyCode == KeyEvent.KEYCODE_DEL && backspaceKeyDown) {
                backspaceRunnable?.let { backspaceHandler.removeCallbacks(it) }
                backspaceRunnable = null
                backspaceKeyDown = false
            }
            return super.onKeyEvent(event)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return super.onKeyEvent(event)
        val keyCode = event.keyCode

        // Active call: DTMF + call control
        if (CallManager.getPhoneState() !is NoCall) {
            mapKeyToDialpad(keyCode)?.let {
                CallManager.playDtmf(it)
                CallActivity.onDtmfChar?.invoke(it)
                return true
            }
            when (keyCode) {
                KeyEvent.KEYCODE_HOME -> {
                    if (!prefs.disableHomeKeyHangup) {
                        val s = (CallManager.getPhoneState() as? SingleCall)?.call?.state
                        if (s == Call.STATE_RINGING) CallManager.reject() else CallManager.hangup()
                        return true
                    }
                }
                KeyEvent.KEYCODE_CALL -> {
                    val s = (CallManager.getPhoneState() as? SingleCall)?.call?.state
                    if (s == Call.STATE_RINGING) { CallManager.accept(); return true }
                }
                KeyEvent.KEYCODE_ENDCALL -> { CallManager.hangup(); return true }
            }
            return super.onKeyEvent(event)
        }

        // App in foreground: send chars via callbacks (bypasses IME)
        if (MainActivity.isInForeground) {
            val screen = MainActivity.currentScreen
            // Recents: intercept QWERTY->dialpad and backspace (bypass IME for layout-independent input)
            if (screen == SCREEN_RECENTS) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (!backspaceKeyDown) {
                        backspaceKeyDown = true
                        MainActivity.onAccessibilityBackspace?.invoke()
                        backspaceRunnable = object : Runnable {
                            override fun run() {
                                MainActivity.onAccessibilityBackspace?.invoke()
                                backspaceHandler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL)
                            }
                        }
                        backspaceHandler.postDelayed(backspaceRunnable!!, BACKSPACE_INITIAL_DELAY)
                    }
                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    MainActivity.onEnterPressed?.invoke(); return true
                }
                if (keyCode == KeyEvent.KEYCODE_CALL) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra(EXTRA_MAKE_CALL, true)
                    }); return true
                }
                mapKeyToDialpad(keyCode)?.let { dialChar ->
                    if (event.repeatCount == 0) {
                        // First press: schedule long-press detection
                        longPressKeyCode = keyCode
                        longPressConsumed = false
                        longPressRunnable = Runnable {
                            longPressConsumed = true
                            if (dialChar == '0') {
                                MainActivity.onAccessibilityBackspace?.invoke()
                                MainActivity.onAccessibilityDialpadChar?.invoke('+')
                            } else {
                                val slot = dialChar.digitToIntOrNull()
                                if (slot != null && slot in 2..9) {
                                    MainActivity.onAccessibilityBackspace?.invoke()
                                    MainActivity.onSpeedDial?.invoke(slot)
                                }
                            }
                        }
                        longPressHandler.postDelayed(longPressRunnable!!, LONG_PRESS_TIMEOUT)
                        // Add digit immediately (will be removed on long-press)
                        MainActivity.onAccessibilityDialpadChar?.invoke(dialChar)
                    }
                    // repeatCount > 0: just consume (key is held, timer handles it)
                    return true
                }
            }
            // Contacts: intercept ENTER for item activation (bypass IME)
            if (screen == SCREEN_CONTACTS) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    MainActivity.onEnterPressed?.invoke(); return true
                }
            }
            // Other keys on contacts/other screens: let IME handle input (proper language support)
            return false
        }

        // Not in foreground: only CALL special key (HOME is left for system)
        when (keyCode) {
            KeyEvent.KEYCODE_CALL -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(EXTRA_MAKE_CALL, true)
                }); return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun mapKeyToDialpad(keyCode: Int): Char? {
        return when (keyCode) {
            // QWERTY layout mapping
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
            // Standard numeric keys
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

    companion object {
        const val EXTRA_MAKE_CALL = "make_call"
        // Must match NavRoutes values
        private const val SCREEN_RECENTS = "recents"
        private const val SCREEN_CONTACTS = "contacts"
    }
}