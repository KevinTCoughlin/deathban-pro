package dev.coughlin.deathban.data

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class PlayerDataManager(
    private val dataFolder: File,
    private val logger: Logger
) {
    private val playersFolder = File(dataFolder, "players")
    private val cache = ConcurrentHashMap<UUID, PlayerData>()
    private val pendingBans = ConcurrentHashMap.newKeySet<UUID>()
    private val pendingBansFile = File(dataFolder, "pending-bans.yml")

    init {
        playersFolder.mkdirs()
        loadPendingBans()
    }

    fun getOrCreate(uuid: UUID): PlayerData =
        cache.getOrPut(uuid) { load(uuid) ?: PlayerData(uuid) }

    fun get(uuid: UUID): PlayerData? =
        cache[uuid] ?: load(uuid)?.also { cache[uuid] = it }

    fun save(data: PlayerData) {
        cache[data.uuid] = data
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

        val deathsList = data.deaths.map { death ->
            mapOf(
                "timestamp" to death.timestamp.toString(),
                "world" to death.world,
                "cause" to death.cause,
                "killer" to death.killer?.toString(),
                "location" to mapOf(
                    "x" to death.location.x,
                    "y" to death.location.y,
                    "z" to death.location.z
                )
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
        data.lastDeathTime = config.getString("last-death-time")?.let { Instant.parse(it) }
        data.pendingPardon = config.getBoolean("pending-pardon", false)

        if (config.contains("current-ban")) {
            data.currentBan = BanRecord(
                startTime = Instant.parse(config.getString("current-ban.start-time")),
                endTime = Instant.parse(config.getString("current-ban.end-time")),
                offenseLevel = config.getInt("current-ban.offense-level"),
                deathCause = config.getString("current-ban.death-cause") ?: "UNKNOWN"
            )
        }

        @Suppress("UNCHECKED_CAST")
        val deathsList = config.getList("deaths") as? List<Map<String, Any>> ?: emptyList()
        deathsList.forEach { deathMap ->
            val locationMap = deathMap["location"] as? Map<String, Any> ?: return@forEach
            data.deaths.add(
                DeathRecord(
                    timestamp = Instant.parse(deathMap["timestamp"] as String),
                    world = deathMap["world"] as String,
                    cause = deathMap["cause"] as String,
                    killer = (deathMap["killer"] as? String)?.let { UUID.fromString(it) },
                    location = LocationData(
                        x = (locationMap["x"] as Number).toDouble(),
                        y = (locationMap["y"] as Number).toDouble(),
                        z = (locationMap["z"] as Number).toDouble()
                    )
                )
            )
        }

        return data
    }

    private fun getPlayerFile(uuid: UUID) = File(playersFolder, "$uuid.yml")

    fun getAllStoredPlayers(): List<UUID> =
        playersFolder.listFiles { f -> f.extension == "yml" }
            ?.mapNotNull { file ->
                runCatching { UUID.fromString(file.nameWithoutExtension) }.getOrNull()
            } ?: emptyList()

    fun getActiveBans(): List<UUID> =
        getAllStoredPlayers().filter { uuid ->
            get(uuid)?.isBanned() == true
        }

    // Pending bans (crash recovery)
    fun addPendingBan(uuid: UUID) {
        pendingBans.add(uuid)
        savePendingBans()
    }

    fun removePendingBan(uuid: UUID) {
        pendingBans.remove(uuid)
        savePendingBans()
    }

    fun getPendingBans(): Set<UUID> = pendingBans.toSet()

    private fun savePendingBans() {
        val config = YamlConfiguration()
        config.set("pending", pendingBans.map { it.toString() })
        config.save(pendingBansFile)
    }

    private fun loadPendingBans() {
        if (!pendingBansFile.exists()) return
        val config = YamlConfiguration.loadConfiguration(pendingBansFile)
        config.getStringList("pending")
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
