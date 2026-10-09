package dev.coughlin.deathban.command

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.manager.SharedLivesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.command.Command
import org.bukkit.entity.Player
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class SharedLifePermissionsTest {
    private val plugin = mockk<DeathBanPlugin>(relaxed = true)
    private val manager = mockk<SharedLivesManager>(relaxed = true)
    private val player = mockk<Player>(relaxed = true)
    private val command = mockk<Command>()
    private val playerId = UUID.randomUUID()
    private val messages = mockk<Messages>(relaxed = true)

    private fun execute(
        args: Array<String>,
        permissions: Set<String>,
    ) {
        every { player.uniqueId } returns playerId
        every { player.hasPermission(any<String>()) } answers { firstArg<String>() in permissions }
        every { plugin.sharedLivesManager } returns manager
        every { plugin.settings.sharedLivesAllowTeams } returns true
        DeathBanCommand(plugin, messages, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            .onCommand(player, command, "deathban", args)
    }

    @Test
    fun `ordinary player cannot refill lives`() {
        execute(arrayOf("lives", "add"), setOf("deathban.use"))
        verify(exactly = 0) { manager.addLife(any(), any()) }
    }

    @Test
    fun `trusted player can refill lives`() {
        execute(arrayOf("lives", "add"), setOf("deathban.use", "deathban.lives.add"))
        verify(exactly = 1) { manager.addLife(playerId, null) }
    }

    @Test
    fun `ordinary player cannot create or switch teams`() {
        for (args in listOf(arrayOf("team", "create", "fresh"), arrayOf("team", "join", "fresh"), arrayOf("team", "leave"))) {
            execute(args, setOf("deathban.use"))
        }
        verify(exactly = 0) { manager.createTeamPool(any(), any()) }
        verify(exactly = 0) { manager.joinPool(any(), any()) }
        verify(exactly = 0) { manager.leavePool(any()) }
    }

    @Test
    fun `trusted player can create team`() {
        execute(arrayOf("team", "create", "fresh"), setOf("deathban.use", "deathban.team.manage"))
        verify(exactly = 1) { manager.createTeamPool("fresh", playerId) }
    }

    @Test
    fun `ordinary player can still read shared life status`() {
        execute(arrayOf("lives"), setOf("deathban.use"))
        verify(exactly = 1) { manager.getPoolForPlayer(playerId) }
        verify(exactly = 0) { manager.addLife(any(), any()) }
    }

    @Test
    fun `completion hides privileged shared life actions`() {
        every { player.hasPermission(any<String>()) } answers { firstArg<String>() == "deathban.use" }
        val completer = DeathBanTabCompleter(plugin, mockk(), mockk())
        assertEquals(emptyList(), completer.onTabComplete(player, command, "deathban", arrayOf("lives", "")))
        assertEquals(emptyList(), completer.onTabComplete(player, command, "deathban", arrayOf("team", "")))
    }
}
