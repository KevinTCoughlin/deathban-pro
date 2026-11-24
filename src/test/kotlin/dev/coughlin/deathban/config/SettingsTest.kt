package dev.coughlin.deathban.config

import io.mockk.every
import io.mockk.mockk
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {
    private fun createMockConfig(
        mode: String? = "individual",
        rollingWindowEnabled: Boolean = true,
        rollingWindowDuration: String = "24h",
        maxDeaths: Int = 3,
        sharedLivesDefault: Int = 10,
        sharedLivesMax: Int = 20,
        offenseResetEnabled: Boolean = true,
        offenseResetPeriod: String = "168h",
        enabledWorlds: List<String> = emptyList(),
        disabledWorlds: List<String> = emptyList(),
        banDurations: Map<String, String>? = null,
    ): FileConfiguration {
        val config = mockk<FileConfiguration>(relaxed = true)
        val banSection = mockk<ConfigurationSection>(relaxed = true)

        every { config.getBoolean("debug", false) } returns false
        every { config.getBoolean("update-check", true) } returns true
        every { config.getBoolean("metrics", true) } returns true
        every { config.getString("mode") } returns mode
        every { config.getInt("shared-lives.default-lives", 10) } returns sharedLivesDefault
        every { config.getInt("shared-lives.max-lives", 20) } returns sharedLivesMax
        every { config.getBoolean("shared-lives.allow-teams", true) } returns true
        every { config.getString("shared-lives.empty-pool-ban") } returns "1h"
        every { config.getBoolean("rolling-window.enabled", true) } returns rollingWindowEnabled
        every { config.getString("rolling-window.duration") } returns rollingWindowDuration
        every { config.getInt("rolling-window.max-deaths", 3) } returns maxDeaths
        every { config.getBoolean("offense-reset.enabled", true) } returns offenseResetEnabled
        every { config.getString("offense-reset.clean-period") } returns offenseResetPeriod
        every { config.getStringList("enabled-worlds") } returns enabledWorlds
        every { config.getStringList("disabled-worlds") } returns disabledWorlds
        every { config.getString("bypass-permission") } returns "deathban.bypass"
        every { config.getString("theme") } returns "default"
        every { config.getBoolean("theme-sounds", true) } returns true
        every { config.getBoolean("theme-particles", true) } returns true

        if (banDurations != null) {
            every { config.getConfigurationSection("ban-durations") } returns banSection
            every { banSection.getKeys(false) } returns banDurations.keys
            banDurations.forEach { (key, value) ->
                every { banSection.getString(key) } returns value
            }
        } else {
            every { config.getConfigurationSection("ban-durations") } returns null
        }

        return config
    }

    @Test
    fun `getBanDuration returns correct duration for offense level`() {
        val config =
            createMockConfig(
                banDurations = mapOf("1" to "1h", "2" to "6h", "3" to "24h"),
            )
        val settings = Settings(config)

        assertEquals(1, settings.getBanDuration(1).toHours())
        assertEquals(6, settings.getBanDuration(2).toHours())
        assertEquals(24, settings.getBanDuration(3).toHours())
    }

    @Test
    fun `getBanDuration returns max level duration for unknown level`() {
        val config =
            createMockConfig(
                banDurations = mapOf("1" to "1h", "2" to "6h", "3" to "24h"),
            )
        val settings = Settings(config)

        assertEquals(24, settings.getBanDuration(99).toHours())
    }

    @Test
    fun `getBanDuration uses defaults when no config section`() {
        val config = createMockConfig()
        val settings = Settings(config)

        assertEquals(1, settings.getBanDuration(1).toHours())
        assertEquals(6, settings.getBanDuration(2).toHours())
        assertEquals(24, settings.getBanDuration(3).toHours())
        assertEquals(72, settings.getBanDuration(4).toHours())
        assertEquals(168, settings.getBanDuration(5).toHours())
    }

    @Test
    fun `isWorldEnabled returns true when no world filters`() {
        val config = createMockConfig()
        val settings = Settings(config)

        assertTrue(settings.isWorldEnabled("world"))
        assertTrue(settings.isWorldEnabled("world_nether"))
    }

    @Test
    fun `isWorldEnabled returns false for disabled world`() {
        val config = createMockConfig(disabledWorlds = listOf("world_nether"))
        val settings = Settings(config)

        assertFalse(settings.isWorldEnabled("world_nether"))
        assertTrue(settings.isWorldEnabled("world"))
    }

    @Test
    fun `isWorldEnabled returns true only for enabled worlds`() {
        val config = createMockConfig(enabledWorlds = listOf("world", "world_the_end"))
        val settings = Settings(config)

        assertTrue(settings.isWorldEnabled("world"))
        assertTrue(settings.isWorldEnabled("world_the_end"))
        assertFalse(settings.isWorldEnabled("world_nether"))
    }

    @Test
    fun `isWorldEnabled disabled takes priority over enabled`() {
        val config =
            createMockConfig(
                enabledWorlds = listOf("world", "world_nether"),
                disabledWorlds = listOf("world_nether"),
            )
        val settings = Settings(config)

        assertTrue(settings.isWorldEnabled("world"))
        assertFalse(settings.isWorldEnabled("world_nether"))
    }

    @Test
    fun `mode defaults to INDIVIDUAL`() {
        val config = createMockConfig(mode = null)
        val settings = Settings(config)

        assertEquals(BanMode.INDIVIDUAL, settings.mode)
    }

    @Test
    fun `mode parses shared correctly`() {
        val config = createMockConfig(mode = "shared")
        val settings = Settings(config)

        assertEquals(BanMode.SHARED, settings.mode)
    }

    @Test
    fun `mode is case insensitive`() {
        val config = createMockConfig(mode = "SHARED")
        val settings = Settings(config)

        assertEquals(BanMode.SHARED, settings.mode)
    }

    @Test
    fun `invalid mode is rejected`() {
        val error = assertThrows<IllegalArgumentException> { Settings(createMockConfig(mode = "typo")) }
        assertTrue(error.message!!.contains("individual"))
        assertTrue(error.message!!.contains("shared"))
    }

    @Test
    fun `failed reload keeps the previous configuration`() {
        val config = YamlConfiguration()
        config.set("mode", "shared")
        assertEquals(BanMode.SHARED, reloadSettings(config) { }.mode)

        val error =
            assertThrows<IllegalArgumentException> {
                reloadSettings(config) { config.set("mode", "typo") }
            }
        assertTrue(error.message!!.contains("individual"))
        assertEquals("shared", config.getString("mode"))
        assertEquals(BanMode.SHARED, Settings(config).mode)
    }

    @Test
    fun `non-positive death threshold is rejected`() {
        assertThrows<IllegalArgumentException> { Settings(createMockConfig(maxDeaths = 0)) }
    }

    @Test
    fun `shared lives defaults must fit configured maximum`() {
        assertThrows<IllegalArgumentException> {
            Settings(createMockConfig(sharedLivesDefault = 11, sharedLivesMax = 10))
        }
    }

    @Test
    fun `invalid ban duration level is rejected`() {
        assertThrows<IllegalArgumentException> {
            Settings(createMockConfig(banDurations = mapOf("first" to "1h")))
        }
    }

    @Test
    fun `every invalid value is reported in one error`() {
        val config = YamlConfiguration()
        config.set("mode", "teams")
        config.set("rolling-window.max-deaths", 0)
        config.set("rolling-window.duration", "later")
        config.set("shared-lives.max-lives", -1)
        config.set("shared-lives.default-lives", 50)
        config.set("shared-lives.empty-pool-ban", "soon")
        config.set("offense-reset.clean-period", "0h")
        config.set("ban-durations.first", "1h")
        config.set("ban-durations.2", "later")

        val message = assertThrows<IllegalArgumentException> { Settings(config) }.message!!

        listOf(
            "mode must be 'individual' or 'shared'",
            "rolling-window.max-deaths must be greater than 0",
            "rolling-window.duration:",
            "shared-lives.max-lives must be greater than 0",
            "shared-lives.default-lives must be between 0 and -1",
            "shared-lives.empty-pool-ban:",
            "offense-reset.clean-period:",
            "ban-durations keys must be positive offense levels: 'first'",
            "ban-durations.2:",
        ).forEach { expected ->
            assertTrue(message.contains(expected), message)
        }
    }
}
