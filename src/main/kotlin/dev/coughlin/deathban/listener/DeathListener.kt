package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.BanMode
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.DeathRecord
import dev.coughlin.deathban.data.LocationData
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.manager.BanManager
import dev.coughlin.deathban.manager.OffenseManager
import dev.coughlin.deathban.manager.SharedLivesManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DeathListener(
    private val plugin: Plugin,
    private val settings: Settings,
    private val messages: Messages,
    private val dataManager: PlayerDataManager,
    private val offenseManager: OffenseManager,
    private val banManager: BanManager,
    private val sharedLivesManager: SharedLivesManager?,
) : Listener {
    private val processingDeaths = ConcurrentHashMap.newKeySet<UUID>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        // Check bypass permission
        if (player.hasPermission(settings.bypassPermission)) {
            debug("${player.name} has bypass permission, skipping")
            return
        }

        // Check world
        if (!settings.isWorldEnabled(player.world.name)) {
            debug("World ${player.world.name} is disabled, skipping")
            return
        }

        // Prevent concurrent processing
        if (!processingDeaths.add(player.uniqueId)) {
            plugin.logger.warning("Skipping duplicate death event for ${player.name}")
            return
        }

        try {
            processDeath(player, event)
        } finally {
            // Clear processing flag after a delay to handle respawn
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    processingDeaths.remove(player.uniqueId)
                },
                100L,
            ) // 5 seconds
        }
    }

    private fun processDeath(
        player: Player,
        event: PlayerDeathEvent,
    ) {
        val deathCause =
            event.entity.lastDamageCause
                ?.cause
                ?.name ?: "UNKNOWN"

        when (settings.mode) {
            BanMode.INDIVIDUAL -> processIndividualDeath(player, event, deathCause)
            BanMode.SHARED -> processSharedDeath(player, deathCause)
        }
    }

    private fun processIndividualDeath(
        player: Player,
        event: PlayerDeathEvent,
        deathCause: String,
    ) {
        dataManager.addPendingBan(player.uniqueId)

        val data = dataManager.getOrCreate(player.uniqueId)

        // Check for offense reset before processing
        if (offenseManager.checkOffenseReset(data)) {
            debug("Reset offense level for ${player.name} due to clean period")
        }

        // Prune old deaths outside rolling window
        offenseManager.pruneOldDeaths(data)

        val killer = event.entity.killer?.uniqueId

        // Record death
        val deathRecord =
            DeathRecord(
                timestamp = Instant.now(),
                world = player.world.name,
                cause = deathCause,
                killer = killer,
                location = LocationData.from(player.location),
            )
        data.deaths.add(deathRecord)
        data.lastDeathTime = Instant.now()

        debug("Recorded death for ${player.name}: $deathCause in ${player.world.name}")

        // Check if ban should be triggered
        if (offenseManager.shouldTriggerBan(data)) {
            data.offenseLevel++
            banManager.applyBan(player, data, deathCause)
        } else {
            // Save data and clear pending
            dataManager.saveAsync(data)
            dataManager.removePendingBan(player.uniqueId)

            // Warn player about remaining lives
            val remaining = offenseManager.getRemainingLives(data)
            if (remaining == 1) {
                player.sendMessage(messages.getWarningFinalLife())
            } else if (remaining <= 2) {
                player.sendMessage(messages.getWarningNearLimit(remaining))
            }
        }
    }

    private fun processSharedDeath(
        player: Player,
        deathCause: String,
    ) {
        val manager = sharedLivesManager ?: return

        // Try to consume a life from the pool
        if (manager.consumeLife(player.uniqueId)) {
            val pool = manager.getPoolForPlayer(player.uniqueId) ?: manager.getGlobalPool()
            player.sendMessage(messages.getLifeConsumed(pool.lives, pool.maxLives))

            // Warn if pool is getting low
            if (pool.lives == 1) {
                player.sendMessage(messages.getWarningFinalLife())
            } else if (pool.lives <= 3) {
                player.sendMessage(messages.getWarningNearLimit(pool.lives))
            }

            debug("Shared life consumed for ${player.name}. Pool: ${pool.lives}/${pool.maxLives}")
        } else {
            // Pool is empty - apply ban
            player.sendMessage(messages.getPoolEmpty())
            banManager.applySharedBan(player, deathCause)
            debug("Shared pool empty - banning ${player.name}")
        }
    }

    private fun debug(message: String) {
        if (settings.debug) {
            plugin.logger.info("[DEBUG] $message")
        }
    }
}
