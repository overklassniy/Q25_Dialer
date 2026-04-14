package com.overklassniy.q25.dialer.ui.screens

import android.telecom.CallAudioState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.data.PreferencesManager
import com.overklassniy.q25.dialer.ui.components.CallControlBar
import com.overklassniy.q25.dialer.ui.components.ContactAvatar
import com.overklassniy.q25.dialer.ui.theme.CallGreen
import com.overklassniy.q25.dialer.ui.theme.CallRed
import com.overklassniy.q25.dialer.ui.theme.DefaultCallBackground
import com.overklassniy.q25.dialer.ui.theme.LightCallControlBtnBg
import com.overklassniy.q25.dialer.ui.theme.LightCallDialpadText
import com.overklassniy.q25.dialer.ui.theme.LightCallHoldBar
import com.overklassniy.q25.dialer.ui.theme.LightCallText
import com.overklassniy.q25.dialer.ui.theme.LightDefaultCallBackground
import com.overklassniy.q25.dialer.ui.theme.Primary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InCallScreen(
    callerName: String,
    callerNumber: String,
    callerPhotoUri: String?,
    callerFullPhotoUri: String? = null,
    isUnknownCaller: Boolean = false,
    statusText: String,
    isIncoming: Boolean,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isOnHold: Boolean,
    showDialpad: Boolean,
    dtmfInput: String,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onEndCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onMessageClick: () -> Unit = {},
    onDtmf: (Char) -> Unit,
    onToggleDialpad: () -> Unit,
    currentAudioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    isBluetoothAvailable: Boolean = false,
    onSetAudioRoute: (Int) -> Unit = {},
    callNotes: String = "",
    onCallNotesChanged: (String) -> Unit = {},
    onAddCall: () -> Unit = {},
    showFullscreenAvatar: Boolean = false,
    hasHeldCall: Boolean = false,
    heldCallerName: String? = null,
    onSwapCall: () -> Unit = {},
    isDarkTheme: Boolean = true,
) {
    var showNotesSheet by remember { mutableStateOf(false) }

    // Read custom call screen colors (separate for incoming and ongoing calls)
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    fun cc(key: String, default: Color) = prefs.getCustomColor(key)?.let { Color(it.toULong()) } ?: default

    // Default colors based on theme
    val defaultBg = if (isDarkTheme) DefaultCallBackground else LightDefaultCallBackground
    val defaultText = if (isDarkTheme) Color.White else LightCallText
    val defaultControlBtnBg = if (isDarkTheme) Color(0xFF3A3A3C) else LightCallControlBtnBg
    val defaultControlBtnIcon = if (isDarkTheme) Color.White else LightCallText
    val defaultDialpadText = if (isDarkTheme) Color.White else LightCallDialpadText
    val defaultHoldBar = if (isDarkTheme) Color(0xFF2C2C2E) else LightCallHoldBar

    // Incoming call colors (used when isIncoming = true)
    val incomingCallBg = remember { cc(PreferencesManager.KEY_COLOR_INCOMING_CALL_BACKGROUND, defaultBg) }
    val incomingCallText = remember { cc(PreferencesManager.KEY_COLOR_INCOMING_CALL_TEXT, defaultText) }
    val incomingCallDeclineBtn = remember { cc(PreferencesManager.KEY_COLOR_INCOMING_CALL_DECLINE_BUTTON, CallRed) }
    val incomingCallAcceptBtn = remember { cc(PreferencesManager.KEY_COLOR_INCOMING_CALL_ACCEPT_BUTTON, CallGreen) }
    val incomingCallMessageBtn = remember { cc(PreferencesManager.KEY_COLOR_INCOMING_CALL_MESSAGE_BUTTON, Primary) }

    // Ongoing call colors (used when isIncoming = false)
    val ongoingCallBg = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_BACKGROUND, defaultBg) }
    val ongoingCallText = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_TEXT, defaultText) }
    val ongoingCallEndBtn = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_END_BUTTON, CallRed) }
    val ongoingCallControlBtnBg = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_BG, defaultControlBtnBg) }
    val ongoingCallControlBtnIcon = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_CONTROL_BUTTON_ICON, defaultControlBtnIcon) }
    val ongoingCallDialpadText = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_DIALPAD_TEXT, defaultDialpadText) }
    val ongoingCallHoldBar = remember { cc(PreferencesManager.KEY_COLOR_ONGOING_CALL_HOLD_BAR, defaultHoldBar) }

    // Select active colors based on call state
    val callBg = if (isIncoming) incomingCallBg else ongoingCallBg
    val callText = if (isIncoming) incomingCallText else ongoingCallText

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(callBg),
    ) {
        // Fullscreen avatar background (use full-size photo, fallback to thumbnail)
        val fullscreenPhoto = callerFullPhotoUri ?: callerPhotoUri
        if (showFullscreenAvatar && fullscreenPhoto != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(fullscreenPhoto)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Dark scrim overlay for readability (lighter in light theme)
            val scrimAlpha = if (isDarkTheme) 0.5f else 0.3f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        }
        // Top section: on-hold bar + action buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            // On-hold call bar (shown when there are two calls)
            if (hasHeldCall && heldCallerName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ongoingCallHoldBar)
                        .clickable(onClick = onSwapCall)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = heldCallerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = callText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.on_hold_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = callText.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = onSwapCall) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = stringResource(R.string.swap_calls),
                            tint = callText,
                        )
                    }
                }
            }

            // Top bar with Add Call (left) and Notes (right)
            if (!isIncoming) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onAddCall) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_call),
                            tint = callText,
                        )
                    }
                    IconButton(onClick = { showNotesSheet = !showNotesSheet }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.call_notes),
                            tint = callText,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = if (!isIncoming) (if (hasHeldCall) 80.dp else 40.dp) else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Caller info
            Spacer(Modifier.height(if (showDialpad && !isIncoming) 8.dp else 16.dp))

            if (!showDialpad || isIncoming) {
                // Caller avatar (hidden when dialpad is open to save space)
                ContactAvatar(
                    name = callerName,
                    photoUri = callerPhotoUri,
                    size = 80.dp,
                    isUnknown = isUnknownCaller,
                )

                Spacer(Modifier.height(8.dp))
            }

            // Caller name + number + notes preview centered together
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = callText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (callerNumber != callerName && callerNumber.isNotEmpty()) {
                    Text(
                        text = callerNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = callText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Notes preview (truncated)
                if (callNotes.isNotBlank()) {
                    val maxLen = 50
                    val preview = if (callNotes.length > maxLen) callNotes.take(maxLen) + "\u2026" else callNotes
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = callText.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Call status
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = callText.copy(alpha = 0.6f),
            )

            Spacer(Modifier.weight(1f))

            if (showDialpad && !isIncoming) {
                // Compact in-call dialpad at bottom

                // DTMF input display
                Text(
                    text = dtmfInput.ifEmpty { " " },
                    style = MaterialTheme.typography.headlineSmall,
                    color = callText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )

                // Compact dialpad grid (smaller keys for 720x720)
                CompactDialpadGrid(
                    onKeyPress = { onDtmf(it) },
                    textColor = ongoingCallDialpadText,
                )

                Spacer(Modifier.height(4.dp))

                // Bottom row: Hide + End Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onToggleDialpad) {
                        Text(
                            text = stringResource(R.string.hide),
                            color = callText,
                            fontSize = 14.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = stringResource(R.string.end_call),
                        tint = ongoingCallText,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ongoingCallEndBtn)
                            .clickable(onClick = onEndCall)
                            .padding(12.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))
            } else {
                // Incoming call: decline / message / accept buttons
                if (isIncoming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Decline
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = stringResource(R.string.decline),
                                tint = incomingCallText,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(incomingCallDeclineBtn)
                                    .clickable(onClick = onDeclineCall)
                                    .padding(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.decline),
                                color = callText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        // Message
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = stringResource(R.string.message),
                                tint = incomingCallText,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(incomingCallMessageBtn)
                                    .clickable(onClick = onMessageClick)
                                    .padding(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.message),
                                color = callText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        // Accept
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = stringResource(R.string.answer),
                                tint = incomingCallText,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(incomingCallAcceptBtn)
                                    .clickable(onClick = onAcceptCall)
                                    .padding(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.answer),
                                color = callText,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                } else {
                    // Ongoing call controls
                    CallControlBar(
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        isOnHold = isOnHold,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onToggleHold = onToggleHold,
                        onShowDialpad = onToggleDialpad,
                        onEndCall = onEndCall,
                        currentAudioRoute = currentAudioRoute,
                        isBluetoothAvailable = isBluetoothAvailable,
                        onSetAudioRoute = onSetAudioRoute,
                        controlButtonBgColor = ongoingCallControlBtnBg,
                        controlButtonIconColor = ongoingCallControlBtnIcon,
                        endCallButtonColor = ongoingCallEndBtn,
                        textColor = ongoingCallText,
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        // Notes overlay
        if (showNotesSheet) {
            val overlayScrim = if (isDarkTheme) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.4f)
            val notesCardBg = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF5F5F5)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayScrim)
                    .clickable { showNotesSheet = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(notesCardBg)
                        .clickable { /* consume clicks */ }
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.call_notes),
                        color = callText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = callNotes,
                        onValueChange = onCallNotesChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = callText,
                            unfocusedTextColor = callText,
                            cursorColor = callText,
                            focusedBorderColor = callText.copy(alpha = 0.5f),
                            unfocusedBorderColor = callText.copy(alpha = 0.3f),
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.call_notes_hint),
                                color = callText.copy(alpha = 0.4f),
                            )
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showNotesSheet = false },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            text = stringResource(R.string.hide),
                            color = callText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact dialpad grid for in-call DTMF input.
 * Smaller keys (36dp height) adapted for 720x720 square screen.
 * No call/backspace row – only digit keys.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactDialpadGrid(
    onKeyPress: (Char) -> Unit,
    textColor: Color = Color.White,
) {
    val dividerColor = textColor.copy(alpha = 0.2f)
    val keys = listOf(
        listOf('1' to "", '2' to "ABC", '3' to "DEF"),
        listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
        listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
        listOf('*' to "", '0' to "+", '#' to ""),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        for ((rowIndex, row) in keys.withIndex()) {
            if (rowIndex > 0) HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                for ((colIndex, pair) in row.withIndex()) {
                    if (colIndex > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(0.5.dp)
                                .background(dividerColor)
                        )
                    }
                    val (digit, letters) = pair
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .combinedClickable(onClick = { onKeyPress(digit) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = digit.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light,
                            color = textColor,
                        )
                        if (letters.isNotEmpty()) {
                            Text(
                                text = letters,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor.copy(alpha = 0.4f),
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
    }
}