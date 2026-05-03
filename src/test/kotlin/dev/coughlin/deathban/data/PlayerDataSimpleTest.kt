package dev.coughlin.deathban.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("PlayerData Additional Tests")
class PlayerDataSimpleTest {
    @Test
    @DisplayName("PlayerData initializes with correct defaults")
    fun testPlayerDataDefaults() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        assertEquals(uuid, data.uuid)
        assertEquals(0, data.offenseLevel)
        assertEquals(0, data.deaths.size)
        assertNull(data.currentBan)
        assertFalse(data.pendingPardon)
    }

    @Test
    @DisplayName("Ban expiration works correctly")
    fun testBanExpiration() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        // No ban means not banned
        assertFalse(data.isBanned())

        // Add a ban in the past (should be expired)
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(3600),
                endTime = Instant.now().minusSeconds(1),
                offenseLevel = 1,
                deathCause = "FALL",
            )

        // Ban should be expired
        assertFalse(data.isBanned())
    }

    @Test
    @DisplayName("Ban in future is active")
    fun testActiveBan() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        // Add a ban in the future
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(60),
                endTime = Instant.now().plusSeconds(3600),
                offenseLevel = 1,
                deathCause = "FALL",
            )

        // Ban should be active
        assertTrue(data.isBanned())
    }

    @Test
    @DisplayName("clearExpiredBan removes expired bans")
    fun testClearExpiredBan() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        // Add an expired ban
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(3600),
                endTime = Instant.now().minusSeconds(1),
                offenseLevel = 1,
                deathCause = "FALL",
            )

        assertTrue(data.currentBan != null)
        data.clearExpiredBan()
        assertNull(data.currentBan)
    }

    @Test
    @DisplayName("clearExpiredBan preserves active bans")
    fun testPreserveActiveBan() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        val ban =
            BanRecord(
                startTime = Instant.now().minusSeconds(60),
                endTime = Instant.now().plusSeconds(3600),
                offenseLevel = 1,
                deathCause = "FALL",
            )
        data.currentBan = ban

        data.clearExpiredBan()
        assertEquals(ban, data.currentBan)
    }

    @Test
    @DisplayName("Multiple deaths can be tracked")
    fun testMultipleDeathTracking() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        repeat(5) { i ->
            data.deaths.add(
                DeathRecord(
                    timestamp = Instant.now().minusSeconds((i * 60).toLong()),
                    world = "world_$i",
                    cause = "CAUSE_$i",
                    killer = null,
                    location = LocationData(x = i.toDouble(), y = 64.0, z = i.toDouble()),
                ),
            )
        }

        assertEquals(5, data.deaths.size)
    }
}
