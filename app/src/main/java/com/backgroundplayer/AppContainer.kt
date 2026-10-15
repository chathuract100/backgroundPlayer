package com.backgroundplayer

import android.content.Context
import androidx.room.Room
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.RoomLibraryRepository
import com.backgroundplayer.data.db.AppDatabase
import com.backgroundplayer.data.mediaimport.AndroidMediaMetadataReader
import com.backgroundplayer.data.mediaimport.DefaultMediaImporter
import com.backgroundplayer.data.mediaimport.MediaImporter
import com.backgroundplayer.data.mediaimport.MediaStoreScanner
import com.backgroundplayer.data.mediaimport.SafImporter
import com.backgroundplayer.playback.PlaybackConnection

/**
 * Hand-written dependency container (no Hilt/Dagger): owns the database,
 * repository, importer, and the UI-side playback connection for the app's life.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "backgroundplayer.db").build()

    val repository: LibraryRepository =
        RoomLibraryRepository(database.trackDao(), database.playlistDao())

    private val metadataReader = AndroidMediaMetadataReader(appContext)

    val importer: MediaImporter = DefaultMediaImporter(
        scanner = MediaStoreScanner(appContext, metadataReader, repository),
        safImporter = SafImporter(appContext, metadataReader, repository),
    )

    val playback: PlaybackConnection = PlaybackConnection(appContext, repository)
}
