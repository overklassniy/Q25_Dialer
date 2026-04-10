package com.overklassniy.q25.dialer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.overklassniy.q25.dialer.R

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSms: (() -> Unit)? = null,
    onCopyNumber: (() -> Unit)? = null,
    onAddContact: (() -> Unit)? = null,
    onBlock: (() -> Unit)? = null,
    isBlocked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = stringResource(R.string.selected_count, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )

        if (selectedCount == 1 && onSms != null) {
            IconButton(onClick = onSms) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = stringResource(R.string.send_sms),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (selectedCount == 1 && onCopyNumber != null) {
            IconButton(onClick = onCopyNumber) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.copy_number),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (selectedCount == 1 && onAddContact != null) {
            IconButton(onClick = onAddContact) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = stringResource(R.string.add_to_contacts),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (selectedCount == 1 && onBlock != null) {
            IconButton(onClick = onBlock) {
                Icon(
                    imageVector = if (isBlocked) Icons.Filled.Block else Icons.Filled.Block,
                    contentDescription = stringResource(
                        if (isBlocked) R.string.unblock_number else R.string.block_number
                    ),
                    modifier = Modifier.size(22.dp),
                    tint = if (isBlocked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        IconButton(onClick = onSelectAll) {
            Icon(
                imageVector = Icons.Filled.SelectAll,
                contentDescription = stringResource(R.string.select_all),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}