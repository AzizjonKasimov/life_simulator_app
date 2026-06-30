package com.azizjonkasimov.lifesimulator.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {
    @Test
    fun newerManifestIsAvailable() {
        val result = parseUpdateManifest(
            body = """
                {
                  "versionCode": 2,
                  "versionName": "0.2.0",
                  "apkUrl": "https://example.com/LifeSimulator-0.2.0.apk",
                  "notes": "New daily actions"
                }
            """.trimIndent(),
            currentVersionCode = 1,
        )

        assertTrue(result is UpdateCheckResult.Available)
        val info = (result as UpdateCheckResult.Available).info
        assertEquals(2L, info.versionCode)
        assertEquals("0.2.0", info.versionName)
        assertEquals("https://example.com/LifeSimulator-0.2.0.apk", info.apkUrl)
    }

    @Test
    fun sameOrOlderManifestIsUpToDate() {
        val result = parseUpdateManifest(
            body = """
                {
                  "versionCode": 1,
                  "versionName": "0.1.0",
                  "apkUrl": "https://example.com/LifeSimulator-0.1.0.apk"
                }
            """.trimIndent(),
            currentVersionCode = 1,
        )

        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    @Test
    fun brokenManifestIsUnavailable() {
        val result = parseUpdateManifest("not json", currentVersionCode = 1)

        assertEquals(UpdateCheckResult.Unavailable, result)
    }
}
