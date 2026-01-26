package dev.coughlin.deathban.manager

import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.PlayerData
import java.time.Instant

class OffenseManager(private val settings: Settings) {

    fun getDeathsInWindow(data: PlayerData): Int {
        if (!settings.rollingWindowEnabled) {
            return data.deaths.size
        }

        val windowStart = Instant.now().minus(settings.rollingWindowDuration)
        return data.deaths.count { it.timestamp.isAfter(windowStart) }
    }

    fun shouldTriggerBan(data: PlayerData): Boolean {
        val deathsInWindow = getDeathsInWindow(data)
        return deathsInWindow >= settings.maxDeathsInWindow
    }

    fun checkOffenseReset(data: PlayerData): Boolean {
        if (!settings.offenseResetEnabled) return false
        if (data.offenseLevel == 0) return false

        val lastDeath = data.lastDeathTime ?: return false
        val resetTime = lastDeath.plus(settings.offenseResetPeriod)

        if (Instant.now().isAfter(resetTime)) {
            data.offenseLevel = 0
            data.deaths.clear()
            return true
        }
        return false
    }

    fun pruneOldDeaths(data: PlayerData) {
        if (!settings.rollingWindowEnabled) return

        val windowStart = Instant.now().minus(settings.rollingWindowDuration)
        data.deaths.removeIf { it.timestamp.isBefore(windowStart) }
    }

    fun getRemainingLives(data: PlayerData): Int {
        val deathsInWindow = getDeathsInWindow(data)
        return (settings.maxDeathsInWindow - deathsInWindow).coerceAtLeast(0)
    }
}
