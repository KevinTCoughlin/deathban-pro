package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
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
        val data = dataManager.get(player.uniqueId)

        // Show return message if ban just expired
        if (data != null && expiredBanReturns.remove(player.uniqueId)) {
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    if (player.isOnline) {
                        player.sendTitle(
                            messages.getReturnTitle(),
                            messages.getReturnSubtitle(),
                            10,
                            70,
                            20,
                        )
                        player.sendMessage(messages.getReturnMessage())
                    }
                },
                20L,
            )
        }

        // Notify of pardon (set by admin while offline)
        if (data?.pendingPardon == true) {
            data.pendingPardon = false
            dataManager.saveAsync(data)

            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    if (player.isOnline) {
                        player.sendMessage(messages.getPardonNotification())
                    }
                },
                40L,
            )
        }

        // Notify ops of updates
        if (player.hasPermission("deathban.admin") && updateNotification != null) {
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    if (player.isOnline) {
                        player.sendMessage(updateNotification!!)
                    }
                },
                60L,
            )
        }
    }
}
