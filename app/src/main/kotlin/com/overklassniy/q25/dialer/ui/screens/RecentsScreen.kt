package com.overklassniy.q25.dialer.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.text.format.DateUtils
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.overklassniy.q25.dialer.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.model.GroupedCallLog
import com.overklassniy.q25.dialer.data.repository.CallLogRepository
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.ui.components.ContactAvatar
import com.overklassniy.q25.dialer.ui.components.ContactItem
import com.overklassniy.q25.dialer.ui.theme.MissedCallRed
import com.overklassniy.q25.dialer.ui.theme.RejectedCallOrange
import com.overklassniy.q25.dialer.util.PermissionHelper
import com.overklassniy.q25.dialer.util.T9Matcher

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(
    searchQuery: String = "",
    showFilters: Boolean = false,
    selectedCallKeys: Set<String> = emptySet(),
    onSelectionChanged: (Set<String>) -> Unit = {},
    refreshTrigger: Int = 0,
    listState: LazyListState = rememberLazyListState(),
    highlightedIndex: Int = -1,
    onItemCount: (Int) -> Unit = {},
    onActivateItem: (((Int) -> Unit)?) -> Unit = {},
    onInfoClick: (GroupedCallLog) -> Unit = {},
    onAllSelectableKeys: ((Set<String>) -> Unit)? = null,
) {
    val context = LocalContext.current

    var hasCallLogPerm by remember { mutableStateOf(PermissionHelper.hasCallLogPermission(context)) }
    var hasContactsPerm by remember { mutableStateOf(PermissionHelper.hasContactsPermission(context)) }

    // Reload permissions when screen resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasCallLogPerm = PermissionHelper.hasCallLogPermission(context)
                hasContactsPerm = PermissionHelper.hasContactsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var callLog by remember { mutableStateOf<List<GroupedCallLog>?>(null) }
    // Increment this to force reload from ContentObserver
    var callLogRefreshTrigger by remember { mutableIntStateOf(0) }
    // Increment this to force reload when contacts change (added/removed)
    var contactsRefreshTrigger by remember { mutableIntStateOf(0) }

    // Real-time call log refresh via ContentObserver
    val callLogObserver = remember {
        object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Trigger reload on next composition
                callLogRefreshTrigger++
            }
        }
    }
    DisposableEffect(hasCallLogPerm) {
        if (hasCallLogPerm) {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            )
        }
        onDispose {
            context.contentResolver.unregisterContentObserver(callLogObserver)
        }
    }

    // Real-time contacts refresh via ContentObserver - triggers both contacts and call log reload
    val contactsObserver = remember {
        object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Trigger reload of both contacts and call log on next composition
                contactsRefreshTrigger++
                callLogRefreshTrigger++
            }
        }
    }
    DisposableEffect(hasContactsPerm) {
        if (hasContactsPerm) {
            context.contentResolver.registerContentObserver(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
            )
        }
        onDispose {
            context.contentResolver.unregisterContentObserver(contactsObserver)
        }
    }

    var allContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var blockedNumbers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Missed, 2=Incoming, 3=Outgoing, 4=Rejected
    val isSelectionMode = selectedCallKeys.isNotEmpty()

    // Load blocked numbers (and refresh when refreshTrigger changes - e.g. after block/unblock)
    LaunchedEffect(refreshTrigger) {
        blockedNumbers = loadBlockedNumbers(context)
    }

    // Reload data when permissions change, after deletion, or when call log/contacts change
    LaunchedEffect(hasCallLogPerm, hasContactsPerm, refreshTrigger, callLogRefreshTrigger, contactsRefreshTrigger) {
        if (hasCallLogPerm) {
            callLog = try {
                CallLogRepository(context).getGroupedCallLog()
            } catch (_: Exception) {
                emptyList()
            }
        }
        if (hasContactsPerm) {
            allContacts = try {
                ContactsRepository(context).getContacts()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    // Map phone number -> photoUri from contacts (to fix missing avatars in call log)
    val contactPhotoMap = remember(allContacts) {
        val map = mutableMapOf<String, String>()
        allContacts.forEach { contact ->
            if (contact.photoUri != null) {
                contact.phoneNumbers.forEach { pn ->
                    val normalized = pn.number.filter { it.isDigit() || it == '+' }
                    map[normalized] = contact.photoUri
                    // Also store last 10 digits for fuzzy matching
                    val digits = pn.number.filter { it.isDigit() }
                    if (digits.length >= 10) {
                        map[digits.takeLast(10)] = contact.photoUri
                    }
                }
            }
        }
        map
    }

    // T9 matching contacts from searchQuery (used as dialpad input)
    val matchingContacts by remember(searchQuery, allContacts) {
        derivedStateOf {
            if (searchQuery.isEmpty()) emptyList()
            else allContacts.filter { contact ->
                T9Matcher.matchesT9(contact.name, searchQuery) ||
                    contact.phoneNumbers.any { T9Matcher.matchesNumber(it.number, searchQuery) }
            }.take(5)
        }
    }

    // Filter call log by type and search query
    val filteredCallLog = remember(callLog, selectedFilter, searchQuery) {
        var filtered = callLog
        
        // Filter by type
        if (filtered != null && selectedFilter != 0) {
            filtered = filtered.filter { group ->
                when (selectedFilter) {
                    1 -> group.latestType == CallLog.Calls.MISSED_TYPE
                    2 -> group.latestType == CallLog.Calls.INCOMING_TYPE
                    3 -> group.latestType == CallLog.Calls.OUTGOING_TYPE
                    4 -> group.latestType == CallLog.Calls.REJECTED_TYPE
                    else -> true
                }
            }
        }
        
        // Filter by search query (name or number)
        if (filtered != null && searchQuery.isNotEmpty()) {
            filtered = filtered.filter { group ->
                val nameMatches = group.name?.contains(searchQuery, ignoreCase = true) ?: false
                val numberMatches = group.number.contains(searchQuery, ignoreCase = true)
                nameMatches || numberMatches
            }
        }
        
        filtered
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Call type filters - matching layout_recents_filters.xml
        if (showFilters) {
            RecentsFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { 
                    selectedFilter = it
                },
            )
        }

        // T9 matching contacts from search input
        if (matchingContacts.isNotEmpty()) {
            matchingContacts.forEach { contact ->
                ContactItem(
                    contact = contact,
                    onClick = {
                        val number = contact.getPrimaryNumber() ?: return@ContactItem
                        try {
                            context.startActivity(Intent(Intent.ACTION_CALL, "tel:$number".toUri()))
                        } catch (_: Exception) { }
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Call log list
        when {
            filteredCallLog == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            filteredCallLog.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_recents),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
            else -> {
                // Report item count and all selectable keys
                LaunchedEffect(filteredCallLog) {
                    onItemCount(filteredCallLog.size)
                    onAllSelectableKeys?.invoke(
                        filteredCallLog.map { "${it.number}_${it.latestDate}" }.toSet()
                    )
                }
                // Scroll to keep highlighted item visible
                LaunchedEffect(highlightedIndex) {
                    if (highlightedIndex >= 0 && highlightedIndex < filteredCallLog.size) {
                        try { listState.animateScrollToItem(highlightedIndex) } catch (_: Exception) {}
                    }
                }
                DisposableEffect(filteredCallLog) {
                    val activator: (Int) -> Unit = { index ->
                        if (index in filteredCallLog.indices) {
                            val group = filteredCallLog[index]
                            try {
                                context.startActivity(Intent(Intent.ACTION_CALL,
                                    "tel:${group.number}".toUri()))
                            } catch (_: Exception) { }
                        }
                    }
                    onActivateItem(activator)
                    onDispose { onActivateItem(null) }
                }

                val density = LocalDensity.current
                val maxFlingPx = with(density) { 4000.dp.toPx() }
                val decayAnimationSpec = rememberSplineBasedDecay<Float>()
                val limitedFlingBehavior = remember(decayAnimationSpec, maxFlingPx) {
                    object : FlingBehavior {
                        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                            val clamped = initialVelocity.coerceIn(-maxFlingPx, maxFlingPx)
                            if (kotlin.math.abs(clamped) < 1f) return initialVelocity
                            var velocityLeft = clamped
                            var lastValue = 0f
                            AnimationState(
                                initialValue = 0f,
                                initialVelocity = clamped,
                            ).animateDecay(decayAnimationSpec) {
                                val delta = value - lastValue
                                lastValue = value
                                val consumed = scrollBy(delta)
                                if (kotlin.math.abs(delta - consumed) > 0.5f) this.cancelAnimation()
                                velocityLeft = this.velocity
                            }
                            return velocityLeft
                        }
                    }
                }
                
                // Scroll to top when filter changes
                LaunchedEffect(selectedFilter) {
                    listState.scrollToItem(0)
                }
                
                LazyColumn(
                    state = listState,
                    flingBehavior = limitedFlingBehavior,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredCallLog, key = { "${it.number}_${it.latestDate}" }, contentType = { "call_log" }) { group ->
                        val callKey = "${group.number}_${group.latestDate}"
                        val isSelected = callKey in selectedCallKeys
                        val itemIndex = filteredCallLog.indexOf(group)
                        val isHighlighted = itemIndex == highlightedIndex
                        // Resolve photo from contacts if call log doesn't have it
                        val resolvedPhotoUri = group.photoUri ?: run {
                            val normalized = group.number.filter { it.isDigit() || it == '+' }
                            contactPhotoMap[normalized]
                                ?: contactPhotoMap[group.number.filter { it.isDigit() }.takeLast(10)]
                        }
                        // Check if this number is blocked
                        val isBlocked = isNumberBlocked(group.number, blockedNumbers)
                        CallLogItem(
                            group = group.copy(photoUri = resolvedPhotoUri),
                            isSelected = isSelected,
                            isHighlighted = isHighlighted,
                            isBlocked = isBlocked,
                            onCall = {
                                if (isSelectionMode) {
                                    val newSet = selectedCallKeys.toMutableSet()
                                    if (isSelected) newSet.remove(callKey) else newSet.add(callKey)
                                    onSelectionChanged(newSet)
                                } else {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_CALL,
                                            "tel:${group.number}".toUri()))
                                    } catch (_: Exception) { }
                                }
                            },
                            onLongClick = {
                                val newSet = selectedCallKeys.toMutableSet()
                                if (isSelected) newSet.remove(callKey) else newSet.add(callKey)
                                onSelectionChanged(newSet)
                            },
                            onInfoClick = { onInfoClick(group) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recents filter bar matching layout_recents_filters.xml
 * Filter buttons: All, Missed, Incoming, Outgoing, Rejected
 */
@Composable
private fun RecentsFilterBar(
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
) {
    val filters = listOf(
        R.string.all to 0,
        R.string.missed to 1,
        R.string.incoming to 2,
        R.string.outgoing to 3,
        R.string.rejected to 4,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        filters.forEach { (labelRes, filterId) ->
            val isSelected = selectedFilter == filterId
            Text(
                text = stringResource(labelRes),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(filterId) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogItem(
    group: GroupedCallLog,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    isBlocked: Boolean = false,
    onCall: () -> Unit,
    onLongClick: () -> Unit = {},
    onInfoClick: () -> Unit = {},
) {
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val highlightedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (isHighlighted) Modifier.background(highlightedBg)
                else if (isSelected) Modifier.background(selectedBg)
                else Modifier
            )
            .combinedClickable(
                onClick = onCall,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar: 48dp - gray ? for unknown, letter for known contacts
            ContactAvatar(
                name = group.name ?: "",
                photoUri = group.photoUri,
                size = 48.dp,
            )

            // Main content column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            ) {
                // Name row with date on the right side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    val nameColor = when (group.latestType) {
                        CallLog.Calls.MISSED_TYPE -> MissedCallRed
                        CallLog.Calls.REJECTED_TYPE -> RejectedCallOrange
                        CallLog.Calls.BLOCKED_TYPE -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onBackground
                    }

                    // Name (or number if no contact) + call count badge + blocked icon
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Blocked icon indicator (shown first for visibility)
                        if (isBlocked) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = stringResource(R.string.blocked),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            text = if (group.name.isNullOrBlank()) group.number else group.name,
                            fontSize = 14.sp,
                            color = nameColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (group.count > 1) {
                            Text(
                                text = " (${group.count})",
                                fontSize = 14.sp,
                                color = nameColor,
                            )
                        }
                    }

                    // Date at top right (e.g., "3 ч назад", "Позавчера")
                    val dateText = DateUtils.getRelativeTimeSpanString(
                        group.latestDate,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE,
                    ).toString()

                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }

                // Time (HH:MM) below the date - aligned to right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left side: call type + duration (no gap), then number
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Icon + duration in Box (absolutely zero spacing)
                        Box(
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val callTypeIcon = when (group.latestType) {
                                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                                CallLog.Calls.REJECTED_TYPE -> Icons.Filled.PhoneDisabled
                                CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block
                                else -> Icons.AutoMirrored.Filled.CallMissed
                            }
                            val iconColor = when (group.latestType) {
                                CallLog.Calls.MISSED_TYPE -> MissedCallRed
                                CallLog.Calls.REJECTED_TYPE -> RejectedCallOrange
                                CallLog.Calls.BLOCKED_TYPE -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }

                            Icon(
                                imageVector = callTypeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = iconColor,
                            )

                            // SIM placeholder only if present - positioned after icon
                            group.simId?.let {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 0.dp)
                                        .size(18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // SIM icon placeholder
                                }
                            }

                            // Duration positioned right after icon (16dp = icon width)
                            if (group.duration > 0) {
                                Text(
                                    text = formatDuration(group.duration),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 21.dp),
                                )
                            }
                        }

                        // ALWAYS show number
                        Text(
                            text = group.number,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Time at right (HH:MM format)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeText = timeFormat.format(Date(group.latestDate))

                    Text(
                        text = timeText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            // Info button
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .size(58.dp)
                    .wrapContentWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.show_call_details),
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // Divider at bottom - no bottom padding (removed gap)
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
    }
}

// Helper function to format call duration
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}

// Load all blocked numbers from system
private fun loadBlockedNumbers(context: Context): Set<String> {
    val blocked = mutableSetOf<String>()
    try {
        val cursor = context.contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null,
            null,
            null
        )
        cursor?.use {
            val numberIdx = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
            while (it.moveToNext()) {
                it.getString(numberIdx)?.let { num -> blocked.add(num) }
            }
        }
    } catch (_: Exception) { }
    return blocked
}

// Check if number is in blocked set (tries multiple formats)
private fun isNumberBlocked(number: String, blockedSet: Set<String>): Boolean {
    val formats = setOf(
        number,
        number.filter { it.isDigit() || it == '+' },
        number.filter { it.isDigit() },
        number.filter { it.isDigit() }.takeLast(10),
    ).filter { it.isNotEmpty() }
    return formats.any { it in blockedSet }
}