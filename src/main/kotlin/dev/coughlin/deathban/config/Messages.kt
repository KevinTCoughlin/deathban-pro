package dev.coughlin.deathban.config

import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.util.ColorUtil
import dev.coughlin.deathban.util.TimeUtil
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Duration

class Messages(
    private val file: File,
) {
    private var config: YamlConfiguration = YamlConfiguration.loadConfiguration(file)

    val prefix: String get() = ColorUtil.colorize(config.getString("prefix") ?: "&8[&cDeathBan&8]&r ")

    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun get(path: String): String = ColorUtil.colorize(config.getString(path) ?: "Missing message: $path")

    fun get(
        path: String,
        vararg replacements: Pair<String, Any>,
    ): String {
        var message = get(path)
        for ((key, value) in replacements) {
            message = message.replace("{$key}", value.toString())
        }
        return message
    }

    fun prefixed(
        path: String,
        vararg replacements: Pair<String, Any>,
    ): String = prefix + get(path, *replacements)

    fun getBanTitle(): String = get("ban.title")

    fun getBanSubtitle(duration: Duration): String = get("ban.subtitle", "duration" to TimeUtil.formatDuration(duration))

    fun getKickMessage(ban: BanRecord): String {
        val remaining = TimeUtil.durationUntil(ban.endTime)
        return get(
            "ban.kick-message",
            "duration" to TimeUtil.formatDuration(remaining),
            "offense_level" to ban.offenseLevel,
            "return_time" to TimeUtil.formatInstant(ban.endTime),
        )
    }

    fun getReturnTitle(): String = get("return.title")

    fun getReturnSubtitle(): String = get("return.subtitle")

    fun getReturnMessage(): String = prefixed("return.message")

    fun getWarningFinalLife(): String = prefixed("warnings.final-life")

    fun getWarningNearLimit(remaining: Int): String = prefixed("warnings.near-limit", "remaining" to remaining)

    fun getHelp(): String = get("commands.help")

    fun getCheckSelf(
        offenseLevel: Int,
        deaths: Int,
        max: Int,
    ): String =
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
    ): String =
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
    ): String =
        prefixed(
            "commands.check-banned",
            "player" to player,
            "return_time" to TimeUtil.formatInstant(ban.endTime),
        )

    fun getResetSuccess(player: String): String = prefixed("commands.reset-success", "player" to player)

    fun getPardonSuccess(player: String): String = prefixed("commands.pardon-success", "player" to player)

    fun getPardonNotBanned(player: String): String = prefixed("commands.pardon-not-banned", "player" to player)

    fun getPardonNotification(): String = prefixed("commands.pardon-notification")

    fun getReloadSuccess(): String = prefixed("commands.reload-success")

    fun getNoPermission(): String = prefixed("errors.no-permission")

    fun getPlayerNotFound(player: String): String = prefixed("errors.player-not-found", "player" to player)

    fun getInvalidUsage(usage: String): String = prefixed("errors.invalid-usage", "usage" to usage)

    // Shared Lives messages
    fun getPoolStatus(
        pool: String,
        lives: Int,
        max: Int,
    ): String =
        prefixed(
            "shared-lives.pool-status",
            "pool" to pool,
            "lives" to lives,
            "max" to max,
        )

    fun getLifeAdded(
        lives: Int,
        max: Int,
    ): String = prefixed("shared-lives.life-added", "lives" to lives, "max" to max)

    fun getLifeConsumed(
        lives: Int,
        max: Int,
    ): String = prefixed("shared-lives.life-consumed", "lives" to lives, "max" to max)

    fun getPoolEmpty(): String = prefixed("shared-lives.pool-empty")

    fun getPoolFull(max: Int): String = prefixed("shared-lives.pool-full", "max" to max)

    fun getNoPool(): String = prefixed("shared-lives.no-pool")

    fun getTeamCreated(team: String): String = prefixed("shared-lives.team-created", "team" to team)

    fun getTeamJoined(team: String): String = prefixed("shared-lives.team-joined", "team" to team)

    fun getTeamLeft(): String = prefixed("shared-lives.team-left")

    fun getTeamExists(): String = prefixed("shared-lives.team-exists")

    fun getTeamNotFound(team: String): String = prefixed("shared-lives.team-not-found", "team" to team)

    fun getTeamsDisabled(): String = prefixed("shared-lives.teams-disabled")

    fun getSharedLivesKickMessage(
        duration: Duration,
        returnTime: String,
    ): String =
        get(
            "shared-lives.kick-message",
            "duration" to TimeUtil.formatDuration(duration),
            "return_time" to returnTime,
        )
}
