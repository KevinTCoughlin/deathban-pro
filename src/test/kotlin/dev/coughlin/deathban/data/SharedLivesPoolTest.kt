package dev.coughlin.deathban.data

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedLivesPoolTest {

    @Test
    fun `addLife increases lives count`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)

        assertTrue(pool.addLife())
        assertEquals(6, pool.lives)
    }

    @Test
    fun `addLife tracks contributor`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)
        val contributor = UUID.randomUUID()

        pool.addLife(contributor)
        pool.addLife(contributor)

        assertEquals(2, pool.getContribution(contributor))
    }

    @Test
    fun `addLife fails when pool is full`() {
        val pool = SharedLivesPool("test", lives = 10, maxLives = 10)

        assertFalse(pool.addLife())
        assertEquals(10, pool.lives)
    }

    @Test
    fun `removeLife decreases lives count`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)

        assertTrue(pool.removeLife())
        assertEquals(4, pool.lives)
    }

    @Test
    fun `removeLife fails when pool is empty`() {
        val pool = SharedLivesPool("test", lives = 0, maxLives = 10)

        assertFalse(pool.removeLife())
        assertEquals(0, pool.lives)
    }

    @Test
    fun `addMember adds player to pool`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)
        val player = UUID.randomUUID()

        assertTrue(pool.addMember(player))
        assertTrue(pool.isMember(player))
    }

    @Test
    fun `addMember returns false for duplicate`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)
        val player = UUID.randomUUID()

        pool.addMember(player)
        assertFalse(pool.addMember(player))
    }

    @Test
    fun `removeMember removes player from pool`() {
        val pool = SharedLivesPool("test", lives = 5, maxLives = 10)
        val player = UUID.randomUUID()

        pool.addMember(player)
        assertTrue(pool.removeMember(player))
        assertFalse(pool.isMember(player))
    }

    @Test
    fun `isEmpty returns true when lives is zero`() {
        val pool = SharedLivesPool("test", lives = 0, maxLives = 10)
        assertTrue(pool.isEmpty())
    }

    @Test
    fun `isEmpty returns false when lives greater than zero`() {
        val pool = SharedLivesPool("test", lives = 1, maxLives = 10)
        assertFalse(pool.isEmpty())
    }

    @Test
    fun `isFull returns true when lives equals maxLives`() {
        val pool = SharedLivesPool("test", lives = 10, maxLives = 10)
        assertTrue(pool.isFull())
    }

    @Test
    fun `isFull returns false when lives less than maxLives`() {
        val pool = SharedLivesPool("test", lives = 9, maxLives = 10)
        assertFalse(pool.isFull())
    }

    @Test
    fun `global pool id constant is correct`() {
        assertEquals("global", SharedLivesPool.GLOBAL_POOL_ID)
    }
}
