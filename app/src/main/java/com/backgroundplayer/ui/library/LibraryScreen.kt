package com.backgroundplayer.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.backgroundplayer.data.MediaFilter
import com.backgroundplayer.data.db.Track

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LibraryScreen(
        state = state,
        onQueryChange = viewModel::setQuery,
        onFilterChange = viewModel::setFilter,
        onTrackClick = viewModel::playTrack,
        onScan = viewModel::scanDevice,
        onImport = viewModel::importFiles,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MediaFilter) -> Unit,
    onTrackClick: (Track) -> Unit,
    onScan: () -> Unit,
    onImport: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) onImport(uris) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Library") },
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
                        TrackRow(track = track, onClick = { onTrackClick(track) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = track.artist?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        leadingContent = {
            AsyncImage(
                model = track.thumbnailUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
