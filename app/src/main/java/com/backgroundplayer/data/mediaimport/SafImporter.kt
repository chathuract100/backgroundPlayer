package com.backgroundplayer.data.mediaimport

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.backgroundplayer.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports user-picked documents via the Storage Access Framework. Persists read
 * permission for each URI so it survives reboots, reads metadata, and registers
 * the track (deduped by URI). Returns the number of tracks registered.
 */
class SafImporter(
    private val context: Context,
    private val reader: MediaMetadataReader,
    private val repository: LibraryRepository,
) {
    suspend fun import(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val raw = reader.read(uri)
            val track = TrackMapper.toTrack(raw).copy(dateAdded = System.currentTimeMillis())
            if (repository.addTrack(track) > 0) count++
        }
        count
    }
}
