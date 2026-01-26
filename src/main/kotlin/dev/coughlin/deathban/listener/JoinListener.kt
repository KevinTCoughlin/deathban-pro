package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.plugin.Plugin

class JoinListener(
    private val plugin: Plugin,
    private val messages: Messages,
    private val dataManager: PlayerDataManager,
    var updateNotification: String? = null
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val data = dataManager.get(event.player.uniqueId) ?: return

        // Clear any expired bans first
        data.clearExpiredBan()

        // Check if still banned
        val ban = data.currentBan
        if (ban != null && data.isBanned()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                messages.getKickMessage(ban)
            )
            return
        }

        // If ban expired, save the cleared state
        if (ban != null && !data.isBanned()) {
            dataManager.save(data)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val data = dataManager.get(player.uniqueId) ?: return

        // Show return message if ban just expired
        if (data.currentBan == null && data.deaths.isNotEmpty()) {
            // Check if they were recently banned (within last login)
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    player.sendTitle(
                        messages.getReturnTitle(),
                        messages.getReturnSubtitle(),
                        10, 70, 20
                    )
                    player.sendMessage(messages.getReturnMessage())
                }
            }, 20L)
        }

        // Notify of pardon (set by admin while offline)
        if (data.pendingPardon) {
            data.pendingPardon = false
            dataManager.save(data)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    player.sendMessage(messages.getPardonNotification())
                }
            }, 40L)
        }

        // Notify ops of updates
        if (player.hasPermission("deathban.admin") && updateNotification != null) {
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    player.sendMessage(updateNotification!!)
                }
            }, 60L)
        }
    }
}
