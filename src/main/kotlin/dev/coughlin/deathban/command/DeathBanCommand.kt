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
    private val banManager: BanManager
) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            return handleHelp(sender)
        }

        when (args[0].lowercase()) {
            "help" -> return handleHelp(sender)
            "check" -> return handleCheck(sender, args)
            "reset" -> return handleReset(sender, args)
            "pardon" -> return handlePardon(sender, args)
            "reload" -> return handleReload(sender)
            else -> {
                sender.sendMessage(messages.getInvalidUsage("/deathban <check|reset|pardon|reload>"))
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

    private fun handleCheck(sender: CommandSender, args: Array<String>): Boolean {
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
                sender.sendMessage(messages.getCheckSelf(
                    data.offenseLevel,
                    deathsInWindow,
                    plugin.settings.maxDeathsInWindow
                ))
            }
            return true
        }

        // Check other player
        if (!sender.hasPermission("deathban.check.others")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        val targetName = args[1]
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
            sender.sendMessage(messages.getCheckOther(
                targetName,
                data.offenseLevel,
                deathsInWindow,
                plugin.settings.maxDeathsInWindow
            ))
        }
        return true
    }

    private fun handleReset(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(messages.getInvalidUsage("/deathban reset <player>"))
            return true
        }

        val targetName = args[1]
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

    private fun handlePardon(sender: CommandSender, args: Array<String>): Boolean {
        if (!sender.hasPermission("deathban.admin")) {
            sender.sendMessage(messages.getNoPermission())
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(messages.getInvalidUsage("/deathban pardon <player>"))
            return true
        }

        val targetName = args[1]
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
