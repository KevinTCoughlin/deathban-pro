package dev.coughlin.deathban.testutils

import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * Factory for creating mock Spigot Event instances.
 * Provides convenient methods for constructing events for testing listeners.
 *
 * Note: These are simple relaxed mocks. For more complex event behavior,
 * use MockK's `every` syntax to configure specific behaviors.
 */
object MockEventFactory {
    /**
     * Create a mock PlayerJoinEvent.
     * Use with MockK to configure specific behaviors.
     */
    fun createPlayerJoinEvent(player: Player): PlayerJoinEvent =
        mockk<PlayerJoinEvent>(relaxed = true) {
            every { getPlayer() } returns player
        }

    /**
     * Create a mock PlayerQuitEvent
     */
    fun createPlayerQuitEvent(player: Player): PlayerQuitEvent =
        mockk<PlayerQuitEvent>(relaxed = true) {
            every { getPlayer() } returns player
        }

    /**
     * Create a mock PlayerDeathEvent
     */
    fun createPlayerDeathEvent(player: Player): PlayerDeathEvent =
        mockk<PlayerDeathEvent>(relaxed = true) {
            every { entity } returns player
            every { drops } returns mutableListOf()
        }

    /**
     * Create a mock AsyncPlayerPreLoginEvent
     */
    fun createAsyncPlayerPreLoginEvent(
        playerName: String = "TestPlayer",
        playerUuid: UUID = UUID.randomUUID(),
    ): AsyncPlayerPreLoginEvent =
        mockk<AsyncPlayerPreLoginEvent>(relaxed = true) {
            every { name } returns playerName
            every { uniqueId } returns playerUuid
        }

    /**
     * Create a mock PlayerLoginEvent
     */
    fun createPlayerLoginEvent(player: Player): PlayerLoginEvent =
        mockk<PlayerLoginEvent>(relaxed = true) {
            every { getPlayer() } returns player
        }
}
