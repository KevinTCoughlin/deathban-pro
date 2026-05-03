package dev.coughlin.deathban.testutils

import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.data.DeathRecord
import dev.coughlin.deathban.data.LocationData
import dev.coughlin.deathban.data.PlayerData
import java.time.Instant
import java.util.UUID

/**
 * Factory for creating test domain objects with sensible defaults.
 * Simplifies test setup and ensures consistency across tests.
 */
object TestDataFactory {
    /**
     * Create a test PlayerData instance
     */
    fun createPlayerData(
        uuid: UUID = UUID.randomUUID(),
        offenseLevel: Int = 0,
        lastDeathTime: Instant? = null,
        deaths: MutableList<DeathRecord> = mutableListOf(),
        currentBan: BanRecord? = null,
        pendingPardon: Boolean = false,
    ): PlayerData =
        PlayerData(
            uuid = uuid,
            offenseLevel = offenseLevel,
            lastDeathTime = lastDeathTime,
            deaths = deaths,
            currentBan = currentBan,
            pendingPardon = pendingPardon,
        )

    /**
     * Create a test BanRecord instance
     */
    fun createBanRecord(
        startTime: Instant = Instant.now().minusSeconds(60),
        endTime: Instant = Instant.now().plusSeconds(3600),
        offenseLevel: Int = 1,
        deathCause: String = "FALL",
    ): BanRecord =
        BanRecord(
            startTime = startTime,
            endTime = endTime,
            offenseLevel = offenseLevel,
            deathCause = deathCause,
        )

    /**
     * Create a test DeathRecord instance
     */
    fun createDeathRecord(
        timestamp: Instant = Instant.now(),
        world: String = "world",
        cause: String = "FALL",
        killer: UUID? = null,
        location: LocationData = LocationData(x = 0.0, y = 64.0, z = 0.0),
    ): DeathRecord =
        DeathRecord(
            timestamp = timestamp,
            world = world,
            cause = cause,
            killer = killer,
            location = location,
        )

    /**
     * Create a test LocationData instance
     */
    fun createLocationData(
        x: Double = 0.0,
        y: Double = 64.0,
        z: Double = 0.0,
    ): LocationData = LocationData(x = x, y = y, z = z)

    /**
     * Create multiple PlayerData instances
     */
    fun createPlayerDataList(
        count: Int,
        baseUuid: UUID = UUID.randomUUID(),
    ): List<PlayerData> =
        (0 until count).map { index ->
            createPlayerData(
                uuid =
                    UUID(
                        baseUuid.mostSignificantBits + index,
                        baseUuid.leastSignificantBits,
                    ),
            )
        }
}
