package dev.coughlin.deathban.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@DisplayName("PlayerData Snapshot Tests")
class PlayerDataSnapshotTest {
    @Test
    @DisplayName("snapshot creates independent copy of deaths list")
    fun testSnapshotDeathsIndependence() {
        val data = PlayerData(UUID.randomUUID())
        data.deaths.add(
            DeathRecord(
                timestamp = Instant.now(),
                world = "world",
                cause = "FALL",
                location = LocationData(0.0, 64.0, 0.0),
            ),
        )

        val snap = data.snapshot()

        // Mutate original — snapshot should be unaffected
        data.deaths.add(
            DeathRecord(
                timestamp = Instant.now(),
                world = "nether",
                cause = "LAVA",
                location = LocationData(10.0, 30.0, 10.0),
            ),
        )

        assertEquals(2, data.deaths.size)
        assertEquals(1, snap.deaths.size)
    }

    @Test
    @DisplayName("snapshot preserves all scalar fields")
    fun testSnapshotPreservesFields() {
        val uuid = UUID.randomUUID()
        val ban =
            BanRecord(
                startTime = Instant.now().minusSeconds(60),
                endTime = Instant.now().plusSeconds(3600),
                offenseLevel = 2,
                deathCause = "VOID",
            )
        val data =
            PlayerData(
                uuid = uuid,
                offenseLevel = 4,
                lastDeathTime = Instant.now().minusSeconds(120),
                currentBan = ban,
                pendingPardon = true,
            )

        val snap = data.snapshot()

        assertEquals(uuid, snap.uuid)
        assertEquals(4, snap.offenseLevel)
        assertEquals(data.lastDeathTime, snap.lastDeathTime)
        assertEquals(ban, snap.currentBan)
        assertEquals(true, snap.pendingPardon)
    }

    @Test
    @DisplayName("snapshot deaths list is a different instance")
    fun testSnapshotDeathsListIsDifferentInstance() {
        val data = PlayerData(UUID.randomUUID())
        val snap = data.snapshot()

        assertNotSame(data.deaths, snap.deaths)
    }
}
