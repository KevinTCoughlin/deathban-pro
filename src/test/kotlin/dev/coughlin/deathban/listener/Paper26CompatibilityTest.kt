package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.PlayerData
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.manager.BanManager
import dev.coughlin.deathban.manager.OffenseManager
import dev.coughlin.deathban.manager.SharedLivesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Paper26CompatibilityTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `fatal fire hit deducts exactly one shared life`() {
        val fixture = sharedFixture()
        val livesBefore = fixture.manager.getGlobalPool().lives

        fixture.deliverFireDamage()
        assertFalse(fixture.fire.isCancelled)
        assertEquals(livesBefore, fixture.manager.getGlobalPool().lives)

        fixture.listener.onPlayerDeath(fixture.death)
        fixture.listener.onPlayerDeath(fixture.death)

        assertEquals(livesBefore - 1, fixture.manager.getGlobalPool().lives)
        assertTrue(fixture.fire.finalDamage >= fixture.player.health)
        assertEquals(EntityDamageEvent.DamageCause.FIRE, fixture.fire.cause)
    }

    @Test
    fun `admin bypass stays immune to fatal fire`() {
        val fixture = sharedFixture(bypass = true)
        val livesBefore = fixture.manager.getGlobalPool().lives

        fixture.deliverFireDamage()
        fixture.listener.onPlayerDeath(fixture.death)

        assertEquals(livesBefore, fixture.manager.getGlobalPool().lives)
    }

    @Test
    fun `fatal fire at the individual threshold bans once and records FIRE`() {
        val player = player()
        val fire = fireDamage(player)
        val death = death(player)
        val settings = Settings(individualConfig(maxDeaths = 1))
        val banManager = mockk<BanManager>(relaxed = true)
        val listener =
            DeathListener(
                mockk<Plugin>(relaxed = true),
                settings,
                mockk<Messages>(relaxed = true),
                PlayerDataManager(tempDir, Logger.getLogger("paper26")),
                OffenseManager(settings),
                banManager,
                null,
            )

        listener.deliver(fire)
        assertFalse(fire.isCancelled)
        listener.onPlayerDeath(death)
        listener.onPlayerDeath(death)

        val recorded = slot<PlayerData>()
        verify(exactly = 1) { banManager.applyBan(player, capture(recorded), "FIRE") }
        assertEquals(listOf("FIRE"), recorded.captured.deaths.map { it.cause })
    }

    @Test
    fun `respawn without bed or anchor keeps vanilla and plugin locations`() {
        val fixture = sharedFixture()
        val livesBefore = fixture.manager.getGlobalPool().lives
        val worldSpawn = mockk<Location>()
        val pluginSpawn = mockk<Location>()
        val worldRespawn = respawn(fixture.player, PlayerRespawnEvent.RespawnReason.DEATH, worldSpawn)
        val pluginRespawn = respawn(fixture.player, PlayerRespawnEvent.RespawnReason.PLUGIN, pluginSpawn)

        fixture.listener.onPlayerDeath(fixture.death)
        fixture.listener.onRespawn(worldRespawn)
        fixture.listener.onPlayerDeath(fixture.death)
        fixture.listener.onRespawn(pluginRespawn)
        fixture.listener.onPlayerDeath(fixture.death)

        assertEquals(livesBefore - 3, fixture.manager.getGlobalPool().lives)
        verify(exactly = 0) { worldRespawn.setRespawnLocation(any()) }
        verify(exactly = 0) { pluginRespawn.setRespawnLocation(any()) }
        assertEquals(worldSpawn, worldRespawn.respawnLocation)
        assertEquals(pluginSpawn, pluginRespawn.respawnLocation)
        assertFalse(worldRespawn.isBedSpawn)
        assertFalse(worldRespawn.isAnchorSpawn)
        assertTrue(worldRespawn.isMissingRespawnBlock)
        assertFalse(pluginRespawn.isBedSpawn)
        assertFalse(pluginRespawn.isAnchorSpawn)
    }

    @Test
    fun `joining with health above base max does not change health or shared lives`() {
        val fixture = sharedFixture(health = 30.0)
        val livesBefore = fixture.manager.getGlobalPool().lives
        val join =
            JoinListener(
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        val joinEvent = mockk<PlayerJoinEvent>(relaxed = true)
        every { joinEvent.player } returns fixture.player

        join.onPlayerJoin(joinEvent)
        fixture.deliverFireDamage()
        fixture.listener.onPlayerDeath(fixture.death)

        verify(exactly = 0) { fixture.player.setHealth(any()) }
        assertEquals(30.0, fixture.player.health)
        assertEquals(livesBefore - 1, fixture.manager.getGlobalPool().lives)
    }

    @Test
    fun `admin with health above base max stays immune to fire`() {
        val fixture = sharedFixture(bypass = true, health = 30.0)
        val livesBefore = fixture.manager.getGlobalPool().lives

        fixture.deliverFireDamage()
        fixture.listener.onPlayerDeath(fixture.death)

        verify(exactly = 0) { fixture.player.setHealth(any()) }
        assertEquals(livesBefore, fixture.manager.getGlobalPool().lives)
    }

    @Test
    fun `fire spread is not cancelled because this plugin has no protected arenas`() {
        val handled =
            listOf(DeathListener::class.java, JoinListener::class.java).flatMap { type ->
                type.declaredMethods
                    .filter { it.isAnnotationPresent(EventHandler::class.java) }
                    .map { it.parameterTypes.single() }
            }
        assertFalse(handled.any { BlockSpreadEvent::class.java.isAssignableFrom(it) })
        val hits =
            File("src/main")
                .walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "yml") }
                .filter { file ->
                    val text = file.readText()
                    text.contains("BlockSpreadEvent") || text.contains("protected arena")
                }.map { it.relativeTo(File("src/main")).path }
                .toList()
        assertEquals(emptyList(), hits)
    }

    @Test
    fun `main sources do not reference dragon breath potions or packet libraries`() {
        val forbidden =
            listOf(
                "DRAGON_BREATH",
                "PotionSplashEvent",
                "ProtocolLib",
                "PacketEvents",
                "net.minecraft",
                "com.comphenix.protocol",
            )
        val hits =
            File("src/main/kotlin")
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    val text = file.readText()
                    forbidden.filter { text.contains(it) }.map { "${file.relativeTo(File("src/main/kotlin")).path}: $it" }
                }.toList()

        assertEquals(emptyList(), hits)
    }

    private fun sharedFixture(
        bypass: Boolean = false,
        health: Double = 10.0,
    ): SharedFixture {
        val player = player(bypass, health)
        val fire = fireDamage(player)
        val manager = SharedLivesManager(tempDir, Logger.getLogger("paper26"), 10, 20)
        val listener =
            DeathListener(
                mockk<Plugin>(relaxed = true),
                Settings(YamlConfiguration().also { it.set("mode", "shared") }),
                mockk<Messages>(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                manager,
            )
        return SharedFixture(player, fire, death(player), listener, manager)
    }

    private fun player(
        bypass: Boolean = false,
        health: Double = 10.0,
    ): Player {
        val player = mockk<Player>(relaxed = true)
        val world = mockk<World>(relaxed = true)
        val location = mockk<Location>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        every { player.hasPermission(any<String>()) } returns bypass
        every { player.world } returns world
        every { world.name } returns "world"
        every { player.location } returns location
        every { location.x } returns 1.0
        every { location.y } returns 64.0
        every { location.z } returns 2.0
        every { player.health } returns health
        return player
    }

    private fun fireDamage(player: Player): EntityDamageByBlockEvent {
        val fire = mockk<EntityDamageByBlockEvent>(relaxed = true)
        every { fire.entity } returns player
        every { fire.cause } returns EntityDamageEvent.DamageCause.FIRE
        every { fire.finalDamage } returns 20.0
        every { fire.isCancelled } returns false
        every { player.lastDamageCause } returns fire
        return fire
    }

    private fun death(player: Player): PlayerDeathEvent {
        val death = mockk<PlayerDeathEvent>(relaxed = true)
        every { death.entity } returns player
        return death
    }

    private fun respawn(
        player: Player,
        reason: PlayerRespawnEvent.RespawnReason,
        location: Location,
    ): PlayerRespawnEvent {
        val respawn = mockk<PlayerRespawnEvent>(relaxed = true)
        every { respawn.player } returns player
        every { respawn.respawnReason } returns reason
        every { respawn.respawnLocation } returns location
        every { respawn.isBedSpawn } returns false
        every { respawn.isAnchorSpawn } returns false
        every { respawn.isMissingRespawnBlock } returns true
        return respawn
    }

    private fun individualConfig(maxDeaths: Int): YamlConfiguration =
        YamlConfiguration().also {
            it.set("mode", "individual")
            it.set("rolling-window.enabled", false)
            it.set("rolling-window.max-deaths", maxDeaths)
        }

    private class SharedFixture(
        val player: Player,
        val fire: EntityDamageByBlockEvent,
        val death: PlayerDeathEvent,
        val listener: DeathListener,
        val manager: SharedLivesManager,
    ) {
        fun deliverFireDamage() = listener.deliver(fire)
    }
}

private fun DeathListener.deliver(event: EntityDamageEvent) {
    javaClass.declaredMethods
        .filter { method ->
            method.getAnnotation(EventHandler::class.java) != null &&
                method.parameterCount == 1 &&
                method.parameterTypes[0].isAssignableFrom(event.javaClass)
        }.forEach { method ->
            method.isAccessible = true
            method.invoke(this, event)
        }
}
