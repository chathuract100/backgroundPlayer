package com.backgroundplayer

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.backgroundplayer.playback.PipPolicy
import com.backgroundplayer.playback.PlaybackState

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
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
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

@Composable
private fun AppRoot() {
    Text(
        text = "backgroundPlayer",
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    )
}
