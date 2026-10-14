package com.backgroundplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    /** Creates a playlist stamped with the current time and returns its new id. */
    suspend fun create(name: String): Long =
        insertPlaylist(Playlist(name = name, createdAt = System.currentTimeMillis()))

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC, id DESC")
    fun observePlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(entry: PlaylistTrack)

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON pt.trackId = t.id
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """,
    )
    fun observeTracks(playlistId: Long): Flow<List<Track>>

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int?

    @Query(
        "UPDATE playlist_tracks SET position = :newPosition " +
            "WHERE playlistId = :playlistId AND trackId = :trackId",
    )
    suspend fun reorder(playlistId: Long, trackId: Long, newPosition: Int)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: Long)
}
