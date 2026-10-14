package com.backgroundplayer

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.backgroundplayer.playback.PipPolicy
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.ui.AppShell
import com.backgroundplayer.ui.permissions.PermissionGate

class MainActivity : ComponentActivity() {

    /**
     * The latest playback state, fed by the app wiring (Task 12). Used to decide
     * whether leaving the app should enter Picture-in-Picture.
     */
    @Volatile
    private var latestState: PlaybackState = PlaybackState.EMPTY

    fun onPlaybackStateChanged(state: PlaybackState) {
        latestState = state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfEligible()
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
            .build()
}
