package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.manager.SharedLivesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Test
import java.util.UUID

class RepeatDeathTest {
    @Test
    fun `duplicate event is ignored but immediate death after respawn consumes another life`() {
        val player = mockk<Player>(relaxed = true)
        val uuid = UUID.randomUUID()
        every { player.uniqueId } returns uuid
        every { player.hasPermission(any<String>()) } returns false
        every { player.world.name } returns "world"
        val event = mockk<PlayerDeathEvent>(relaxed = true)
        every { event.entity } returns player
        val respawn = mockk<PlayerRespawnEvent>()
        every { respawn.player } returns player
        val config = YamlConfiguration().also { it.set("mode", "shared") }
        val manager = mockk<SharedLivesManager>(relaxed = true)
        every { manager.consumeLife(uuid) } returns true
        val listener =
            DeathListener(
                mockk<Plugin>(relaxed = true),
                Settings(config),
                mockk<Messages>(relaxed = true),
                mockk<PlayerDataManager>(),
                mockk(),
                mockk(),
                manager,
            )
        listener.onPlayerDeath(event)
        listener.onPlayerDeath(event)
        verify(exactly = 1) { manager.consumeLife(uuid) }
        listener.onRespawn(respawn)
        listener.onPlayerDeath(event)
        verify(exactly = 2) { manager.consumeLife(uuid) }
    }
}
