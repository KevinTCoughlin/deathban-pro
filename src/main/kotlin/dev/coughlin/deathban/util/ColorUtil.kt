package dev.coughlin.deathban.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import java.time.Duration

/**
 * Parses player-facing config and theme strings into Adventure components.
 *
 * MiniMessage is the primary format. Values that contain legacy `&` or `§`
 * color codes are read with [LegacyComponentSerializer] instead, so existing
 * configs keep the same colors. Plain text is used when MiniMessage cannot
 * parse a value that has no legacy codes.
 */
object ColorUtil {
    private const val MILLIS_PER_TICK = 50L
    private const val LEGACY_COLOR_CODES = "0123456789abcdefklmnorx"

    private val miniMessage = MiniMessage.miniMessage()
    private val legacySection =
        LegacyComponentSerializer
            .builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()
    private val plainText = PlainTextComponentSerializer.plainText()

    /** Same 10 / 70 / 20 tick fade-in, stay, and fade-out the old sendTitle calls used. */
    private val titleTimes =
        Title.Times.times(
            Duration.ofMillis(10 * MILLIS_PER_TICK),
            Duration.ofMillis(70 * MILLIS_PER_TICK),
            Duration.ofMillis(20 * MILLIS_PER_TICK),
        )

    fun component(message: String): Component {
        if (containsLegacyCodes(message)) {
            return legacySection.deserialize(translateAmpersand(message))
        }
        return try {
            miniMessage.deserialize(message)
        } catch (_: ParsingException) {
            legacySection.deserialize(message)
        }
    }

    fun components(messages: List<String>): List<Component> = messages.map(::component)

    fun plain(message: String): String = plainText.serialize(component(message))

    fun title(
        title: String,
        subtitle: String,
    ): Title = title(component(title), component(subtitle))

    fun title(
        title: Component,
        subtitle: Component,
    ): Title = Title.title(title, subtitle, titleTimes)

    private fun containsLegacyCodes(message: String): Boolean {
        for (index in 0 until message.length - 1) {
            val marker = message[index]
            if ((marker == '&' || marker == '§') && isLegacyColorCode(message[index + 1])) {
                return true
            }
        }
        return false
    }

    /**
     * Converts `&` plus a recognized color code to `§`, and lowercases that code.
     * Other ampersands stay literal, matching the previous alternate-code translation.
     */
    private fun translateAmpersand(message: String): String {
        val chars = message.toCharArray()
        var index = 0
        while (index < chars.size - 1) {
            if (chars[index] == '&' && isLegacyColorCode(chars[index + 1])) {
                chars[index] = '§'
                chars[index + 1] = chars[index + 1].lowercaseChar()
            }
            index++
        }
        return String(chars)
    }

    private fun isLegacyColorCode(code: Char): Boolean = LEGACY_COLOR_CODES.indexOf(code.lowercaseChar()) >= 0
}
