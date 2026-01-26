package dev.coughlin.deathban.manager

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.BanRecord
import dev.coughlin.deathban.data.PlayerData
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.theme.BanContext
import dev.coughlin.deathban.util.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class BanManager(
    private val plugin: DeathBanPlugin,
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

        val theme = plugin.themeManager.getActiveTheme()

        // Show title
        player.sendTitle(
            theme.getBanTitle(),
            theme.getBanSubtitle(TimeUtil.formatDuration(duration)),
            10, 70, 20
        )

        // Play sound if enabled
        if (settings.themeSoundsEnabled) {
            theme.getDeathSound()?.let { sound ->
                player.playSound(player.location, sound, 1.0f, 1.0f)
            }
        }

        // Show particles if enabled
        if (settings.themeParticlesEnabled) {
            theme.getDeathParticle()?.let { particle ->
                player.world.spawnParticle(particle, player.location, 30, 0.5, 1.0, 0.5)
            }
        }

        // Schedule kick after respawn sequence completes
        val context = BanContext(
            playerName = player.name,
            duration = TimeUtil.formatDuration(duration),
            offenseLevel = data.offenseLevel,
            deathCause = deathCause,
            returnTime = TimeUtil.formatInstant(ban.endTime)
        )

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                player.kickPlayer(theme.getKickMessage(context))
            }
        }, 60L) // 3 seconds after death

        plugin.logger.info("Banned ${player.name} for ${TimeUtil.formatDuration(duration)} (offense level ${data.offenseLevel})")
    }

    fun applySharedBan(player: Player, deathCause: String) {
        val duration = settings.sharedLivesEmptyPoolBan
        val now = Instant.now()

        val ban = BanRecord(
            startTime = now,
            endTime = now.plus(duration),
            offenseLevel = 0, // Not used in shared mode
            deathCause = deathCause
        )

        val data = dataManager.getOrCreate(player.uniqueId)
        data.currentBan = ban
        dataManager.save(data)
        totalBansIssued.incrementAndGet()

        val theme = plugin.themeManager.getActiveTheme()

        // Show title
        player.sendTitle(
            theme.getBanTitle(),
            theme.getBanSubtitle(TimeUtil.formatDuration(duration)),
            10, 70, 20
        )

        // Play sound if enabled
        if (settings.themeSoundsEnabled) {
            theme.getDeathSound()?.let { sound ->
                player.playSound(player.location, sound, 1.0f, 1.0f)
            }
        }

        // Show particles if enabled
        if (settings.themeParticlesEnabled) {
            theme.getDeathParticle()?.let { particle ->
                player.world.spawnParticle(particle, player.location, 30, 0.5, 1.0, 0.5)
            }
        }

        // Build context for kick message
        val pool = plugin.sharedLivesManager?.getGlobalPool()
        val context = BanContext(
            playerName = player.name,
            duration = TimeUtil.formatDuration(duration),
            offenseLevel = 0,
            deathCause = deathCause,
            returnTime = TimeUtil.formatInstant(ban.endTime),
            isSharedMode = true,
            poolLives = pool?.lives ?: 0,
            poolMax = pool?.maxLives ?: 0
        )

        // Schedule kick after respawn sequence completes
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                player.kickPlayer(theme.getKickMessage(context))
            }
        }, 60L)

        plugin.logger.info("Banned ${player.name} for ${TimeUtil.formatDuration(duration)} (shared pool empty)")
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
