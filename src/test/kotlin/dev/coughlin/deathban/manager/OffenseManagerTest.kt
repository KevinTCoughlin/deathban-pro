package dev.coughlin.deathban.manager

import dev.coughlin.deathban.config.OffenseConfig
import dev.coughlin.deathban.data.DeathRecord
import dev.coughlin.deathban.data.LocationData
import dev.coughlin.deathban.data.PlayerData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OffenseManagerTest {
    private lateinit var offenseManager: OffenseManager

    @BeforeEach
    fun setup() {
        offenseManager = OffenseManager(TestOffenseConfig())
    }

    @Test
    fun `getDeathsInWindow counts only recent deaths`() {
        val data = PlayerData(UUID.randomUUID())

        // Add death from 2 hours ago (should count)
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(2))))

        // Add death from 48 hours ago (should not count)
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(48))))

        assertEquals(1, offenseManager.getDeathsInWindow(data))
    }

    @Test
    fun `getDeathsInWindow counts all deaths when rolling window disabled`() {
        offenseManager = OffenseManager(TestOffenseConfig(rollingWindowEnabled = false))

        val data = PlayerData(UUID.randomUUID())
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(2))))
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(48))))

        assertEquals(2, offenseManager.getDeathsInWindow(data))
    }

    @Test
    fun `shouldTriggerBan returns true when deaths reach threshold`() {
        val data = PlayerData(UUID.randomUUID())

        // Add 3 recent deaths
        repeat(3) {
            data.deaths.add(createDeath(Instant.now().minus(Duration.ofMinutes(it.toLong() + 1))))
        }

        assertTrue(offenseManager.shouldTriggerBan(data))
    }

    @Test
    fun `shouldTriggerBan returns false when below threshold`() {
        val data = PlayerData(UUID.randomUUID())

        // Add 2 recent deaths
        repeat(2) {
            data.deaths.add(createDeath(Instant.now().minus(Duration.ofMinutes(it.toLong() + 1))))
        }

        assertFalse(offenseManager.shouldTriggerBan(data))
    }

    @Test
    fun `checkOffenseReset resets offense level after clean period`() {
        val data = PlayerData(UUID.randomUUID())
        data.offenseLevel = 3
        data.lastDeathTime = Instant.now().minus(Duration.ofDays(8)) // 8 days ago

        assertTrue(offenseManager.checkOffenseReset(data))
        assertEquals(0, data.offenseLevel)
        assertTrue(data.deaths.isEmpty())
    }

    @Test
    fun `checkOffenseReset does not reset during clean period`() {
        val data = PlayerData(UUID.randomUUID())
        data.offenseLevel = 3
        data.lastDeathTime = Instant.now().minus(Duration.ofDays(5)) // 5 days ago

        assertFalse(offenseManager.checkOffenseReset(data))
        assertEquals(3, data.offenseLevel)
    }

    @Test
    fun `checkOffenseReset does nothing when disabled`() {
        offenseManager = OffenseManager(TestOffenseConfig(offenseResetEnabled = false))

        val data = PlayerData(UUID.randomUUID())
        data.offenseLevel = 3
        data.lastDeathTime = Instant.now().minus(Duration.ofDays(30))

        assertFalse(offenseManager.checkOffenseReset(data))
        assertEquals(3, data.offenseLevel)
    }

    @Test
    fun `pruneOldDeaths removes deaths outside window`() {
        val data = PlayerData(UUID.randomUUID())

        // Add recent death
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(2))))

        // Add old death
        data.deaths.add(createDeath(Instant.now().minus(Duration.ofHours(48))))

        offenseManager.pruneOldDeaths(data)

        assertEquals(1, data.deaths.size)
    }

    @Test
    fun `getRemainingLives calculates correctly`() {
        val data = PlayerData(UUID.randomUUID())

        assertEquals(3, offenseManager.getRemainingLives(data))

        data.deaths.add(createDeath(Instant.now()))
        assertEquals(2, offenseManager.getRemainingLives(data))

        data.deaths.add(createDeath(Instant.now()))
        assertEquals(1, offenseManager.getRemainingLives(data))

        data.deaths.add(createDeath(Instant.now()))
        assertEquals(0, offenseManager.getRemainingLives(data))
    }

    @Test
    fun `getRemainingLives never goes negative`() {
        val data = PlayerData(UUID.randomUUID())

        // Add more deaths than max
        repeat(5) {
            data.deaths.add(createDeath(Instant.now().minus(Duration.ofMinutes(it.toLong()))))
        }

        assertEquals(0, offenseManager.getRemainingLives(data))
    }

    private fun createDeath(timestamp: Instant) =
        DeathRecord(
            timestamp = timestamp,
            world = "world",
            cause = "FALL",
            killer = null,
            location = LocationData(0.0, 64.0, 0.0),
        )

    private class TestOffenseConfig(
        override val rollingWindowEnabled: Boolean = true,
        override val rollingWindowDuration: Duration = Duration.ofHours(24),
        override val maxDeathsInWindow: Int = 3,
        override val offenseResetEnabled: Boolean = true,
        override val offenseResetPeriod: Duration = Duration.ofDays(7),
    ) : OffenseConfig
}
