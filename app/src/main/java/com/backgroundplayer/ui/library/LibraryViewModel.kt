package com.backgroundplayer.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.MediaFilter
import com.backgroundplayer.data.db.Track
import com.backgroundplayer.data.mediaimport.MediaImporter
import com.backgroundplayer.playback.PlaybackController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val filter: MediaFilter = MediaFilter.ALL,
    val query: String = "",
    val isEmpty: Boolean = true,
)

/**
 * Drives the library screen: exposes the filtered/searched track list and turns
 * UI actions into repository/importer/playback calls. The track list is held in
 * an eagerly-shared [uiState] so [playTrack] can read the current list without a
 * live collector.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: LibraryRepository,
    private val playback: PlaybackController,
    private val importer: MediaImporter,
) : ViewModel() {

    private val filter = MutableStateFlow(MediaFilter.ALL)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> =
        combine(filter, query) { f, q -> f to q }
            .flatMapLatest { (f, q) ->
                repository.library(f, q).map { tracks ->
                    LibraryUiState(tracks = tracks, filter = f, query = q, isEmpty = tracks.isEmpty())
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    fun setFilter(filter: MediaFilter) {
        this.filter.value = filter
    }

    fun setQuery(query: String) {
        this.query.value = query
    }

    /** Plays the currently shown list, starting at [track]. */
    fun playTrack(track: Track) {
        val tracks = uiState.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playback.play(tracks, index)
    }

    fun scanDevice() {
        viewModelScope.launch { importer.scan() }
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch { importer.import(uris) }
    }
}
