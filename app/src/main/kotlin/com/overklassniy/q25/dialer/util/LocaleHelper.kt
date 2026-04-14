package com.overklassniy.q25.dialer.util

import android.content.Context
import android.content.res.Configuration
import com.overklassniy.q25.dialer.data.PreferencesManager
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context) {
        val prefs = PreferencesManager(context)
        val lang = prefs.language
        if (lang == PreferencesManager.LANG_SYSTEM) return

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun wrap(context: Context): Context {
        val prefs = PreferencesManager(context)
        val lang = prefs.language
        if (lang == PreferencesManager.LANG_SYSTEM) return context

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}