package com.backgroundplayer

import android.app.Application

/**
 * Application subclass that holds the app's [AppContainer]. The container is
 * built lazily on first access so merely instantiating the Application (e.g.
 * under Robolectric) does not spin up the database or playback connection.
 */
class App : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
