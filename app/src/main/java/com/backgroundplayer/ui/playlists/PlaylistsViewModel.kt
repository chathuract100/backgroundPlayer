package com.backgroundplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.db.Playlist
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lists playlists and creates/renames/deletes them. */
class PlaylistsViewModel(private val repository: LibraryRepository) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> =
        repository.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun create(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
