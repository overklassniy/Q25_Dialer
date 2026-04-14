package com.overklassniy.q25.dialer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.App
import com.overklassniy.q25.dialer.BuildConfig
import com.overklassniy.q25.dialer.MainActivity
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.PreferencesManager
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    highlightedIndex: Int = -1,
    activateTrigger: Int = 0,
    onItemCount: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onThemeModeChanged: (String) -> Unit = {},
    onColorsChanged: () -> Unit = {},
    onNavigateToColorSettings: () -> Unit = {},
    onNavigateToSpeedDialSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var hideVirtualDialpad by remember { mutableStateOf(prefs.hideVirtualDialpad) }
    var expandedBottomNav by remember { mutableStateOf(prefs.expandedBottomNav) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var themeDialogHighlightedIndex by remember { mutableIntStateOf(
        when (prefs.themeMode) {
            PreferencesManager.THEME_DARK -> 1
            PreferencesManager.THEME_LIGHT -> 2
            else -> 0
        }
    ) }
    var themeDialogActivateTrigger by remember { mutableIntStateOf(0) }
    var disableVerticalArrows by remember { mutableStateOf(prefs.disableVerticalArrows) }
    var disableHorizontalArrows by remember { mutableStateOf(prefs.disableHorizontalArrows) }
    var disableHomeKeyHangup by remember { mutableStateOf(prefs.disableHomeKeyHangup) }
    var sendAnonymousStats by remember { mutableStateOf(prefs.sendAnonymousStats) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showHistoryLimitDialog by remember { mutableStateOf(false) }
    var callHistoryLimit by remember { mutableStateOf(prefs.callHistoryLimit) }
    var showChangelogDialog by remember { mutableStateOf(false) }

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

    val scrollState = rememberScrollState()

    // Reset highlighted index to current theme when dialog opens
    LaunchedEffect(showThemeDialog) {
        if (showThemeDialog) {
            themeDialogHighlightedIndex = when (prefs.themeMode) {
                PreferencesManager.THEME_DARK -> 1
                PreferencesManager.THEME_LIGHT -> 2
                else -> 0
            }
        }
    }

    // Intercept key events when theme dialog is open
    DisposableEffect(showThemeDialog) {
        if (showThemeDialog) {
            val originalScrollUp = MainActivity.onScrollUp
            val originalScrollDown = MainActivity.onScrollDown
            val originalEnter = MainActivity.onEnterPressed

            MainActivity.onScrollUp = {
                if (themeDialogHighlightedIndex > 0) themeDialogHighlightedIndex--
            }
            MainActivity.onScrollDown = {
                if (themeDialogHighlightedIndex < 2) themeDialogHighlightedIndex++
            }
            MainActivity.onEnterPressed = {
                themeDialogActivateTrigger++
            }

            onDispose {
                MainActivity.onScrollUp = originalScrollUp
                MainActivity.onScrollDown = originalScrollDown
                MainActivity.onEnterPressed = originalEnter
            }
        } else {
            onDispose { }
        }
    }

    // Compute total items and report to parent for keyboard navigation bounds
    val totalItems = 17 +
        (if (isCheckingUpdate || hasUpdate) 1 else 0) +
        (if (BuildConfig.DEBUG) 2 else 0)
    LaunchedEffect(totalItems) { onItemCount(totalItems) }

    // Track positions of items for scroll
    val itemPositions = remember { mutableMapOf<Int, Int>() }

    // Scroll to keep highlighted item fully visible (using actual positions)
    LaunchedEffect(highlightedIndex, itemPositions[highlightedIndex]) {
        if (highlightedIndex >= 0) {
            val position = itemPositions[highlightedIndex]
            if (position != null) {
                scrollState.animateScrollTo(position.coerceAtMost(scrollState.maxValue))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
    ) {
        // Track item index for keyboard navigation highlighting (resets each composition)
        var itemIndex = 0
        
        // Helper to get and increment index
        fun nextIndex(): Int = itemIndex++

        // Section: Dialpad
        SettingsSectionHeader(stringResource(R.string.settings_dialpad_section))
        
        val idx1 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_hide_virtual_dialpad),
            subtitle = stringResource(R.string.settings_hide_virtual_dialpad_desc),
            checked = hideVirtualDialpad,
            onCheckedChange = {
                hideVirtualDialpad = it
                prefs.hideVirtualDialpad = it
            },
            isHighlighted = highlightedIndex == idx1,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx1] = pos },
        )
        
        val idx2 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.onboarding_setup),
            subtitle = stringResource(R.string.onboarding_default_dialer_desc),
            onClick = { showOnboarding = true },
            isHighlighted = highlightedIndex == idx2,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx2] = pos },
        )
        
        val idx2b = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_speed_dial),
            subtitle = stringResource(R.string.settings_speed_dial_desc),
            onClick = onNavigateToSpeedDialSettings,
            isHighlighted = highlightedIndex == idx2b,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx2b] = pos },
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Section: Interface
        SettingsSectionHeader(stringResource(R.string.settings_interface_section))
        
        val idx3 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_expanded_bottom_nav),
            subtitle = stringResource(R.string.settings_expanded_bottom_nav_desc),
            checked = expandedBottomNav,
            onCheckedChange = {
                expandedBottomNav = it
                prefs.expandedBottomNav = it
            },
            isHighlighted = highlightedIndex == idx3,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx3] = pos },
        )
        
        val idx4 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_theme),
            subtitle = getThemeDisplayName(themeMode),
            onClick = { showThemeDialog = true },
            isHighlighted = highlightedIndex == idx4,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx4] = pos },
        )
        
        val idx5 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_custom_colors),
            subtitle = stringResource(R.string.settings_custom_colors_desc),
            onClick = onNavigateToColorSettings,
            isHighlighted = highlightedIndex == idx5,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx5] = pos },
        )
        
        var fullscreenAvatar by remember { mutableStateOf(prefs.fullscreenAvatar) }
        val idx6 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_fullscreen_avatar),
            subtitle = stringResource(R.string.settings_fullscreen_avatar_desc),
            checked = fullscreenAvatar,
            onCheckedChange = {
                fullscreenAvatar = it
                prefs.fullscreenAvatar = it
            },
            isHighlighted = highlightedIndex == idx6,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx6] = pos },
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Section: Keyboard
        SettingsSectionHeader(stringResource(R.string.settings_keyboard_section))
        
        val idx7 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_disable_vertical_arrows),
            subtitle = stringResource(R.string.settings_disable_vertical_arrows_desc),
            checked = disableVerticalArrows,
            onCheckedChange = {
                disableVerticalArrows = it
                prefs.disableVerticalArrows = it
            },
            isHighlighted = highlightedIndex == idx7,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx7] = pos },
        )
        
        val idx8 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_disable_horizontal_arrows),
            subtitle = stringResource(R.string.settings_disable_horizontal_arrows_desc),
            checked = disableHorizontalArrows,
            onCheckedChange = {
                disableHorizontalArrows = it
                prefs.disableHorizontalArrows = it
            },
            isHighlighted = highlightedIndex == idx8,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx8] = pos },
        )

        val idx8c = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_disable_home_key_hangup),
            subtitle = stringResource(R.string.settings_disable_home_key_hangup_desc),
            checked = disableHomeKeyHangup,
            onCheckedChange = {
                disableHomeKeyHangup = it
                prefs.disableHomeKeyHangup = it
            },
            isHighlighted = highlightedIndex == idx8c,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx8c] = pos },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Section: Call history
        SettingsSectionHeader(stringResource(R.string.settings_call_history_section))
        
        val idx9 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_call_history_limit),
            subtitle = stringResource(R.string.settings_call_history_limit_desc, callHistoryLimit),
            onClick = { showHistoryLimitDialog = true },
            isHighlighted = highlightedIndex == idx9,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx9] = pos },
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Section: Language
        SettingsSectionHeader(stringResource(R.string.settings_language_section))
        
        val idx10 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_language),
            subtitle = getLanguageDisplayName(prefs.language),
            onClick = { showLanguageDialog = true },
            isHighlighted = highlightedIndex == idx10,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx10] = pos },
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Section: Privacy
        SettingsSectionHeader(stringResource(R.string.settings_privacy_section))
        
        val idx11 = nextIndex()
        SettingsSwitchItem(
            title = stringResource(R.string.settings_send_anonymous_stats),
            subtitle = stringResource(R.string.settings_send_anonymous_stats_desc),
            checked = sendAnonymousStats,
            onCheckedChange = {
                sendAnonymousStats = it
                prefs.sendAnonymousStats = it
                App.updateSentryState(it)
            },
            isHighlighted = highlightedIndex == idx11,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx11] = pos },
        )
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        // Section: About
        SettingsSectionHeader(stringResource(R.string.settings_about_section))
        
        val idx12 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.settings_version, currentVersion),
            onClick = { },
            isHighlighted = highlightedIndex == idx12,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx12] = pos },
        )
        
        if (isCheckingUpdate) {
            val idx13 = nextIndex()
            SettingsClickItem(
                title = stringResource(R.string.settings_checking_updates),
                subtitle = "",
                onClick = { },
                isHighlighted = highlightedIndex == idx13,
                activateTrigger = activateTrigger,
                onPositioned = { pos -> itemPositions[idx13] = pos },
            )
        } else if (hasUpdate) {
            val idx13 = nextIndex()
            SettingsClickItem(
                title = stringResource(R.string.settings_open_github),
                subtitle = stringResource(R.string.settings_update_available, latestVersion!!.removePrefix("v")),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/overklassniy/Q25_Dialer/releases/latest"),
                    )
                    context.startActivity(intent)
                },
                isHighlighted = highlightedIndex == idx13,
                activateTrigger = activateTrigger,
                onPositioned = { pos -> itemPositions[idx13] = pos },
            )
        }
        
        val idx14 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_changelog),
            subtitle = stringResource(R.string.settings_changelog_desc),
            onClick = { showChangelogDialog = true },
            isHighlighted = highlightedIndex == idx14,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx14] = pos },
        )
        
        val idx15 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_github),
            subtitle = stringResource(R.string.settings_github_desc),
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/overklassniy/Q25_Dialer"),
                )
                context.startActivity(intent)
            },
            isHighlighted = highlightedIndex == idx15,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx15] = pos },
        )
        
        val idx16 = nextIndex()
        SettingsClickItem(
            title = stringResource(R.string.settings_donate),
            subtitle = stringResource(R.string.settings_donate_desc),
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.donationalerts.com/r/overklassniy"),
                )
                context.startActivity(intent)
            },
            isHighlighted = highlightedIndex == idx16,
            activateTrigger = activateTrigger,
            onPositioned = { pos -> itemPositions[idx16] = pos },
        )
        
        // Section: Debug (only in debug builds)
        if (BuildConfig.DEBUG) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            SettingsSectionHeader(stringResource(R.string.settings_debug_section))
            
            val idx17 = nextIndex()
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
                isHighlighted = highlightedIndex == idx17,
                activateTrigger = activateTrigger,
                onPositioned = { pos -> itemPositions[idx17] = pos },
            )
            
            val idx18 = nextIndex()
            SettingsClickItem(
                title = stringResource(R.string.settings_debug_test_crash),
                subtitle = stringResource(R.string.settings_debug_test_crash_desc),
                onClick = {
                    throw RuntimeException("Test crash from Q25 Dialer debug menu")
                },
                isHighlighted = highlightedIndex == idx18,
                activateTrigger = activateTrigger,
                onPositioned = { pos -> itemPositions[idx18] = pos },
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
        // Scroll to keep highlighted item fully visible (using actual positions)
        LaunchedEffect(highlightedIndex, itemPositions[highlightedIndex]) {
            if (highlightedIndex >= 0) {
                val position = itemPositions[highlightedIndex]
                if (position != null) {
                    scrollState.animateScrollTo(position.coerceAtMost(scrollState.maxValue))
                }
            }
        }
        ThemePickerDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                prefs.themeMode = mode
                themeMode = mode
                showThemeDialog = false
                onThemeModeChanged(mode)
            },
            highlightedIndex = themeDialogHighlightedIndex,
            activateTrigger = themeDialogActivateTrigger,
        )
    }

    if (showChangelogDialog) {
        ChangelogDialog(onDismiss = { showChangelogDialog = false })
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
    isHighlighted: Boolean = false,
    activateTrigger: Int = 0,
    onPositioned: ((Int) -> Unit)? = null,
) {
    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        androidx.compose.ui.graphics.Color.Transparent
    // Activate when highlighted and trigger fires
    LaunchedEffect(activateTrigger) {
        if (isHighlighted && activateTrigger > 0) {
            onCheckedChange(!checked)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coordinates ->
                onPositioned?.invoke(coordinates.positionInParent().y.toInt())
            }
            .background(backgroundColor)
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
    isHighlighted: Boolean = false,
    activateTrigger: Int = 0,
    onPositioned: ((Int) -> Unit)? = null,
) {
    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        androidx.compose.ui.graphics.Color.Transparent
    // Activate when highlighted and trigger fires
    LaunchedEffect(activateTrigger) {
        if (isHighlighted && activateTrigger > 0) {
            onClick()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coordinates ->
                onPositioned?.invoke(coordinates.positionInParent().y.toInt())
            }
            .background(backgroundColor)
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
    highlightedIndex: Int = -1,
    activateTrigger: Int = 0,
) {
    val options = listOf(
        PreferencesManager.THEME_SYSTEM to stringResource(R.string.theme_system),
        PreferencesManager.THEME_DARK to stringResource(R.string.theme_dark),
        PreferencesManager.THEME_LIGHT to stringResource(R.string.theme_light),
    )

    // Activate when highlighted and trigger fires
    LaunchedEffect(activateTrigger) {
        if (highlightedIndex >= 0 && highlightedIndex < options.size && activateTrigger > 0) {
            onSelect(options[highlightedIndex].first)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                options.forEachIndexed { index, (mode, label) ->
                    val isHighlighted = index == highlightedIndex
                    val backgroundColor = if (isHighlighted)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        androidx.compose.ui.graphics.Color.Transparent
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
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

@Composable
private fun ChangelogDialog(onDismiss: () -> Unit) {
    var changelogText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val result = fetchChangelog()
        if (result != null) {
            changelogText = result
        } else {
            isError = true
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_changelog)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 400.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    isError -> Text(
                        text = stringResource(R.string.settings_changelog_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    changelogText != null -> {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                        ) {
                            MarkdownText(changelogText!!)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun MarkdownText(markdown: String) {
    val annotated = remember(markdown) { parseMarkdown(markdown) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        // Skip the first line ("# Changelog" heading)
        val skipFirst = lines.firstOrNull()?.startsWith("# Changelog") == true
        val linesToProcess = if (skipFirst) lines.drop(1) else lines
        var h2Count = 0
        for ((index, line) in linesToProcess.withIndex()) {
            val trimmed = line.trimEnd()
            // Add extra spacing before 2nd and subsequent ## headers
            if (trimmed.startsWith("## ")) {
                h2Count++
                if (h2Count > 1) {
                    append("\n")
                }
            }
            when {
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp))) {
                        appendBoldInline(trimmed.removePrefix("# "))
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))) {
                        appendBoldInline(trimmed.removePrefix("## "))
                    }
                }
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))) {
                        appendBoldInline(trimmed.removePrefix("### "))
                    }
                }
                trimmed.startsWith("- ") -> {
                    append("  \u2022 ")
                    appendBoldInline(trimmed.removePrefix("- "))
                }
                trimmed.isNotEmpty() -> {
                    appendBoldInline(trimmed)
                }
            }
            // Add spacing after each line (small line spacing)
            if (index < linesToProcess.lastIndex) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendBoldInline(text: String) {
    val boldRegex = """\*\*(.+?)\*\*""".toRegex()
    var lastIndex = 0
    for (match in boldRegex.findAll(text)) {
        append(text.substring(lastIndex, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}

private suspend fun fetchChangelog(): String? = withContext(Dispatchers.IO) {
    try {
        val url = java.net.URL("https://raw.githubusercontent.com/overklassniy/Q25_Dialer/master/CHANGELOG.md")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (connection.responseCode == 200) {
            connection.inputStream.bufferedReader().readText()
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}

private suspend fun fetchLatestGitHubVersion(): String? = withContext(Dispatchers.IO) {
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