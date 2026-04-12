package com.overklassniy.q25.dialer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.telecom.TelecomManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.overklassniy.q25.dialer.data.PreferencesManager
import com.overklassniy.q25.dialer.service.QwertyAccessibilityService
import com.overklassniy.q25.dialer.ui.components.DialpadGrid
import com.overklassniy.q25.dialer.ui.components.SelectionActionBar
import com.overklassniy.q25.dialer.ui.screens.ContactDetailScreen
import com.overklassniy.q25.dialer.ui.screens.ContactsScreen
import com.overklassniy.q25.dialer.ui.screens.OnboardingScreen
import com.overklassniy.q25.dialer.ui.screens.RecentsScreen
import com.overklassniy.q25.dialer.ui.screens.ColorSettingsScreen
import com.overklassniy.q25.dialer.ui.screens.SettingsScreen
import com.overklassniy.q25.dialer.ui.theme.ActivatedItemForeground
import com.overklassniy.q25.dialer.ui.theme.CallGreen
import com.overklassniy.q25.dialer.ui.theme.Q25DialerTheme
import com.overklassniy.q25.dialer.util.LocaleHelper
import com.overklassniy.q25.dialer.util.PermissionHelper

// Maps a native Android keyCode to its dialpad character (QWERTY mapping)
private fun keyCodeToDialpad(keyCode: Int): Char? {
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

// Maps a native Android keyCode to its literal character for normal text input
private fun keyCodeToChar(keyCode: Int): Char? {
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
        KeyEvent.KEYCODE_AT -> '@'
        else -> null
    }
}

// Maps a QWERTY character to a dialpad character
private fun qwertyCharToDialpad(c: Char): Char? {
    return when (c.lowercaseChar()) {
        'q' -> '#'
        'a' -> '*'
        'w' -> '1'
        'e' -> '2'
        'r' -> '3'
        's' -> '4'
        'd' -> '5'
        'f' -> '6'
        'z' -> '7'
        'x' -> '8'
        'c' -> '9'
        'o' -> '+'
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c
        '*', '#', '+' -> c
        else -> null
    }
}

// Transforms a full string through QWERTY -> dialpad mapping, filtering invalid chars
private fun transformToDialpad(input: String): String {
    return input.mapNotNull { qwertyCharToDialpad(it) }.joinToString("")
}

class MainActivity : ComponentActivity() {

    // Triggered by KEY_CALL intent from QwertyAccessibilityService
    var makeCallRequested by mutableStateOf(false)
        private set

