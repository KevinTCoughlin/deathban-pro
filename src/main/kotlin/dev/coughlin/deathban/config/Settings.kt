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
    val debug: Boolean = config.getBoolean("debug", false)
    val updateCheck: Boolean = config.getBoolean("update-check", true)
    val metrics: Boolean = config.getBoolean("metrics", true)

    // Mode configuration
    val mode: BanMode =
        when (config.getString("mode")?.lowercase()) {
            null, "individual" -> BanMode.INDIVIDUAL
            "shared" -> BanMode.SHARED
            else -> throw IllegalArgumentException("mode must be 'individual' or 'shared'")
        }

    // Shared lives configuration
    val sharedLivesMax: Int =
        config
            .getInt("shared-lives.max-lives", 20)
            .also { require(it > 0) { "shared-lives.max-lives must be greater than 0" } }
    val sharedLivesDefault: Int =
        config
            .getInt("shared-lives.default-lives", 10)
            .also { require(it in 0..sharedLivesMax) { "shared-lives.default-lives must be between 0 and $sharedLivesMax" } }
    val sharedLivesAllowTeams: Boolean = config.getBoolean("shared-lives.allow-teams", true)
    val sharedLivesEmptyPoolBan: Duration =
        TimeUtil.parseDuration(
            config.getString("shared-lives.empty-pool-ban") ?: "1h",
        )

    override val rollingWindowEnabled: Boolean = config.getBoolean("rolling-window.enabled", true)
    override val rollingWindowDuration: Duration =
        TimeUtil.parseDuration(
            config.getString("rolling-window.duration") ?: "24h",
        )
    override val maxDeathsInWindow: Int =
        config
            .getInt("rolling-window.max-deaths", 3)
            .also { require(it > 0) { "rolling-window.max-deaths must be greater than 0" } }

    val banDurations: Map<Int, Duration> = loadBanDurations(config)

    override val offenseResetEnabled: Boolean = config.getBoolean("offense-reset.enabled", true)
    override val offenseResetPeriod: Duration =
        TimeUtil.parseDuration(
            config.getString("offense-reset.clean-period") ?: "168h",
        )

    val enabledWorlds: Set<String> = config.getStringList("enabled-worlds").toSet()
    val disabledWorlds: Set<String> = config.getStringList("disabled-worlds").toSet()

    val bypassPermission: String = config.getString("bypass-permission") ?: "deathban.bypass"

    // Theme configuration
    val themeId: String = config.getString("theme") ?: "default"
    val themeSoundsEnabled: Boolean = config.getBoolean("theme-sounds", true)
    val themeParticlesEnabled: Boolean = config.getBoolean("theme-particles", true)

    private fun loadBanDurations(config: FileConfiguration): Map<Int, Duration> {
        val section = config.getConfigurationSection("ban-durations") ?: return defaultBanDurations()
        val durations =
            section.getKeys(false).associate { key ->
                val level = key.toIntOrNull()
                require(level != null && level > 0) { "ban-durations keys must be positive offense levels: '$key'" }
                val duration = TimeUtil.parseDuration(section.getString(key) ?: "1h")
                level to duration
            }
        require(durations.isNotEmpty()) { "ban-durations must contain at least one offense level" }
        return durations
    }

    private fun defaultBanDurations() =
        mapOf(
            1 to Duration.ofHours(1),
            2 to Duration.ofHours(6),
            3 to Duration.ofHours(24),
            4 to Duration.ofHours(72),
            5 to Duration.ofHours(168),
        )

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
