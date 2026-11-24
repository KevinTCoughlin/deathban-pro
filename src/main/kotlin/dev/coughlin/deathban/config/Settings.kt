package dev.coughlin.deathban.config

import dev.coughlin.deathban.util.TimeUtil
import org.bukkit.configuration.file.FileConfiguration
import java.time.Duration

enum class BanMode {
    INDIVIDUAL,
    SHARED,
}

class Settings(
    config: FileConfiguration,
) : OffenseConfig {
    private val errors = mutableListOf<String>()

    val debug: Boolean = config.getBoolean("debug", false)
    val updateCheck: Boolean = config.getBoolean("update-check", true)
    val metrics: Boolean = config.getBoolean("metrics", true)

    // Mode configuration
    val mode: BanMode =
        when (config.getString("mode")?.lowercase()) {
            null, "individual" -> BanMode.INDIVIDUAL
            "shared" -> BanMode.SHARED
            else -> {
                reject("mode must be 'individual' or 'shared'")
                BanMode.INDIVIDUAL
            }
        }

    // Shared lives configuration
    val sharedLivesMax: Int = positive("shared-lives.max-lives", config.getInt("shared-lives.max-lives", 20))
    val sharedLivesDefault: Int =
        config.getInt("shared-lives.default-lives", 10).also { value ->
            if (value !in 0..sharedLivesMax) {
                reject("shared-lives.default-lives must be between 0 and $sharedLivesMax")
            }
        }
    val sharedLivesAllowTeams: Boolean = config.getBoolean("shared-lives.allow-teams", true)
    val sharedLivesEmptyPoolBan: Duration = duration("shared-lives.empty-pool-ban", config.getString("shared-lives.empty-pool-ban") ?: "1h")

    override val rollingWindowEnabled: Boolean = config.getBoolean("rolling-window.enabled", true)
    override val rollingWindowDuration: Duration = duration("rolling-window.duration", config.getString("rolling-window.duration") ?: "24h")
    override val maxDeathsInWindow: Int = positive("rolling-window.max-deaths", config.getInt("rolling-window.max-deaths", 3))

    val banDurations: Map<Int, Duration> = loadBanDurations(config)

    override val offenseResetEnabled: Boolean = config.getBoolean("offense-reset.enabled", true)
    override val offenseResetPeriod: Duration =
        duration(
            "offense-reset.clean-period",
            config.getString("offense-reset.clean-period") ?: "168h",
        )

    val enabledWorlds: Set<String> = config.getStringList("enabled-worlds").toSet()
    val disabledWorlds: Set<String> = config.getStringList("disabled-worlds").toSet()

    val bypassPermission: String = config.getString("bypass-permission") ?: "deathban.bypass"

    // Theme configuration
    val themeId: String = config.getString("theme") ?: "default"
    val themeSoundsEnabled: Boolean = config.getBoolean("theme-sounds", true)
    val themeParticlesEnabled: Boolean = config.getBoolean("theme-particles", true)

    init {
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString(prefix = "Invalid configuration:\n", separator = "\n"))
        }
    }

    private fun loadBanDurations(config: FileConfiguration): Map<Int, Duration> {
        val section = config.getConfigurationSection("ban-durations") ?: return defaultBanDurations()
        val keys = section.getKeys(false)
        if (keys.isEmpty()) {
            reject("ban-durations must contain at least one offense level")
            return defaultBanDurations()
        }
        val durations = linkedMapOf<Int, Duration>()
        for (key in keys) {
            val level = key.toIntOrNull()
            if (level == null || level <= 0) {
                reject("ban-durations keys must be positive offense levels: '$key'")
                continue
            }
            val raw = section.getString(key) ?: "1h"
            try {
                durations[level] = TimeUtil.parseDuration(raw)
            } catch (e: IllegalArgumentException) {
                reject("ban-durations.$key: ${e.message}")
            }
        }
        return durations.ifEmpty { defaultBanDurations() }
    }

    private fun defaultBanDurations() =
        mapOf(
            1 to Duration.ofHours(1),
            2 to Duration.ofHours(6),
            3 to Duration.ofHours(24),
            4 to Duration.ofHours(72),
            5 to Duration.ofHours(168),
        )

    private fun positive(
        path: String,
        value: Int,
    ): Int {
        if (value <= 0) reject("$path must be greater than 0")
        return value
    }

    private fun duration(
        path: String,
        raw: String,
    ): Duration =
        try {
            TimeUtil.parseDuration(raw)
        } catch (e: IllegalArgumentException) {
            reject("$path: ${e.message}")
            Duration.ofHours(1)
        }

    private fun reject(message: String) {
        errors += message
    }

    fun getBanDuration(offenseLevel: Int): Duration =
        banDurations[offenseLevel]
            ?: banDurations[banDurations.keys.maxOrNull() ?: 1]
            ?: Duration.ofHours(1)

    fun isWorldEnabled(worldName: String): Boolean {
        if (disabledWorlds.contains(worldName)) return false
        if (enabledWorlds.isEmpty()) return true
        return enabledWorlds.contains(worldName)
    }
}

/**
 * Reloads [config] and parses it.
 * A configuration that fails validation leaves the previous in-memory values in place.
 */
internal fun reloadSettings(
    config: FileConfiguration,
    reload: () -> Unit,
): Settings {
    val snapshot = config.saveToString()
    reload()
    return try {
        Settings(config)
    } catch (e: IllegalArgumentException) {
        config.loadFromString(snapshot)
        throw e
    }
}
