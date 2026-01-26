package dev.coughlin.deathban.manager

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.data.PlayerData
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.util.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class BanManager(
    private val plugin: Plugin,
    private val settings: Settings,
    private val messages: Messages,
    private val dataManager: PlayerDataManager
) {
    private val totalBansIssued = AtomicInteger(0)

    fun applyBan(player: Player, data: PlayerData, deathCause: String) {
        val duration = settings.getBanDuration(data.offenseLevel)
        val now = Instant.now()

        val ban = BanRecord(
            startTime = now,
            endTime = now.plus(duration),
            offenseLevel = data.offenseLevel,
            deathCause = deathCause
        )

        data.currentBan = ban
        dataManager.save(data)
        dataManager.removePendingBan(player.uniqueId)
        totalBansIssued.incrementAndGet()

        // Show title
        player.sendTitle(
            messages.getBanTitle(),
            messages.getBanSubtitle(duration),
            10, 70, 20
        )

        // Schedule kick after respawn sequence completes
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                player.kickPlayer(messages.getKickMessage(ban))
            }
        }, 60L) // 3 seconds after death

        plugin.logger.info("Banned ${player.name} for ${TimeUtil.formatDuration(duration)} (offense level ${data.offenseLevel})")
    }

    fun pardon(uuid: UUID): Boolean {
        val data = dataManager.get(uuid) ?: return false
        if (!data.isBanned()) return false

        data.currentBan = null

        // If player is offline, mark for notification on join
        val player = Bukkit.getPlayer(uuid)
        if (player == null) {
            data.pendingPardon = true
        }

        dataManager.save(data)
        return true
    }

    fun reset(uuid: UUID): Boolean {
        val data = dataManager.get(uuid) ?: return false

        data.offenseLevel = 0
        data.deaths.clear()
        data.currentBan = null
        data.lastDeathTime = null
        data.pendingPardon = false

        dataManager.save(data)
        return true
    }

    fun getActiveBans(): List<UUID> = dataManager.getActiveBans()

    fun getTotalBansIssued(): Int = totalBansIssued.get()
}
