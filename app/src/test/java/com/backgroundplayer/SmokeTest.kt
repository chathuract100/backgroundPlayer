package com.backgroundplayer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Confirms the build is wired up: the generated BuildConfig exists and carries
 * the correct application id. This is the project's smoke test for the unit-test
 * harness itself.
 */
class SmokeTest {
    @Test
    fun appPackageIsConfigured() {
        assertEquals("com.backgroundplayer", BuildConfig.APPLICATION_ID)
    }
}
