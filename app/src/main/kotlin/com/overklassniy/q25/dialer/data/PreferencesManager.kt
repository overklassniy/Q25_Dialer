package com.overklassniy.q25.dialer.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var hideVirtualDialpad: Boolean
        get() = prefs.getBoolean(KEY_HIDE_VIRTUAL_DIALPAD, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_VIRTUAL_DIALPAD, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var callHistoryLimit: Int
        get() = prefs.getInt(KEY_CALL_HISTORY_LIMIT, 15)
        set(value) = prefs.edit().putInt(KEY_CALL_HISTORY_LIMIT, value).apply()

    var sendAnonymousStats: Boolean
        get() = prefs.getBoolean(KEY_SEND_ANONYMOUS_STATS, true)
        set(value) = prefs.edit().putBoolean(KEY_SEND_ANONYMOUS_STATS, value).apply()

    var expandedBottomNav: Boolean
        get() = prefs.getBoolean(KEY_EXPANDED_BOTTOM_NAV, false)
        set(value) = prefs.edit().putBoolean(KEY_EXPANDED_BOTTOM_NAV, value).apply()

    var disableVerticalArrows: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_VERTICAL_ARROWS, false)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_VERTICAL_ARROWS, value).apply()

    var disableHorizontalArrows: Boolean
        get() = prefs.getBoolean(KEY_DISABLE_HORIZONTAL_ARROWS, false)
        set(value) = prefs.edit().putBoolean(KEY_DISABLE_HORIZONTAL_ARROWS, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    fun getCustomColor(key: String): Long? {
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }

    fun setCustomColor(key: String, colorValue: Long) {
        prefs.edit().putLong(key, colorValue).apply()
    }

    fun removeCustomColor(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun resetCustomColors() {
        val editor = prefs.edit()
        ALL_COLOR_KEYS.forEach { editor.remove(it) }
        editor.apply()
    }

    fun hasCustomColors(): Boolean {
        return ALL_COLOR_KEYS.any { prefs.contains(it) }
    }

    var fullscreenAvatar: Boolean
        get() = prefs.getBoolean(KEY_FULLSCREEN_AVATAR, false)
        set(value) = prefs.edit().putBoolean(KEY_FULLSCREEN_AVATAR, value).apply()

    fun getCallNote(phoneNumber: String): String {
        return prefs.getString("$KEY_CALL_NOTE_PREFIX$phoneNumber", "") ?: ""
    }

    fun setCallNote(phoneNumber: String, note: String) {
        prefs.edit().putString("$KEY_CALL_NOTE_PREFIX$phoneNumber", note).apply()
    }

    companion object {
        private const val PREFS_NAME = "q25_dialer_prefs"
        private const val KEY_HIDE_VIRTUAL_DIALPAD = "hide_virtual_dialpad"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_CALL_HISTORY_LIMIT = "call_history_limit"
        private const val KEY_SEND_ANONYMOUS_STATS = "send_anonymous_stats"
        private const val KEY_EXPANDED_BOTTOM_NAV = "expanded_bottom_nav"
        private const val KEY_DISABLE_VERTICAL_ARROWS = "disable_vertical_arrows"
        private const val KEY_DISABLE_HORIZONTAL_ARROWS = "disable_horizontal_arrows"
        private const val KEY_THEME_MODE = "theme_mode"

        const val KEY_COLOR_PRIMARY = "custom_color_primary"
        const val KEY_COLOR_SECONDARY = "custom_color_secondary"
        const val KEY_COLOR_BACKGROUND = "custom_color_background"
        const val KEY_COLOR_SURFACE = "custom_color_surface"
        const val KEY_COLOR_ON_PRIMARY = "custom_color_on_primary"
        const val KEY_COLOR_ON_BACKGROUND = "custom_color_on_background"
        const val KEY_COLOR_ON_SURFACE = "custom_color_on_surface"
        const val KEY_COLOR_SURFACE_VARIANT = "custom_color_surface_variant"
        const val KEY_COLOR_ON_SURFACE_VARIANT = "custom_color_on_surface_variant"

        // Incoming call screen colors
        const val KEY_COLOR_INCOMING_CALL_BACKGROUND = "custom_color_incoming_call_background"
        const val KEY_COLOR_INCOMING_CALL_TEXT = "custom_color_incoming_call_text"
        const val KEY_COLOR_INCOMING_CALL_DECLINE_BUTTON = "custom_color_incoming_call_decline_button"
        const val KEY_COLOR_INCOMING_CALL_ACCEPT_BUTTON = "custom_color_incoming_call_accept_button"
        const val KEY_COLOR_INCOMING_CALL_MESSAGE_BUTTON = "custom_color_incoming_call_message_button"

        // Ongoing call screen colors
        const val KEY_COLOR_ONGOING_CALL_BACKGROUND = "custom_color_ongoing_call_background"
        const val KEY_COLOR_ONGOING_CALL_TEXT = "custom_color_ongoing_call_text"
        const val KEY_COLOR_ONGOING_CALL_END_BUTTON = "custom_color_ongoing_call_end_button"
        const val KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_BG = "custom_color_ongoing_call_control_button_bg"
        const val KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_ICON = "custom_color_ongoing_call_control_button_icon"
        const val KEY_COLOR_ONGOING_CALL_DIALPAD_BG = "custom_color_ongoing_call_dialpad_bg"
        const val KEY_COLOR_ONGOING_CALL_DIALPAD_TEXT = "custom_color_ongoing_call_dialpad_text"
        const val KEY_COLOR_ONGOING_CALL_HOLD_BAR = "custom_color_ongoing_call_hold_bar"

        private const val KEY_FULLSCREEN_AVATAR = "fullscreen_avatar"
        private const val KEY_CALL_NOTE_PREFIX = "call_note_"

        val ALL_COLOR_KEYS = listOf(
            KEY_COLOR_PRIMARY, KEY_COLOR_SECONDARY, KEY_COLOR_BACKGROUND, KEY_COLOR_SURFACE,
            KEY_COLOR_ON_PRIMARY, KEY_COLOR_ON_BACKGROUND, KEY_COLOR_ON_SURFACE,
            KEY_COLOR_SURFACE_VARIANT, KEY_COLOR_ON_SURFACE_VARIANT,
            // Incoming call colors
            KEY_COLOR_INCOMING_CALL_BACKGROUND, KEY_COLOR_INCOMING_CALL_TEXT,
            KEY_COLOR_INCOMING_CALL_DECLINE_BUTTON, KEY_COLOR_INCOMING_CALL_ACCEPT_BUTTON,
            KEY_COLOR_INCOMING_CALL_MESSAGE_BUTTON,
            // Ongoing call colors
            KEY_COLOR_ONGOING_CALL_BACKGROUND, KEY_COLOR_ONGOING_CALL_TEXT,
            KEY_COLOR_ONGOING_CALL_END_BUTTON, KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_BG,
            KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_ICON, KEY_COLOR_ONGOING_CALL_DIALPAD_BG,
            KEY_COLOR_ONGOING_CALL_DIALPAD_TEXT, KEY_COLOR_ONGOING_CALL_HOLD_BAR,
        )

        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"

        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_RU = "ru"
    }
}