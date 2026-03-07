package dev.coughlin.deathban.data

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BanRecordTest {
    @Test
    fun `BanRecord stores all fields correctly`() {
        val start = Instant.parse("2024-10-15T10:00:00Z")
        val end = Instant.parse("2024-10-15T11:00:00Z")

        val record =
            BanRecord(
                startTime = start,
                endTime = end,
                offenseLevel = 2,
                deathCause = "LAVA",
            )

        assertEquals(start, record.startTime)
        assertEquals(end, record.endTime)
        assertEquals(2, record.offenseLevel)
        assertEquals("LAVA", record.deathCause)
    }

    @Test
    fun `BanRecord equality works`() {
        val start = Instant.parse("2024-10-15T10:00:00Z")
        val end = Instant.parse("2024-10-15T11:00:00Z")

        val record1 = BanRecord(start, end, 1, "FALL")
        val record2 = BanRecord(start, end, 1, "FALL")

        assertEquals(record1, record2)
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun `BanRecord inequality for different offense levels`() {
        val start = Instant.parse("2024-10-15T10:00:00Z")
        val end = Instant.parse("2024-10-15T11:00:00Z")

        val record1 = BanRecord(start, end, 1, "FALL")
        val record2 = BanRecord(start, end, 2, "FALL")

        assertNotEquals(record1, record2)
    }

    @Test
    fun `DeathRecord stores all fields correctly`() {
        val timestamp = Instant.parse("2024-10-15T10:00:00Z")
        val location = LocationData(100.0, 64.0, -200.0)

        val record =
            DeathRecord(
                timestamp = timestamp,
                world = "world_nether",
                cause = "LAVA",
                killer = null,
                location = location,
            )

        assertEquals(timestamp, record.timestamp)
        assertEquals("world_nether", record.world)
        assertEquals("LAVA", record.cause)
        assertEquals(null, record.killer)
        assertEquals(location, record.location)
    }

    @Test
    fun `DeathRecord with killer UUID`() {
        val timestamp = Instant.parse("2024-10-15T10:00:00Z")
        val killer = java.util.UUID.randomUUID()

        val record =
            DeathRecord(
                timestamp = timestamp,
                world = "world",
                cause = "ENTITY_ATTACK",
                killer = killer,
                location = LocationData(0.0, 64.0, 0.0),
            )

        assertEquals(killer, record.killer)
    }

    @Test
    fun `LocationData equality works`() {
        val loc1 = LocationData(1.0, 2.0, 3.0)
        val loc2 = LocationData(1.0, 2.0, 3.0)

        assertEquals(loc1, loc2)
        assertEquals(loc1.hashCode(), loc2.hashCode())
    }

    @Test
    fun `LocationData inequality for different coordinates`() {
        val loc1 = LocationData(1.0, 2.0, 3.0)
        val loc2 = LocationData(1.0, 2.0, 4.0)

        assertNotEquals(loc1, loc2)
    }
}
