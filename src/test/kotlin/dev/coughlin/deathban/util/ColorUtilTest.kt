package dev.coughlin.deathban.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ColorUtilTest {
    @Test
    fun `colorize translates color codes`() {
        val result = ColorUtil.colorize("&cHello &7World")
        assertEquals("\u00A7cHello \u00A77World", result)
    }

    @Test
    fun `colorize handles string without color codes`() {
        val result = ColorUtil.colorize("Hello World")
        assertEquals("Hello World", result)
    }

    @Test
    fun `colorize handles list of strings`() {
        val result = ColorUtil.colorize(listOf("&cRed", "&7Gray"))
        assertEquals(listOf("\u00A7cRed", "\u00A77Gray"), result)
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
