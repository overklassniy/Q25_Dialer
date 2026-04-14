package com.overklassniy.q25.dialer.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overklassniy.q25.dialer.ui.theme.LetterBackgroundColors
import com.overklassniy.q25.dialer.util.AvatarCache

private val WhitespaceRegex = "\\s+".toRegex()

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 40.dp,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isUnknown: Boolean = false,
) {
    val initials = remember(name, isUnknown) {
        if (isUnknown || name.isBlank()) "?"
        else name.trim()
            .split(WhitespaceRegex)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    val bgColor = remember(name, isUnknown) {
        if (isUnknown || name.isBlank()) Color.Gray
        else {
            val colorIndex = name.sumOf { it.code } % LetterBackgroundColors.size
            LetterBackgroundColors[colorIndex]
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            val context = LocalContext.current

            // Initialize state from the process-level singleton cache.
            // On first visit the cache is empty -> null; on tab switch it is populated -> instant.
            var bitmap by remember(photoUri) {
                mutableStateOf(AvatarCache.get(photoUri))
            }

            if (bitmap != null) {
                // Cache hit — render synchronously, zero async delay, no flicker
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Cache miss (first-ever load): show initials, load in background
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Medium,
                )
                LaunchedEffect(photoUri) {
                    bitmap = AvatarCache.loadAndCache(context, photoUri)
                }
            }
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}