    val prefs by lazy { PreferencesManager(this) }

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocale(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Disable autofill suggestions across the whole activity
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        PermissionHelper.launchMissing(this, permissionLauncher)

        handleMakeCallIntent(intent)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var colorRefreshKey by remember { mutableIntStateOf(0) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                PreferencesManager.THEME_DARK -> true
                PreferencesManager.THEME_LIGHT -> false
                else -> systemDark
            }
            Q25DialerTheme(darkTheme = isDarkTheme, colorRefreshKey = colorRefreshKey) {
                val localPrefs = remember { PreferencesManager(this@MainActivity) }
                var showOnboarding by remember { mutableStateOf(!localPrefs.onboardingCompleted) }

                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            prefs.onboardingCompleted = true
                            showOnboarding = false
                        },
                    )
                } else {
                    MainScreen(
                        activity = this@MainActivity,
                        onThemeModeChanged = { mode -> themeMode = mode },
                        onColorsChanged = { colorRefreshKey++ },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleMakeCallIntent(intent)
    }

    private fun handleMakeCallIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(QwertyAccessibilityService.EXTRA_MAKE_CALL, false) == true) {
            makeCallRequested = true
            intent.removeExtra(QwertyAccessibilityService.EXTRA_MAKE_CALL)
        }
    }

    fun consumeMakeCall() {
        makeCallRequested = false
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Handle BACK key before Compose (focus system consumes BACK to clear focus)
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                backKeyConsumed = onBackPressed?.invoke() ?: false
                if (backKeyConsumed) return true
            } else if (event.action == KeyEvent.ACTION_UP && backKeyConsumed) {
                backKeyConsumed = false
                return true
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            // Recents: map QWERTY keycodes directly to dialpad chars (bypasses IME)
            if (currentScreen == NavRoutes.RECENTS) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    onAccessibilityBackspace?.invoke()
                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_CALL) {
                    makeCallRequested = true
                    return true
                }
                keyCodeToDialpad(keyCode)?.let {
                    onAccessibilityDialpadChar?.invoke(it)
                    return true
                }
            }
            // Consume vertical arrows when disabled (all screens with list navigation)
            if (currentScreen == NavRoutes.RECENTS || currentScreen == NavRoutes.CONTACTS || currentScreen == NavRoutes.SETTINGS || currentScreen == NavRoutes.COLOR_SETTINGS) {
                if (prefs.disableVerticalArrows) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true
                    }
                }
            }
            // Recents, Contacts, Settings & Color Settings: navigate lists with DPAD UP/DOWN, activate with ENTER
            if (currentScreen == NavRoutes.RECENTS || currentScreen == NavRoutes.CONTACTS || currentScreen == NavRoutes.SETTINGS || currentScreen == NavRoutes.COLOR_SETTINGS) {
                if (!prefs.disableVerticalArrows) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        onScrollUp?.invoke()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        onScrollDown?.invoke()
                        return true
                    }
                }
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onEnterPressed?.invoke()
                    return true
                }
            }
            // Consume horizontal arrows when disabled
            if (!prefs.disableHorizontalArrows) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    // Let system handle
                }
            } else {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return true // consume, do nothing
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        @Volatile
        var isInForeground = false
        @Volatile
        var currentScreen = ""
        // Callbacks invoked by QwertyAccessibilityService on main thread
        var onAccessibilityDialpadChar: ((Char) -> Unit)? = null
        var onAccessibilityBackspace: (() -> Unit)? = null
        var onScrollUp: (() -> Unit)? = null
        var onScrollDown: (() -> Unit)? = null
        var onEnterPressed: (() -> Unit)? = null
        var onBackPressed: (() -> Boolean)? = null
        private var backKeyConsumed = false
    }
}

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

object NavRoutes {
    const val RECENTS = "recents"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"
    const val CONTACT_DETAIL = "contact_detail/{contactId}/{phoneNumber}"
    const val COLOR_SETTINGS = "color_settings"
    const val ONBOARDING = "onboarding"

    fun contactDetail(contactId: Long = -1, phoneNumber: String = "") =
        "contact_detail/$contactId/$phoneNumber"
}

