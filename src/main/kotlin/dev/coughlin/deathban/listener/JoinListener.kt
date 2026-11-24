package dev.coughlin.deathban.listener

import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.util.ColorUtil
import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.connection.PlayerConnection
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class JoinListener(
    private val plugin: Plugin,
    private val messages: Messages,
    private val dataManager: PlayerDataManager,
    var updateNotification: Component? = null,
) : Listener {
    private val expiredBanReturns = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Pre-load player data on the async login thread so the connection
     * validate handler never touches the filesystem.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onAsyncPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult != AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        dataManager.preload(event.uniqueId)
    }

    /**
     * Deny an active death ban before the player enters the world.
     *
     * Paper calls this during login and again when configuration finishes, both
     * after [onAsyncPreLogin]. Expired-ban cleanup is idempotent, so the second
     * call does not record history or the return title twice. This does not call
     * [PlayerConnectionValidateLoginEvent.allow], which would skip the server's own checks.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onConnectionValidate(event: PlayerConnectionValidateLoginEvent) {
        val playerId = playerId(event.connection) ?: return
        val data = dataManager.get(playerId) ?: return

        val hadBan = data.currentBan != null
        data.clearExpiredBan()

        val ban = data.currentBan
        if (ban != null && data.isBanned()) {
            event.kickMessage(messages.getKickMessage(ban))
            return
        }

        if (hadBan && data.currentBan == null) {
            expiredBanReturns.add(playerId)
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
                online.showTitle(ColorUtil.title(messages.getReturnTitle(), messages.getReturnSubtitle()))
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

    private fun playerId(connection: PlayerConnection): UUID? {
        val profile =
            when (connection) {
                is PlayerLoginConnection -> connection.authenticatedProfile ?: connection.unsafeProfile
                is PlayerConfigurationConnection -> connection.profile
                else -> null
            }
        return profile?.id
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
