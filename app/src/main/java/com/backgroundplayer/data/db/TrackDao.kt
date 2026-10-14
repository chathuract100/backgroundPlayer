package com.backgroundplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    /** Inserts a track; ignores the insert (returns -1) when its URI already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(track: Track): Long

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC, id DESC")
    fun observeAll(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isVideo = :isVideo ORDER BY dateAdded DESC, id DESC")
    fun observeByType(isVideo: Boolean): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): Track?

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Track?

    @Query("UPDATE tracks SET isAvailable = :available WHERE id = :id")
    suspend fun setAvailability(id: Long, available: Boolean)
}
