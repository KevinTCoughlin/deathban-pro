package dev.coughlin.deathban.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeUtilTest {
    @Test
    fun `parseDuration handles seconds`() {
        assertEquals(Duration.ofSeconds(30), TimeUtil.parseDuration("30s"))
        assertEquals(Duration.ofSeconds(1), TimeUtil.parseDuration("1s"))
    }

    @Test
    fun `parseDuration handles minutes`() {
        assertEquals(Duration.ofMinutes(5), TimeUtil.parseDuration("5m"))
        assertEquals(Duration.ofMinutes(60), TimeUtil.parseDuration("60m"))
    }

    @Test
    fun `parseDuration handles hours`() {
        assertEquals(Duration.ofHours(1), TimeUtil.parseDuration("1h"))
        assertEquals(Duration.ofHours(24), TimeUtil.parseDuration("24h"))
        assertEquals(Duration.ofHours(168), TimeUtil.parseDuration("168h"))
    }

    @Test
    fun `parseDuration handles days`() {
        assertEquals(Duration.ofDays(1), TimeUtil.parseDuration("1d"))
        assertEquals(Duration.ofDays(7), TimeUtil.parseDuration("7d"))
    }

    @Test
    fun `parseDuration handles weeks`() {
        assertEquals(Duration.ofDays(7), TimeUtil.parseDuration("1w"))
        assertEquals(Duration.ofDays(14), TimeUtil.parseDuration("2w"))
    }

    @Test
    fun `parseDuration trims whitespace`() {
        assertEquals(Duration.ofHours(1), TimeUtil.parseDuration("  1h  "))
    }

    @Test
    fun `parseDuration is case insensitive`() {
        assertEquals(Duration.ofHours(1), TimeUtil.parseDuration("1H"))
        assertEquals(Duration.ofDays(1), TimeUtil.parseDuration("1D"))
    }

    @Test
    fun `parseDuration throws on invalid format`() {
        assertThrows<IllegalArgumentException> { TimeUtil.parseDuration("abc") }
        assertThrows<IllegalArgumentException> { TimeUtil.parseDuration("1x") }
        assertThrows<IllegalArgumentException> { TimeUtil.parseDuration("h") }
    }

    @Test
    fun `formatDuration shows hours and minutes`() {
        assertEquals("1h 30m", TimeUtil.formatDuration(Duration.ofMinutes(90)))
    }

    @Test
    fun `formatDuration shows days`() {
        assertEquals("1d 2h", TimeUtil.formatDuration(Duration.ofHours(26)))
    }

    @Test
    fun `formatDuration shows seconds for short durations`() {
        assertEquals("30s", TimeUtil.formatDuration(Duration.ofSeconds(30)))
    }

    @Test
    fun `formatDuration handles zero`() {
        assertEquals("0s", TimeUtil.formatDuration(Duration.ZERO))
    }

    @Test
    fun `formatDuration handles negative as zero`() {
        assertEquals("0s", TimeUtil.formatDuration(Duration.ofSeconds(-10)))
    }

    @Test
    fun `durationUntil calculates correctly`() {
        val future = Instant.now().plusSeconds(60)
        val duration = TimeUtil.durationUntil(future)
        assertTrue(duration.seconds in 59..61)
    }

    @Test
    fun `formatInstant produces readable output`() {
        val instant = Instant.parse("2024-10-15T14:30:00Z")
        val formatted = TimeUtil.formatInstant(instant)
        assertTrue(formatted.contains("2024"))
        assertTrue(formatted.contains("15"))
    }
}
