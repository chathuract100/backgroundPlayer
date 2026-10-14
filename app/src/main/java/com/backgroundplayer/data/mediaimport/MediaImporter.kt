package com.backgroundplayer.data.mediaimport

import android.net.Uri

/**
 * Single import surface the UI depends on: a device-wide MediaStore [scan] and a
 * user-picked SAF [import]. An interface so ViewModels test against a fake.
 */
interface MediaImporter {
    suspend fun scan(): Int
    suspend fun import(uris: List<Uri>): Int
}

/** Delegates to the concrete [MediaStoreScanner] and [SafImporter]. */
class DefaultMediaImporter(
    private val scanner: MediaStoreScanner,
    private val safImporter: SafImporter,
) : MediaImporter {
    override suspend fun scan(): Int = scanner.scan()
    override suspend fun import(uris: List<Uri>): Int = safImporter.import(uris)
}
