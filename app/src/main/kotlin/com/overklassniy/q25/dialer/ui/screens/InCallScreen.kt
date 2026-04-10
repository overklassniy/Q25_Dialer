package com.overklassniy.q25.dialer.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.ui.components.CallControlBar
import com.overklassniy.q25.dialer.ui.components.ContactAvatar
import com.overklassniy.q25.dialer.ui.theme.CallGreen
import com.overklassniy.q25.dialer.ui.theme.CallRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InCallScreen(
    callerName: String,
    callerNumber: String,
    callerPhotoUri: String?,
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
    onDtmf: (Char) -> Unit,
    onToggleDialpad: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                )

                Spacer(Modifier.height(8.dp))
            }

            // Caller name + number centered together
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (callerNumber != callerName && callerNumber.isNotEmpty()) {
                    Text(
                        text = callerNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Call status
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(Modifier.weight(1f))

            if (showDialpad && !isIncoming) {
                // Compact in-call dialpad at bottom

                // DTMF input display
                Text(
                    text = dtmfInput.ifEmpty { " " },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
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
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = stringResource(R.string.end_call),
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CallRed)
                            .clickable(onClick = onEndCall)
                            .padding(12.dp),
                    )
                }

                Spacer(Modifier.height(4.dp))
            } else {
                // Incoming call: accept/decline buttons
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
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CallRed)
                                    .clickable(onClick = onDeclineCall)
                                    .padding(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.decline),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        // Accept
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = stringResource(R.string.answer),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CallGreen)
                                    .clickable(onClick = onAcceptCall)
                                    .padding(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.answer),
                                color = Color.White,
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
                    )
                }

                Spacer(Modifier.height(8.dp))
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
) {
    val dividerColor = Color(0xFF333333)
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
                            color = Color.White,
                        )
                        if (letters.isNotEmpty()) {
                            Text(
                                text = letters,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.4f),
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