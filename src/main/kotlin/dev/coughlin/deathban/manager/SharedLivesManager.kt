package dev.coughlin.deathban.manager

import dev.coughlin.deathban.data.SharedLivesPool
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

class SharedLivesManager(
    private val dataFolder: File,
    private val logger: Logger,
    private val defaultLives: Int,
    private val maxLives: Int,
    private val plugin: Plugin? = null,
) {
    private val poolsFile = File(dataFolder, "shared-lives.yml")
    private val pools = mutableMapOf<String, SharedLivesPool>()

    @Volatile private var dirty = false

    init {
        load()
    }

    fun getGlobalPool(): SharedLivesPool = getOrCreatePool(SharedLivesPool.GLOBAL_POOL_ID)

    fun getOrCreatePool(id: String): SharedLivesPool =
        pools.getOrPut(id) {
            SharedLivesPool(
                id = id,
                lives = defaultLives,
                maxLives = maxLives,
            ).also { saveAsync() }
        }

    fun getPool(id: String): SharedLivesPool? = pools[id]

    fun getPoolForPlayer(uuid: UUID): SharedLivesPool? {
        // First check team pools
        pools.values.find { it.id != SharedLivesPool.GLOBAL_POOL_ID && it.isMember(uuid) }?.let {
            return it
        }
        // Fall back to global pool if player is a member
        val global = pools[SharedLivesPool.GLOBAL_POOL_ID]
        if (global?.isMember(uuid) == true) return global
        return null
    }

    fun consumeLife(uuid: UUID): Boolean {
        val pool = getPoolForPlayer(uuid) ?: getGlobalPool()
        if (pool.removeLife()) {
            saveAsync()
            logger.info("Life consumed from pool '${pool.id}'. Remaining: ${pool.lives}/${pool.maxLives}")
            return true
        }
        return false
    }

    fun addLife(
        uuid: UUID,
        poolId: String? = null,
    ): Boolean {
        val pool = poolId?.let { getOrCreatePool(it) } ?: getPoolForPlayer(uuid) ?: getGlobalPool()
        if (pool.addLife(uuid)) {
            saveAsync()
            logger.info("Life added to pool '${pool.id}' by $uuid. Total: ${pool.lives}/${pool.maxLives}")
            return true
        }
        return false
    }

    fun joinPool(
        uuid: UUID,
        poolId: String,
    ): Boolean {
        // Leave current pool first
        leavePool(uuid)

        val pool = getOrCreatePool(poolId)
        if (pool.addMember(uuid)) {
            saveAsync()
            return true
        }
        return false
    }

    fun leavePool(uuid: UUID): Boolean {
        var left = false
        pools.values.forEach { pool ->
            if (pool.removeMember(uuid)) {
                left = true
            }
        }
        if (left) saveAsync()
        return left
    }

    fun createTeamPool(
        id: String,
        creator: UUID,
    ): SharedLivesPool? {
        if (pools.containsKey(id)) return null
        if (id == SharedLivesPool.GLOBAL_POOL_ID) return null

        val pool =
            SharedLivesPool(
                id = id,
                lives = defaultLives,
                maxLives = maxLives,
            )
        pool.addMember(creator)
        pools[id] = pool
        saveAsync()
        return pool
    }

    fun deletePool(id: String): Boolean {
        if (id == SharedLivesPool.GLOBAL_POOL_ID) return false
        val removed = pools.remove(id) != null
        if (removed) saveAsync()
        return removed
    }

    fun getAllPools(): List<SharedLivesPool> = pools.values.toList()

    /**
     * Schedule an async write. The in-memory state is always authoritative;
     * this just persists the snapshot to disk off the main thread.
     */
    fun saveAsync() {
        dirty = true
        plugin?.let { p ->
            p.server.scheduler.runTaskAsynchronously(
                p,
                Runnable {
                    try {
                        writeToDisk()
                        dirty = false
                    } catch (e: Exception) {
                        logger.severe("Failed to async-save shared lives: ${e.message}")
                    }
                },
            )
        } ?: run {
            writeToDisk()
            dirty = false
        }
    }

    /**
     * Synchronous flush — call from onDisable() to guarantee persistence.
     */
    fun saveAll() {
        if (dirty) {
            writeToDisk()
            dirty = false
            logger.info("Flushed shared lives pools to disk")
        }
    }

    fun save() {
        writeToDisk()
    }

    private fun writeToDisk() {
        val config = YamlConfiguration()

        pools.forEach { (id, pool) ->
            config.set("$id.lives", pool.lives)
            config.set("$id.max-lives", pool.maxLives)
            config.set("$id.members", pool.members.map { it.toString() })
            config.set("$id.last-modified", pool.lastModified.toString())

            val contribs =
                pool.contributions.map { (uuid, count) ->
                    mapOf("uuid" to uuid.toString(), "count" to count)
                }
            config.set("$id.contributions", contribs)
        }

        config.save(poolsFile)
    }

    private fun load() {
        if (!poolsFile.exists()) {
            // Create default global pool
            pools[SharedLivesPool.GLOBAL_POOL_ID] =
                SharedLivesPool(
                    id = SharedLivesPool.GLOBAL_POOL_ID,
                    lives = defaultLives,
                    maxLives = maxLives,
                )
            save()
            return
        }

        val config = YamlConfiguration.loadConfiguration(poolsFile)

        config.getKeys(false).forEach { id ->
            val pool =
                SharedLivesPool(
                    id = id,
                    lives = config.getInt("$id.lives", defaultLives),
                    maxLives = config.getInt("$id.max-lives", maxLives),
                )

            config.getStringList("$id.members").forEach { uuidStr ->
                runCatching { UUID.fromString(uuidStr) }.getOrNull()?.let {
                    pool.members.add(it)
                }
            }

            config.getString("$id.last-modified")?.let {
                runCatching { Instant.parse(it) }.getOrNull()?.let { instant ->
                    pool.lastModified = instant
                }
            }

            @Suppress("UNCHECKED_CAST")
            val contribs = config.getList("$id.contributions") as? List<Map<String, Any>> ?: emptyList()
            contribs.forEach { contrib ->
                val uuid = (contrib["uuid"] as? String)?.let { UUID.fromString(it) }
                val count = (contrib["count"] as? Number)?.toInt() ?: 0
                if (uuid != null) {
                    pool.contributions[uuid] = count
                }
            }

            pools[id] = pool
        }

        logger.info("Loaded ${pools.size} shared lives pool(s)")
    }

    fun reload() {
        pools.clear()
        load()
    }
}
