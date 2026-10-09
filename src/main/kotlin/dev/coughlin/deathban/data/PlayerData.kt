package dev.coughlin.deathban.data

import org.bukkit.Location
import java.time.Instant
import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    var offenseLevel: Int = 0,
    var lastDeathTime: Instant? = null,
    val deaths: MutableList<DeathRecord> = mutableListOf(),
    var currentBan: BanRecord? = null,
    var pendingPardon: Boolean = false,
    val history: MutableList<HistoryRecord> = mutableListOf(),
) {
    fun isBanned(): Boolean {
        val ban = currentBan ?: return false
        return Instant.now().isBefore(ban.endTime)
    }

    fun clearExpiredBan() {
        val ban = currentBan ?: return
        if (!Instant.now().isBefore(ban.endTime)) {
            recordHistory("EXPIRED", ban, timestamp = ban.endTime)
            currentBan = null
        }
    }

    fun recordHistory(
        action: String,
        ban: BanRecord? = currentBan,
        actor: String? = null,
        timestamp: Instant = Instant.now(),
    ) {
        history.add(HistoryRecord(timestamp, action, ban, actor))
        while (history.size > HISTORY_LIMIT) history.removeAt(0)
    }

    companion object {
        const val HISTORY_LIMIT = 100
    }

    /**
     * Returns a deep copy safe to read from another thread.
     * BanRecord and DeathRecord are immutable data classes, so shallow copies suffice for them.
     */
    fun snapshot(): PlayerData =
        PlayerData(
            uuid = uuid,
            offenseLevel = offenseLevel,
            lastDeathTime = lastDeathTime,
            deaths = deaths.toMutableList(),
            currentBan = currentBan,
            pendingPardon = pendingPardon,
            history = history.toMutableList(),
        )
}

data class DeathRecord(
    val timestamp: Instant,
    val world: String,
    val cause: String,
    val killer: UUID? = null,
    val location: LocationData,
)

data class LocationData(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    companion object {
        fun from(location: Location) =
            LocationData(
                x = location.x,
                y = location.y,
                z = location.z,
            )
    }
}

data class BanRecord(
    val startTime: Instant,
    val endTime: Instant,
    val offenseLevel: Int,
    val deathCause: String,
    val poolId: String? = null,
)
