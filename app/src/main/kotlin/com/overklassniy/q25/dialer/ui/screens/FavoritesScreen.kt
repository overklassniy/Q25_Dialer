package com.overklassniy.q25.dialer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.provider.ContactsContract
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.ui.components.ContactItem
import com.overklassniy.q25.dialer.util.PermissionHelper

@Composable
fun FavoritesScreen(searchQuery: String = "") {
    val context = LocalContext.current
    val hasPermission = remember { PermissionHelper.hasContactsPermission(context) }
    val repository = remember { if (hasPermission) ContactsRepository(context) else null }
    var favorites by remember { mutableStateOf<List<Contact>?>(null) }

    // Trigger to reload data when contacts change (added/removed)
    var contactsRefreshTrigger by remember { mutableIntStateOf(0) }

    // Real-time contacts refresh via ContentObserver
    val contactsObserver = remember {
        object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Trigger reload on next composition
                contactsRefreshTrigger++
            }
        }
    }
    DisposableEffect(hasPermission) {
        if (hasPermission) {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
            )
        }
        onDispose {
            context.contentResolver.unregisterContentObserver(contactsObserver)
        }
    }

    LaunchedEffect(Unit, contactsRefreshTrigger) {
        favorites = try {
            repository?.getFavorites() ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    // Filter by search query
    val filteredFavorites = remember(favorites, searchQuery) {
        if (searchQuery.isEmpty()) {
            favorites
        } else {
            favorites?.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.number.contains(searchQuery) }
            }
        }
    }

    when {
        filteredFavorites == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        filteredFavorites.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_favorites),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredFavorites, key = { it.id }) { contact ->
                    ContactItem(
                        contact = contact,
                        onClick = {
                            val number = contact.getPrimaryNumber() ?: return@ContactItem
                            try {
                                context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                            } catch (_: Exception) { }
                        },
                    )
                }
            }
        }
    }
}