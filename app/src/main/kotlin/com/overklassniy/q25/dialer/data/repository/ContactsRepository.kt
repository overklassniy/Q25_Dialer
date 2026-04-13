package com.overklassniy.q25.dialer.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.model.PhoneNumber
import com.overklassniy.q25.dialer.util.PhoneUtils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<Long, MutableContact>()

        // First pass: get contact info
        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.LOOKUP_KEY,
        )

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val photoFullIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
            val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                contactsMap[id] = MutableContact(
                    id = id,
                    name = cursor.getString(nameIdx) ?: "",
                    photoUri = cursor.getString(photoIdx),
                    photoFullUri = cursor.getString(photoFullIdx),
                    starred = cursor.getInt(starredIdx) == 1,
                    lookupKey = cursor.getString(lookupIdx),
                )
            }
        }

        // Second pass: get phone numbers
        val phoneProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            phoneProjection,
            null,
            null,
            null,
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normalizedIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdIdx)
                val number = cursor.getString(numberIdx) ?: continue
                val normalized = cursor.getString(normalizedIdx) ?: PhoneUtils.normalizeNumber(number)
                val type = cursor.getInt(typeIdx)
                val label = cursor.getString(labelIdx) ?: ""

                contactsMap[contactId]?.phoneNumbers?.add(
                    PhoneNumber(number, normalized, type, label)
                )
            }
        }

        contactsMap.values
            .filter { it.phoneNumbers.isNotEmpty() }
            .map { it.toContact() }
    }

    suspend fun getFavorites(): List<Contact> {
        return getContacts().filter { it.starred }
    }

    fun getContactByNumber(number: String): Contact? {
        // Try standard PhoneLookup first
        phoneLookup(number)?.let { return it }

        // Fallback for Russian numbers: 8XXXXXXXXXX = +7XXXXXXXXXX
        val normalized = PhoneUtils.normalizeNumber(number)
        if (normalized.startsWith("+7") && normalized.length == 12) {
            phoneLookup("8${normalized.drop(2)}")?.let { return it }
        } else if (normalized.startsWith("8") && normalized.length == 11) {
            phoneLookup("+7${normalized.drop(1)}")?.let { return it }
        }

        // Last-resort fallback: match by last 10 digits against Phone table
        if (normalized.length >= 7) {
            val suffix = normalized.takeLast(10)
            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection, null, null, null,
            )?.use { cursor ->
                val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val candidateNumber = cursor.getString(numberIdx) ?: continue
                    val candidateNorm = PhoneUtils.normalizeNumber(candidateNumber)
                    if (candidateNorm.takeLast(10) == suffix) {
                        val contactId = cursor.getLong(contactIdIdx)
                        return phoneLookupById(contactId, number)
                    }
                }
            }
        }
        return null
    }

    private fun phoneLookup(number: String): Contact? {
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.STARRED,
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return Contact(
                    id = cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)),
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)) ?: "",
                    phoneNumbers = persistentListOf(PhoneNumber(number, PhoneUtils.normalizeNumber(number))),
                    photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)),
                    photoFullUri = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)),
                    lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)),
                    starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)) == 1,
                )
            }
        }
        return null
    }

    private fun phoneLookupById(contactId: Long, originalNumber: String): Contact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.STARRED,
        )
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return Contact(
                    id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: "",
                    phoneNumbers = persistentListOf(PhoneNumber(originalNumber, PhoneUtils.normalizeNumber(originalNumber))),
                    photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)),
                    photoFullUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)),
                    lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)),
                    starred = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)) == 1,
                )
            }
        }
        return null
    }

    suspend fun toggleFavorite(contactId: Long, currentlyStarred: Boolean): Boolean = withContext(Dispatchers.IO) {
        val newValue = if (currentlyStarred) 0 else 1
        val values = android.content.ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, newValue)
        }
        val updated = context.contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
        updated > 0
    }

    suspend fun getContactById(contactId: Long): Contact? = withContext(Dispatchers.IO) {
        val contacts = getContacts()
        contacts.find { it.id == contactId }
    }

    private data class MutableContact(
        val id: Long,
        val name: String,
        val photoUri: String?,
        val photoFullUri: String? = null,
        val starred: Boolean,
        val lookupKey: String?,
        val phoneNumbers: MutableList<PhoneNumber> = mutableListOf(),
    ) {
        fun toContact() = Contact(id, name, phoneNumbers.toImmutableList(), photoUri, photoFullUri, starred, lookupKey)
    }
}