package com.backgroundplayer.permissions

import android.Manifest
import android.os.Build

/**
 * The runtime permissions the app must request, by platform level. API 33+ uses
 * the granular media permissions plus notifications; older levels fall back to
 * `READ_EXTERNAL_STORAGE`.
 */
object MediaPermissions {
    /** Everything to request up front, including the optional notifications permission. */
    fun requiredMediaPermissions(sdkInt: Int): List<String> = buildList {
        addAll(blockingPermissions(sdkInt))
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * The permissions the app genuinely cannot function without (reading media).
     * Notifications are intentionally excluded: denying them must not block
     * browsing or playback — it only suppresses the media notification.
     */
    fun blockingPermissions(sdkInt: Int): List<String> =
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
}
