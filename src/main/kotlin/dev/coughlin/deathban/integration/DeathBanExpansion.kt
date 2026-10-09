package dev.coughlin.deathban.integration

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.BanMode
import dev.coughlin.deathban.data.PlayerData
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.time.Duration
import java.time.Instant

class DeathBanExpansion(
    private val plugin: DeathBanPlugin,
) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "deathban"

    override fun getAuthor(): String = "kevintcoughlin"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onRequest(
        player: OfflinePlayer?,
        params: String,
    ): String? {
        // Pool state and settings belong to the server thread; never block or read files here.
        if (player == null || !Bukkit.isPrimaryThread()) return null
        val data = plugin.dataManager.getCached(player.uniqueId) ?: PlayerData(player.uniqueId)
        val manager = plugin.sharedLivesManager
        val pool = manager?.getPoolForPlayer(player.uniqueId) ?: manager?.getGlobalPool()
        val seconds = data.currentBan?.let { Duration.between(Instant.now(), it.endTime).seconds.coerceAtLeast(0) } ?: 0
        return when (params.lowercase()) {
            "remaining_lives" ->
                if (plugin.settings.mode == BanMode.SHARED) {
                    (pool?.lives ?: 0).toString()
                } else {
                    plugin.offenseManager.getRemainingLives(data).toString()
                }
            "offense_level" -> data.offenseLevel.toString()
            "ban_remaining_seconds" -> seconds.toString()
            "ban_remaining" ->
                dev.coughlin.deathban.util.TimeUtil
                    .formatDuration(Duration.ofSeconds(seconds))
            "team" -> pool?.id?.takeUnless { it == "global" } ?: "none"
            "pool_lives" -> (pool?.lives ?: 0).toString()
            "pool_max_lives" -> (pool?.maxLives ?: 0).toString()
            else -> null
        }
    }
}
