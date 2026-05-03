package dev.coughlin.deathban.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("DeathRecord Tests")
class DeathRecordTest {
    @Test
    @DisplayName("DeathRecord stores all fields")
    fun testDeathRecordFields() {
        val timestamp = Instant.now()
        val killer = UUID.randomUUID()
        val location = LocationData(100.0, 64.0, 200.0)

        val death =
            DeathRecord(
                timestamp = timestamp,
                world = "world",
                cause = "FALL",
                killer = killer,
                location = location,
            )

        assertEquals(timestamp, death.timestamp)
        assertEquals("world", death.world)
        assertEquals("FALL", death.cause)
        assertEquals(killer, death.killer)
        assertEquals(location, death.location)
    }

    @Test
    @DisplayName("DeathRecord can have no killer")
    fun testDeathRecordNoKiller() {
        val death =
            DeathRecord(
                timestamp = Instant.now(),
                world = "world",
                cause = "FALL",
                killer = null,
                location = LocationData(0.0, 64.0, 0.0),
            )

        assertNull(death.killer)
    }

    @Test
    @DisplayName("DeathRecord tracks different death causes")
    fun testDifferentDeathCauses() {
        val causes = listOf("FALL", "LAVA", "FIRE", "DROWNING", "EXPLOSION", "SUFFOCATION")

        causes.forEach { cause ->
            val death =
                DeathRecord(
                    timestamp = Instant.now(),
                    world = "world",
                    cause = cause,
                    killer = null,
                    location = LocationData(0.0, 64.0, 0.0),
                )

            assertEquals(cause, death.cause)
        }
    }

    @Test
    @DisplayName("DeathRecord tracks different worlds")
    fun testDifferentWorlds() {
        val worlds = listOf("world", "world_nether", "world_the_end", "custom_world")

        worlds.forEach { world ->
            val death =
                DeathRecord(
                    timestamp = Instant.now(),
                    world = world,
                    cause = "FALL",
                    killer = null,
                    location = LocationData(0.0, 64.0, 0.0),
                )

            assertEquals(world, death.world)
        }
    }
}
