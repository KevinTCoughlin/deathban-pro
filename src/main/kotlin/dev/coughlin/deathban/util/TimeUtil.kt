package dev.coughlin.deathban.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {
    private val durationPattern = Regex("([1-9]\\d*)([smhdw])")
    private val formatter =
        DateTimeFormatter
            .ofPattern("MMM d, yyyy h:mm a")
            .withZone(ZoneId.systemDefault())

    fun parseDuration(input: String): Duration {
        val trimmed = input.trim().lowercase()
        val match =
            durationPattern.matchEntire(trimmed)
                ?: throw IllegalArgumentException("Invalid duration '$input'; expected a positive value such as 30m, 24h, or 7d")
        val number =
            match.groupValues[1].toLongOrNull()
                ?: throw IllegalArgumentException("Duration is too large: $input")

        return try {
            when (match.groupValues[2][0]) {
                's' -> Duration.ofSeconds(number)
                'm' -> Duration.ofMinutes(number)
                'h' -> Duration.ofHours(number)
                'd' -> Duration.ofDays(number)
                'w' -> Duration.ofDays(Math.multiplyExact(number, 7))
                else -> error("Unreachable duration unit")
            }
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Duration is too large: $input", e)
        }
    }

    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds
        if (totalSeconds < 0) return "0s"

        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 && days == 0L && hours == 0L) append("${seconds}s")
        }.trim().ifEmpty { "0s" }
    }

    fun formatInstant(instant: Instant): String = formatter.format(instant)

    fun durationUntil(instant: Instant): Duration = Duration.between(Instant.now(), instant)
}
