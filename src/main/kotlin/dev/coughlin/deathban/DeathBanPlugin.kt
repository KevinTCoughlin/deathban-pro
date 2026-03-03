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
import dev.coughlin.deathban.theme.ThemeManager
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

    lateinit var themeManager: ThemeManager
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

        // Initialize theme manager
        themeManager = ThemeManager(dataFolder, logger)
        if (!themeManager.setActiveTheme(settings.themeId)) {
            logger.warning("Theme '${settings.themeId}' not found, using default")
        }

        // Initialize data manager
        dataManager = PlayerDataManager(dataFolder, logger, this)

        // Initialize managers
        offenseManager = OffenseManager(settings)
        banManager = BanManager(this, settings, messages, dataManager)

        // Initialize shared lives manager if in shared mode
        if (settings.mode == BanMode.SHARED) {
            sharedLivesManager =
                SharedLivesManager(
                    dataFolder,
                    logger,
                    settings.sharedLivesDefault,
                    settings.sharedLivesMax,
                    this,
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
        logger.info("Theme: ${themeManager.getActiveTheme().name}")
        if (settings.mode == BanMode.INDIVIDUAL) {
            logger.info("Rolling window: ${if (settings.rollingWindowEnabled) "enabled" else "disabled"}")
            logger.info("Max deaths before ban: ${settings.maxDeathsInWindow}")
        } else {
            logger.info("Shared lives: ${settings.sharedLivesDefault}/${settings.sharedLivesMax}")
        }
    }

    override fun onDisable() {
        // Flush any dirty data still in memory to disk before shutdown
        dataManager.saveAll()
        sharedLivesManager?.saveAll()
        dataManager.clearCache()
        logger.info("DeathBan Pro disabled")
    }

    fun reload() {
        reloadConfig()
        settings = Settings(config)
        messages.reload()
        themeManager.reload()
        themeManager.setActiveTheme(settings.themeId)
        dataManager.clearCache()
        sharedLivesManager?.reload()
    }

    private fun registerListeners() {
        val deathListener =
            DeathListener(
                this,
                settings,
                messages,
                dataManager,
                offenseManager,
                banManager,
                sharedLivesManager,
            )
        joinListener = JoinListener(this, messages, dataManager)

        server.pluginManager.registerEvents(deathListener, this)
        server.pluginManager.registerEvents(joinListener, this)
    }

    private fun registerCommands() {
        val command = getCommand("deathban") ?: return
        val executor = DeathBanCommand(this, messages, dataManager, offenseManager, banManager)
        val tabCompleter = DeathBanTabCompleter(this, dataManager, banManager)

        command.setExecutor(executor)
        command.tabCompleter = tabCompleter
    }

    private fun initializeMetrics() {
        if (BSTATS_PLUGIN_ID == 0) {
            logger.info("bStats metrics disabled (plugin ID not configured)")
            return
        }

        val metrics = Metrics(this, BSTATS_PLUGIN_ID)

        metrics.addCustomChart(
            SimplePie("ban_mode") {
                settings.mode.name.lowercase()
            },
        )

        metrics.addCustomChart(
            SimplePie("active_theme") {
                themeManager.getActiveThemeId()
            },
        )

        metrics.addCustomChart(
            SimplePie("rolling_window") {
                if (settings.rollingWindowEnabled) "enabled" else "disabled"
            },
        )

        metrics.addCustomChart(
            SimplePie("max_deaths_threshold") {
                settings.maxDeathsInWindow.toString()
            },
        )

        metrics.addCustomChart(
            SingleLineChart("total_bans") {
                banManager.getTotalBansIssued()
            },
        )

        logger.info("bStats metrics initialized")
    }

    private fun checkForUpdates() {
        if (SPIGOT_RESOURCE_ID == 0) {
            logger.info("Update check disabled (SpigotMC resource ID not configured)")
            return
        }

        // Skip update check for SNAPSHOT/beta versions during development
        if (description.version.contains("SNAPSHOT") || description.version.contains("beta")) {
            logger.info("Skipping update check for development version")
            return
        }

        UpdateChecker(this, SPIGOT_RESOURCE_ID) { current, latest ->
            updateNotification =
                ColorUtil.colorize(
                    "&e[DeathBan Pro] &fUpdate available: &a$latest &f(running &c$current&f)",
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
        /**
         * bStats plugin ID. Register at https://bstats.org/getting-started
         * Set to 0 to disable metrics reporting.
         */
        const val BSTATS_PLUGIN_ID = 0

        /**
         * SpigotMC resource ID for update checking.
         * Set after publishing to SpigotMC. Set to 0 to disable update checks.
         */
        const val SPIGOT_RESOURCE_ID = 0
    }
}
