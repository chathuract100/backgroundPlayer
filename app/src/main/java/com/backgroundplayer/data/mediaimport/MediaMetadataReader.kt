package com.backgroundplayer.data.mediaimport

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Metadata read from a media file at import time, before mapping to a [com.backgroundplayer.data.db.Track].
 * All fields except [uri] and [displayName] may be absent depending on the file.
 */
data class RawMedia(
    val uri: String,
    val mimeType: String?,
    val displayName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val thumbnailUri: String?,
)

interface MediaMetadataReader {
    suspend fun read(uri: Uri): RawMedia
}

/**
 * Reads tags via [MediaMetadataRetriever] and the display name via the content
 * resolver. Falls back gracefully when a file exposes no tags.
 */
class AndroidMediaMetadataReader(private val context: Context) : MediaMetadataReader {

    override suspend fun read(uri: Uri): RawMedia = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: uri.toString()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            RawMedia(
                uri = uri.toString(),
                mimeType = mimeType,
                displayName = displayName,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull(),
                thumbnailUri = null,
            )
        } catch (e: RuntimeException) {
            // Unreadable/corrupt file: keep the entry with only what we know.
            RawMedia(
                uri = uri.toString(),
                mimeType = mimeType,
                displayName = displayName,
                title = null,
                artist = null,
                album = null,
                durationMs = null,
                thumbnailUri = null,
            )
        } finally {
            retriever.release()
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        }.getOrNull()
}
