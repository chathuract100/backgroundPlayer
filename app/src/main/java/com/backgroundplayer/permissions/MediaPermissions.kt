package com.backgroundplayer.permissions

import android.Manifest
import android.os.Build

/**
 * The runtime permissions the app must request, by platform level. API 33+ uses
 * the granular media permissions plus notifications; older levels fall back to
 * `READ_EXTERNAL_STORAGE`.
 */
object MediaPermissions {
    fun requiredMediaPermissions(sdkInt: Int): List<String> = buildList {
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
