package com.backgroundplayer.data

import com.backgroundplayer.data.db.Playlist
import com.backgroundplayer.data.db.PlaylistDao
import com.backgroundplayer.data.db.PlaylistTrack
import com.backgroundplayer.data.db.Track
import com.backgroundplayer.data.db.TrackDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class MediaFilter { ALL, AUDIO, VIDEO }

/**
 * Single source of truth over the Room DAOs for library browsing and playlist
 * editing. Media is referenced by URI and never copied. Unavailable tracks
 * (file moved/deleted or permission revoked) are hidden from [library] rather
 * than left as dead entries.
 */
class LibraryRepository(
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
) {

    /**
     * Observes the library filtered by media type and a free-text [query] that
     * matches title or artist case-insensitively (blank = no text filter).
     * Only available tracks are emitted.
     */
    fun library(filter: MediaFilter, query: String): Flow<List<Track>> {
        val source = when (filter) {
            MediaFilter.ALL -> trackDao.observeAll()
            MediaFilter.AUDIO -> trackDao.observeByType(isVideo = false)
            MediaFilter.VIDEO -> trackDao.observeByType(isVideo = true)
        }
        val q = query.trim()
        return source.map { tracks ->
            tracks.filter { it.isAvailable }
                .filter { track ->
                    q.isBlank() ||
                        track.title.contains(q, ignoreCase = true) ||
                        track.artist?.contains(q, ignoreCase = true) == true
                }
        }
    }

    /**
     * Inserts [track], deduping by URI: when the URI already exists the insert
     * is ignored and the existing row's id is returned, so no duplicate appears.
     */
    suspend fun addTrack(track: Track): Long {
        val id = trackDao.insert(track)
        return if (id == -1L) trackDao.findByUri(track.uri)?.id ?: -1L else id
    }

    suspend fun markUnavailable(trackId: Long) = trackDao.setAvailability(trackId, false)

    suspend fun createPlaylist(name: String): Long = playlistDao.create(name)

    suspend fun rename(id: Long, name: String) = playlistDao.rename(id, name)

    suspend fun delete(id: Long) = playlistDao.delete(id)

    fun playlists(): Flow<List<Playlist>> = playlistDao.observePlaylists()

    fun playlistTracks(id: Long): Flow<List<Track>> = playlistDao.observeTracks(id)

    /** Appends [trackId] to the end of the playlist (position = current max + 1). */
    suspend fun addToPlaylist(playlistId: Long, trackId: Long) {
        val next = (playlistDao.maxPosition(playlistId) ?: -1) + 1
        playlistDao.addTrack(PlaylistTrack(playlistId, trackId, next))
    }

    suspend fun reorder(playlistId: Long, trackId: Long, newPosition: Int) =
        playlistDao.reorder(playlistId, trackId, newPosition)

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) =
        playlistDao.removeTrack(playlistId, trackId)
}
