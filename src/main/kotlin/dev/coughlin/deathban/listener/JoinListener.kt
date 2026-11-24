package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class JoinListener(
    private val plugin: Plugin,
    private val messages: Messages,
    private val dataManager: PlayerDataManager,
    var updateNotification: String? = null,
) : Listener {
    private val expiredBanReturns = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Pre-load player data from disk on the async login thread so that
     * the subsequent [onPlayerLogin] (main thread) never touches the filesystem.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onAsyncPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        dataManager.preload(event.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val data = dataManager.get(event.player.uniqueId) ?: return

        // Track whether a ban existed before clearing
        val hadBan = data.currentBan != null

        // Clear any expired bans first
        data.clearExpiredBan()

        // Check if still banned
        val ban = data.currentBan
        if (ban != null && data.isBanned()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                messages.getKickMessage(ban),
            )
            return
        }

        // If ban was just cleared as expired, persist the cleanup
        if (hadBan && data.currentBan == null) {
            expiredBanReturns.add(event.player.uniqueId)
            dataManager.saveAsync(data)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerId = player.uniqueId
        val data = dataManager.get(playerId)

        // Show return message if ban just expired
        if (data != null && expiredBanReturns.remove(playerId)) {
            later(playerId, 20L) { online ->
                online.sendTitle(
                    messages.getReturnTitle(),
                    messages.getReturnSubtitle(),
                    10,
                    70,
                    20,
                )
                online.sendMessage(messages.getReturnMessage())
            }
        }

        // Notify of pardon (set by admin while offline)
        if (data?.pendingPardon == true) {
            data.pendingPardon = false
            dataManager.saveAsync(data)

            later(playerId, 40L) { online ->
                online.sendMessage(messages.getPardonNotification())
            }
        }

        // Notify ops of updates
        val notice = updateNotification
        if (player.hasPermission("deathban.admin") && notice != null) {
            later(playerId, 60L) { online ->
                online.sendMessage(notice)
            }
        }
    }

    private fun later(
        playerId: UUID,
        delayTicks: Long,
        action: (Player) -> Unit,
    ) {
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                val online = plugin.server.getPlayer(playerId) ?: return@Runnable
                action(online)
            },
            delayTicks,
        )
    }
}
