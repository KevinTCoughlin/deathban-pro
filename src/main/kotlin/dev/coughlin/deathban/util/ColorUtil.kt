package dev.coughlin.deathban.util

import org.bukkit.ChatColor

object ColorUtil {
    fun colorize(message: String): String = ChatColor.translateAlternateColorCodes('&', message)

    fun colorize(messages: List<String>): List<String> = messages.map { colorize(it) }

    fun stripColor(message: String): String = ChatColor.stripColor(colorize(message)) ?: message
}
