package dev.coughlin.deathban.config

import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.util.ColorUtil
import dev.coughlin.deathban.util.TimeUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Duration

class Messages(
    private val file: File,
) {
    private var config: YamlConfiguration = load()

    private fun load(): YamlConfiguration =
        YamlConfiguration.loadConfiguration(file).also { loaded ->
            Messages::class.java.getResourceAsStream("/messages.yml")?.reader()?.use { reader ->
                loaded.setDefaults(YamlConfiguration.loadConfiguration(reader))
            }
        }

    val prefix: Component
        get() = ColorUtil.component(config.getString("prefix") ?: "&8[&cDeathBan&8]&r ")

    fun reload() {
        config = load()
    }

    fun get(path: String): Component = render(config.getString(path) ?: "Missing message: $path")

    fun get(
        path: String,
        vararg replacements: Pair<String, Any>,
    ): Component = render(config.getString(path) ?: "Missing message: $path", replacements)

    fun prefixed(
        path: String,
        vararg replacements: Pair<String, Any>,
    ): Component = prefix.append(get(path, *replacements))

    fun getBanTitle(): Component = get("ban.title")

    fun getBanSubtitle(duration: Duration): Component = get("ban.subtitle", "duration" to TimeUtil.formatDuration(duration))

    fun getKickMessage(ban: BanRecord): Component {
        val remaining = TimeUtil.durationUntil(ban.endTime)
        return get(
            "ban.kick-message",
            "duration" to TimeUtil.formatDuration(remaining),
            "offense_level" to ban.offenseLevel,
            "return_time" to TimeUtil.formatInstant(ban.endTime),
        )
    }

    fun getReturnTitle(): Component = get("return.title")

    fun getReturnSubtitle(): Component = get("return.subtitle")

    fun getReturnMessage(): Component = prefixed("return.message")

    fun getWarningFinalLife(): Component = prefixed("warnings.final-life")

    fun getWarningNearLimit(remaining: Int): Component = prefixed("warnings.near-limit", "remaining" to remaining)

    fun getHelp(): Component = get("commands.help")

    fun getCheckSelf(
        offenseLevel: Int,
        deaths: Int,
        max: Int,
    ): Component =
        prefixed(
            "commands.check-self",
            "offense_level" to offenseLevel,
            "deaths" to deaths,
            "max" to max,
        )

    fun getCheckOther(
        player: String,
        offenseLevel: Int,
        deaths: Int,
        max: Int,
    ): Component =
        prefixed(
            "commands.check-other",
            "player" to player,
            "offense_level" to offenseLevel,
            "deaths" to deaths,
            "max" to max,
        )

    fun getCheckBanned(
        player: String,
        ban: BanRecord,
    ): Component =
        prefixed(
            "commands.check-banned",
            "player" to player,
            "return_time" to TimeUtil.formatInstant(ban.endTime),
        )

    fun getResetSuccess(player: String): Component = prefixed("commands.reset-success", "player" to player)

    fun getPardonSuccess(player: String): Component = prefixed("commands.pardon-success", "player" to player)

    fun getPardonNotBanned(player: String): Component = prefixed("commands.pardon-not-banned", "player" to player)

    fun getPardonNotification(): Component = prefixed("commands.pardon-notification")

    fun getReloadSuccess(): Component = prefixed("commands.reload-success")

    fun getNoPermission(): Component = prefixed("errors.no-permission")

    fun getPlayerNotFound(player: String): Component = prefixed("errors.player-not-found", "player" to player)

    fun getInvalidUsage(usage: String): Component = prefixed("errors.invalid-usage", "usage" to usage)

    // Shared Lives messages
    fun getPoolStatus(
        pool: String,
        lives: Int,
        max: Int,
    ): Component =
        prefixed(
            "shared-lives.pool-status",
            "pool" to pool,
            "lives" to lives,
            "max" to max,
        )

    fun getLifeAdded(
        lives: Int,
        max: Int,
    ): Component = prefixed("shared-lives.life-added", "lives" to lives, "max" to max)

    fun getLifeConsumed(
        lives: Int,
        max: Int,
    ): Component = prefixed("shared-lives.life-consumed", "lives" to lives, "max" to max)

    fun getPoolEmpty(): Component = prefixed("shared-lives.pool-empty")

    fun getPoolFull(max: Int): Component = prefixed("shared-lives.pool-full", "max" to max)

    fun getNoPool(): Component = prefixed("shared-lives.no-pool")

    fun getTeamCreated(team: String): Component = prefixed("shared-lives.team-created", "team" to team)

    fun getTeamJoined(team: String): Component = prefixed("shared-lives.team-joined", "team" to team)

    fun getTeamLeft(): Component = prefixed("shared-lives.team-left")

    fun getTeamExists(): Component = prefixed("shared-lives.team-exists")

    fun getTeamNotFound(team: String): Component = prefixed("shared-lives.team-not-found", "team" to team)

    fun getTeamsDisabled(): Component = prefixed("shared-lives.teams-disabled")

    fun getSharedLivesKickMessage(
        duration: Duration,
        returnTime: String,
    ): Component =
        get(
            "shared-lives.kick-message",
            "duration" to TimeUtil.formatDuration(duration),
            "return_time" to returnTime,
        )

    /**
     * Formats the template before placeholder replacement, matching the previous
     * colorize-then-replace order. Replacement values are plain text unless the
     * caller already built a [Component].
     */
    private fun render(
        raw: String,
        replacements: Array<out Pair<String, Any>> = emptyArray(),
    ): Component {
        var rendered = ColorUtil.component(raw)
        for ((key, value) in replacements) {
            val replacement =
                when (value) {
                    is Component -> value
                    else -> Component.text(value.toString())
                }
            rendered =
                rendered.replaceText(
                    TextReplacementConfig
                        .builder()
                        .matchLiteral("{$key}")
                        .replacement(replacement)
                        .build(),
                )
        }
        return rendered
    }
}
