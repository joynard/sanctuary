package com.sanctuary.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScrollPlatformTest {

    @Test
    fun `regional TikTok package maps to TikTok`() {
        assertEquals(
            ScrollPlatform.TIKTOK,
            ScrollPlatform.fromPackageName("com.ss.android.ugc.trill"),
        )
        assertEquals(
            ScrollPlatform.TIKTOK,
            ScrollPlatform.fromPackageName("com.zhiliaoapp.musically"),
        )
    }

    @Test
    fun `unknown package and empty package return null`() {
        assertNull(ScrollPlatform.fromPackageName("com.example.app"))
        assertNull(ScrollPlatform.fromPackageName(null))
        assertNull(ScrollPlatform.fromPackageName(""))
    }

    @Test
    fun `key round trip is stable`() {
        ScrollPlatform.entries.forEach { platform ->
            assertEquals(platform, ScrollPlatform.fromKey(platform.key))
        }
        assertNull(ScrollPlatform.fromKey("facebook"))
    }
}
