package dev.coughlin.deathban.theme

import dev.coughlin.deathban.util.ColorUtil
import org.bukkit.Particle
import org.bukkit.Sound

/**
 * Halloween-themed death ban experience.
 * Spooky messages, eerie sounds, and ghostly particles.
 */
class HalloweenTheme : Theme {
    override val id = "halloween"
    override val name = "Halloween"
    override val author = "DeathBan Pro"
    override val version = "1.0.0"

    override fun getBanTitle(): String =
        ColorUtil.colorize("&6&l\u2620 &c&lDEATH CLAIMS YOU &6&l\u2620")

    override fun getBanSubtitle(duration: String): String =
        ColorUtil.colorize("&8Your soul is trapped for &6$duration")

    override fun getKickMessage(context: BanContext): String = ColorUtil.colorize(buildString {
        appendLine("&8&m                                        ")
        appendLine("&6&l       \u2620 DEATH BAN \u2620")
        appendLine("&8&m                                        ")
        appendLine()
        appendLine("&7The spirits have claimed your soul...")
        appendLine()
        if (context.isSharedMode) {
            appendLine("&8\u00BB &cThe shared life force is depleted!")
        } else {
            appendLine("&8\u00BB &7Death Count: &c${context.offenseLevel}")
        }
        appendLine("&8\u00BB &7Sentence: &6${context.duration}")
        appendLine()
        appendLine("&7You may return at: &a${context.returnTime}")
        appendLine()
        appendLine("&8&oThe dead shall rise again...")
        appendLine("&8&m                                        ")
    })

    override fun getReturnTitle(): String =
        ColorUtil.colorize("&a&l\u2605 RESURRECTION \u2605")

    override fun getReturnSubtitle(): String =
        ColorUtil.colorize("&7You have risen from the grave")

    override fun getReturnMessage(): String =
        ColorUtil.colorize("&6\u2620 &7The spirits release you... &afor now. &7Be wary, mortal!")

    override fun getWarningFinalLife(): String =
        ColorUtil.colorize("&6\u2620 &c&lDEATH LOOMS! &7One more fall and your soul is forfeit!")

    override fun getWarningNearLimit(remaining: Int): String =
        ColorUtil.colorize("&6\u2620 &e&lBEWARE! &7Only &c$remaining &7lives remain before eternal rest...")

    override fun getDeathSound(): Sound = Sound.ENTITY_WITHER_SPAWN
    override fun getReturnSound(): Sound = Sound.ENTITY_ZOMBIE_VILLAGER_CURE

    override fun getDeathParticle(): Particle = Particle.SOUL
    override fun getReturnParticle(): Particle = Particle.TOTEM_OF_UNDYING
}
