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
) {
    fun isBanned(): Boolean {
        val ban = currentBan ?: return false
        return Instant.now().isBefore(ban.endTime)
    }

    fun clearExpiredBan() {
        val ban = currentBan ?: return
        if (!Instant.now().isBefore(ban.endTime)) {
            currentBan = null
        }
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
)
