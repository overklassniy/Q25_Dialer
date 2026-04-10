package com.overklassniy.q25.dialer.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.App
import com.overklassniy.q25.dialer.BuildConfig
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.PreferencesManager
import io.sentry.Sentry

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onThemeModeChanged: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var hideVirtualDialpad by remember { mutableStateOf(prefs.hideVirtualDialpad) }
    var expandedBottomNav by remember { mutableStateOf(prefs.expandedBottomNav) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var disableVerticalArrows by remember { mutableStateOf(prefs.disableVerticalArrows) }
    var disableHorizontalArrows by remember { mutableStateOf(prefs.disableHorizontalArrows) }
    var sendAnonymousStats by remember { mutableStateOf(prefs.sendAnonymousStats) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showHistoryLimitDialog by remember { mutableStateOf(false) }
    var callHistoryLimit by remember { mutableStateOf(prefs.callHistoryLimit) }

    // GitHub version check
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(true) }
    val currentVersion = remember { getAppVersion(context) }

    LaunchedEffect(Unit) {
        isCheckingUpdate = true
        latestVersion = fetchLatestGitHubVersion()
        isCheckingUpdate = false
    }

    val hasUpdate = latestVersion != null && latestVersion != currentVersion &&
            latestVersion!!.removePrefix("v") != currentVersion

    if (showOnboarding) {
        OnboardingScreen(
            onComplete = { showOnboarding = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Section: Dialpad
        SettingsSectionHeader(stringResource(R.string.settings_dialpad_section))

        SettingsSwitchItem(
            title = stringResource(R.string.settings_hide_virtual_dialpad),
            subtitle = stringResource(R.string.settings_hide_virtual_dialpad_desc),
            checked = hideVirtualDialpad,
            onCheckedChange = {
                hideVirtualDialpad = it
                prefs.hideVirtualDialpad = it
            },
        )

        SettingsClickItem(
            title = stringResource(R.string.onboarding_setup),
            subtitle = stringResource(R.string.onboarding_default_dialer_desc),
            onClick = { showOnboarding = true },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Interface
        SettingsSectionHeader(stringResource(R.string.settings_interface_section))

        SettingsSwitchItem(
            title = stringResource(R.string.settings_expanded_bottom_nav),
            subtitle = stringResource(R.string.settings_expanded_bottom_nav_desc),
            checked = expandedBottomNav,
            onCheckedChange = {
                expandedBottomNav = it
                prefs.expandedBottomNav = it
            },
        )

        SettingsClickItem(
            title = stringResource(R.string.settings_theme),
            subtitle = getThemeDisplayName(themeMode),
            onClick = { showThemeDialog = true },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Keyboard
        SettingsSectionHeader(stringResource(R.string.settings_keyboard_section))

        SettingsSwitchItem(
            title = stringResource(R.string.settings_disable_vertical_arrows),
            subtitle = stringResource(R.string.settings_disable_vertical_arrows_desc),
            checked = disableVerticalArrows,
            onCheckedChange = {
                disableVerticalArrows = it
                prefs.disableVerticalArrows = it
            },
        )

        SettingsSwitchItem(
            title = stringResource(R.string.settings_disable_horizontal_arrows),
            subtitle = stringResource(R.string.settings_disable_horizontal_arrows_desc),
            checked = disableHorizontalArrows,
            onCheckedChange = {
                disableHorizontalArrows = it
                prefs.disableHorizontalArrows = it
            },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Call history
        SettingsSectionHeader(stringResource(R.string.settings_call_history_section))

        SettingsClickItem(
            title = stringResource(R.string.settings_call_history_limit),
            subtitle = stringResource(R.string.settings_call_history_limit_desc, callHistoryLimit),
            onClick = { showHistoryLimitDialog = true },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Language
        SettingsSectionHeader(stringResource(R.string.settings_language_section))

        SettingsClickItem(
            title = stringResource(R.string.settings_language),
            subtitle = getLanguageDisplayName(prefs.language),
            onClick = { showLanguageDialog = true },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Privacy
        SettingsSectionHeader(stringResource(R.string.settings_privacy_section))

        SettingsSwitchItem(
            title = stringResource(R.string.settings_send_anonymous_stats),
            subtitle = stringResource(R.string.settings_send_anonymous_stats_desc),
            checked = sendAnonymousStats,
            onCheckedChange = {
                sendAnonymousStats = it
                prefs.sendAnonymousStats = it
                App.updateSentryState(it)
            },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: About
        SettingsSectionHeader(stringResource(R.string.settings_about_section))

        SettingsClickItem(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.settings_version, currentVersion),
            onClick = { },
        )

        if (isCheckingUpdate) {
            SettingsClickItem(
                title = stringResource(R.string.settings_checking_updates),
                subtitle = "",
                onClick = { },
            )
        } else if (hasUpdate) {
            SettingsClickItem(
                title = stringResource(R.string.settings_open_github),
                subtitle = stringResource(R.string.settings_update_available, latestVersion!!.removePrefix("v")),
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/overklassniy/Q25_Dialer/releases/latest"),
                    )
                    context.startActivity(intent)
                },
            )
        }

        // Section: Debug (only in debug builds)
        if (BuildConfig.DEBUG) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader(stringResource(R.string.settings_debug_section))

            SettingsClickItem(
                title = stringResource(R.string.settings_debug_test_sentry),
                subtitle = stringResource(R.string.settings_debug_test_sentry_desc),
                onClick = {
                    try {
                        Sentry.captureMessage("Test event from Q25 Dialer debug menu")
                        Toast.makeText(context, R.string.settings_debug_sentry_sent, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Sentry error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
            )

            SettingsClickItem(
                title = stringResource(R.string.settings_debug_test_crash),
                subtitle = stringResource(R.string.settings_debug_test_crash_desc),
                onClick = {
                    throw RuntimeException("Test crash from Q25 Dialer debug menu")
                },
            )
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = prefs.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                prefs.language = lang
                showLanguageDialog = false
                // Restart activity to apply language
                val intent = (context as? android.app.Activity)?.intent
                (context as? android.app.Activity)?.finish()
                if (intent != null) context.startActivity(intent)
            },
        )
    }

    if (showHistoryLimitDialog) {
        CallHistoryLimitDialog(
            currentLimit = callHistoryLimit,
            onDismiss = { showHistoryLimitDialog = false },
            onSelect = { limit ->
                prefs.callHistoryLimit = limit
                callHistoryLimit = limit
                showHistoryLimitDialog = false
            },
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                prefs.themeMode = mode
                themeMode = mode
                showThemeDialog = false
                onThemeModeChanged(mode)
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    // Uppercase section headers with primary color
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        PreferencesManager.LANG_SYSTEM to stringResource(R.string.language_system),
        PreferencesManager.LANG_EN to stringResource(R.string.language_en),
        PreferencesManager.LANG_RU to stringResource(R.string.language_ru),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                options.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (code == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                            color = if (code == currentLanguage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun getLanguageDisplayName(code: String): String {
    return when (code) {
        PreferencesManager.LANG_EN -> stringResource(R.string.language_en)
        PreferencesManager.LANG_RU -> stringResource(R.string.language_ru)
        else -> stringResource(R.string.language_system)
    }
}

@Composable
private fun CallHistoryLimitDialog(
    currentLimit: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(15, 30, 45)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_call_history_limit)) },
        text = {
            Column {
                options.forEach { limit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(limit) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$limit",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (limit == currentLimit) FontWeight.Bold else FontWeight.Normal,
                            color = if (limit == currentLimit) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ThemePickerDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        PreferencesManager.THEME_SYSTEM to stringResource(R.string.theme_system),
        PreferencesManager.THEME_DARK to stringResource(R.string.theme_dark),
        PreferencesManager.THEME_LIGHT to stringResource(R.string.theme_light),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun getThemeDisplayName(mode: String): String {
    return when (mode) {
        PreferencesManager.THEME_DARK -> stringResource(R.string.theme_dark)
        PreferencesManager.THEME_LIGHT -> stringResource(R.string.theme_light)
        else -> stringResource(R.string.theme_system)
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}

private suspend fun fetchLatestGitHubVersion(): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url = java.net.URL("https://api.github.com/repos/overklassniy/Q25_Dialer/releases/latest")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (connection.responseCode == 200) {
            val body = connection.inputStream.bufferedReader().readText()
            // Simple JSON parsing for "tag_name":"vX.Y.Z"
            val regex = """"tag_name"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(body)?.groupValues?.get(1)
        } else null
    } catch (_: Exception) {
        null
    }
}