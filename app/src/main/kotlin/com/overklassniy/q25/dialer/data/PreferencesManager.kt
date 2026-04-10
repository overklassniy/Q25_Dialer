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

        const val THEME_SYSTEM = "system"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"

        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_RU = "ru"
    }
}