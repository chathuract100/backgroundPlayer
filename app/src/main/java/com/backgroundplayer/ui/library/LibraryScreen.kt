package com.backgroundplayer.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.backgroundplayer.data.MediaFilter
import com.backgroundplayer.data.db.Playlist
import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    LibraryScreen(
        state = state,
        playlists = playlists,
        onQueryChange = viewModel::setQuery,
        onFilterChange = viewModel::setFilter,
        onTrackClick = viewModel::playTrack,
        onScan = viewModel::scanDevice,
        onImport = viewModel::importFiles,
        onAddToPlaylist = viewModel::addToPlaylist,
        onCreatePlaylistAndAdd = viewModel::createPlaylistAndAdd,
        onRemoveFromLibrary = { viewModel.removeFromLibrary(it.id) },
        onUndoRemove = viewModel::undoRemove,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    playlists: List<Playlist>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onTrackClick: (Track) -> Unit,
    onScan: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    onAddToPlaylist: (playlistId: Long, trackId: Long) -> Unit,
    onCreatePlaylistAndAdd: (name: String, trackId: Long) -> Unit,
    onRemoveFromLibrary: (Track) -> Unit,
    onUndoRemove: (trackId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) onImport(uris) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var addTarget by remember { mutableStateOf<Track?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan device")
                    }
                    IconButton(onClick = { picker.launch(arrayOf("audio/*", "video/*")) }) {
                        Icon(Icons.Filled.LibraryAdd, contentDescription = "Import files")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search title or artist") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                FilterChip(
                    selected = state.filter == MediaFilter.ALL,
                    onClick = { onFilterChange(MediaFilter.ALL) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = state.filter == MediaFilter.AUDIO,
                    onClick = { onFilterChange(MediaFilter.AUDIO) },
                    label = { Text("Audio") },
                )
                FilterChip(
                    selected = state.filter == MediaFilter.VIDEO,
                    onClick = { onFilterChange(MediaFilter.VIDEO) },
                    label = { Text("Video") },
                )
            }

            if (state.isEmpty) {
                EmptyLibrary(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onTrackClick(track) },
                            onAddToPlaylist = { addTarget = track },
                            onRemove = {
                                onRemoveFromLibrary(track)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Removed “${track.title}”",
                                        actionLabel = "Undo",
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onUndoRemove(track.id)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    addTarget?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addTarget = null },
            onPick = { playlistId ->
                onAddToPlaylist(playlistId, track.id)
                addTarget = null
                scope.launch { snackbarHostState.showSnackbar("Added to playlist") }
            },
            onCreate = { name ->
                onCreatePlaylistAndAdd(name, track.id)
                addTarget = null
                scope.launch { snackbarHostState.showSnackbar("Added to “$name”") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = track.artist?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = {
            val icon = rememberVectorPainter(Icons.Filled.MusicNote)
            AsyncImage(
                model = track.thumbnailUri,
                contentDescription = null,
                placeholder = icon,
                error = icon,
                fallback = icon,
                colorFilter = if (track.thumbnailUri == null) {
                    ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    null
                },
                modifier = Modifier.size(48.dp),
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = { menuOpen = false; onAddToPlaylist() },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from library") },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreate: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NewPlaylistDialog(
            onConfirm = { onCreate(it) },
            onDismiss = { creating = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(modifier = Modifier.heightIn(max = 360.dp)) {
                DropdownMenuItem(
                    text = { Text("New playlist…") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { creating = true },
                )
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        DropdownMenuItem(
                            text = { Text(playlist.name) },
                            onClick = { onPick(playlist.id) },
                        )
                    }
                }
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet — create one above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewPlaylistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
        Text(
            "Scan your device or import files to get started.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