@Composable
fun MainScreen(
    activity: MainActivity? = null,
    onThemeModeChanged: (String) -> Unit = {},
    onColorsChanged: () -> Unit = {},
) {
    val navController = rememberNavController()
    // Separate search queries per screen
    var recentsQuery by rememberSaveable { mutableStateOf("") }
    var contactsQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { PreferencesManager(context) }
    val hideVirtualDialpad = remember { mutableStateOf(prefs.hideVirtualDialpad) }
    val expandedBottomNav = remember { mutableStateOf(prefs.expandedBottomNav) }

    // Virtual dialpad visibility (only on Recents when hideVirtualDialpad is off)
    var showVirtualDialpad by rememberSaveable { mutableStateOf(false) }

    // Confirmation dialogs
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }
    var blockTargetNumber by remember { mutableStateOf("") }
    var blockTargetIsBlocked by remember { mutableStateOf(false) }

    // Selection state for Recents
    var selectedCallKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Selection state for Contacts
    var selectedContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // All selectable keys/ids (reported by screens for select-all)
    var allCallKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Refresh triggers: increment to force data reload after deletion
    var recentsRefreshTrigger by remember { mutableIntStateOf(0) }
    var contactsRefreshTrigger by remember { mutableIntStateOf(0) }

    // Hoisted list states for keyboard scrolling
    val recentsListState = rememberLazyListState()
    val contactsListState = rememberLazyListState()

    // Highlighted item index for keyboard navigation (-1 = no selection)
    var highlightedRecentsIndex by remember { mutableIntStateOf(-1) }
    var highlightedContactsIndex by remember { mutableIntStateOf(-1) }
    // Total item counts for bounds checking (updated by screens)
    var recentsItemCount by remember { mutableIntStateOf(0) }
    var contactsItemCount by remember { mutableIntStateOf(0) }
    // Activation callbacks (invoke the click action on highlighted item)
    var onActivateRecentsItem: ((Int) -> Unit)? by remember { mutableStateOf(null) }
    var onActivateContactsItem: ((Int) -> Unit)? by remember { mutableStateOf(null) }

    // Settings keyboard navigation state
    var highlightedSettingsIndex by remember { mutableIntStateOf(-1) }
    var settingsItemCount by remember { mutableIntStateOf(0) }
    var settingsActivateTrigger by remember { mutableIntStateOf(0) }

    // Color settings keyboard navigation state
    var highlightedColorSettingsIndex by remember { mutableIntStateOf(-1) }
    var colorSettingsItemCount by remember { mutableIntStateOf(0) }
    var colorSettingsActivateTrigger by remember { mutableIntStateOf(0) }

    // GitHub update check
    var hasUpdate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val latest = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/overklassniy/Q25_Dialer/releases/latest")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val regex = """"tag_name"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(body)?.groupValues?.get(1)
                } else null
            } catch (_: Exception) { null }
        }
        if (latest != null) {
            val currentVer = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (_: Exception) { "" }
            hasUpdate = latest != currentVer && latest.removePrefix("v") != currentVer
        }
    }

    // Focus requesters
    val searchFieldFocusRequester = remember { FocusRequester() }

    // Track current route
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: NavRoutes.RECENTS

    // Bottom nav items - 2 tabs only (no favorites, no settings)
    val bottomNavItems = listOf(
        BottomNavItem(NavRoutes.RECENTS, R.string.recents, Icons.Filled.History, Icons.Outlined.History),
        BottomNavItem(NavRoutes.CONTACTS, R.string.contacts, Icons.Filled.Contacts, Icons.Outlined.Contacts),
    )

    // Derive selected tab from current route
    val selectedTab = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    // Update currentScreen immediately for dispatchKeyEvent (synchronous, not LaunchedEffect)
    MainActivity.currentScreen = currentRoute

    // Clear selection when switching tabs; hide dialpad when leaving Recents
    // Re-read settings when route changes
    LaunchedEffect(currentRoute) {
        // Re-read settings so changes apply without restart
        hideVirtualDialpad.value = prefs.hideVirtualDialpad
        expandedBottomNav.value = prefs.expandedBottomNav
        if (currentRoute == NavRoutes.RECENTS) selectedContactIds = emptySet()
        if (currentRoute == NavRoutes.CONTACTS) {
            selectedCallKeys = emptySet()
            showVirtualDialpad = false
        }
    }

    // Reset highlighted index when switching screens
    LaunchedEffect(currentRoute) {
        highlightedRecentsIndex = -1
        highlightedContactsIndex = -1
        highlightedSettingsIndex = -1
        settingsActivateTrigger = 0
        highlightedColorSettingsIndex = -1
        colorSettingsActivateTrigger = 0
    }

    // Callbacks: receive key events from dispatchKeyEvent / accessibility service
    DisposableEffect(Unit) {
        MainActivity.onAccessibilityDialpadChar = { char -> recentsQuery += char }
        MainActivity.onAccessibilityBackspace = {
            if (recentsQuery.isNotEmpty()) recentsQuery = recentsQuery.dropLast(1)
        }
        MainActivity.onScrollUp = {
            when (MainActivity.currentScreen) {
                NavRoutes.CONTACTS -> {
                    if (highlightedContactsIndex > 0) highlightedContactsIndex--
                    else if (highlightedContactsIndex < 0 && contactsItemCount > 0) highlightedContactsIndex = 0
                }
                NavRoutes.SETTINGS -> {
                    if (highlightedSettingsIndex > 0) highlightedSettingsIndex--
                    else if (highlightedSettingsIndex < 0 && settingsItemCount > 0) highlightedSettingsIndex = 0
                }
                NavRoutes.COLOR_SETTINGS -> {
                    if (highlightedColorSettingsIndex > 0) highlightedColorSettingsIndex--
                    else if (highlightedColorSettingsIndex < 0 && colorSettingsItemCount > 0) highlightedColorSettingsIndex = 0
                }
                else -> {
                    if (highlightedRecentsIndex > 0) highlightedRecentsIndex--
                    else if (highlightedRecentsIndex < 0 && recentsItemCount > 0) highlightedRecentsIndex = 0
                }
            }
        }
        MainActivity.onScrollDown = {
            when (MainActivity.currentScreen) {
                NavRoutes.CONTACTS -> {
                    if (highlightedContactsIndex < contactsItemCount - 1) highlightedContactsIndex++
                    else if (highlightedContactsIndex < 0 && contactsItemCount > 0) highlightedContactsIndex = 0
                }
                NavRoutes.SETTINGS -> {
                    if (highlightedSettingsIndex < settingsItemCount - 1) highlightedSettingsIndex++
                    else if (highlightedSettingsIndex < 0 && settingsItemCount > 0) highlightedSettingsIndex = 0
                }
                NavRoutes.COLOR_SETTINGS -> {
                    if (highlightedColorSettingsIndex < colorSettingsItemCount - 1) highlightedColorSettingsIndex++
                    else if (highlightedColorSettingsIndex < 0 && colorSettingsItemCount > 0) highlightedColorSettingsIndex = 0
                }
                else -> {
                    if (highlightedRecentsIndex < recentsItemCount - 1) highlightedRecentsIndex++
                    else if (highlightedRecentsIndex < 0 && recentsItemCount > 0) highlightedRecentsIndex = 0
                }
            }
        }
        MainActivity.onEnterPressed = {
            when (MainActivity.currentScreen) {
                NavRoutes.RECENTS -> {
                    if (highlightedRecentsIndex >= 0) {
                        onActivateRecentsItem?.invoke(highlightedRecentsIndex)
                    } else if (recentsQuery.isNotEmpty()) {
                        // No item highlighted but number is typed – place call
                        try {
                            val encoded = android.net.Uri.encode(recentsQuery, "+*")
                            activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$encoded")))
                        } catch (_: Exception) { }
                    }
                }
                NavRoutes.CONTACTS -> {
                    if (highlightedContactsIndex >= 0) {
                        onActivateContactsItem?.invoke(highlightedContactsIndex)
                    }
                }
                NavRoutes.SETTINGS -> {
                    if (highlightedSettingsIndex >= 0) settingsActivateTrigger++
                }
                NavRoutes.COLOR_SETTINGS -> {
                    if (highlightedColorSettingsIndex >= 0) colorSettingsActivateTrigger++
                }
            }
        }
        MainActivity.onBackPressed = {
            when {
                showVirtualDialpad && MainActivity.currentScreen == NavRoutes.RECENTS -> {
                    showVirtualDialpad = false
                    isSearchActive = false
                    recentsQuery = ""
                    true
                }
                MainActivity.currentScreen == NavRoutes.CONTACTS -> {
                    contactsQuery = ""
                    navController.navigate(NavRoutes.RECENTS) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                    true
                }
                MainActivity.currentScreen == NavRoutes.SETTINGS -> {
                    navController.popBackStack()
                    onColorsChanged()
                    true
                }
                MainActivity.currentScreen == NavRoutes.COLOR_SETTINGS ||
                MainActivity.currentScreen == NavRoutes.CONTACT_DETAIL -> {
                    navController.popBackStack()
                    true
                }
                else -> false
            }
        }
        onDispose {
            MainActivity.onAccessibilityDialpadChar = null
            MainActivity.onAccessibilityBackspace = null
            MainActivity.onScrollUp = null
            MainActivity.onScrollDown = null
            MainActivity.onEnterPressed = null
            MainActivity.onBackPressed = null
            MainActivity.currentScreen = ""
        }
    }

    // Handle KEY_CALL from accessibility service
    val makeCallRequested = activity?.makeCallRequested == true
    LaunchedEffect(makeCallRequested) {
        if (makeCallRequested && recentsQuery.isNotEmpty()) {
            try {
                val encoded = Uri.encode(recentsQuery, "+*")
                context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$encoded")))
            } catch (_: Exception) { }
            activity?.consumeMakeCall()
        } else if (makeCallRequested) {
            activity?.consumeMakeCall()
        }
    }

    val isCallSelectionMode = selectedCallKeys.isNotEmpty() && currentRoute == NavRoutes.RECENTS
    val isContactSelectionMode = selectedContactIds.isNotEmpty() && currentRoute == NavRoutes.CONTACTS
    val isAnySelectionMode = isCallSelectionMode || isContactSelectionMode
    val isOnDetailScreen = currentRoute == NavRoutes.CONTACT_DETAIL
    val isOnSettingsScreen = currentRoute == NavRoutes.SETTINGS

    // Helper to place a call or send USSD with current recents query
    val placeCall = {
        if (recentsQuery.isNotEmpty()) {
            try {
                val encoded = Uri.encode(recentsQuery, "+*")
                context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$encoded")))
            } catch (_: Exception) { }
        }
    }

    // Focus requester for key event handling
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyCode = keyEvent.key.nativeKeyCode
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (currentRoute == NavRoutes.RECENTS) {
                                navController.navigate(NavRoutes.CONTACTS) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (currentRoute == NavRoutes.CONTACTS) {
                                navController.navigate(NavRoutes.RECENTS) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
        topBar = {
            if (!isOnDetailScreen && !isOnSettingsScreen && currentRoute != NavRoutes.COLOR_SETTINGS) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    if (isAnySelectionMode) {
                        // Selection action bar replaces search bar
                        val selectedNumberForBlock = if (isCallSelectionMode && selectedCallKeys.size == 1) {
                            selectedCallKeys.first().substringBefore("_")
                        } else ""
                        // Recalculate blocked status whenever selection changes
                        val isSelectedNumberBlocked by remember(selectedCallKeys) {
                            mutableStateOf(
                                if (selectedNumberForBlock.isNotEmpty()) {
                                    isNumberBlocked(context, selectedNumberForBlock)
                                } else false
                            )
                        }

                        SelectionActionBar(
                            selectedCount = if (isCallSelectionMode) selectedCallKeys.size else selectedContactIds.size,
                            onClose = {
                                selectedCallKeys = emptySet()
                                selectedContactIds = emptySet()
                            },
                            onSelectAll = {
                                if (isCallSelectionMode) {
                                    selectedCallKeys = if (selectedCallKeys == allCallKeys) emptySet() else allCallKeys
                                } else if (isContactSelectionMode) {
                                    selectedContactIds = if (selectedContactIds == allContactIds) emptySet() else allContactIds
                                }
                            },
                            onSms = if (isCallSelectionMode && selectedCallKeys.size == 1) {
                                {
                                    val number = selectedCallKeys.first().substringBefore("_")
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
                                    } catch (_: Exception) { }
                                    selectedCallKeys = emptySet()
                                }
                            } else null,
                            onCopyNumber = if (isCallSelectionMode && selectedCallKeys.size == 1) {
                                {
                                    val number = selectedCallKeys.first().substringBefore("_")
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("phone", number))
                                    Toast.makeText(context, R.string.number_copied, Toast.LENGTH_SHORT).show()
                                    selectedCallKeys = emptySet()
                                }
                            } else null,
                            onBlock = if (isCallSelectionMode && selectedCallKeys.size == 1) {
                                {
                                    blockTargetNumber = selectedNumberForBlock
                                    blockTargetIsBlocked = isSelectedNumberBlocked
                                    showBlockConfirmation = true
                                }
                            } else null,
                            isBlocked = isSelectedNumberBlocked,
                            onDelete = if (isCallSelectionMode || isContactSelectionMode) {
                                { showDeleteConfirmation = true }
                            } else null,
                        )
                    } else {
                        // Normal search bar
                        SearchBar(
                            query = if (currentRoute == NavRoutes.RECENTS) recentsQuery else contactsQuery,
                            onQueryChange = { newValue ->
                                if (currentRoute == NavRoutes.RECENTS) {
                                    recentsQuery = newValue
                                } else {
                                    contactsQuery = newValue
                                }
                            },
                            readOnly = currentRoute == NavRoutes.RECENTS,
                            isActive = isSearchActive,
                            onActiveChange = { active ->
                                isSearchActive = active
                                if (active && currentRoute == NavRoutes.RECENTS && !hideVirtualDialpad.value) {
                                    showVirtualDialpad = true
                                }
                                if (!active) {
                                    showVirtualDialpad = false
                                }
                            },
                            searchFieldFocusRequester = searchFieldFocusRequester,
                            onSearchFieldFocused = {
                                if (currentRoute == NavRoutes.RECENTS && !hideVirtualDialpad.value) {
                                    isSearchActive = true
                                    showVirtualDialpad = true
                                }
                            },
                            placeholder = when (currentRoute) {
                                NavRoutes.RECENTS -> stringResource(R.string.enter_number)
                                else -> stringResource(R.string.search)
                            },
                            leadingIcon = when (currentRoute) {
                                NavRoutes.RECENTS -> Icons.Filled.Dialpad
                                else -> Icons.Filled.Search
                            },
                            onSettingsClick = {
                                // Clear focus from search field to prevent double-back issue
                                focusManager.clearFocus()
                                navController.navigate(NavRoutes.SETTINGS) {
                                    launchSingleTop = true
                                }
                            },
                            showUpdateBadge = hasUpdate,
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }
        },
        bottomBar = {
            // Bottom navigation bar - only show on main tabs (not settings, not contact detail)
            val showBottomBar = currentRoute == NavRoutes.RECENTS || currentRoute == NavRoutes.CONTACTS
            if (showBottomBar) {
                BottomNavigation(
                    items = bottomNavItems,
                    selectedIndex = selectedTab,
                    onItemSelected = { index ->
                        navController.navigate(bottomNavItems[index].route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    expanded = expandedBottomNav.value,
                )
            }
        },
        floatingActionButton = {
            // Show call FAB only when virtual dialpad is hidden (call button is inside dialpad otherwise)
            if (currentRoute == NavRoutes.RECENTS && recentsQuery.isNotEmpty() && hideVirtualDialpad.value) {
                FloatingActionButton(
                    onClick = { placeCall() },
                    containerColor = CallGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = stringResource(R.string.call),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MainNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                recentsQuery = recentsQuery,
                contactsQuery = contactsQuery,
                currentRoute = currentRoute,
                selectedCallKeys = selectedCallKeys,
                onCallSelectionChanged = { selectedCallKeys = it },
                selectedContactIds = selectedContactIds,
                onContactSelectionChanged = { selectedContactIds = it },
                onAllCallKeys = { allCallKeys = it },
                onAllContactIds = { allContactIds = it },
                recentsRefreshTrigger = recentsRefreshTrigger,
                contactsRefreshTrigger = contactsRefreshTrigger,
                recentsListState = recentsListState,
                contactsListState = contactsListState,
                highlightedRecentsIndex = highlightedRecentsIndex,
                highlightedContactsIndex = highlightedContactsIndex,
                onRecentsItemCount = { recentsItemCount = it },
                onContactsItemCount = { contactsItemCount = it },
                onActivateRecentsItem = { onActivateRecentsItem = it },
                onActivateContactsItem = { onActivateContactsItem = it },
                highlightedSettingsIndex = highlightedSettingsIndex,
                settingsActivateTrigger = settingsActivateTrigger,
                onSettingsItemCount = { settingsItemCount = it },
                highlightedColorSettingsIndex = highlightedColorSettingsIndex,
                colorSettingsActivateTrigger = colorSettingsActivateTrigger,
                onColorSettingsItemCount = { colorSettingsItemCount = it },
                onNavigateBack = {
                    navController.popBackStack()
                    // Trigger color refresh when exiting settings
                    onColorsChanged()
                },
                onThemeModeChanged = onThemeModeChanged,
                onColorsChanged = onColorsChanged,
            )

            // Virtual dialpad overlay at bottom (only on Recents when hideVirtualDialpad is off)
            AnimatedVisibility(
                visible = showVirtualDialpad && currentRoute == NavRoutes.RECENTS,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    DialpadGrid(
                        onKeyPress = { digit ->
                            recentsQuery += digit
                        },
                        onKeyLongPress = { digit ->
                            if (digit == "0") {
                                recentsQuery += "+"
                            }
                        },
                        onCallPress = { placeCall() },
                        onBackspacePress = {
                            if (recentsQuery.isNotEmpty()) recentsQuery = recentsQuery.dropLast(1)
                        },
                        onBackspaceLongPress = { recentsQuery = "" },
                    )
                }
            }
        }
    }

    // Focus management: Contacts->BasicTextField (for IME input), Recents->Scaffold
    LaunchedEffect(currentRoute) {
        if (currentRoute == NavRoutes.CONTACTS) {
            try { searchFieldFocusRequester.requestFocus() } catch (_: Exception) {}
        } else if (currentRoute == NavRoutes.RECENTS) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
        // For SETTINGS / CONTACT_DETAIL / COLOR_SETTINGS: let those screens manage their own focus
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = {
                Text(stringResource(
                    if (isCallSelectionMode) R.string.delete_calls_confirmation
                    else R.string.delete_contacts_confirmation
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isCallSelectionMode) {
                        selectedCallKeys.forEach { key ->
                            val number = key.substringBefore("_")
                            try {
                                context.contentResolver.delete(
                                    CallLog.Calls.CONTENT_URI,
                                    "${CallLog.Calls.NUMBER} = ?",
                                    arrayOf(number)
                                )
                            } catch (_: Exception) { }
                        }
                        selectedCallKeys = emptySet()
                        recentsRefreshTrigger++
                    } else if (isContactSelectionMode) {
                        selectedContactIds.forEach { contactId ->
                            try {
                                val uri = Uri.withAppendedPath(
                                    android.provider.ContactsContract.Contacts.CONTENT_URI,
                                    contactId.toString()
                                )
                                context.contentResolver.delete(uri, null, null)
                            } catch (_: Exception) { }
                        }
                        selectedContactIds = emptySet()
                        contactsRefreshTrigger++
                    }
                    showDeleteConfirmation = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // Block/unblock confirmation dialog
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text(stringResource(
                if (blockTargetIsBlocked) R.string.unblock_number else R.string.block_number
            )) },
            text = {
                Text(stringResource(
                    if (blockTargetIsBlocked) R.string.confirm_unblock_number else R.string.confirm_block_number,
                    blockTargetNumber
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (blockTargetIsBlocked) {
                            // Unblock: delete from blocked numbers
                            context.contentResolver.delete(
                                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                                arrayOf(blockTargetNumber)
                            )
                        } else {
                            // Block: add to blocked numbers
                            val values = android.content.ContentValues().apply {
                                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, blockTargetNumber)
                            }
                            context.contentResolver.insert(
                                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                                values
                            )
                        }
                    } catch (_: Exception) { }
                    showBlockConfirmation = false
                    selectedCallKeys = emptySet()
                    recentsRefreshTrigger++
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

// Check if a number is already blocked (tries multiple formats)
private fun isNumberBlocked(context: Context, number: String): Boolean {
    val formatsToCheck = setOf(
        number,
        number.filter { it.isDigit() || it == '+' },
        number.filter { it.isDigit() },
        number.filter { it.isDigit() }.takeLast(10),
    ).filter { it.isNotEmpty() }

    for (format in formatsToCheck) {
        try {
            val cursor = context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID),
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                arrayOf(format),
                null
            )
            val isBlocked = cursor?.count?.let { it > 0 } ?: false
            cursor?.close()
            if (isBlocked) return true
        } catch (_: Exception) {
            continue
        }
    }
    return false
}

/**
 * Bottom navigation with rounded pill-shaped selected indicator.
 */
@Composable
fun BottomNavigation(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    expanded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Background color from bottom_tabs_dark_background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (expanded) 64.dp else 48.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                BottomNavItemInternal(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemSelected(index) },
                    showLabel = expanded,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemInternal(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) ActivatedItemForeground else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.labelRes),
                    modifier = Modifier.size(24.dp),
                    tint = tint,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = stringResource(item.labelRes),
                modifier = Modifier.size(28.dp),
                tint = tint,
            )
        }
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    recentsQuery: String = "",
    contactsQuery: String = "",
    currentRoute: String = NavRoutes.RECENTS,
    selectedCallKeys: Set<String> = emptySet(),
    onCallSelectionChanged: (Set<String>) -> Unit = {},
    selectedContactIds: Set<Long> = emptySet(),
    onContactSelectionChanged: (Set<Long>) -> Unit = {},
    onAllCallKeys: ((Set<String>) -> Unit)? = null,
    onAllContactIds: ((Set<Long>) -> Unit)? = null,
    recentsRefreshTrigger: Int = 0,
    contactsRefreshTrigger: Int = 0,
    recentsListState: LazyListState = rememberLazyListState(),
    contactsListState: LazyListState = rememberLazyListState(),
    highlightedRecentsIndex: Int = -1,
    highlightedContactsIndex: Int = -1,
    onRecentsItemCount: (Int) -> Unit = {},
    onContactsItemCount: (Int) -> Unit = {},
    onActivateRecentsItem: (((Int) -> Unit)?) -> Unit = {},
    onActivateContactsItem: (((Int) -> Unit)?) -> Unit = {},
    highlightedSettingsIndex: Int = -1,
    settingsActivateTrigger: Int = 0,
    onSettingsItemCount: (Int) -> Unit = {},
    highlightedColorSettingsIndex: Int = -1,
    colorSettingsActivateTrigger: Int = 0,
    onColorSettingsItemCount: (Int) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onThemeModeChanged: (String) -> Unit = {},
    onColorsChanged: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.RECENTS,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(NavRoutes.RECENTS) { 
            RecentsScreen(
                searchQuery = recentsQuery,
                showFilters = true,
                selectedCallKeys = selectedCallKeys,
                onSelectionChanged = onCallSelectionChanged,
                refreshTrigger = recentsRefreshTrigger,
                listState = recentsListState,
                highlightedIndex = highlightedRecentsIndex,
                onItemCount = onRecentsItemCount,
                onActivateItem = onActivateRecentsItem,
                onInfoClick = { group ->
                    navController.navigate(NavRoutes.contactDetail(phoneNumber = group.number))
                },
                onAllSelectableKeys = onAllCallKeys,
            ) 
        }
        composable(NavRoutes.CONTACTS) { 
            ContactsScreen(
                searchQuery = contactsQuery,
                selectedContactIds = selectedContactIds,
                onSelectionChanged = onContactSelectionChanged,
                refreshTrigger = contactsRefreshTrigger,
                listState = contactsListState,
                highlightedIndex = highlightedContactsIndex,
                onItemCount = onContactsItemCount,
                onActivateItem = onActivateContactsItem,
                onContactClick = { contactId ->
                    navController.navigate(NavRoutes.contactDetail(contactId = contactId))
                },
                onAllSelectableIds = onAllContactIds,
            ) 
        }
        composable(NavRoutes.SETTINGS) { 
            SettingsScreen(
                highlightedIndex = highlightedSettingsIndex,
                activateTrigger = settingsActivateTrigger,
                onItemCount = onSettingsItemCount,
                onNavigateBack = onNavigateBack,
                onThemeModeChanged = onThemeModeChanged,
                onColorsChanged = onColorsChanged,
                onNavigateToColorSettings = {
                    navController.navigate(NavRoutes.COLOR_SETTINGS)
                },
            ) 
        }
        composable(NavRoutes.COLOR_SETTINGS) {
            ColorSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onColorsChanged = onColorsChanged,
                highlightedIndex = highlightedColorSettingsIndex,
                activateTrigger = colorSettingsActivateTrigger,
                onItemCount = onColorSettingsItemCount,
            )
        }
        composable(NavRoutes.CONTACT_DETAIL) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")?.toLongOrNull() ?: -1
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            ContactDetailScreen(
                contactId = contactId,
                phoneNumber = if (phoneNumber.isNotEmpty() && phoneNumber != "-1") phoneNumber else null,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    readOnly: Boolean = false,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    placeholder: String,
    onSettingsClick: () -> Unit,
    showUpdateBadge: Boolean = false,
    leadingIcon: ImageVector = Icons.Filled.Search,
    searchFieldFocusRequester: FocusRequester? = null,
    onSearchFieldFocused: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Use TextFieldValue internally so cursor moves to end on external changes
    var textFieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    if (textFieldValue.text != query) {
        textFieldValue = TextFieldValue(query, TextRange(query.length))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != query) {
                    onQueryChange(newValue.text)
                }
            },
            readOnly = readOnly,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .then(
                    if (searchFieldFocusRequester != null) Modifier.focusRequester(searchFieldFocusRequester)
                    else Modifier
                )
                .onFocusChanged { if (it.isFocused) onSearchFieldFocused() },
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onActiveChange(false) }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                )
            }
        }

        // Settings button in top-right corner
        Box {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showUpdateBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp),
                )
            }
        }
    }
}