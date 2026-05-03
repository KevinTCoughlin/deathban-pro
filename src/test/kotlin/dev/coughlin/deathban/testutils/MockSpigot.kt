package dev.coughlin.deathban.testutils

import dev.coughlin.deathban.DeathBanPlugin
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Mock Spigot utilities for creating test doubles of Spigot objects.
 * Provides factory methods for commonly mocked Spigot/Bukkit classes.
 */
object MockSpigot {
    /**
     * Create a mock Plugin instance
     */
    fun createMockPlugin(
        name: String = "TestPlugin",
        version: String = "1.0.0",
    ): Plugin =
        mockk<Plugin>(relaxed = true) {
            every { this@mockk.name } returns name
            every { description.version } returns version
            every { isEnabled } returns true
            every { logger } returns mockk(relaxed = true)
        }

    /**
     * Create a mock DeathBanPlugin instance
     */
    fun createMockDeathBanPlugin(
        name: String = "DeathBan",
        version: String = "1.0.0",
    ): DeathBanPlugin =
        mockk<DeathBanPlugin>(relaxed = true) {
            every { this@mockk.name } returns name
            every { description.version } returns version
            every { isEnabled } returns true
            every { logger } returns mockk(relaxed = true)
        }

    /**
     * Create a mock Player instance with customizable properties
     */
    fun createMockPlayer(
        name: String = "TestPlayer",
        uuid: UUID = UUID.randomUUID(),
        isOnline: Boolean = true,
        hasPermission: (String) -> Boolean = { false },
    ): Player =
        mockk<Player>(relaxed = true) {
            every { this@mockk.name } returns name
            every { this@mockk.uniqueId } returns uuid
            every { isOnline } returns isOnline
            every { hasPermission(any<String>()) } answers { call ->
                hasPermission(call.invocation.args[0] as String)
            }
            every { world } returns mockk(relaxed = true) { every { name } returns "world" }
            every { location } returns
                mockk(relaxed = true) {
                    every { x } returns 0.0
                    every { y } returns 64.0
                    every { z } returns 0.0
                }
        }
}
