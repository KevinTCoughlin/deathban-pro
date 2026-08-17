package dev.coughlin.deathban.manager

import dev.coughlin.deathban.data.SharedLivesPool
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("SharedLivesManager Tests")
class SharedLivesManagerTest {
    @TempDir
    lateinit var tempDir: File

    private val logger = Logger.getLogger("TestLogger")

    private fun createManager(
        defaultLives: Int = 10,
        maxLives: Int = 20,
    ): SharedLivesManager = SharedLivesManager(tempDir, logger, defaultLives, maxLives)

    @Test
    @DisplayName("initializes with global pool")
    fun testInitializesWithGlobalPool() {
        val manager = createManager()
        val global = manager.getGlobalPool()

        assertEquals(SharedLivesPool.GLOBAL_POOL_ID, global.id)
        assertEquals(10, global.lives)
        assertEquals(20, global.maxLives)
    }

    @Test
    @DisplayName("save and reload preserves pool state")
    fun testSaveAndReload() {
        val manager = createManager()
        val player = UUID.randomUUID()

        manager.getGlobalPool().addMember(player)
        manager.consumeLife(player)
        manager.save()

        val manager2 = createManager()
        val global = manager2.getGlobalPool()

        assertEquals(9, global.lives)
        assertTrue(global.isMember(player))
    }

    @Test
    @DisplayName("consumeLife decrements pool lives")
    fun testConsumeLife() {
        val manager = createManager(defaultLives = 5, maxLives = 10)
        val player = UUID.randomUUID()

        assertTrue(manager.consumeLife(player))
        assertEquals(4, manager.getGlobalPool().lives)
    }

    @Test
    @DisplayName("consumeLife returns false when pool empty")
    fun testConsumeLifeEmpty() {
        val manager = createManager(defaultLives = 0, maxLives = 10)
        val player = UUID.randomUUID()

        assertFalse(manager.consumeLife(player))
    }

    @Test
    @DisplayName("addLife increments pool lives")
    fun testAddLife() {
        val manager = createManager(defaultLives = 5, maxLives = 10)
        val player = UUID.randomUUID()

        assertTrue(manager.addLife(player))
        assertEquals(6, manager.getGlobalPool().lives)
    }

    @Test
    @DisplayName("addLife returns false when pool full")
    fun testAddLifeFull() {
        val manager = createManager(defaultLives = 10, maxLives = 10)
        val player = UUID.randomUUID()

        assertFalse(manager.addLife(player))
    }

    @Test
    @DisplayName("createTeamPool creates a new pool with creator as member")
    fun testCreateTeamPool() {
        val manager = createManager()
        val creator = UUID.randomUUID()

        val pool = manager.createTeamPool("alpha", creator)

        assertNotNull(pool)
        assertEquals("alpha", pool.id)
        assertTrue(pool.isMember(creator))
        assertEquals(10, pool.lives)
    }

    @Test
    @DisplayName("createTeamPool returns null for duplicate name")
    fun testCreateTeamPoolDuplicate() {
        val manager = createManager()
        val creator = UUID.randomUUID()

        manager.createTeamPool("alpha", creator)
        val dup = manager.createTeamPool("alpha", UUID.randomUUID())

        assertNull(dup)
    }

    @Test
    @DisplayName("createTeamPool returns null for global pool id")
    fun testCreateTeamPoolGlobal() {
        val manager = createManager()
        val pool = manager.createTeamPool(SharedLivesPool.GLOBAL_POOL_ID, UUID.randomUUID())

        assertNull(pool)
    }

    @Test
    @DisplayName("createTeamPool rejects IDs unsafe for YAML paths")
    fun testCreateTeamPoolInvalidId() {
        val manager = createManager()

        assertNull(manager.createTeamPool("team.with.dots", UUID.randomUUID()))
        assertNull(manager.createTeamPool("../team", UUID.randomUUID()))
        assertNull(manager.createTeamPool("a".repeat(33), UUID.randomUUID()))
    }

    @Test
    @DisplayName("joinPool moves player to new pool")
    fun testJoinPool() {
        val manager = createManager()
        val player = UUID.randomUUID()
        val creator = UUID.randomUUID()

        manager.createTeamPool("team1", creator)
        assertTrue(manager.joinPool(player, "team1"))

        val pool = manager.getPoolForPlayer(player)
        assertNotNull(pool)
        assertEquals("team1", pool.id)
    }

    @Test
    @DisplayName("leavePool removes player from pool")
    fun testLeavePool() {
        val manager = createManager()
        val player = UUID.randomUUID()

        manager.createTeamPool("team1", player)
        assertTrue(manager.leavePool(player))
        assertNull(manager.getPoolForPlayer(player))
    }

    @Test
    @DisplayName("deletePool removes non-global pool")
    fun testDeletePool() {
        val manager = createManager()
        manager.createTeamPool("temp", UUID.randomUUID())

        assertTrue(manager.deletePool("temp"))
        assertNull(manager.getPool("temp"))
    }

    @Test
    @DisplayName("deletePool refuses to delete global pool")
    fun testDeleteGlobalPool() {
        val manager = createManager()

        assertFalse(manager.deletePool(SharedLivesPool.GLOBAL_POOL_ID))
        assertNotNull(manager.getPool(SharedLivesPool.GLOBAL_POOL_ID))
    }

    @Test
    @DisplayName("getPoolForPlayer returns team pool over global")
    fun testGetPoolForPlayerTeamPriority() {
        val manager = createManager()
        val player = UUID.randomUUID()

        // Add to global
        manager.getGlobalPool().addMember(player)

        // Add to team
        manager.createTeamPool("team1", player)

        val pool = manager.getPoolForPlayer(player)
        assertNotNull(pool)
        assertEquals("team1", pool.id)
    }

    @Test
    @DisplayName("getAllPools returns all pools")
    fun testGetAllPools() {
        val manager = createManager()
        manager.createTeamPool("a", UUID.randomUUID())
        manager.createTeamPool("b", UUID.randomUUID())

        val pools = manager.getAllPools()
        assertEquals(3, pools.size) // global + a + b
    }

    @Test
    @DisplayName("saveAsync writes data synchronously when no plugin")
    fun testSaveAsyncNoPlugin() {
        val manager = createManager()
        manager.consumeLife(UUID.randomUUID())
        // saveAsync is called internally by consumeLife, and without a plugin it falls back to sync

        val manager2 = createManager()
        assertEquals(9, manager2.getGlobalPool().lives)
    }
}
