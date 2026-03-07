package dev.coughlin.deathban.theme

import dev.coughlin.deathban.util.ColorUtil
import org.bukkit.Sound

/**
 * Default built-in theme for DeathBan Pro.
 * Uses standard Minecraft formatting with red/gray color scheme.
 */
class DefaultTheme : Theme {
    override val id = "default"
    override val name = "Default"
    override val author = "DeathBan Pro"
    override val version = "1.0.0"

    override fun getBanTitle(): String = ColorUtil.colorize("&c&lYOU DIED")

    override fun getBanSubtitle(duration: String): String = ColorUtil.colorize("&7Banned for &e$duration")

    override fun getKickMessage(context: BanContext): String =
        ColorUtil.colorize(
            buildString {
                appendLine("&c&lDeath Ban")
                appendLine()
                if (context.isSharedMode) {
                    appendLine("&7The shared lives pool is empty!")
                } else {
                    appendLine("&7You have been temporarily banned for dying.")
                    appendLine("&7Offense Level: &c${context.offenseLevel}")
                }
                appendLine("&7Duration: &e${context.duration}")
                appendLine()
                appendLine("&7Return: &a${context.returnTime}")
            },
        )

    override fun getReturnTitle(): String = ColorUtil.colorize("&a&lWELCOME BACK")

    override fun getReturnSubtitle(): String = ColorUtil.colorize("&7Your ban has expired")

    override fun getReturnMessage(): String = ColorUtil.colorize("&aYou have returned from your death ban. Be careful!")

    override fun getWarningFinalLife(): String = ColorUtil.colorize("&c&lWARNING: &7This is your final life! Next death = ban.")

    override fun getWarningNearLimit(remaining: Int): String =
        ColorUtil.colorize("&e&lCAUTION: &7$remaining lives remaining in this period.")

    override fun getDeathSound(): Sound = Sound.ENTITY_WITHER_DEATH

    override fun getReturnSound(): Sound = Sound.ENTITY_PLAYER_LEVELUP
}
