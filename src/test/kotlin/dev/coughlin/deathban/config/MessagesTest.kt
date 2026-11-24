package dev.coughlin.deathban.config

import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.util.ColorUtil
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessagesTest {
    @TempDir
    lateinit var tempDir: File

    private val plain = PlainTextComponentSerializer.plainText()
    private val legacy = LegacyComponentSerializer.legacySection()

    @Test
    fun `shipped legacy messages keep their wording and colors`() {
        val file = File(tempDir, "messages.yml")
        file.writeText(javaClass.getResource("/messages.yml")!!.readText())
        val messages = Messages(file)
        val ban =
            BanRecord(
                startTime = Instant.parse("2026-11-24T00:00:00Z"),
                endTime = Instant.parse("2026-11-24T01:30:00Z"),
                offenseLevel = 2,
                deathCause = "FALL",
            )

        assertEquals(ColorUtil.component("&c&lYOU DIED"), messages.getBanTitle())
        val kick = plain.serialize(messages.getKickMessage(ban))
        assertTrue(kick.contains("Death Ban"))
        assertTrue(kick.contains("You have been temporarily banned for dying."))
        assertTrue(kick.contains("Offense Level: 2"))
        assertFalse(kick.contains("{duration}"))
        assertFalse(kick.contains("&"))
        assertFalse(kick.contains("§"))
        assertTrue(legacy.serialize(messages.get("return.title")).contains("§a"))
    }

    @Test
    fun `minimessage is used when a value has no legacy codes`() {
        val messages = messages("ban:\n  title: \"<red><bold>YOU DIED\"\n")
        val title = messages.getBanTitle()
        assertEquals("YOU DIED", plain.serialize(title))
        assertEquals(NamedTextColor.RED, title.color())
    }

    @Test
    fun `placeholder values are inserted as plain text`() {
        val messages = messages("commands:\n  check-other: \"&7Name {player}\"\n")
        val rendered = messages.getCheckOther("<red>&cSteve", 1, 2, 3)
        assertTrue(plain.serialize(rendered).contains("<red>&cSteve"))
        assertFalse(legacy.serialize(rendered).contains("§cSteve"))
    }

    @Test
    fun `a component replacement keeps its own color`() {
        val messages = messages("theme:\n  list-item: \"&7{id}{active}\"\n")
        val rendered =
            messages.get(
                "theme.list-item",
                "id" to "default",
                "active" to ColorUtil.component(" &a(active)"),
            )
        assertEquals("default (active)", plain.serialize(rendered))
        assertTrue(legacy.serialize(rendered).contains("§a(active)"))
    }

    private fun messages(yaml: String): Messages {
        val file = File(tempDir, "messages.yml")
        file.writeText(yaml)
        return Messages(file)
    }
}
