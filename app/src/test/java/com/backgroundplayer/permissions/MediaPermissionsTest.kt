package com.backgroundplayer.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionsTest {

    @Test
    fun api33RequestsGranularMediaAndNotifications() {
        val perms = MediaPermissions.requiredMediaPermissions(33)

        assertTrue(perms.contains("android.permission.READ_MEDIA_AUDIO"))
        assertTrue(perms.contains("android.permission.READ_MEDIA_VIDEO"))
        assertTrue(perms.contains("android.permission.POST_NOTIFICATIONS"))
        assertFalse(perms.contains("android.permission.READ_EXTERNAL_STORAGE"))
    }

    @Test
    fun api29RequestsReadExternalStorage() {
        val perms = MediaPermissions.requiredMediaPermissions(29)

        assertTrue(perms.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(perms.contains("android.permission.READ_MEDIA_AUDIO"))
        assertFalse(perms.contains("android.permission.POST_NOTIFICATIONS"))
    }
}
