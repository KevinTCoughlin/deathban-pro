package dev.coughlin.deathban.theme

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BanContextTest {
    @Test
    fun `default values are correct`() {
        val context =
            BanContext(
                playerName = "TestPlayer",
                duration = "1h",
                offenseLevel = 1,
                deathCause = "FALL",
                returnTime = "Oct 15, 2024 3:30 PM",
            )

        assertEquals("TestPlayer", context.playerName)
        assertEquals("1h", context.duration)
        assertEquals(1, context.offenseLevel)
        assertEquals("FALL", context.deathCause)
        assertEquals("Oct 15, 2024 3:30 PM", context.returnTime)
        assertFalse(context.isSharedMode)
        assertEquals(0, context.poolLives)
        assertEquals(0, context.poolMax)
    }

    @Test
    fun `shared mode values are set correctly`() {
        val context =
            BanContext(
                playerName = "SharedPlayer",
                duration = "2h",
                offenseLevel = 0,
                deathCause = "VOID",
                returnTime = "Oct 15, 2024 5:00 PM",
                isSharedMode = true,
                poolLives = 3,
                poolMax = 10,
            )

        assertTrue(context.isSharedMode)
        assertEquals(3, context.poolLives)
        assertEquals(10, context.poolMax)
    }

    @Test
    fun `copy preserves values`() {
        val original =
            BanContext(
                playerName = "Player1",
                duration = "1h",
                offenseLevel = 2,
                deathCause = "LAVA",
                returnTime = "Oct 15, 2024 4:00 PM",
            )

        val copy = original.copy(playerName = "Player2")

        assertEquals("Player2", copy.playerName)
        assertEquals(original.duration, copy.duration)
        assertEquals(original.offenseLevel, copy.offenseLevel)
        assertEquals(original.deathCause, copy.deathCause)
        assertEquals(original.returnTime, copy.returnTime)
    }

    @Test
    fun `equality works for identical contexts`() {
        val context1 =
            BanContext(
                playerName = "Player",
                duration = "1h",
                offenseLevel = 1,
                deathCause = "FALL",
                returnTime = "Oct 15, 2024 3:30 PM",
            )
        val context2 =
            BanContext(
                playerName = "Player",
                duration = "1h",
                offenseLevel = 1,
                deathCause = "FALL",
                returnTime = "Oct 15, 2024 3:30 PM",
            )

        assertEquals(context1, context2)
        assertEquals(context1.hashCode(), context2.hashCode())
    }
}
