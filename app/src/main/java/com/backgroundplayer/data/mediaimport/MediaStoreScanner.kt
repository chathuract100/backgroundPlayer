package com.backgroundplayer.data.mediaimport

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.backgroundplayer.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device's shared audio and video collections via MediaStore and
 * registers each item in the library (deduped by URI). Returns the number of
 * tracks registered.
 */
class MediaStoreScanner(
    private val context: Context,
    private val reader: MediaMetadataReader,
    private val repository: LibraryRepository,
) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val collections = listOf(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
        for (collection in collections) {
            context.contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null,
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (cursor.moveToNext()) {
                    val itemUri = ContentUris.withAppendedId(collection, cursor.getLong(idIdx))
                    val raw = reader.read(itemUri)
                    val track = TrackMapper.toTrack(raw).copy(dateAdded = System.currentTimeMillis())
                    if (repository.addTrack(track) > 0) count++
                }
            }
        }
        count
    }
}
