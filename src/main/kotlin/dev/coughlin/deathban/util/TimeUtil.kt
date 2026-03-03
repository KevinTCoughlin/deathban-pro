package dev.coughlin.deathban.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {
    private val formatter =
        DateTimeFormatter
            .ofPattern("MMM d, yyyy h:mm a")
            .withZone(ZoneId.systemDefault())

    fun parseDuration(input: String): Duration {
        val trimmed = input.trim().lowercase()
        val number =
            trimmed.dropLast(1).toLongOrNull()
                ?: throw IllegalArgumentException("Invalid duration format: $input")

        return when (trimmed.last()) {
            's' -> Duration.ofSeconds(number)
            'm' -> Duration.ofMinutes(number)
            'h' -> Duration.ofHours(number)
            'd' -> Duration.ofDays(number)
            'w' -> Duration.ofDays(number * 7)
            else -> throw IllegalArgumentException("Unknown time unit in: $input")
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
