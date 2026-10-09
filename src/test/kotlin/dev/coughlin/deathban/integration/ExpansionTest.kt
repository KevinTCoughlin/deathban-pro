package dev.coughlin.deathban.integration

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.data.PlayerData
import dev.coughlin.deathban.data.SharedLivesPool
import dev.coughlin.deathban.manager.OffenseManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpansionTest {
    @Test
    fun `optional placeholders resolve cached player and team data without disk lookup`() {
        mockkStatic(Bukkit::class)
        try {
            every { Bukkit.isPrimaryThread() } returns true
            val plugin = mockk<DeathBanPlugin>(relaxed = true)
            val player = mockk<OfflinePlayer>()
            val uuid = UUID.randomUUID()
            every { player.uniqueId } returns uuid
            val data = PlayerData(uuid, offenseLevel = 2)
            data.currentBan = BanRecord(Instant.now(), Instant.now().plusSeconds(120), 2, "LAVA")
            val dataManager = plugin.dataManager
            every { dataManager.getCached(uuid) } returns data
            every { plugin.settings } returns Settings(YamlConfiguration())
            every { plugin.offenseManager } returns OffenseManager(plugin.settings)
            every { plugin.sharedLivesManager } returns null
            val expansion = DeathBanExpansion(plugin)
            assertEquals("3", expansion.onRequest(player, "remaining_lives"))
            assertEquals("2", expansion.onRequest(player, "offense_level"))
            assertTrue(expansion.onRequest(player, "ban_remaining_seconds")!!.toLong() in 118..120)
            assertEquals("none", expansion.onRequest(player, "team"))
            assertNull(expansion.onRequest(null, "remaining_lives"))
            assertNull(expansion.onRequest(player, "unknown"))
            val config = YamlConfiguration().also { it.set("mode", "shared") }
            every { plugin.settings } returns Settings(config)
            val pool = SharedLivesPool("red", 4, 10)
            every { plugin.sharedLivesManager } returns mockk(relaxed = true)
            every { plugin.sharedLivesManager!!.getPoolForPlayer(uuid) } returns pool
            assertEquals("4", expansion.onRequest(player, "remaining_lives"))
            assertEquals("red", expansion.onRequest(player, "team"))
            assertEquals("10", expansion.onRequest(player, "pool_max_lives"))
            every { Bukkit.isPrimaryThread() } returns false
            assertNull(expansion.onRequest(player, "team"))
            verify(exactly = 0) { dataManager.get(any()) }
        } finally {
            unmockkStatic(Bukkit::class)
        }
    }
}
