package com.overklassniy.q25.dialer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.telecom.Call
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.overklassniy.q25.dialer.MainActivity
import com.overklassniy.q25.dialer.call.CallActivity
import com.overklassniy.q25.dialer.call.CallManager
import com.overklassniy.q25.dialer.call.NoCall
import com.overklassniy.q25.dialer.call.SingleCall
import com.overklassniy.q25.dialer.data.PreferencesManager

class QwertyAccessibilityService : AccessibilityService() {

    private val prefs by lazy { PreferencesManager(this) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used – we only need key event filtering
    }

    override fun onInterrupt() {
        // Not used
    }

    @Suppress("DEPRECATION")
    override fun onKeyEvent(event: KeyEvent): Boolean {
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
                    MainActivity.onAccessibilityBackspace?.invoke(); return true
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
                mapKeyToDialpad(keyCode)?.let { MainActivity.onAccessibilityDialpadChar?.invoke(it); return true }
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

    private fun mapKeyToLetter(keyCode: Int): Char? {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> 'a'; KeyEvent.KEYCODE_B -> 'b'
            KeyEvent.KEYCODE_C -> 'c'; KeyEvent.KEYCODE_D -> 'd'
            KeyEvent.KEYCODE_E -> 'e'; KeyEvent.KEYCODE_F -> 'f'
            KeyEvent.KEYCODE_G -> 'g'; KeyEvent.KEYCODE_H -> 'h'
            KeyEvent.KEYCODE_I -> 'i'; KeyEvent.KEYCODE_J -> 'j'
            KeyEvent.KEYCODE_K -> 'k'; KeyEvent.KEYCODE_L -> 'l'
            KeyEvent.KEYCODE_M -> 'm'; KeyEvent.KEYCODE_N -> 'n'
            KeyEvent.KEYCODE_O -> 'o'; KeyEvent.KEYCODE_P -> 'p'
            KeyEvent.KEYCODE_Q -> 'q'; KeyEvent.KEYCODE_R -> 'r'
            KeyEvent.KEYCODE_S -> 's'; KeyEvent.KEYCODE_T -> 't'
            KeyEvent.KEYCODE_U -> 'u'; KeyEvent.KEYCODE_V -> 'v'
            KeyEvent.KEYCODE_W -> 'w'; KeyEvent.KEYCODE_X -> 'x'
            KeyEvent.KEYCODE_Y -> 'y'; KeyEvent.KEYCODE_Z -> 'z'
            KeyEvent.KEYCODE_0 -> '0'; KeyEvent.KEYCODE_1 -> '1'
            KeyEvent.KEYCODE_2 -> '2'; KeyEvent.KEYCODE_3 -> '3'
            KeyEvent.KEYCODE_4 -> '4'; KeyEvent.KEYCODE_5 -> '5'
            KeyEvent.KEYCODE_6 -> '6'; KeyEvent.KEYCODE_7 -> '7'
            KeyEvent.KEYCODE_8 -> '8'; KeyEvent.KEYCODE_9 -> '9'
            KeyEvent.KEYCODE_SPACE -> ' '
            KeyEvent.KEYCODE_MINUS -> '-'
            KeyEvent.KEYCODE_PERIOD -> '.'
            else -> null
        }
    }

    companion object {
        const val EXTRA_DIALPAD_CHAR = "dialpad_char"
        const val EXTRA_OPEN_DIALPAD = "open_dialpad"
        const val EXTRA_MAKE_CALL = "make_call"
        // Must match NavRoutes values
        private const val SCREEN_RECENTS = "recents"
        private const val SCREEN_CONTACTS = "contacts"
    }
}