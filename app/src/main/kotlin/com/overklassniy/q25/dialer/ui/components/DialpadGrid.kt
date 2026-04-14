package com.overklassniy.q25.dialer.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.ui.theme.CallGreen

data class DialpadKey(
    val digit: String,
    val letters: String = "",
)

private val DIALPAD_KEYS = listOf(
    listOf(DialpadKey("1", ""), DialpadKey("2", "ABC"), DialpadKey("3", "DEF")),
    listOf(DialpadKey("4", "GHI"), DialpadKey("5", "JKL"), DialpadKey("6", "MNO")),
    listOf(DialpadKey("7", "PQRS"), DialpadKey("8", "TUV"), DialpadKey("9", "WXYZ")),
    listOf(DialpadKey("*", ""), DialpadKey("0", "+"), DialpadKey("#", "")),
)

private val DividerColor = Color(0xFF333333)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadGrid(
    onKeyPress: (String) -> Unit,
    onKeyLongPress: (String) -> Unit = {},
    onCallPress: () -> Unit = {},
    onBackspacePress: () -> Unit = {},
    onBackspaceLongPress: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Top divider
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // 4 rows of digit keys
        for ((rowIndex, row) in DIALPAD_KEYS.withIndex()) {
            if (rowIndex > 0) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                for ((colIndex, key) in row.withIndex()) {
                    if (colIndex > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(0.5.dp)
                                .background(DividerColor)
                        )
                    }
                    DialpadButton(
                        key = key,
                        onClick = { onKeyPress(key.digit) },
                        onLongClick = { onKeyLongPress(key.digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Bottom action row: [empty] [Call] [Backspace]
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Left: empty space
            Box(modifier = Modifier.weight(1f).height(56.dp))

            Box(
                Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(DividerColor)
            )

            // Center: Call button (green circle)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .combinedClickable(onClick = onCallPress),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CallGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .background(DividerColor)
            )

            // Right: Backspace (long press to clear all)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .combinedClickable(
                        onClick = onBackspacePress,
                        onLongClick = onBackspaceLongPress,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadButton(
    key: DialpadKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = key.digit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (key.letters.isNotEmpty()) {
            Text(
                text = key.letters,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
            )
        }
    }
}