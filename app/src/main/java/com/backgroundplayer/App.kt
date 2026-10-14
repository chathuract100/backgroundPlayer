package com.backgroundplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Application subclass that holds the app's [AppContainer]. The container is
 * built lazily on first access so merely instantiating the Application (e.g.
 * under Robolectric) does not spin up the database or playback connection.
 *
 * Also provides the Coil [ImageLoader] with a video-frame decoder so video
 * thumbnails render from their content URIs.
 */
class App : Application(), ImageLoaderFactory {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
