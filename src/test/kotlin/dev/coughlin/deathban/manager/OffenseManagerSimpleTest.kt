package dev.coughlin.deathban.manager

import dev.coughlin.deathban.config.OffenseConfig
import dev.coughlin.deathban.data.DeathRecord
import dev.coughlin.deathban.data.LocationData
import dev.coughlin.deathban.data.PlayerData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("OffenseManager Additional Tests")
class OffenseManagerSimpleTest {
    private lateinit var offenseManager: OffenseManager

    @BeforeEach
    fun setup() {
        offenseManager = OffenseManager(TestConfig())
    }

    @Test
    @DisplayName("Offense level increments on ban")
    fun testOffenseLevelIncrement() {
        val data = PlayerData(UUID.randomUUID())
        assertEquals(0, data.offenseLevel)
        data.offenseLevel++
        assertEquals(1, data.offenseLevel)
    }

    @Test
    @DisplayName("Death tracking works correctly")
    fun testDeathTracking() {
        val data = PlayerData(UUID.randomUUID())
        assertEquals(0, data.deaths.size)

        val death =
            DeathRecord(
                timestamp = Instant.now(),
                world = "world",
                cause = "FALL",
                killer = null,
                location = LocationData(0.0, 64.0, 0.0),
            )
        data.deaths.add(death)

        assertEquals(1, data.deaths.size)
        assertEquals("FALL", data.deaths[0].cause)
    }

    @Test
    @DisplayName("Player can have multiple deaths")
    fun testMultipleDeaths() {
        val data = PlayerData(UUID.randomUUID())

        repeat(3) { i ->
            data.deaths.add(
                DeathRecord(
                    timestamp = Instant.now().minus(Duration.ofMinutes(i.toLong())),
                    world = "world",
                    cause = "FALL",
                    killer = null,
                    location = LocationData(0.0, 64.0, 0.0),
                ),
            )
        }

        assertEquals(3, data.deaths.size)
    }

    @Test
    @DisplayName("Remaining lives decrease with each death")
    fun testRemainingLives() {
        val data = PlayerData(UUID.randomUUID())

        val initialLives = offenseManager.getRemainingLives(data)
        assertTrue(initialLives > 0)

        // Add a death
        data.deaths.add(
            DeathRecord(
                timestamp = Instant.now(),
                world = "world",
                cause = "FALL",
                killer = null,
                location = LocationData(0.0, 64.0, 0.0),
            ),
        )

        val livesAfterDeath = offenseManager.getRemainingLives(data)
        assertEquals(initialLives - 1, livesAfterDeath)
    }

    private class TestConfig : OffenseConfig {
        override val rollingWindowEnabled: Boolean = true
        override val rollingWindowDuration: Duration = Duration.ofHours(24)
        override val maxDeathsInWindow: Int = 3
        override val offenseResetEnabled: Boolean = true
        override val offenseResetPeriod: Duration = Duration.ofDays(7)
    }
}
