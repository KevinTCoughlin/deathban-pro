package dev.coughlin.deathban.data

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

class PlayerDataManager(
    private val dataFolder: File,
    private val logger: Logger,
    private val plugin: Plugin? = null,
) {
    private val playersFolder = File(dataFolder, "players")
    private val cache = ConcurrentHashMap<UUID, PlayerData>()
    private val dirty = ConcurrentHashMap.newKeySet<UUID>()
    private val saveRevisions = ConcurrentHashMap<UUID, AtomicLong>()
    private val saveLocks = ConcurrentHashMap<UUID, Any>()
    private val pendingBans = ConcurrentHashMap.newKeySet<UUID>()
    private val pendingBansFile = File(dataFolder, "pending-bans.yml")
    private val pendingBansRevision = AtomicLong()
    private val pendingBansLock = Any()

    @Volatile private var pendingBansDirty = false

    init {
        playersFolder.mkdirs()
        loadPendingBans()
    }

    fun getOrCreate(uuid: UUID): PlayerData = cache.getOrPut(uuid) { load(uuid) ?: PlayerData(uuid) }

    fun get(uuid: UUID): PlayerData? = cache[uuid] ?: load(uuid)?.also { cache[uuid] = it }

    /**
     * Pre-load a player's data into the cache.
     * Safe to call from any thread (e.g. AsyncPlayerPreLoginEvent).
     */
    fun preload(uuid: UUID) {
        if (cache.containsKey(uuid)) return
        load(uuid)?.let { cache[uuid] = it }
    }

    /**
     * Synchronous save — writes player data to disk immediately.
     * Use [saveAsync] instead when calling from the main thread.
     */
    fun save(data: PlayerData) {
        cache[data.uuid] = data
        val revision = nextRevision(data.uuid)
        persistSnapshot(data.uuid, revision, data.snapshot())
    }

    /**
     * Marks the player data as dirty and schedules an async write.
     * The in-memory cache is updated immediately so subsequent reads
     * on the main thread see the latest state.
     * A snapshot of the data is captured on the calling thread to avoid
     * concurrent modification in the async writer.
     */
    fun saveAsync(data: PlayerData) {
        cache[data.uuid] = data
        dirty.add(data.uuid)
        val revision = nextRevision(data.uuid)

        // Snapshot mutable state on the calling thread for thread safety
        val snapshot = data.snapshot()

        plugin?.let { p ->
            p.server.scheduler.runTaskAsynchronously(
                p,
                Runnable {
                    try {
                        persistSnapshot(data.uuid, revision, snapshot)
                    } catch (e: Exception) {
                        logger.severe("Failed to async-save data for ${data.uuid}: ${e.message}")
                    }
                },
            )
        } ?: run {
            persistSnapshot(data.uuid, revision, snapshot)
        }
    }

    /**
     * Flush all dirty entries to disk synchronously.
     * Call this from onDisable() to guarantee nothing is lost.
     */
    fun saveAll() {
        val dirtyUuids = dirty.toSet()
        for (uuid in dirtyUuids) {
            cache[uuid]?.let { data ->
                val revision = nextRevision(uuid)
                persistSnapshot(uuid, revision, data.snapshot())
            }
        }
        if (pendingBansDirty) {
            val revision = pendingBansRevision.incrementAndGet()
            persistPendingBans(revision, pendingBans.toList())
        }
        if (dirtyUuids.isNotEmpty()) {
            logger.info("Flushed ${dirtyUuids.size} dirty player record(s) to disk")
        }
    }

    private fun nextRevision(uuid: UUID): Long =
        saveRevisions
            .computeIfAbsent(uuid) { AtomicLong() }
            .incrementAndGet()

    private fun persistSnapshot(
        uuid: UUID,
        revision: Long,
        snapshot: PlayerData,
    ) {
        val lock = saveLocks.computeIfAbsent(uuid) { Any() }
        synchronized(lock) {
            if (saveRevisions[uuid]?.get() != revision) return
            writeToDisk(snapshot)
            if (saveRevisions[uuid]?.get() == revision) {
                dirty.remove(uuid)
            }
        }
    }

    private fun writeToDisk(data: PlayerData) {
        val file = getPlayerFile(data.uuid)
        val config = YamlConfiguration()

        config.set("offense-level", data.offenseLevel)
        config.set("last-death-time", data.lastDeathTime?.toString())
        config.set("pending-pardon", data.pendingPardon)

        data.currentBan?.let { ban ->
            config.set("current-ban.start-time", ban.startTime.toString())
            config.set("current-ban.end-time", ban.endTime.toString())
            config.set("current-ban.offense-level", ban.offenseLevel)
            config.set("current-ban.death-cause", ban.deathCause)
        }

        val deathsList =
            data.deaths.map { death ->
                mapOf(
                    "timestamp" to death.timestamp.toString(),
                    "world" to death.world,
                    "cause" to death.cause,
                    "killer" to death.killer?.toString(),
                    "location" to
                        mapOf(
                            "x" to death.location.x,
                            "y" to death.location.y,
                            "z" to death.location.z,
                        ),
                )
            }
        config.set("deaths", deathsList)

        config.save(file)
    }

    private fun load(uuid: UUID): PlayerData? {
        val file = getPlayerFile(uuid)
        if (!file.exists()) return null

        val config = YamlConfiguration.loadConfiguration(file)
        val data = PlayerData(uuid)

        data.offenseLevel = config.getInt("offense-level", 0)
        data.lastDeathTime =
            config.getString("last-death-time")?.let {
                runCatching { Instant.parse(it) }.getOrNull()
            }
        data.pendingPardon = config.getBoolean("pending-pardon", false)

        if (config.contains("current-ban")) {
            val startTime =
                config.getString("current-ban.start-time")?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }
            val endTime =
                config.getString("current-ban.end-time")?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }

            if (startTime != null && endTime != null) {
                data.currentBan =
                    BanRecord(
                        startTime = startTime,
                        endTime = endTime,
                        offenseLevel = config.getInt("current-ban.offense-level"),
                        deathCause = config.getString("current-ban.death-cause") ?: "UNKNOWN",
                    )
            } else {
                logger.warning("Skipping corrupted ban record for $uuid (missing or invalid timestamps)")
            }
        }

        val deathsList = config.getMapList("deaths")
        deathsList.forEach { deathMap ->
            runCatching {
                val locationMap = deathMap["location"] as? Map<*, *> ?: return@forEach
                data.deaths.add(
                    DeathRecord(
                        timestamp = Instant.parse(deathMap["timestamp"] as String),
                        world = deathMap["world"] as String,
                        cause = deathMap["cause"] as String,
                        killer = (deathMap["killer"] as? String)?.let { UUID.fromString(it) },
                        location =
                            LocationData(
                                x = (locationMap["x"] as Number).toDouble(),
                                y = (locationMap["y"] as Number).toDouble(),
                                z = (locationMap["z"] as Number).toDouble(),
                            ),
                    ),
                )
            }.onFailure { e ->
                logger.warning("Skipping corrupted death record for $uuid: ${e.message}")
            }
        }

        return data
    }

    private fun getPlayerFile(uuid: UUID) = File(playersFolder, "$uuid.yml")

    fun getAllStoredPlayers(): List<UUID> =
        playersFolder
            .listFiles { f -> f.extension == "yml" }
            ?.mapNotNull { file ->
                runCatching { UUID.fromString(file.nameWithoutExtension) }.getOrNull()
            } ?: emptyList()

    fun getActiveBans(): List<UUID> =
        getAllStoredPlayers().filter { uuid ->
            // Use load() directly without polluting the cache
            val data = cache[uuid] ?: load(uuid)
            data?.isBanned() == true
        }

    // Pending bans (crash recovery)
    fun addPendingBan(uuid: UUID) {
        if (pendingBans.add(uuid)) savePendingBansAsync()
    }

    fun removePendingBan(uuid: UUID) {
        if (pendingBans.remove(uuid)) savePendingBansAsync()
    }

    fun getPendingBans(): Set<UUID> = pendingBans.toSet()

    private fun savePendingBansAsync() {
        pendingBansDirty = true
        val revision = pendingBansRevision.incrementAndGet()
        val snapshot = pendingBans.toList()
        plugin?.let { p ->
            p.server.scheduler.runTaskAsynchronously(
                p,
                Runnable {
                    try {
                        persistPendingBans(revision, snapshot)
                    } catch (e: Exception) {
                        logger.severe("Failed to async-save pending bans: ${e.message}")
                    }
                },
            )
        } ?: persistPendingBans(revision, snapshot)
    }

    private fun persistPendingBans(
        revision: Long,
        snapshot: List<UUID>,
    ) {
        synchronized(pendingBansLock) {
            if (pendingBansRevision.get() != revision) return
            writePendingBans(snapshot)
            if (pendingBansRevision.get() == revision) {
                pendingBansDirty = false
            }
        }
    }

    private fun writePendingBans(uuids: List<UUID>) {
        val config = YamlConfiguration()
        config.set("pending", uuids.map { it.toString() })
        config.save(pendingBansFile)
    }

    private fun loadPendingBans() {
        if (!pendingBansFile.exists()) return
        val config = YamlConfiguration.loadConfiguration(pendingBansFile)
        config
            .getStringList("pending")
            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            .forEach { pendingBans.add(it) }

        if (pendingBans.isNotEmpty()) {
            logger.warning("Recovered ${pendingBans.size} pending bans from previous session")
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
