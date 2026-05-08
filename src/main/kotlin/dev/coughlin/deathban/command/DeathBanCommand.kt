package dev.coughlin.deathban.command

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.manager.BanManager
import dev.coughlin.deathban.manager.OffenseManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DeathBanCommand(
    private val plugin: DeathBanPlugin,
    private val messages: Messages,
    private val dataManager: PlayerDataManager,
    private val offenseManager: OffenseManager,
    private val banManager: BanManager,
) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (args.isEmpty()) {
            return handleHelp(sender)
        }

        when (args[0].lowercase()) {
            "help" -> return handleHelp(sender)
            "check" -> return handleCheck(sender, args)
            "reset" -> return handleReset(sender, args)
            "pardon" -> return handlePardon(sender, args)
            "lives" -> return handleLives(sender, args)
            "team" -> return handleTeam(sender, args)
            "theme" -> return handleTheme(sender, args)
            "reload" -> return handleReload(sender)
            else -> {
                sender.sendMessage(messages.getInvalidUsage("/deathban <check|reset|pardon|lives|theme|reload>"))
                return true
            }
        }
    }

    private fun handleHelp(sender: CommandSender): Boolean {
        if (!sender.hasPermission("deathban.use")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }
        sender.sendMessage(messages.getHelp())
        return true
    }

    private fun handleCheck(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        // Check self
        if (args.size == 1) {
            if (!sender.hasPermission("deathban.check")) {
                sender.sendMessage(messages.getNoPermission())
                return true
            }
            if (sender !is Player) {
                sender.sendMessage(messages.get("errors.console-only-player"))
                return true
            }

            val data = dataManager.getOrCreate(sender.uniqueId)
            val deathsInWindow = offenseManager.getDeathsInWindow(data)

            if (data.isBanned()) {
                sender.sendMessage(messages.getCheckBanned(sender.name, data.currentBan!!))
            } else {
                sender.sendMessage(
                    messages.getCheckSelf(
                        data.offenseLevel,
                        deathsInWindow,
                        plugin.settings.maxDeathsInWindow,
                    ),
                )
            }
            return true
        }

        // Check other player
        if (!sender.hasPermission("deathban.check.others")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        val targetName = args[1]

        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)

        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(messages.getPlayerNotFound(targetName))
            return true
        }

        val data = dataManager.get(target.uniqueId)
        if (data == null) {
            sender.sendMessage(messages.getCheckOther(targetName, 0, 0, plugin.settings.maxDeathsInWindow))
            return true
        }

        val deathsInWindow = offenseManager.getDeathsInWindow(data)

        if (data.isBanned()) {
            sender.sendMessage(messages.getCheckBanned(targetName, data.currentBan!!))
        } else {
            sender.sendMessage(
                messages.getCheckOther(
                    targetName,
                    data.offenseLevel,
                    deathsInWindow,
                    plugin.settings.maxDeathsInWindow,
                ),
            )
        }
        return true
    }

    private fun handleReset(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(messages.getInvalidUsage("/deathban reset <player>"))
            return true
        }

        val targetName = args[1]

        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)

        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(messages.getPlayerNotFound(targetName))
            return true
        }

        banManager.reset(target.uniqueId)
        sender.sendMessage(messages.getResetSuccess(targetName))
        plugin.logger.info("${sender.name} reset offense data for $targetName")
        return true
    }

    private fun handlePardon(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(messages.getInvalidUsage("/deathban pardon <player>"))
            return true
        }

        val targetName = args[1]

        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)

        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(messages.getPlayerNotFound(targetName))
            return true
        }

        if (banManager.pardon(target.uniqueId)) {
            sender.sendMessage(messages.getPardonSuccess(targetName))
            plugin.logger.info("${sender.name} pardoned $targetName")
        } else {
            sender.sendMessage(messages.getPardonNotBanned(targetName))
        }
        return true
    }

    private fun handleLives(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        if (!sender.hasPermission("deathban.use")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        val manager = plugin.sharedLivesManager
        if (manager == null) {
            sender.sendMessage(messages.prefixed("errors.no-permission"))
            return true
        }

        // /deathban lives - show pool status
        if (args.size == 1) {
            val pool =
                if (sender is Player) {
                    manager.getPoolForPlayer(sender.uniqueId) ?: manager.getGlobalPool()
                } else {
                    manager.getGlobalPool()
                }
            sender.sendMessage(messages.getPoolStatus(pool.id, pool.lives, pool.maxLives))
            return true
        }

        when (args[1].lowercase()) {
            "add" -> {
                if (sender !is Player) {
                    sender.sendMessage(messages.get("errors.console-only-player"))
                    return true
                }
                if (manager.addLife(sender.uniqueId)) {
                    val pool = manager.getPoolForPlayer(sender.uniqueId) ?: manager.getGlobalPool()
                    sender.sendMessage(messages.getLifeAdded(pool.lives, pool.maxLives))
                } else {
                    val pool = manager.getPoolForPlayer(sender.uniqueId) ?: manager.getGlobalPool()
                    sender.sendMessage(messages.getPoolFull(pool.maxLives))
                }
            }
            "set" -> {
                if (!sender.hasPermission("deathban.admin")) {
                    sender.sendMessage(messages.getNoPermission())
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban lives set <amount>"))
                    return true
                }
                val amount = args[2].toIntOrNull()
                if (amount == null || amount < 0) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban lives set <amount>"))
                    return true
                }
                val pool = manager.getGlobalPool()
                pool.lives = amount.coerceAtMost(pool.maxLives)
                manager.save()
                sender.sendMessage(messages.getPoolStatus(pool.id, pool.lives, pool.maxLives))
            }
            else -> {
                sender.sendMessage(messages.getInvalidUsage("/deathban lives [add|set <amount>]"))
            }
        }
        return true
    }

    private fun handleTeam(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(messages.get("errors.console-only-player"))
            return true
        }

        if (!sender.hasPermission("deathban.use")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        val manager = plugin.sharedLivesManager
        if (manager == null) {
            sender.sendMessage(messages.prefixed("errors.no-permission"))
            return true
        }

        if (!plugin.settings.sharedLivesAllowTeams) {
            sender.sendMessage(messages.getTeamsDisabled())
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(messages.getInvalidUsage("/deathban team <create|join|leave> [name]"))
            return true
        }

        when (args[1].lowercase()) {
            "create" -> {
                if (args.size < 3) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban team create <name>"))
                    return true
                }
                val teamName = args[2].lowercase()
                val pool = manager.createTeamPool(teamName, sender.uniqueId)
                if (pool != null) {
                    sender.sendMessage(messages.getTeamCreated(teamName))
                } else {
                    sender.sendMessage(messages.getTeamExists())
                }
            }
            "join" -> {
                if (args.size < 3) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban team join <name>"))
                    return true
                }
                val teamName = args[2].lowercase()
                if (manager.getPool(teamName) == null) {
                    sender.sendMessage(messages.getTeamNotFound(teamName))
                    return true
                }
                manager.joinPool(sender.uniqueId, teamName)
                sender.sendMessage(messages.getTeamJoined(teamName))
            }
            "leave" -> {
                if (manager.leavePool(sender.uniqueId)) {
                    sender.sendMessage(messages.getTeamLeft())
                } else {
                    sender.sendMessage(messages.getNoPool())
                }
            }
            else -> {
                sender.sendMessage(messages.getInvalidUsage("/deathban team <create|join|leave> [name]"))
            }
        }
        return true
    }

    private fun handleTheme(
        sender: CommandSender,
        args: Array<String>,
    ): Boolean {
        if (!sender.hasPermission("deathban.use")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        val themeManager = plugin.themeManager

        // /deathban theme - show current theme and list available
        if (args.size == 1) {
            val active = themeManager.getActiveTheme()
            val available = themeManager.getAvailableThemeIds()
            sender.sendMessage(messages.prefixed("theme.current", "theme" to active.name))
            sender.sendMessage(messages.get("theme.available", "themes" to available.joinToString(", ")))
            return true
        }

        when (args[1].lowercase()) {
            "list" -> {
                val themes = themeManager.getAllThemes()
                sender.sendMessage(messages.prefixed("theme.list-header"))
                themes.forEach { theme ->
                    val active = if (theme.id == themeManager.getActiveThemeId()) " &a(active)" else ""
                    sender.sendMessage(
                        messages.get(
                            "theme.list-item",
                            "id" to theme.id,
                            "name" to theme.name,
                            "author" to theme.author,
                            "active" to active,
                        ),
                    )
                }
            }
            "set" -> {
                if (!sender.hasPermission("deathban.admin")) {
                    sender.sendMessage(messages.getNoPermission())
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban theme set <id>"))
                    return true
                }
                val themeId = args[2].lowercase()
                if (themeManager.setActiveTheme(themeId)) {
                    sender.sendMessage(messages.prefixed("theme.set-success", "theme" to themeManager.getActiveTheme().name))
                    plugin.logger.info("${sender.name} changed theme to: $themeId")
                } else {
                    sender.sendMessage(messages.prefixed("theme.not-found", "id" to themeId))
                }
            }
            "preview" -> {
                if (sender !is Player) {
                    sender.sendMessage(messages.get("errors.console-only-player"))
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(messages.getInvalidUsage("/deathban theme preview <id>"))
                    return true
                }
                val themeId = args[2].lowercase()
                val theme = themeManager.getTheme(themeId)
                if (theme == null) {
                    sender.sendMessage(messages.prefixed("theme.not-found", "id" to themeId))
                    return true
                }
                // Show preview
                sender.sendTitle(theme.getBanTitle(), theme.getBanSubtitle("1h 30m"), 10, 70, 20)
                sender.sendMessage(messages.prefixed("theme.preview", "theme" to theme.name))
            }
            else -> {
                sender.sendMessage(messages.getInvalidUsage("/deathban theme [list|set <id>|preview <id>]"))
            }
        }
        return true
    }

    private fun handleReload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        plugin.reload()
        sender.sendMessage(messages.getReloadSuccess())
        plugin.logger.info("Configuration reloaded by ${sender.name}")
        return true
    }
}
