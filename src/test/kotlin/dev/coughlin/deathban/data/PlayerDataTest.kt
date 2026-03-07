package dev.coughlin.deathban.data

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerDataTest {
    @Test
    fun `isBanned returns true when ban is active`() {
        val data = PlayerData(UUID.randomUUID())
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(60),
                endTime = Instant.now().plusSeconds(3600), // 1 hour from now
                offenseLevel = 1,
                deathCause = "FALL",
            )

        assertTrue(data.isBanned())
    }

    @Test
    fun `isBanned returns false when ban expired`() {
        val data = PlayerData(UUID.randomUUID())
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(7200),
                endTime = Instant.now().minusSeconds(3600), // 1 hour ago
                offenseLevel = 1,
                deathCause = "FALL",
            )

        assertFalse(data.isBanned())
    }

    @Test
    fun `isBanned returns false when no ban`() {
        val data = PlayerData(UUID.randomUUID())
        assertFalse(data.isBanned())
    }

    @Test
    fun `clearExpiredBan removes expired ban`() {
        val data = PlayerData(UUID.randomUUID())
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(7200),
                endTime = Instant.now().minusSeconds(3600),
                offenseLevel = 1,
                deathCause = "FALL",
            )

        data.clearExpiredBan()

        assertNull(data.currentBan)
    }

    @Test
    fun `clearExpiredBan keeps active ban`() {
        val data = PlayerData(UUID.randomUUID())
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
    fun `default values are correct`() {
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)

        assertEquals(uuid, data.uuid)
        assertEquals(0, data.offenseLevel)
        assertNull(data.lastDeathTime)
        assertTrue(data.deaths.isEmpty())
        assertNull(data.currentBan)
        assertFalse(data.pendingPardon)
    }

    @Test
    fun `LocationData from creates correct data`() {
        // Can't test with real Location without Bukkit, but test the data class
        val location = LocationData(100.5, 64.0, -200.3)

        assertEquals(100.5, location.x)
        assertEquals(64.0, location.y)
        assertEquals(-200.3, location.z)
    }
}
