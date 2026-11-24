package dev.coughlin.deathban.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorUtilTest {
    private val plain = PlainTextComponentSerializer.plainText()
    private val legacy = LegacyComponentSerializer.legacySection()

    @Test
    fun `legacy ampersand codes keep the same visible text and colors`() {
        val component = ColorUtil.component("&cHello &7World")
        assertEquals("Hello World", plain.serialize(component))
        assertEquals("§cHello §7World", legacy.serialize(component))
    }

    @Test
    fun `section codes use the legacy serializer`() {
        assertEquals("Hello", ColorUtil.plain("§aHello"))
        assertEquals("§aHello", legacy.serialize(ColorUtil.component("§aHello")))
    }

    @Test
    fun `plain text is unchanged`() {
        assertEquals("Hello World", ColorUtil.plain("Hello World"))
        assertEquals(Component.text("Hello World"), ColorUtil.component("Hello World"))
    }

    @Test
    fun `minimessage formats values that have no legacy codes`() {
        val component = ColorUtil.component("<red><bold>Hello")
        assertEquals("Hello", plain.serialize(component))
        assertEquals(NamedTextColor.RED, component.color())
        assertTrue(component.hasDecoration(TextDecoration.BOLD))
    }

    @Test
    fun `legacy codes take priority over minimessage tags`() {
        assertEquals("<red>Hi There", ColorUtil.plain("<red>Hi &cThere"))
    }

    @Test
    fun `invalid minimessage stays literal`() {
        assertEquals("score < 3", ColorUtil.plain("score < 3"))
    }

    @Test
    fun `an ampersand that is not a color code stays literal`() {
        assertEquals("Tom & Jerry", ColorUtil.plain("Tom & Jerry"))
    }

    @Test
    fun `repeated hex codes become colored text without leftover codes`() {
        assertEquals("Hi", ColorUtil.plain("&x&f&f&5&5&5&5Hi"))
    }

    @Test
    fun `components maps each string and an empty list`() {
        val result = ColorUtil.components(listOf("&cRed", "&7Gray"))
        assertEquals(listOf("Red", "Gray"), result.map(plain::serialize))
        assertEquals(emptyList(), ColorUtil.components(emptyList()))
    }

    @Test
    fun `titles keep the 10 70 20 tick timings`() {
        val title = ColorUtil.title("&cHi", "&7There")
        val times = title.times()
        assertEquals(Duration.ofMillis(500), times?.fadeIn())
        assertEquals(Duration.ofMillis(3_500), times?.stay())
        assertEquals(Duration.ofMillis(1_000), times?.fadeOut())
        assertEquals("Hi", plain.serialize(title.title()))
        assertEquals("There", plain.serialize(title.subtitle()))
    }
}
