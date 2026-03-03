package dev.coughlin.deathban.util

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.ChatColor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorUtilTest {
    @BeforeEach
    fun setup() {
        mockkStatic(ChatColor::class)
        every { ChatColor.translateAlternateColorCodes('&', any()) } answers {
            secondArg<String>().replace("&c", "§c").replace("&7", "§7").replace("&r", "§r")
        }
        every { ChatColor.stripColor(any()) } answers {
            firstArg<String>().replace(Regex("§[0-9a-fk-or]"), "")
        }
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(ChatColor::class)
    }

    @Test
    fun `colorize translates color codes`() {
        val result = ColorUtil.colorize("&cHello &7World")
        assertEquals("§cHello §7World", result)
    }

    @Test
    fun `colorize handles string without color codes`() {
        val result = ColorUtil.colorize("Hello World")
        assertEquals("Hello World", result)
    }

    @Test
    fun `colorize handles list of strings`() {
        val result = ColorUtil.colorize(listOf("&cRed", "&7Gray"))
        assertEquals(listOf("§cRed", "§7Gray"), result)
    }

    @Test
    fun `colorize handles empty list`() {
        val result = ColorUtil.colorize(emptyList())
        assertEquals(emptyList(), result)
    }

    @Test
    fun `stripColor removes color codes`() {
        val result = ColorUtil.stripColor("&cHello &7World")
        assertEquals("Hello World", result)
    }
}
