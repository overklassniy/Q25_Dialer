package com.overklassniy.q25.dialer

import android.app.Application
import com.overklassniy.q25.dialer.data.PreferencesManager
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        val prefs = PreferencesManager(this)
        if (prefs.sendAnonymousStats) {
            initSentry()
        }
    }

    companion object {
        lateinit var instance: App
            private set

        private fun decodeDsn(): String {
            val encoded = BuildConfig.SENTRY_DSN_ENCODED
            if (encoded.isEmpty()) return ""
            val key = "com.overklassniy.q25.dialer"
            val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            return bytes.mapIndexed { i, b ->
                (b.toInt() xor key[i % key.length].code).toChar()
            }.joinToString("")
        }

        private fun initSentry() {
            val dsn = decodeDsn()
            if (dsn.isEmpty()) return
            SentryAndroid.init(instance) { options ->
                options.dsn = dsn
                options.tracesSampleRate = 0.01
                options.isEnableAutoSessionTracking = false
                options.release = "${instance.packageName}@${BuildConfig.VERSION_NAME}"
                options.environment = if (@Suppress("SENSELESS_COMPARISON") BuildConfig.DEBUG) "debug" else "production"
            }
        }

        fun updateSentryState(enabled: Boolean) {
            if (enabled) {
                initSentry()
            } else {
                Sentry.close()
            }
        }
    }
}