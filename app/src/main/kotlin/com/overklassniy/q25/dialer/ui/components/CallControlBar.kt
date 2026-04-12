package com.overklassniy.q25.dialer.ui.components

import android.telecom.CallAudioState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.R
import com.overklassniy.q25.dialer.ui.theme.CallRed

@Composable
fun CallControlBar(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isOnHold: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onShowDialpad: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    currentAudioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    isBluetoothAvailable: Boolean = false,
    onSetAudioRoute: (Int) -> Unit = {},
) {
    var showAudioRouteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CallControlButton(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = stringResource(if (isMuted) R.string.unmute else R.string.mute),
                isActive = isMuted,
                onClick = onToggleMute,
                modifier = Modifier.weight(1f),
            )
            CallControlButton(
                icon = Icons.Filled.Dialpad,
                label = stringResource(R.string.dialpad),
                onClick = onShowDialpad,
                modifier = Modifier.weight(1f),
            )
            // If Bluetooth is available, show BT icon; tapping opens audio route picker
            if (isBluetoothAvailable) {
                val audioIcon = when (currentAudioRoute) {
                    CallAudioState.ROUTE_BLUETOOTH -> Icons.Filled.Bluetooth
                    CallAudioState.ROUTE_SPEAKER -> Icons.Filled.VolumeUp
                    else -> Icons.Filled.Hearing
                }
                val audioLabel = when (currentAudioRoute) {
                    CallAudioState.ROUTE_BLUETOOTH -> stringResource(R.string.audio_bluetooth)
                    CallAudioState.ROUTE_SPEAKER -> stringResource(R.string.speaker)
                    else -> stringResource(R.string.audio_earpiece)
                }
                CallControlButton(
                    icon = audioIcon,
                    label = audioLabel,
                    isActive = currentAudioRoute == CallAudioState.ROUTE_BLUETOOTH || isSpeakerOn,
                    onClick = { showAudioRouteDialog = true },
                    modifier = Modifier.weight(1f),
                )
            } else {
                CallControlButton(
                    icon = Icons.Filled.VolumeUp,
                    label = stringResource(R.string.speaker),
                    isActive = isSpeakerOn,
                    onClick = onToggleSpeaker,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(0.5f))
            CallControlButton(
                icon = if (isOnHold) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                label = stringResource(if (isOnHold) R.string.unhold else R.string.hold),
                isActive = isOnHold,
                onClick = onToggleHold,
                modifier = Modifier.weight(1f),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = stringResource(R.string.end_call),
                    tint = Color.White,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CallRed)
                        .clickable(onClick = onEndCall)
                        .padding(14.dp),
                )
                Text(
                    text = stringResource(R.string.end_call),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.weight(0.5f))
        }
    }

    if (showAudioRouteDialog) {
        AudioRoutePickerDialog(
            currentRoute = currentAudioRoute,
            onSelectRoute = { route ->
                onSetAudioRoute(route)
                showAudioRouteDialog = false
            },
            onDismiss = { showAudioRouteDialog = false },
        )
    }
}

@Composable
private fun AudioRoutePickerDialog(
    currentRoute: Int,
    onSelectRoute: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val routes = listOf(
        CallAudioState.ROUTE_EARPIECE to (stringResource(R.string.audio_earpiece) to Icons.Filled.Hearing),
        CallAudioState.ROUTE_SPEAKER to (stringResource(R.string.speaker) to Icons.Filled.VolumeUp),
        CallAudioState.ROUTE_BLUETOOTH to (stringResource(R.string.audio_bluetooth) to Icons.Filled.Bluetooth),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_output)) },
        text = {
            Column {
                routes.forEach { (route, labelIcon) ->
                    val (label, icon) = labelIcon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRoute(route) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (route == currentRoute) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (route == currentRoute) FontWeight.Bold else FontWeight.Normal,
                            color = if (route == currentRoute) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
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

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}