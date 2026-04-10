package com.overklassniy.q25.dialer.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class Contact(
    val id: Long,
    val name: String,
    val phoneNumbers: ImmutableList<PhoneNumber>,
    val photoUri: String? = null,
    val starred: Boolean = false,
    val lookupKey: String? = null,
) {
    fun getDisplayName(): String = name.ifEmpty { phoneNumbers.firstOrNull()?.number ?: "" }

    fun getPrimaryNumber(): String? = phoneNumbers.firstOrNull()?.number
}

@Immutable
data class PhoneNumber(
    val number: String,
    val normalizedNumber: String,
    val type: Int = 0,
    val label: String = "",
)