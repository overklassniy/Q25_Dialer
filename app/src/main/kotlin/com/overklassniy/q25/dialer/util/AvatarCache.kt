package com.overklassniy.q25.dialer.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-level avatar bitmap cache that survives Compose navigation / recomposition.
 * Bitmaps are loaded from ContentResolver once and kept in memory, so switching
 * between tabs never causes the "initials → photo" flicker.
 */
object AvatarCache {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    /** Synchronous lookup — returns cached ImageBitmap or null. */
    fun get(uri: String): ImageBitmap? = cache[uri]

    /** Store a bitmap in the cache. */
    fun put(uri: String, bitmap: ImageBitmap) {
        cache[uri] = bitmap
    }

    /**
     * Load a contact photo from [ContentResolver], cache it, and return the bitmap.
     * Returns null if the URI cannot be resolved or decoded.
     * Safe to call from any coroutine; the heavy work runs on [Dispatchers.IO].
     */
    suspend fun loadAndCache(context: Context, uri: String): ImageBitmap? {
        cache[uri]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bitmap ->
                        val imageBitmap = bitmap.asImageBitmap()
                        cache[uri] = imageBitmap
                        imageBitmap
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}