package dev.coughlin.deathban.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BanModeTest {
    @Test
    fun `BanMode has correct values`() {
        assertEquals(2, BanMode.entries.size)
        assertEquals(BanMode.INDIVIDUAL, BanMode.valueOf("INDIVIDUAL"))
        assertEquals(BanMode.SHARED, BanMode.valueOf("SHARED"))
    }
}
