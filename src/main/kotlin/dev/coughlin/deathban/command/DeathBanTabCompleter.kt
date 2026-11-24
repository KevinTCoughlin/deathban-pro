package dev.coughlin.deathban.command

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.data.SharedLivesPool
import dev.coughlin.deathban.manager.BanManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.IllegalPluginAccessException

class DeathBanTabCompleter(
    private val plugin: DeathBanPlugin,
    private val dataManager: PlayerDataManager,
    private val banManager: BanManager,
) : TabCompleter {
    @Volatile private var cachedStoredNames: List<String> = emptyList()

    @Volatile private var cachedBannedNames: List<String> = emptyList()

    @Volatile private var lastCacheRefresh: Long = 0L

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String> =
        when (args.size) {
            1 -> filterStartsWith(getSubcommands(sender), args[0])
            2 -> filterStartsWith(getSecondArg(sender, args[0]), args[1])
            3 -> filterStartsWith(getThirdArg(sender, args[0], args[1]), args[2])
            else -> emptyList()
        }

    private fun getSubcommands(sender: CommandSender): List<String> =
        buildList {
            add("help")
            if (sender.hasPermission("deathban.check")) add("check")
            if (sender.hasPermission("deathban.use")) {
                add("theme")
                if (plugin.sharedLivesManager != null) {
                    add("lives")
                    add("team")
                }
            }
            if (sender.hasPermission("deathban.admin")) {
                add("history")
                add("reset")
                add("pardon")
                add("reload")
            }
        }

    private fun getSecondArg(
        sender: CommandSender,
        subcommand: String,
    ): List<String> =
        when (subcommand.lowercase()) {
            "check" ->
                if (sender.hasPermission("deathban.check.others")) {
                    (getOnlinePlayerNames() + getStoredPlayerNames()).distinct()
                } else {
                    emptyList()
                }

            "reset", "history" ->
                if (sender.hasPermission("deathban.admin")) {
                    getOnlinePlayerNames() + getStoredPlayerNames()
                } else {
                    emptyList()
                }

            "pardon" ->
                if (sender.hasPermission("deathban.admin")) {
                    getBannedPlayerNames()
                } else {
                    emptyList()
                }

            "lives" ->
                if (plugin.sharedLivesManager == null) {
                    emptyList()
                } else {
                    buildList {
                        if (sender.hasPermission("deathban.use") && sender.hasPermission("deathban.lives.add")) add("add")
                        if (sender.hasPermission("deathban.use") && sender.hasPermission("deathban.admin")) add("set")
                    }
                }

            "team" ->
                if (
                    plugin.sharedLivesManager != null &&
                    sender.hasPermission("deathban.use") &&
                    sender.hasPermission("deathban.team.manage")
                ) {
                    listOf("create", "join", "leave")
                } else {
                    emptyList()
                }

            "theme" ->
                buildList {
                    add("list")
                    if (sender.hasPermission("deathban.admin")) add("set")
                    add("preview")
                }

            else -> emptyList()
        }

    @Suppress("UNUSED_PARAMETER")
    fun getThirdArg(
        sender: CommandSender,
        subcommand: String,
        secondArg: String,
    ): List<String> =
        when (subcommand.lowercase()) {
            "theme" ->
                when (secondArg.lowercase()) {
                    "set", "preview" -> plugin.themeManager.getAvailableThemeIds()
                    else -> emptyList()
                }
            "team" ->
                if (secondArg.equals("join", ignoreCase = true) && sender.hasPermission("deathban.team.manage")) {
                    plugin.sharedLivesManager
                        ?.getAllPools()
                        ?.map { it.id }
                        ?.filter { it != SharedLivesPool.GLOBAL_POOL_ID }
                        .orEmpty()
                } else {
                    emptyList()
                }
            else -> emptyList()
        }

    private fun getOnlinePlayerNames(): List<String> = Bukkit.getOnlinePlayers().map { it.name }

    /**
     * Returns cached stored player names, refreshing asynchronously if stale.
     * Avoids filesystem I/O on the main thread during tab completion.
     */
    private fun getStoredPlayerNames(): List<String> {
        refreshCacheIfStale()
        return cachedStoredNames.filter { name -> Bukkit.getPlayer(name) == null }
    }

    /**
     * Returns cached banned player names, refreshing asynchronously if stale.
     */
    private fun getBannedPlayerNames(): List<String> {
        refreshCacheIfStale()
        return cachedBannedNames
    }

    private fun refreshCacheIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastCacheRefresh < CACHE_TTL_MS) return
        lastCacheRefresh = now

        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                if (!plugin.isEnabled) return@Runnable
                val storedUuids = dataManager.getAllStoredPlayers()
                val bannedUuids = banManager.getActiveBans()
                runOnMainThread {
                    cachedStoredNames = storedUuids.mapNotNull { Bukkit.getOfflinePlayer(it).name }
                    cachedBannedNames = bannedUuids.mapNotNull { Bukkit.getOfflinePlayer(it).name }
                }
            },
        )
    }

    private fun runOnMainThread(task: Runnable) {
        if (!plugin.isEnabled) return
        try {
            plugin.server.scheduler.runTask(plugin, task)
        } catch (e: IllegalPluginAccessException) {
            plugin.logger.fine("Dropped tab-complete refresh after disable: ${e.message}")
        }
    }

    private fun filterStartsWith(
        options: List<String>,
        prefix: String,
    ): List<String> = options.filter { it.lowercase().startsWith(prefix.lowercase()) }

    companion object {
        private const val CACHE_TTL_MS = 30_000L // 30 seconds
    }
}
