package dev.coughlin.deathban.data

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryTest {
    @TempDir lateinit var folder: File

    @Test
    fun `history stays bounded and survives restart independently of deaths`() {
        val manager = PlayerDataManager(folder, Logger.getAnonymousLogger())
        val data = manager.getOrCreate(UUID.randomUUID())
        repeat(105) { data.recordHistory("RESET", actor = "admin-$it") }
        val snapshot = data.snapshot()
        snapshot.recordHistory("PARDONED")
        assertEquals(100, data.history.size)
        assertEquals("admin-5", data.history.first().actor)
        manager.save(data)
        val reloaded = PlayerDataManager(folder, Logger.getAnonymousLogger()).get(data.uuid)!!
        assertEquals(data.history, reloaded.history)
        assertEquals(0, reloaded.deaths.size)
    }

    @Test
    fun `expiry records original ban once with its actual end time`() {
        val data = PlayerData(UUID.randomUUID())
        val now = Instant.now()
        val ban = BanRecord(now.minusSeconds(120), now.minusSeconds(60), 2, "LAVA", "red")
        data.currentBan = ban
        data.clearExpiredBan()
        data.clearExpiredBan()
        assertNull(data.currentBan)
        assertEquals(listOf(HistoryRecord(ban.endTime, "EXPIRED", ban)), data.history)
    }
}
