package com.overklassniy.q25.dialer.data.repository

import android.content.Context
import android.provider.CallLog
import com.overklassniy.q25.dialer.data.model.CallLogEntry
import com.overklassniy.q25.dialer.data.model.GroupedCallLog
import com.overklassniy.q25.dialer.util.PhoneUtils
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    private val contactsRepository by lazy { ContactsRepository(context) }

    suspend fun getCallLog(limit: Int = 500): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_PHOTO_URI,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
            CallLog.Calls.CACHED_LOOKUP_URI,
        )

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val photoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val lookupIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_LOOKUP_URI)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val number = cursor.getString(numberIdx) ?: ""
                val cachedName = cursor.getString(nameIdx)
                val cachedPhotoUri = cursor.getString(photoIdx)

                // Resolve contact info in real-time to handle added/removed contacts
                val resolvedContact = if (number.isNotEmpty()) {
                    try {
                        contactsRepository.getContactByNumber(number)
                    } catch (_: Exception) { null }
                } else null

                entries.add(
                    CallLogEntry(
                        id = cursor.getLong(idIdx),
                        number = number,
                        name = resolvedContact?.name ?: cachedName,
                        photoUri = resolvedContact?.photoUri ?: cachedPhotoUri,
                        date = cursor.getLong(dateIdx),
                        duration = cursor.getLong(durationIdx),
                        type = cursor.getInt(typeIdx),
                        cachedLookupUri = cursor.getString(lookupIdx),
                    )
                )
                count++
            }
        }

        entries
    }

    suspend fun getGroupedCallLog(limit: Int = 500): List<GroupedCallLog> {
        val entries = getCallLog(limit)
        val groups = mutableListOf<GroupedCallLog>()

        for (entry in entries) {
            val lastGroup = groups.lastOrNull()
            if (lastGroup != null &&
                PhoneUtils.numbersMatch(lastGroup.number, entry.number) &&
                lastGroup.entries.first().type == entry.type
            ) {
                val updatedEntries = (lastGroup.entries + entry).toImmutableList()
                groups[groups.lastIndex] = lastGroup.copy(entries = updatedEntries)
            } else {
                groups.add(
                    GroupedCallLog(
                        number = entry.number,
                        name = entry.name,
                        photoUri = entry.photoUri,
                        entries = kotlinx.collections.immutable.persistentListOf(entry),
                    )
                )
            }
        }

        return groups
    }

    suspend fun deleteCallLog() = withContext(Dispatchers.IO) {
        context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
    }
}