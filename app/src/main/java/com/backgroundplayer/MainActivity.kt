package com.backgroundplayer

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.backgroundplayer.playback.PipPolicy
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.ui.AppShell
import com.backgroundplayer.ui.permissions.PermissionGate
import com.backgroundplayer.ui.player.VideoSurface

class MainActivity : ComponentActivity() {

    /** The latest playback state, fed by [AppShell]; drives the PiP decision and controls. */
    @Volatile
    private var latestState: PlaybackState = PlaybackState.EMPTY

    private var inPip by mutableStateOf(false)

    /** Routes the PiP window's RemoteAction buttons to the playback controller. */
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val playback = (application as App).container.playback
            when (intent?.getStringExtra(EXTRA_ACTION)) {
                ACTION_PLAY_PAUSE -> if (latestState.isPlaying) playback.pause() else playback.resume()
                ACTION_NEXT -> playback.next()
            }
        }
    }

    fun onPlaybackStateChanged(state: PlaybackState) {
        latestState = state
        // Refresh the PiP play/pause icon as playback flips while in the window.
        if (inPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.registerReceiver(
                this,
                pipReceiver,
                IntentFilter(ACTION_PIP_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (inPip) {
                        // In PiP, show only the video — the system renders the controls.
                        VideoSurface(
                            player = container.playback.mediaPlayer,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PermissionGate {
                            AppShell(
                                container = container,
                                onStateChanged = ::onPlaybackStateChanged,
                                onMinimizeToPip = ::enterPipIfEligible,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfEligible()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { unregisterReceiver(pipReceiver) }
        }
        super.onDestroy()
    }

    private fun enterPipIfEligible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PipPolicy.shouldEnterPip(latestState)) {
            enterPictureInPictureMode(buildPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams =
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(
                listOf(
                    remoteAction(
                        iconRes = if (latestState.isPlaying) {
                            android.R.drawable.ic_media_pause
                        } else {
                            android.R.drawable.ic_media_play
                        },
                        title = if (latestState.isPlaying) "Pause" else "Play",
                        requestCode = REQ_PLAY_PAUSE,
                        action = ACTION_PLAY_PAUSE,
                    ),
                    remoteAction(
                        iconRes = android.R.drawable.ic_media_next,
                        title = "Next",
                        requestCode = REQ_NEXT,
                        action = ACTION_NEXT,
                    ),
                ),
            )
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun remoteAction(iconRes: Int, title: String, requestCode: Int, action: String): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(packageName)
            .putExtra(EXTRA_ACTION, action)
        val pending = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pending)
    }

    private companion object {
        const val ACTION_PIP_CONTROL = "com.backgroundplayer.PIP_CONTROL"
        const val EXTRA_ACTION = "action"
        const val ACTION_PLAY_PAUSE = "play_pause"
        const val ACTION_NEXT = "next"
        const val REQ_PLAY_PAUSE = 1
        const val REQ_NEXT = 2
    }
}
