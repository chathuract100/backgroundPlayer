package com.backgroundplayer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single playable media item in the library, stored by content-URI reference
 * (never copied). [isVideo] is derived from the file's MIME type at import time.
 */
@Entity(
    tableName = "tracks",
    indices = [Index(value = ["uri"], unique = true)],
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0,
    val isVideo: Boolean = false,
    val thumbnailUri: String? = null,
    val dateAdded: Long = 0,
    val isAvailable: Boolean = true,
)
