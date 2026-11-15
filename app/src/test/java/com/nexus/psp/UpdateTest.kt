package com.nexus.psp

import com.nexus.psp.data.Update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    @Test fun versionFromReleaseTitle() {
        assertEquals("0.36.4", Update.versionOf("KAPSULA 0.36.4"))
        assertEquals(null, Update.versionOf("KAPSULA — последняя сборка"))
    }
    @Test fun compareVersions() {
        assertTrue(Update.newer("0.36.10", "0.36.9")); assertTrue(Update.newer("0.37", "0.36.4")); assertTrue(Update.newer("1.0.0", "0.99.9"))
        assertFalse(Update.newer("0.36.3", "0.36.3")); assertFalse(Update.newer("0.36.2", "0.36.3"))
    }
}
