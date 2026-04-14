package com.overklassniy.q25.dialer.ui.screens

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.model.Contact
import com.overklassniy.q25.dialer.data.repository.ContactsRepository
import com.overklassniy.q25.dialer.ui.components.ContactItem
import com.overklassniy.q25.dialer.util.PermissionHelper
import com.overklassniy.q25.dialer.util.T9Matcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    searchQuery: String = "",
    selectedContactIds: Set<Long> = emptySet(),
    onSelectionChanged: (Set<Long>) -> Unit = {},
    refreshTrigger: Int = 0,
    listState: LazyListState = rememberLazyListState(),
    highlightedIndex: Int = -1,
    onItemCount: (Int) -> Unit = {},
    onActivateItem: (((Int) -> Unit)?) -> Unit = {},
    onContactClick: (Long) -> Unit = {},
    onFavoriteToggled: () -> Unit = {},
    onAllSelectableIds: ((Set<Long>) -> Unit)? = null,
) {
    val context = LocalContext.current
    val hasPermission = remember { PermissionHelper.hasContactsPermission(context) }
    val repository = remember { if (hasPermission) ContactsRepository(context) else null }
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    var favorites by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val isSelectionMode = selectedContactIds.isNotEmpty()

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
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                true,
                contactsObserver
            )
        }
        onDispose {
            context.contentResolver.unregisterContentObserver(contactsObserver)
        }
    }

    // Reload data when permissions change, after deletion, or when contacts change
    LaunchedEffect(hasPermission, refreshTrigger, contactsRefreshTrigger) {
        if (hasPermission) {
            try {
                contacts = ContactsRepository(context).getContacts()
                favorites = ContactsRepository(context).getFavorites()
            } catch (_: Exception) {
                contacts = emptyList()
                favorites = emptyList()
            }
        }
    }

    val filteredContacts by remember(contacts, favorites, searchQuery) {
        derivedStateOf {
            val list = contacts ?: return@derivedStateOf null
            val favList = favorites
            
            // Filter and separate favorites from regular contacts
            val filtered = if (searchQuery.isEmpty()) {
                list
            } else {
                val isDigitQuery = searchQuery.all { it.isDigit() || it == '+' || it == '*' || it == '#' }
                list.filter { contact ->
                    if (isDigitQuery) {
                        T9Matcher.matchesT9(contact.name, searchQuery) ||
                            contact.phoneNumbers.any { T9Matcher.matchesNumber(it.number, searchQuery) }
                    } else {
                        contact.name.contains(searchQuery, ignoreCase = true) ||
                            contact.phoneNumbers.any { it.number.contains(searchQuery) }
                    }
                }
            }
            
            // Separate favorites from regular contacts
            val favoriteIds = favList.map { it.id }.toSet()
            val favoriteContacts = filtered.filter { it.id in favoriteIds }
            val regularContacts = filtered.filter { it.id !in favoriteIds }
                .sortedBy { it.name.uppercase() }
            
            Pair(favoriteContacts, regularContacts)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            filteredContacts == null -> {
                // Loading state - show empty screen without spinner
                Box(Modifier.fillMaxSize())
            }
            filteredContacts!!.first.isEmpty() && filteredContacts!!.second.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_contacts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
            else -> {
                val (favoriteContacts, regularContacts) = filteredContacts!!
                
                val favoriteIds = remember(favoriteContacts) { favoriteContacts.map { it.id }.toSet() }
                
                // Combined list for keyboard navigation (favorites first, then regular)
                val allContactsList = remember(favoriteContacts, regularContacts) {
                    favoriteContacts + regularContacts
                }

                // Report item count and all selectable IDs
                LaunchedEffect(allContactsList) {
                    onItemCount(allContactsList.size)
                    onAllSelectableIds?.invoke(allContactsList.map { it.id }.toSet())
                }
                // Scroll to keep highlighted item visible (offset for header items)
                LaunchedEffect(highlightedIndex, favoriteContacts.size, regularContacts.size) {
                    if (highlightedIndex >= 0 && highlightedIndex < allContactsList.size) {
                        val lazyIndex = if (highlightedIndex < favoriteContacts.size) {
                            1 + highlightedIndex // +1 for favorites header
                        } else {
                            val headersCount = (if (favoriteContacts.isNotEmpty()) 1 else 0) +
                                    (if (regularContacts.isNotEmpty()) 1 else 0)
                            headersCount + highlightedIndex
                        }
                        try { listState.animateScrollToItem(lazyIndex) } catch (_: Exception) {}
                    }
                }
                DisposableEffect(allContactsList) {
                    val activator: (Int) -> Unit = { index ->
                        if (index in allContactsList.indices) {
                            onContactClick(allContactsList[index].id)
                        }
                    }
                    onActivateItem(activator)
                    onDispose { onActivateItem(null) }
                }

                Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        
                    // Main contact list – limited fling speed for smooth scroll
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
                                    // Stop if we hit the edge
                                    if (kotlin.math.abs(delta - consumed) > 0.5f) this.cancelAnimation()
                                    velocityLeft = this.velocity
                                }
                                return velocityLeft
                            }
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        flingBehavior = limitedFlingBehavior,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        // Favorites section
                        if (favoriteContacts.isNotEmpty()) {
                            item(contentType = "header") {
                                Text(
                                    text = stringResource(R.string.favorites),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            items(favoriteContacts, key = { "fav_${it.id}" }, contentType = { "contact" }) { contact ->
                                val isSelected = contact.id in selectedContactIds
                                val flatIndex = allContactsList.indexOf(contact)
                                val isHighlighted = flatIndex == highlightedIndex
                                ContactItem(
                                    contact = contact,
                                    onClick = {
                                        if (isSelectionMode) {
                                            val newSet = selectedContactIds.toMutableSet()
                                            if (isSelected) newSet.remove(contact.id) else newSet.add(contact.id)
                                            onSelectionChanged(newSet)
                                        } else {
                                            onContactClick(contact.id)
                                        }
                                    },
                                    onLongClick = {
                                        val newSet = selectedContactIds.toMutableSet()
                                        if (isSelected) newSet.remove(contact.id) else newSet.add(contact.id)
                                        onSelectionChanged(newSet)
                                    },
                                    isSelected = isSelected || isHighlighted,
                                    isFavorite = true,
                                    onFavoriteClick = {
                                        scope.launch {
                                            repository?.toggleFavorite(contact.id, true)
                                            contacts = ContactsRepository(context).getContacts()
                                            favorites = ContactsRepository(context).getFavorites()
                                            onFavoriteToggled()
                                        }
                                    },
                                )
                            }
                        }
                        
                        // All contacts section header
                        if (regularContacts.isNotEmpty()) {
                            item(contentType = "header") {
                                Text(
                                    text = stringResource(R.string.all_contacts),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        items(regularContacts, key = { it.id }, contentType = { "contact" }) { contact ->
                            val isFav = contact.id in favoriteIds
                            val isSelected = contact.id in selectedContactIds
                            val flatIndex = allContactsList.indexOf(contact)
                            val isHighlighted = flatIndex == highlightedIndex
                            ContactItem(
                                contact = contact,
                                onClick = {
                                    if (isSelectionMode) {
                                        val newSet = selectedContactIds.toMutableSet()
                                        if (isSelected) newSet.remove(contact.id) else newSet.add(contact.id)
                                        onSelectionChanged(newSet)
                                    } else {
                                        onContactClick(contact.id)
                                    }
                                },
                                onLongClick = {
                                    val newSet = selectedContactIds.toMutableSet()
                                    if (isSelected) newSet.remove(contact.id) else newSet.add(contact.id)
                                    onSelectionChanged(newSet)
                                },
                                isSelected = isSelected || isHighlighted,
                                isFavorite = isFav,
                                onFavoriteClick = {
                                    scope.launch {
                                        repository?.toggleFavorite(contact.id, isFav)
                                        contacts = ContactsRepository(context).getContacts()
                                        favorites = ContactsRepository(context).getFavorites()
                                        onFavoriteToggled()
                                    }
                                },
                            )
                        }
                    }
                    
                    // Alphabet scrollbar – proportionally scaled to fit screen
                    if (searchQuery.isEmpty() && regularContacts.isNotEmpty()) {
                        // Group non-letter chars into '*'
                        val letters = remember(regularContacts) {
                            regularContacts
                                .map { c ->
                                    val first = c.name.firstOrNull()?.uppercaseChar()
                                    if (first != null && first.isLetter()) first else '*'
                                }
                                .distinct()
                                .sorted()
                                .let { list ->
                                    val hasSymbol = list.contains('*')
                                    if (hasSymbol) listOf('*') + list.filter { it != '*' }
                                    else list
                                }
                        }
                        
                        // Map letter -> first contact index
                        val letterToContactIndex = remember(letters, regularContacts) {
                            letters.associateWith { letter ->
                                if (letter == '*') {
                                    regularContacts.indexOfFirst { c ->
                                        val first = c.name.firstOrNull()?.uppercaseChar()
                                        first == null || !first.isLetter()
                                    }
                                } else {
                                    regularContacts.indexOfFirst { c ->
                                        c.name.firstOrNull()?.uppercaseChar() == letter
                                    }
                                }
                            }
                        }
                        
                        val favOffset = if (favoriteContacts.isNotEmpty()) 1 + favoriteContacts.size else 0
                        
                        var selectedIndex by remember { mutableIntStateOf(0) }
                        var dragIndex by remember { mutableIntStateOf(-1) }
                        val coroutineScope = rememberCoroutineScope()
                        
                        // Sync selected letter with list scroll – only update state when letter actually changes
                        LaunchedEffect(listState, letters, regularContacts) {
                            snapshotFlow { listState.firstVisibleItemIndex }
                                .collect { visibleIndex ->
                                    if (dragIndex >= 0) return@collect
                                    val contactIdx = visibleIndex - favOffset
                                    if (contactIdx in regularContacts.indices) {
                                        val first = regularContacts[contactIdx].name.firstOrNull()?.uppercaseChar()
                                        val ch = if (first != null && first.isLetter()) first else '*'
                                        val newIndex = letters.indexOf(ch).coerceIn(0, letters.size - 1)
                                        if (newIndex != selectedIndex) {
                                            selectedIndex = newIndex
                                        }
                                    }
                                }
                        }
                        
                        // All letters scaled proportionally to fill available height
                        Box(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .width(20.dp)
                                .fillMaxHeight()
                                .pointerInput(letters, regularContacts) {
                                    val totalHeight = size.height.toFloat()
                                    detectVerticalDragGestures(
                                        onDragStart = { offset ->
                                            val index = ((offset.y / totalHeight) * letters.size).toInt()
                                                .coerceIn(0, letters.size - 1)
                                            dragIndex = index
                                            selectedIndex = index
                                            val contactIndex = letterToContactIndex[letters[index]] ?: -1
                                            if (contactIndex >= 0) {
                                                coroutineScope.launch {
                                                    listState.scrollToItem(contactIndex + favOffset)
                                                }
                                            }
                                        },
                                        onVerticalDrag = { change, _ ->
                                            change.consume()
                                            val totalHeight2 = size.height.toFloat()
                                            val index = ((change.position.y / totalHeight2) * letters.size).toInt()
                                                .coerceIn(0, letters.size - 1)
                                            if (index != dragIndex) {
                                                dragIndex = index
                                                selectedIndex = index
                                                val contactIndex = letterToContactIndex[letters[index]] ?: -1
                                                if (contactIndex >= 0) {
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(contactIndex + favOffset)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            dragIndex = -1
                                        }
                                    )
                                },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                letters.forEachIndexed { index, letter ->
                                    val isSelected = selectedIndex == index
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(
                                                if (isSelected) primaryColor.copy(alpha = 0.3f)
                                                else androidx.compose.ui.graphics.Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = letter.toString(),
                                            fontSize = 8.sp,
                                            lineHeight = 9.sp,
                                            color = if (isSelected) primaryColor else normalColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}