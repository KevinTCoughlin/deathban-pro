package dev.coughlin.deathban.config

import dev.coughlin.deathban.util.TimeUtil
import org.bukkit.configuration.file.FileConfiguration
import java.time.Duration

class Settings(config: FileConfiguration) : OffenseConfig {

    val debug: Boolean = config.getBoolean("debug", false)
    val updateCheck: Boolean = config.getBoolean("update-check", true)
    val metrics: Boolean = config.getBoolean("metrics", true)

    override val rollingWindowEnabled: Boolean = config.getBoolean("rolling-window.enabled", true)
    override val rollingWindowDuration: Duration = TimeUtil.parseDuration(
        config.getString("rolling-window.duration") ?: "24h"
    )
    override val maxDeathsInWindow: Int = config.getInt("rolling-window.max-deaths", 3)

    val banDurations: Map<Int, Duration> = loadBanDurations(config)

    override val offenseResetEnabled: Boolean = config.getBoolean("offense-reset.enabled", true)
    override val offenseResetPeriod: Duration = TimeUtil.parseDuration(
        config.getString("offense-reset.clean-period") ?: "168h"
    )

    val enabledWorlds: Set<String> = config.getStringList("enabled-worlds").toSet()
    val disabledWorlds: Set<String> = config.getStringList("disabled-worlds").toSet()

    val bypassPermission: String = config.getString("bypass-permission") ?: "deathban.bypass"

    private fun loadBanDurations(config: FileConfiguration): Map<Int, Duration> {
        val section = config.getConfigurationSection("ban-durations") ?: return defaultBanDurations()
        return section.getKeys(false).associate { key ->
            val level = key.toIntOrNull() ?: 1
            val duration = TimeUtil.parseDuration(section.getString(key) ?: "1h")
            level to duration
        }
    }

    private fun defaultBanDurations() = mapOf(
        1 to Duration.ofHours(1),
        2 to Duration.ofHours(6),
        3 to Duration.ofHours(24),
        4 to Duration.ofHours(72),
        5 to Duration.ofHours(168)
    )

    fun getBanDuration(offenseLevel: Int): Duration {
        return banDurations[offenseLevel]
            ?: banDurations[banDurations.keys.maxOrNull() ?: 1]
            ?: Duration.ofHours(1)
    }

    fun isWorldEnabled(worldName: String): Boolean {
        if (disabledWorlds.contains(worldName)) return false
        if (enabledWorlds.isEmpty()) return true
        return enabledWorlds.contains(worldName)
    }
}
