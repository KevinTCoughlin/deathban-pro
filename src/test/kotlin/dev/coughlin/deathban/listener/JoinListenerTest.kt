package dev.coughlin.deathban.listener

import com.destroystokyo.paper.profile.PlayerProfile
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.data.PlayerDataManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoinListenerTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `async preload still runs only after the login is allowed`() {
        val dataManager = mockk<PlayerDataManager>(relaxed = true)
        val listener = JoinListener(mockk(relaxed = true), mockk(relaxed = true), dataManager)
        val event = mockk<AsyncPlayerPreLoginEvent>(relaxed = true)
        val playerId = UUID.randomUUID()
        every { event.uniqueId } returns playerId
        every { event.loginResult } returns AsyncPlayerPreLoginEvent.Result.ALLOWED

        listener.onAsyncPreLogin(event)
        verify(exactly = 1) { dataManager.preload(playerId) }

        every { event.loginResult } returns AsyncPlayerPreLoginEvent.Result.KICK_OTHER
        listener.onAsyncPreLogin(event)
        verify(exactly = 1) { dataManager.preload(any()) }
    }

    @Test
    fun `active ban is denied on both validate calls without skipping server checks`() {
        val playerId = UUID.randomUUID()
        val dataManager = PlayerDataManager(tempDir, Logger.getLogger("join"))
        val data = dataManager.getOrCreate(playerId)
        data.currentBan =
            BanRecord(
                startTime = Instant.parse("2026-11-24T00:00:00Z"),
                endTime = Instant.now().plusSeconds(3600),
                offenseLevel = 1,
                deathCause = "FALL",
            )
        dataManager.save(data)
        val listener = JoinListener(mockk(relaxed = true), messages(), dataManager)
        val event = validateEvent(playerId)

        listener.onConnectionValidate(event)
        listener.onConnectionValidate(event)

        val reason = mutableListOf<Component>()
        verify(exactly = 2) { event.kickMessage(capture(reason)) }
        verify(exactly = 0) { event.allow() }
        assertTrue(reason.all { PlainTextComponentSerializer.plainText().serialize(it).contains("Death Ban") })
        assertTrue(dataManager.get(playerId)!!.isBanned())
    }

    @Test
    fun `configuration phase uses the same ban denial`() {
        val playerId = UUID.randomUUID()
        val dataManager = PlayerDataManager(tempDir, Logger.getLogger("join-config"))
        val data = dataManager.getOrCreate(playerId)
        data.currentBan =
            BanRecord(
                startTime = Instant.now(),
                endTime = Instant.now().plusSeconds(60),
                offenseLevel = 1,
                deathCause = "FIRE",
            )
        dataManager.save(data)
        val listener = JoinListener(mockk(relaxed = true), messages(), dataManager)
        val event = validateEvent(playerId, loginPhase = false)

        listener.onConnectionValidate(event)

        verify(exactly = 1) { event.kickMessage(any<Component>()) }
        verify(exactly = 0) { event.allow() }
    }

    @Test
    fun `expired ban is cleared once and the return title uses the old timings`() {
        val playerId = UUID.randomUUID()
        val dataManager = PlayerDataManager(tempDir, Logger.getLogger("join-expired"))
        val data = dataManager.getOrCreate(playerId)
        data.currentBan =
            BanRecord(
                startTime = Instant.now().minusSeconds(7200),
                endTime = Instant.now().minusSeconds(30),
                offenseLevel = 1,
                deathCause = "FALL",
            )
        dataManager.save(data)
        val tasks = mutableListOf<Runnable>()
        val scheduler = mockk<BukkitScheduler>(relaxed = true)
        val plugin = mockk<Plugin>(relaxed = true)
        every { plugin.server.scheduler } returns scheduler
        every { scheduler.runTaskLater(plugin, any<Runnable>(), any<Long>()) } answers {
            tasks += secondArg<Runnable>()
            mockk(relaxed = true)
        }
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns playerId
        every { plugin.server.getPlayer(playerId) } returns player
        val listener = JoinListener(plugin, messages(), dataManager)
        val event = validateEvent(playerId)

        listener.onConnectionValidate(event)
        listener.onConnectionValidate(event)

        verify(exactly = 0) { event.kickMessage(any<Component>()) }
        verify(exactly = 0) { event.allow() }
        val stored = dataManager.get(playerId)!!
        assertNull(stored.currentBan)
        assertEquals(1, stored.history.count { it.action == "EXPIRED" })

        val join = mockk<PlayerJoinEvent>(relaxed = true)
        every { join.player } returns player
        listener.onPlayerJoin(join)
        assertEquals(1, tasks.size)
        tasks.single().run()

        val shown = slot<Title>()
        verify(exactly = 1) { player.showTitle(capture(shown)) }
        assertEquals(Duration.ofMillis(500), shown.captured.times()?.fadeIn())
        assertEquals(Duration.ofMillis(3_500), shown.captured.times()?.stay())
        assertEquals(Duration.ofMillis(1_000), shown.captured.times()?.fadeOut())
        assertEquals("WELCOME BACK", PlainTextComponentSerializer.plainText().serialize(shown.captured.title()))
    }

    private fun messages(): Messages {
        val file = File(tempDir, "messages.yml")
        file.writeText(javaClass.getResource("/messages.yml")!!.readText())
        return Messages(file)
    }

    private fun validateEvent(
        playerId: UUID,
        loginPhase: Boolean = true,
    ): PlayerConnectionValidateLoginEvent {
        val playerProfile =
            mockk<PlayerProfile>(relaxed = true) {
                every { id } returns playerId
            }
        val connection =
            if (loginPhase) {
                mockk<PlayerLoginConnection>(relaxed = true) {
                    every { authenticatedProfile } returns playerProfile
                    every { unsafeProfile } returns playerProfile
                }
            } else {
                mockk<PlayerConfigurationConnection>(relaxed = true) {
                    every { profile } returns playerProfile
                }
            }
        return mockk(relaxed = true) {
            every { getConnection() } returns connection
        }
    }
}
