package com.overklassniy.q25.dialer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.db.AppDatabase
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.model.SpeedDial
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.data.repository.SpeedDialRepository
import com.overklassniy.q25.dialer.ui.components.ContactAvatar
import com.overklassniy.q25.dialer.util.PermissionHelper
import kotlinx.coroutines.launch

private val SPEED_DIAL_SLOTS = (2..9).toList()

@Composable
fun SpeedDialSettingsScreen(
    onNavigateBack: () -> Unit = {},
    highlightedIndex: Int = -1,
    activateTrigger: Int = 0,
    onItemCount: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { SpeedDialRepository(db) }
    val scope = rememberCoroutineScope()

    val speedDials by repo.getAll().collectAsState(initial = emptyList())
    val speedDialMap = remember(speedDials) {
        speedDials.associateBy { it.slot }
    }

    var showContactPicker by remember { mutableStateOf<Int?>(null) } // slot being assigned
    var contactPickerQuery by remember { mutableStateOf("") }

    val totalItems = SPEED_DIAL_SLOTS.size
    LaunchedEffect(totalItems) { onItemCount(totalItems) }

    // Track positions for keyboard scroll
    val itemPositions = remember { mutableMapOf<Int, Int>() }
    val listState = rememberLazyListState()

    // Scroll to highlighted item
    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex >= 0 && highlightedIndex < SPEED_DIAL_SLOTS.size) {
            listState.animateScrollToItem(highlightedIndex)
        }
    }

    // Activate highlighted item on trigger
    LaunchedEffect(activateTrigger) {
        if (highlightedIndex >= 0 && highlightedIndex < SPEED_DIAL_SLOTS.size && activateTrigger > 0) {
            showContactPicker = SPEED_DIAL_SLOTS[highlightedIndex]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.settings_speed_dial),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = SPEED_DIAL_SLOTS,
                key = { it },
            ) { slot ->
                val index = slot - 2
                val assignment = speedDialMap[slot]
                val isHighlighted = highlightedIndex == index

                SpeedDialSlotItem(
                    slot = slot,
                    assignment = assignment,
                    isHighlighted = isHighlighted,
                    onAssign = { showContactPicker = slot },
                    onRemove = {
                        scope.launch { repo.remove(slot) }
                    },
                    onCall = { number ->
                        try {
                            val encoded = Uri.encode(number, "+*")
                            context.startActivity(
                                Intent(Intent.ACTION_CALL, Uri.parse("tel:$encoded"))
                            )
                        } catch (_: Exception) {}
                    },
                    onPositioned = { pos -> itemPositions[index] = pos },
                )
            }
        }
    }

    // Contact picker dialog
    showContactPicker?.let { targetSlot ->
        ContactPickerDialog(
            query = contactPickerQuery,
            onQueryChange = { contactPickerQuery = it },
            onContactSelected = { contact ->
                val number = contact.getPrimaryNumber() ?: return@ContactPickerDialog
                val name = contact.getDisplayName()
                scope.launch {
                    repo.set(SpeedDial(slot = targetSlot, number = number, name = name))
                }
                showContactPicker = null
                contactPickerQuery = ""
            },
            onDismiss = {
                showContactPicker = null
                contactPickerQuery = ""
            },
        )
    }
}

@Composable
private fun SpeedDialSlotItem(
    slot: Int,
    assignment: SpeedDial?,
    isHighlighted: Boolean,
    onAssign: () -> Unit,
    onRemove: () -> Unit,
    onCall: (String) -> Unit,
    onPositioned: ((Int) -> Unit)? = null,
) {
    val backgroundColor = if (isHighlighted)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coordinates ->
                onPositioned?.invoke(coordinates.positionInParent().y.toInt())
            }
            .background(backgroundColor)
            .clickable(onClick = onAssign)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Digit circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = slot.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(16.dp))

        // Contact info or "Not assigned"
        Column(modifier = Modifier.weight(1f)) {
            if (assignment != null) {
                Text(
                    text = assignment.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = assignment.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.speed_dial_not_assigned),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Action buttons
        if (assignment != null) {
            // Call button
            IconButton(
                onClick = { onCall(assignment.number) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = stringResource(R.string.call),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.speed_dial_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    )
}

@Composable
private fun ContactPickerDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onContactSelected: (Contact) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val hasPermission = remember { PermissionHelper.hasContactsPermission(context) }
    val repository = remember { if (hasPermission) ContactsRepository(context) else null }
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (hasPermission) {
            try {
                contacts = ContactsRepository(context).getContacts()
            } catch (_: Exception) {
                contacts = emptyList()
            }
        }
    }

    val filtered = remember(contacts, query) {
        if (contacts == null) return@remember emptyList()
        if (query.isBlank()) return@remember contacts!!
        contacts!!.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumbers.any { it.number.contains(query) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.speed_dial_pick_contact)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search field
                BasicTextField(
                    value = TextFieldValue(query, selection = TextRange(query.length)),
                    onValueChange = { onQueryChange(it.text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.speed_dial_search_contacts),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )

                // Contact list
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_contacts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    items(
                        items = filtered,
                        key = { it.id },
                    ) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSelected(contact) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ContactAvatar(
                                photoUri = contact.photoUri,
                                name = contact.getDisplayName(),
                                size = 36.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.getDisplayName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                contact.getPrimaryNumber()?.let { number ->
                                    Text(
                                        text = number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
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