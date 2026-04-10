package com.overklassniy.q25.dialer.data.model

import android.provider.CallLog
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val photoUri: String?,
    val date: Long,
    val duration: Long,
    val type: Int,
    val cachedLookupUri: String? = null,
) {
    val isIncoming: Boolean get() = type == CallLog.Calls.INCOMING_TYPE
    val isOutgoing: Boolean get() = type == CallLog.Calls.OUTGOING_TYPE
    val isMissed: Boolean get() = type == CallLog.Calls.MISSED_TYPE
    val isRejected: Boolean get() = type == CallLog.Calls.REJECTED_TYPE
    val isBlocked: Boolean get() = type == CallLog.Calls.BLOCKED_TYPE
}

@Immutable
data class GroupedCallLog(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val entries: ImmutableList<CallLogEntry>,
) {
    val latestDate: Long get() = entries.maxOf { it.date }
    val latestType: Int get() = entries.maxByOrNull { it.date }?.type ?: 0
    val count: Int get() = entries.size
    val duration: Long get() = entries.maxByOrNull { it.date }?.duration ?: 0
    val simId: Int? get() = entries.maxByOrNull { it.date }?.let { 1 } // Placeholder for SIM ID
}