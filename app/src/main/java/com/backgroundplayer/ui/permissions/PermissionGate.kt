package com.backgroundplayer.ui.permissions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.backgroundplayer.permissions.MediaPermissions

/**
 * Gates [content] behind the required media permissions. Requests them on first
 * composition; if denied, shows a rationale with retry and a link to settings.
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Request everything (incl. notifications) but only *gate* on the media-read
    // permissions, so denying notifications never blocks browsing or playback.
    val toRequest = remember { MediaPermissions.requiredMediaPermissions(Build.VERSION.SDK_INT) }
    val blocking = remember { MediaPermissions.blockingPermissions(Build.VERSION.SDK_INT) }

    fun blockingGranted(): Boolean = blocking.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(blockingGranted()) }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = blockingGranted() }

    if (granted) {
        content()
        return
    }

    PermissionRationale(
        onGrant = {
            asked = true
            launcher.launch(toRequest.toTypedArray())
        },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
        showSettings = asked,
    )
}

@Composable
private fun PermissionRationale(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    showSettings: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Media access needed",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "backgroundPlayer needs permission to read audio and video on your " +
                "device so it can build your library. Files are never copied or uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onGrant) { Text("Grant permission") }
        if (showSettings) {
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }
}
