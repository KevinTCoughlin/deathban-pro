package dev.coughlin.deathban.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("LocationData Tests")
class LocationDataTest {
    @Test
    @DisplayName("LocationData stores coordinates correctly")
    fun testLocationDataCoordinates() {
        val loc = LocationData(x = 100.5, y = 64.0, z = -50.25)

        assertEquals(100.5, loc.x)
        assertEquals(64.0, loc.y)
        assertEquals(-50.25, loc.z)
    }

    @Test
    @DisplayName("LocationData can be created with zeros")
    fun testZeroCoordinates() {
        val loc = LocationData(x = 0.0, y = 0.0, z = 0.0)

        assertEquals(0.0, loc.x)
        assertEquals(0.0, loc.y)
        assertEquals(0.0, loc.z)
    }

    @Test
    @DisplayName("LocationData can be created with negative coordinates")
    fun testNegativeCoordinates() {
        val loc = LocationData(x = -100.0, y = -50.0, z = -200.0)

        assertEquals(-100.0, loc.x)
        assertEquals(-50.0, loc.y)
        assertEquals(-200.0, loc.z)
    }
}
