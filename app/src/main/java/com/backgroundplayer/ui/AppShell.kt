package com.backgroundplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.backgroundplayer.AppContainer
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.playback.RepeatMode
import com.backgroundplayer.ui.library.LibraryScreen
import com.backgroundplayer.ui.library.LibraryViewModel
import com.backgroundplayer.ui.player.MiniPlayerBar
import com.backgroundplayer.ui.player.PlayerScreen
import com.backgroundplayer.ui.playlists.PlaylistDetailScreen
import com.backgroundplayer.ui.playlists.PlaylistDetailViewModel
import com.backgroundplayer.ui.playlists.PlaylistsScreen
import com.backgroundplayer.ui.playlists.PlaylistsViewModel

private enum class Tab { LIBRARY, PLAYLISTS }

/**
 * Top-level app shell: bottom navigation between Library and Playlists, a pinned
 * mini-player, and the full now-playing screen as an overlay. Playback state is
 * pushed to [onStateChanged] so the activity can drive Picture-in-Picture.
 */
@Composable
fun AppShell(
    container: AppContainer,
    onStateChanged: (PlaybackState) -> Unit,
    onMinimizeToPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = container.playback
    val state by playback.state.collectAsState()
    LaunchedEffect(state) { onStateChanged(state) }

    val libraryVm = remember { LibraryViewModel(container.repository, playback, container.importer) }
    val playlistsVm = remember { PlaylistsViewModel(container.repository) }

    var tab by remember { mutableStateOf(Tab.LIBRARY) }
    var openPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showPlayer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.LIBRARY,
                    onClick = { tab = Tab.LIBRARY },
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text("Library") },
                )
                NavigationBarItem(
                    selected = tab == Tab.PLAYLISTS,
                    onClick = { tab = Tab.PLAYLISTS },
                    icon = { Icon(Icons.Filled.PlaylistPlay, contentDescription = null) },
                    label = { Text("Playlists") },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (tab) {
                Tab.LIBRARY -> LibraryScreen(libraryVm)
                Tab.PLAYLISTS -> {
                    val playlistId = openPlaylistId
                    if (playlistId == null) {
                        PlaylistsScreen(
                            viewModel = playlistsVm,
                            onOpenPlaylist = { openPlaylistId = it },
                        )
                    } else {
                        val detailVm = remember(playlistId) {
                            PlaylistDetailViewModel(container.repository, playback, playlistId)
                        }
                        PlaylistDetailScreen(
                            viewModel = detailVm,
                            title = "Playlist",
                            onBack = { openPlaylistId = null },
                        )
                    }
                }
            }

            MiniPlayerBar(
                state = state,
                onPlayPause = { playPause(state, playback::pause, playback::resume) },
                onNext = playback::next,
                onExpand = { showPlayer = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showPlayer && state.currentTrack != null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PlayerScreen(
                state = state,
                player = playback.mediaPlayer,
                onPlayPause = { playPause(state, playback::pause, playback::resume) },
                onNext = playback::next,
                onPrevious = playback::previous,
                onSeek = playback::seekTo,
                onToggleShuffle = { playback.setShuffle(!state.shuffle) },
                onCycleRepeat = { playback.setRepeat(state.repeat.next()) },
                onMinimizeToPip = onMinimizeToPip,
            )
        }
    }
}

private inline fun playPause(state: PlaybackState, pause: () -> Unit, resume: () -> Unit) {
    if (state.isPlaying) pause() else resume()
}

private fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}
