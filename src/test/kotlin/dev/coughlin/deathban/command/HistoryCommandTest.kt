package dev.coughlin.deathban.command

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Test

class HistoryCommandTest {
    @Test
    fun `unauthorized history and invalid pages never access player storage`() {
        val sender = mockk<CommandSender>(relaxed = true)
        val data = mockk<PlayerDataManager>()
        val messages = mockk<Messages>(relaxed = true)
        val command = DeathBanCommand(mockk<DeathBanPlugin>(), messages, data, mockk(), mockk())
        every { sender.hasPermission("deathban.admin") } returns false
        command.onCommand(sender, mockk(), "deathban", arrayOf("history", "player"))
        verify { messages.getNoPermission() }
        every { sender.hasPermission("deathban.admin") } returns true
        for (page in listOf("0", "-1", "2147483648", "abc")) {
            command.onCommand(sender, mockk(), "deathban", arrayOf("history", "player", page))
        }
        verify(exactly = 0) { data.get(any()) }
    }
}
