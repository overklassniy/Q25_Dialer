package com.overklassniy.q25.dialer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.overklassniy.q25.dialer.ui.theme.LetterBackgroundColors

private val WhitespaceRegex = "\\s+".toRegex()

@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val isUnknown = name.isBlank()
    
    val initials = remember(name) {
        if (name.isBlank()) "?"
        else name.trim()
            .split(WhitespaceRegex)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { "?" }
    }
    
    val bgColor = remember(name) {
        if (name.isBlank()) Color.Gray
        else {
            val colorIndex = name.sumOf { it.code } % LetterBackgroundColors.size
            LetterBackgroundColors[colorIndex]
        }
    }
    
    // Always render letter avatar first (instant), overlay photo on top (async)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Medium,
        )
        
        if (photoUri != null) {
            val context = LocalContext.current
            val density = LocalDensity.current
            val sizePx = remember(size, density) { with(density) { size.roundToPx() } }
            val imageRequest = remember(photoUri, sizePx) {
                ImageRequest.Builder(context)
                    .data(photoUri)
                    .size(Size(sizePx, sizePx))
                    .memoryCacheKey(photoUri)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}