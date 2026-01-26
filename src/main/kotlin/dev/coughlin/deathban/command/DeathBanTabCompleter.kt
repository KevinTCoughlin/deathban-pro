package dev.coughlin.deathban.command

import dev.coughlin.deathban.DeathBanPlugin
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.manager.BanManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class DeathBanTabCompleter(
    private val plugin: DeathBanPlugin,
    private val dataManager: PlayerDataManager,
    private val banManager: BanManager
) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        return when (args.size) {
            1 -> filterStartsWith(getSubcommands(sender), args[0])
            2 -> filterStartsWith(getSecondArg(sender, args[0]), args[1])
            3 -> filterStartsWith(getThirdArg(sender, args[0], args[1]), args[2])
            else -> emptyList()
        }
    }

    private fun getSubcommands(sender: CommandSender): List<String> {
        return buildList {
            add("help")
            if (sender.hasPermission("deathban.check")) add("check")
            if (sender.hasPermission("deathban.use")) {
                add("lives")
                add("team")
                add("theme")
            }
            if (sender.hasPermission("deathban.admin")) {
                add("reset")
                add("pardon")
                add("reload")
            }
        }
    }

    private fun getSecondArg(sender: CommandSender, subcommand: String): List<String> {
        return when (subcommand.lowercase()) {
            "check" -> if (sender.hasPermission("deathban.check.others")) {
                getOnlinePlayerNames()
            } else emptyList()

            "reset" -> if (sender.hasPermission("deathban.admin")) {
                getOnlinePlayerNames() + getStoredPlayerNames()
            } else emptyList()

            "pardon" -> if (sender.hasPermission("deathban.admin")) {
                getBannedPlayerNames()
            } else emptyList()

            "lives" -> listOf("add", "set")

            "team" -> listOf("create", "join", "leave")

            "theme" -> buildList {
                add("list")
                if (sender.hasPermission("deathban.admin")) add("set")
                add("preview")
            }

            else -> emptyList()
        }
    }

    fun getThirdArg(sender: CommandSender, subcommand: String, secondArg: String): List<String> {
        return when (subcommand.lowercase()) {
            "theme" -> when (secondArg.lowercase()) {
                "set", "preview" -> plugin.themeManager.getAvailableThemeIds()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getOnlinePlayerNames(): List<String> =
        Bukkit.getOnlinePlayers().map { it.name }

    private fun getStoredPlayerNames(): List<String> =
        dataManager.getAllStoredPlayers()
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }
            .filter { name -> Bukkit.getPlayer(name) == null }

    private fun getBannedPlayerNames(): List<String> =
        banManager.getActiveBans()
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }

    private fun filterStartsWith(options: List<String>, prefix: String): List<String> =
        options.filter { it.lowercase().startsWith(prefix.lowercase()) }
}
