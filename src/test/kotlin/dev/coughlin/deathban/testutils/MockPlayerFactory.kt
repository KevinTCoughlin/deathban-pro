package dev.coughlin.deathban.testutils

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Factory for creating and managing mock player instances for testing.
 * Provides various player configurations and tracks created players for lifecycle management.
 */
object MockPlayerFactory {
    private val createdPlayers = mutableListOf<Player>()

    /**
     * Create a standard player mock
     */
    fun createPlayer(
        name: String = "TestPlayer",
        uuid: UUID = UUID.randomUUID(),
    ): Player {
        val player = MockSpigot.createMockPlayer(name = name, uuid = uuid)
        createdPlayers.add(player)
        return player
    }

    /**
     * Create an operator (admin) player mock
     */
    fun createOpPlayer(
        name: String = "AdminPlayer",
        uuid: UUID = UUID.randomUUID(),
    ): Player {
        val player =
            MockSpigot.createMockPlayer(
                name = name,
                uuid = uuid,
                hasPermission = { true },
            )
        createdPlayers.add(player)
        return player
    }

    /**
     * Create a dead player mock
     */
    fun createDeadPlayer(
        name: String = "DeadPlayer",
        uuid: UUID = UUID.randomUUID(),
    ): Player {
        val player =
            MockSpigot.createMockPlayer(name = name, uuid = uuid, isOnline = false)
        createdPlayers.add(player)
        return player
    }

    /**
     * Get all created players
     */
    fun getAllPlayers(): List<Player> = createdPlayers.toList()

    /**
     * Clear all created players
     */
    fun clearAll() {
        createdPlayers.clear()
    }
}
