package dev.coughlin.deathban

import dev.coughlin.deathban.command.DeathBanCommand
import dev.coughlin.deathban.command.DeathBanTabCompleter
import dev.coughlin.deathban.config.BanMode
import dev.coughlin.deathban.config.Messages
import dev.coughlin.deathban.config.Settings
import dev.coughlin.deathban.data.PlayerDataManager
import dev.coughlin.deathban.listener.DeathListener
import dev.coughlin.deathban.listener.JoinListener
import dev.coughlin.deathban.manager.BanManager
import dev.coughlin.deathban.manager.OffenseManager
import dev.coughlin.deathban.manager.SharedLivesManager
import dev.coughlin.deathban.util.ColorUtil
import dev.coughlin.deathban.util.UpdateChecker
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bstats.charts.SingleLineChart
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class DeathBanPlugin : JavaPlugin() {

    lateinit var settings: Settings
        private set

    lateinit var messages: Messages
        private set

    lateinit var dataManager: PlayerDataManager
        private set

    lateinit var offenseManager: OffenseManager
        private set

    lateinit var banManager: BanManager
        private set

    var sharedLivesManager: SharedLivesManager? = null
        private set

    private lateinit var joinListener: JoinListener
    private var updateNotification: String? = null

    override fun onEnable() {
        // Save default configs
        saveDefaultConfig()
        saveResource("messages.yml", false)

        // Initialize configuration
        settings = Settings(config)
        messages = Messages(File(dataFolder, "messages.yml"))

        // Initialize data manager
        dataManager = PlayerDataManager(dataFolder, logger)

        // Initialize managers
        offenseManager = OffenseManager(settings)
        banManager = BanManager(this, settings, messages, dataManager)

        // Initialize shared lives manager if in shared mode
        if (settings.mode == BanMode.SHARED) {
            sharedLivesManager = SharedLivesManager(
                dataFolder, logger,
                settings.sharedLivesDefault,
                settings.sharedLivesMax
            )
        }

        // Register listeners
        registerListeners()

        // Register commands
        registerCommands()

        // Initialize metrics
        if (settings.metrics) {
            initializeMetrics()
        }

        // Check for updates
        if (settings.updateCheck) {
            checkForUpdates()
        }

        // Process any pending bans from crash recovery
        processPendingBans()

        logger.info("DeathBan Pro v${description.version} enabled!")
        logger.info("Mode: ${settings.mode.name.lowercase()}")
        if (settings.mode == BanMode.INDIVIDUAL) {
            logger.info("Rolling window: ${if (settings.rollingWindowEnabled) "enabled" else "disabled"}")
            logger.info("Max deaths before ban: ${settings.maxDeathsInWindow}")
        } else {
            logger.info("Shared lives: ${settings.sharedLivesDefault}/${settings.sharedLivesMax}")
        }
    }

    override fun onDisable() {
        dataManager.clearCache()
        logger.info("DeathBan Pro disabled")
    }

    fun reload() {
        reloadConfig()
        settings = Settings(config)
        messages.reload()
        dataManager.clearCache()
    }

    private fun registerListeners() {
        val deathListener = DeathListener(
            this, settings, messages, dataManager, offenseManager, banManager, sharedLivesManager
        )
        joinListener = JoinListener(this, messages, dataManager)

        server.pluginManager.registerEvents(deathListener, this)
        server.pluginManager.registerEvents(joinListener, this)
    }

    private fun registerCommands() {
        val command = getCommand("deathban") ?: return
        val executor = DeathBanCommand(this, messages, dataManager, offenseManager, banManager)
        val tabCompleter = DeathBanTabCompleter(dataManager, banManager)

        command.setExecutor(executor)
        command.tabCompleter = tabCompleter
    }

    private fun initializeMetrics() {
        val metrics = Metrics(this, BSTATS_PLUGIN_ID)

        metrics.addCustomChart(SimplePie("ban_mode") {
            settings.mode.name.lowercase()
        })

        metrics.addCustomChart(SimplePie("rolling_window") {
            if (settings.rollingWindowEnabled) "enabled" else "disabled"
        })

        metrics.addCustomChart(SimplePie("max_deaths_threshold") {
            settings.maxDeathsInWindow.toString()
        })

        metrics.addCustomChart(SingleLineChart("total_bans") {
            banManager.getTotalBansIssued()
        })

        logger.info("bStats metrics initialized")
    }

    private fun checkForUpdates() {
        // Skip update check for SNAPSHOT versions during development
        if (description.version.contains("SNAPSHOT")) {
            logger.info("Skipping update check for SNAPSHOT version")
            return
        }

        UpdateChecker(this, SPIGOT_RESOURCE_ID) { current, latest ->
            updateNotification = ColorUtil.colorize(
                "&e[DeathBan Pro] &fUpdate available: &a$latest &f(running &c$current&f)"
            )
            logger.warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            logger.warning("  DeathBan Pro Update Available!")
            logger.warning("  Current: $current → Latest: $latest")
            logger.warning("  Download: https://spigotmc.org/resources/$SPIGOT_RESOURCE_ID")
            logger.warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            joinListener.updateNotification = updateNotification
        }.checkForUpdates()
    }

    private fun processPendingBans() {
        val pending = dataManager.getPendingBans()
        if (pending.isEmpty()) return

        logger.info("Processing ${pending.size} pending bans from previous session...")

        for (uuid in pending) {
            val player = server.getPlayer(uuid)
            if (player != null && player.isOnline) {
                val data = dataManager.get(uuid)
                if (data?.isBanned() == true) {
                    player.kickPlayer(messages.getKickMessage(data.currentBan!!))
                }
            }
            dataManager.removePendingBan(uuid)
        }
    }

    companion object {
        // Replace with actual IDs after SpigotMC submission
        const val BSTATS_PLUGIN_ID = 0
        const val SPIGOT_RESOURCE_ID = 0
    }
}
