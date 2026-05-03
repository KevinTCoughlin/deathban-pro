package dev.coughlin.deathban.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

@DisplayName("BanRecord Extended Tests")
class BanRecordExtendedTest {
    @Test
    @DisplayName("BanRecord stores all fields correctly")
    fun testBanRecordFields() {
        val startTime = Instant.now()
        val endTime = startTime.plus(Duration.ofHours(24))

        val ban =
            BanRecord(
                startTime = startTime,
                endTime = endTime,
                offenseLevel = 3,
                deathCause = "FALL",
            )

        assertEquals(startTime, ban.startTime)
        assertEquals(endTime, ban.endTime)
        assertEquals(3, ban.offenseLevel)
        assertEquals("FALL", ban.deathCause)
    }

    @Test
    @DisplayName("BanRecord duration calculation")
    fun testBanRecordDuration() {
        val startTime = Instant.now()
        val endTime = startTime.plus(Duration.ofHours(1))

        val ban =
            BanRecord(
                startTime = startTime,
                endTime = endTime,
                offenseLevel = 1,
                deathCause = "FALL",
            )

        val duration = Duration.between(ban.startTime, ban.endTime)
        assertEquals(Duration.ofHours(1), duration)
    }

    @Test
    @DisplayName("BanRecord different offense levels")
    fun testDifferentOffenseLevels() {
        val startTime = Instant.now()
        val endTime = startTime.plus(Duration.ofHours(1))

        val ban1 = BanRecord(startTime, endTime, 0, "FALL")
        val ban2 = BanRecord(startTime, endTime, 1, "FALL")
        val ban3 = BanRecord(startTime, endTime, 5, "FALL")

        assertEquals(0, ban1.offenseLevel)
        assertEquals(1, ban2.offenseLevel)
        assertEquals(5, ban3.offenseLevel)
    }

    @Test
    @DisplayName("BanRecord different death causes")
    fun testDifferentDeathCauses() {
        val startTime = Instant.now()
        val endTime = startTime.plus(Duration.ofHours(1))

        val causes = listOf("FALL", "LAVA", "FIRE", "DROWNING", "EXPLOSION")
        val bans = causes.map { BanRecord(startTime, endTime, 1, it) }

        bans.forEachIndexed { index, ban ->
            assertEquals(causes[index], ban.deathCause)
        }
    }
}
