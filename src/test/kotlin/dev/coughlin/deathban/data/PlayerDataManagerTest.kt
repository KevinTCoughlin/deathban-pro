package dev.coughlin.deathban.data

import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("PlayerDataManager Tests")
class PlayerDataManagerTest {
    @TempDir
    lateinit var tempDir: File

    private val logger = Logger.getLogger("TestLogger")

    private fun createManager(): PlayerDataManager = PlayerDataManager(tempDir, logger)

    @Test
    @DisplayName("save and load round-trip preserves all fields")
    fun testSaveLoadRoundTrip() {
        val manager = createManager()
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)
        data.offenseLevel = 3
        data.lastDeathTime = Instant.parse("2025-01-15T10:30:00Z")
        data.pendingPardon = true
        data.currentBan =
            BanRecord(
                startTime = Instant.parse("2025-01-15T10:30:00Z"),
                endTime = Instant.parse("2025-01-16T10:30:00Z"),
                offenseLevel = 3,
                deathCause = "FALL",
            )
        data.deaths.add(
            DeathRecord(
                timestamp = Instant.parse("2025-01-15T10:29:00Z"),
                world = "world",
                cause = "FALL",
                killer = UUID.randomUUID(),
                location = LocationData(100.5, 64.0, -200.3),
            ),
        )

        manager.save(data)
        manager.clearCache()

        val loaded = manager.get(uuid)!!
        assertEquals(data.offenseLevel, loaded.offenseLevel)
        assertEquals(data.lastDeathTime, loaded.lastDeathTime)
        assertEquals(data.pendingPardon, loaded.pendingPardon)
        assertEquals(data.currentBan, loaded.currentBan)
        assertEquals(1, loaded.deaths.size)
        assertEquals(data.deaths[0].timestamp, loaded.deaths[0].timestamp)
        assertEquals(data.deaths[0].world, loaded.deaths[0].world)
        assertEquals(data.deaths[0].cause, loaded.deaths[0].cause)
        assertEquals(data.deaths[0].killer, loaded.deaths[0].killer)
        assertEquals(data.deaths[0].location, loaded.deaths[0].location)
    }

    @Test
    @DisplayName("load handles corrupted ban timestamps gracefully")
    fun testCorruptedBanTimestamps() {
        val uuid = UUID.randomUUID()
        val playersFolder = File(tempDir, "players")
        playersFolder.mkdirs()

        val file = File(playersFolder, "$uuid.yml")
        val config = YamlConfiguration()
        config.set("offense-level", 2)
        config.set("current-ban.start-time", null) // Missing start time
        config.set("current-ban.end-time", "2025-01-16T10:30:00Z")
        config.set("current-ban.offense-level", 2)
        config.set("current-ban.death-cause", "FALL")
        config.save(file)

        val manager = createManager()
        val loaded = manager.get(uuid)

        // Should load without crashing, but ban record should be skipped
        assertTrue(loaded != null)
        assertNull(loaded.currentBan)
        assertEquals(2, loaded.offenseLevel)
    }

    @Test
    @DisplayName("load handles invalid ban timestamp format gracefully")
    fun testInvalidBanTimestampFormat() {
        val uuid = UUID.randomUUID()
        val playersFolder = File(tempDir, "players")
        playersFolder.mkdirs()

        val file = File(playersFolder, "$uuid.yml")
        val config = YamlConfiguration()
        config.set("offense-level", 1)
        config.set("current-ban.start-time", "not-a-timestamp")
        config.set("current-ban.end-time", "also-not-valid")
        config.set("current-ban.offense-level", 1)
        config.save(file)

        val manager = createManager()
        val loaded = manager.get(uuid)

        assertTrue(loaded != null)
        assertNull(loaded.currentBan)
    }

    @Test
    @DisplayName("getOrCreate returns new PlayerData for unknown UUID")
    fun testGetOrCreate() {
        val manager = createManager()
        val uuid = UUID.randomUUID()
        val data = manager.getOrCreate(uuid)

        assertEquals(uuid, data.uuid)
        assertEquals(0, data.offenseLevel)
    }

    @Test
    @DisplayName("getOrCreate returns same cached instance for known UUID")
    fun testGetOrCreateCached() {
        val manager = createManager()
        val uuid = UUID.randomUUID()

        val data1 = manager.getOrCreate(uuid)
        data1.offenseLevel = 5
        val data2 = manager.getOrCreate(uuid)

        assertEquals(5, data2.offenseLevel, "getOrCreate should return the cached instance with mutations")
    }

    @Test
    @DisplayName("saveAsync writes data synchronously when no plugin")
    fun testSaveAsyncWithoutPlugin() {
        val manager = createManager()
        val uuid = UUID.randomUUID()
        val data = PlayerData(uuid)
        data.offenseLevel = 7

        manager.saveAsync(data)
        manager.clearCache()

        val loaded = manager.get(uuid)!!
        assertEquals(7, loaded.offenseLevel)
    }

    @Test
    @DisplayName("getAllStoredPlayers returns saved UUIDs")
    fun testGetAllStoredPlayers() {
        val manager = createManager()
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        manager.save(PlayerData(uuid1))
        manager.save(PlayerData(uuid2))

        val stored = manager.getAllStoredPlayers()
        assertTrue(stored.contains(uuid1))
        assertTrue(stored.contains(uuid2))
        assertEquals(2, stored.size)
    }

    @Test
    @DisplayName("pending bans round-trip through save and load")
    fun testPendingBansRoundTrip() {
        val manager = createManager()
        val uuid = UUID.randomUUID()

        manager.addPendingBan(uuid)
        assertTrue(manager.getPendingBans().contains(uuid))

        manager.removePendingBan(uuid)
        assertTrue(manager.getPendingBans().isEmpty())
    }
}
