package com.overklassniy.q25.dialer.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.PreferencesManager
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.model.CallLogEntry
import com.overklassniy.q25.dialer.data.model.GroupedCallLog
import com.overklassniy.q25.dialer.data.repository.CallLogRepository
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.ui.components.ContactAvatar
import com.overklassniy.q25.dialer.ui.theme.MissedCallRed
import com.overklassniy.q25.dialer.util.PermissionHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactDetailScreen(
    contactId: Long = -1,
    phoneNumber: String? = null,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasContactsPerm = remember { PermissionHelper.hasContactsPermission(context) }
    val hasCallLogPerm = remember { PermissionHelper.hasCallLogPermission(context) }
    val contactsRepo = remember { if (hasContactsPerm) ContactsRepository(context) else null }

    var contact by remember { mutableStateOf<Contact?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var allCallEntries by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    val prefs = remember { PreferencesManager(context) }
    val historyLimit = prefs.callHistoryLimit
    var displayNumber by remember { mutableStateOf(phoneNumber ?: "") }

    LaunchedEffect(contactId, phoneNumber) {
        if (contactsRepo != null) {
            contact = if (contactId > 0) {
                contactsRepo.getContactById(contactId)
            } else if (!phoneNumber.isNullOrEmpty()) {
                contactsRepo.getContactByNumber(phoneNumber)
            } else null

            contact?.let {
                isFavorite = it.starred
                if (displayNumber.isEmpty()) {
                    displayNumber = it.getPrimaryNumber() ?: ""
                }
            }
        }

        if (hasCallLogPerm) {
            val number = contact?.getPrimaryNumber() ?: phoneNumber ?: ""
            if (number.isNotEmpty()) {
                try {
                    val allCalls = CallLogRepository(context).getGroupedCallLog()
                    val matchingGroups = allCalls.filter { group ->
                        group.number.replace(" ", "").replace("-", "")
                            .endsWith(number.replace(" ", "").replace("-", "").takeLast(7))
                    }
                    allCallEntries = matchingGroups.flatMap { it.entries }
                        .sortedByDescending { it.date }
                        .take(historyLimit)
                } catch (_: Exception) { }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar with back button and star
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (contact != null) {
                IconButton(onClick = {
                    try {
                        val editIntent = Intent(Intent.ACTION_EDIT).apply {
                            data = Uri.withAppendedPath(
                                ContactsContract.Contacts.CONTENT_URI,
                                contact!!.id.toString()
                            )
                        }
                        context.startActivity(editIntent)
                    } catch (_: Exception) { }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_contact),
                    )
                }
            }

            if (contact != null) {
                IconButton(onClick = {
                    scope.launch {
                        contactsRepo?.toggleFavorite(contact!!.id, isFavorite)
                        isFavorite = !isFavorite
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Avatar + Name
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContactAvatar(
                name = contact?.name ?: "",
                photoUri = contact?.photoUri,
                size = 80.dp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = contact?.getDisplayName() ?: displayNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (contact != null && displayNumber.isNotEmpty() && displayNumber != contact?.name) {
                Text(
                    text = displayNumber,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        // Action buttons: Call, SMS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Call button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (displayNumber.isNotEmpty()) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$displayNumber")))
                        } catch (_: Exception) { }
                    }
                }.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.call),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // SMS button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (displayNumber.isNotEmpty()) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$displayNumber")))
                        } catch (_: Exception) { }
                    }
                }.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.send_sms),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Phone numbers section
        if (contact != null && contact!!.phoneNumbers.isNotEmpty()) {
            Text(
                text = stringResource(R.string.phone_numbers),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )

            contact!!.phoneNumbers.forEach { phone ->
                val typeLabel = when (phone.type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> stringResource(R.string.mobile)
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> stringResource(R.string.home)
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> stringResource(R.string.work)
                    else -> stringResource(R.string.other)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${phone.number}")))
                            } catch (_: Exception) { }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = phone.number,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = typeLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Call history section
        Text(
            text = stringResource(R.string.call_history),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        if (allCallEntries.isEmpty()) {
            Text(
                text = stringResource(R.string.no_call_history),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            allCallEntries.forEach { entry ->
                CallHistoryRow(
                    type = entry.type,
                    date = entry.date,
                    duration = entry.duration,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CallHistoryRow(
    type: Int,
    date: Long,
    duration: Long,
) {
    val callTypeIcon = when (type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.Filled.CallMissed
    }
    val iconColor = when (type) {
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> MissedCallRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val dateText = DateUtils.getRelativeTimeSpanString(
        date,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(date))
    val durationText = if (duration > 0) formatDurationDetail(duration) else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = callTypeIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (durationText.isNotEmpty()) {
                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        Text(
            text = timeText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

private fun formatDurationDetail(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}