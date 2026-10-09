package dev.coughlin.deathban.manager

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.theme.DefaultTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BanManagerTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var manager: BanManager
    private lateinit var dataManager: PlayerDataManager
    private lateinit var player: Player
    private lateinit var kick: Runnable

    @BeforeEach
    fun setUp() {
        mockkStatic(Bukkit::class)
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        every { Bukkit.getScheduler() } returns scheduler
        every { scheduler.runTaskLater(any(), any<Runnable>(), 60L) } answers {
            assertTrue(PlayerDataManager(tempDir, Logger.getLogger("ReloadTest")).get(player.uniqueId)!!.isBanned())
            kick = secondArg()
            mockk(relaxed = true)
        }
        val plugin = mockk<DeathBanPlugin>(relaxed = true)
        every { plugin.themeManager.getActiveTheme() } returns DefaultTheme()
        val config = YamlConfiguration()
        config.set("theme-sounds", false)
        config.set("theme-particles", false)
        dataManager = PlayerDataManager(tempDir, Logger.getLogger("BanManagerTest"))
        manager = BanManager(plugin, Settings(config), mockk<Messages>(relaxed = true), dataManager)
        player = mockk(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.isOnline } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    @Test
    fun `ban pardon and reset history commits together and survives restart`() {
        every { Bukkit.getPlayer(player.uniqueId) } returns player
        val data = dataManager.getOrCreate(player.uniqueId)
        data.offenseLevel = 1
        manager.applyBan(player, data, "FALL")
        manager.pardon(player.uniqueId, "Moderator")
        manager.reset(player.uniqueId, "Owner")
        val reloaded = PlayerDataManager(tempDir, Logger.getAnonymousLogger()).get(player.uniqueId)!!
        kotlin.test.assertEquals(listOf("BANNED", "PARDONED", "RESET"), reloaded.history.map { it.action })
        kotlin.test.assertEquals(
            "FALL",
            reloaded.history
                .first()
                .ban!!
                .deathCause,
        )
        kotlin.test.assertEquals("Moderator", reloaded.history[1].actor)
        kotlin.test.assertEquals("Owner", reloaded.history[2].actor)
        assertNull(reloaded.currentBan)
    }

    @Test
    fun `active individual ban kicks after delay`() {
        val data = dataManager.getOrCreate(player.uniqueId)
        data.offenseLevel = 1
        manager.applyBan(player, data, "FALL")

        kick.run()

        verify(exactly = 1) { player.kickPlayer(any()) }
    }

    @Test
    fun `retained player reference cannot clear committed ban when saved again`() {
        val retained = dataManager.getOrCreate(player.uniqueId)
        retained.offenseLevel = 1
        manager.applyBan(player, retained, "FALL")

        assertTrue(retained.isBanned())
        dataManager.saveAsync(retained)

        assertTrue(PlayerDataManager(tempDir, Logger.getLogger("ReloadTest")).get(player.uniqueId)!!.isBanned())
    }

    @Test
    fun `retained player reference cannot restore pardoned or reset ban`() {
        every { Bukkit.getPlayer(player.uniqueId) } returns player
        val retained = dataManager.getOrCreate(player.uniqueId)
        retained.offenseLevel = 1
        manager.applyBan(player, retained, "FALL")
        manager.pardon(player.uniqueId)
        assertFalse(retained.isBanned())
        dataManager.saveAsync(retained)
        assertFalse(PlayerDataManager(tempDir, Logger.getLogger("ReloadTest")).get(player.uniqueId)!!.isBanned())

        manager.applyBan(player, retained, "FALL")
        manager.reset(player.uniqueId)
        assertFalse(retained.isBanned())
        dataManager.saveAsync(retained)
        assertFalse(PlayerDataManager(tempDir, Logger.getLogger("ReloadTest")).get(player.uniqueId)!!.isBanned())
    }

    @Test
    fun `reset before delayed individual kick keeps player connected`() {
        val data = dataManager.getOrCreate(player.uniqueId)
        data.offenseLevel = 1
        manager.applyBan(player, data, "FALL")
        manager.reset(player.uniqueId)

        kick.run()

        verify(exactly = 0) { player.kickPlayer(any()) }
    }

    @Test
    fun `older scheduled kick does not enforce a replacement ban`() {
        val data = dataManager.getOrCreate(player.uniqueId)
        data.offenseLevel = 1
        manager.applyBan(player, data, "FALL")
        val current = dataManager.get(player.uniqueId)!!
        current.currentBan = current.currentBan!!.copy(endTime = Instant.now().plusSeconds(7200))

        kick.run()

        verify(exactly = 0) { player.kickPlayer(any()) }
    }

    @Test
    fun `pardon before delayed shared kick keeps player connected`() {
        every { Bukkit.getPlayer(player.uniqueId) } returns player
        manager.applySharedBan(player, "FALL")
        manager.pardon(player.uniqueId)

        kick.run()

        verify(exactly = 0) { player.kickPlayer(any()) }
    }

    @Test
    fun `failed ban write leaves player unbanned without title or scheduled kick`() {
        val data = dataManager.getOrCreate(player.uniqueId)
        data.offenseLevel = 1
        blockRecordWrite()

        assertFailsWith<IOException> { manager.applyBan(player, data, "FALL") }

        assertNull(dataManager.get(player.uniqueId)!!.currentBan)
        verify(exactly = 0) { player.sendTitle(any(), any(), any(), any(), any()) }
        assertTrue(!this::kick.isInitialized)
    }

    @Test
    fun `failed pardon or reset write preserves active cached ban`() {
        every { Bukkit.getPlayer(player.uniqueId) } returns player
        manager.applySharedBan(player, "FALL")
        blockRecordWrite()

        assertFailsWith<IOException> { manager.pardon(player.uniqueId) }
        assertTrue(dataManager.get(player.uniqueId)!!.isBanned())
        assertFailsWith<IOException> { manager.reset(player.uniqueId) }
        assertTrue(dataManager.get(player.uniqueId)!!.isBanned())
    }

    private fun blockRecordWrite() {
        val record = File(tempDir, "players/${player.uniqueId}.yml")
        record.delete()
        record.mkdirs()
        File(record, "sentinel").writeText("keep")
    }
}
