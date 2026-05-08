# DeathBan Pro - Technical Specification v2.0

## Overview

DeathBan Pro is a Minecraft plugin that implements hardcore-style death penalties with configurable ban durations, escalating consequences, and optional shared lives for group play. Built for Spigot 1.21+ with Paper enhancements.

---

## Build System

### Gradle Configuration (Spigot-Compatible)

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.bstats", "com.example.deathban.metrics")
        minimize()
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}

kotlin {
    jvmToolchain(21)
}
```

### Paper Detection (Runtime)

```kotlin
object PlatformDetector {
    val isPaper: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    fun getServerBrand(): String = when {
        isPaper -> "Paper"
        else -> "Spigot"
    }
}
```

---

## Project Structure

```
deathban-pro/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/
│   ├── kotlin/com/example/deathban/
│   │   ├── DeathBanPlugin.kt          # Main plugin class
│   │   ├── command/
│   │   │   ├── DeathBanCommand.kt     # Command executor
│   │   │   └── DeathBanTabCompleter.kt # Tab completion
│   │   ├── config/
│   │   │   ├── Settings.kt            # Config wrapper
│   │   │   └── Messages.kt            # Message templates
│   │   ├── data/
│   │   │   ├── PlayerData.kt          # Player state model
│   │   │   ├── PlayerDataManager.kt   # YAML persistence
│   │   │   └── SharedLivesPool.kt     # Group lives (Phase 2)
│   │   ├── listener/
│   │   │   ├── DeathListener.kt       # Death event handling
│   │   │   ├── JoinListener.kt        # Rejoin handling
│   │   │   └── RespawnListener.kt     # Post-death actions
│   │   ├── manager/
│   │   │   ├── BanManager.kt          # Ban logic orchestrator
│   │   │   └── OffenseManager.kt      # Escalation tracking
│   │   ├── metrics/
│   │   │   └── MetricsManager.kt      # bStats integration
│   │   ├── theme/
│   │   │   ├── Theme.kt               # Theme interface
│   │   │   ├── ThemeLoader.kt         # External theme loading
│   │   │   └── DefaultTheme.kt        # Built-in theme
│   │   ├── update/
│   │   │   └── UpdateChecker.kt       # SpigotMC API checker
│   │   └── util/
│   │       ├── PlatformDetector.kt    # Spigot/Paper detection
│   │       ├── TimeFormatter.kt       # Duration display
│   │       └── ColorUtil.kt           # Legacy color codes
│   └── resources/
│       ├── plugin.yml
│       ├── config.yml
│       └── messages.yml
└── docs/
    ├── SPEC-v2.md
    ├── SPIGOTMC-LISTING.md
    └── ROADMAP.md
```

---

## Configuration

### config.yml

```yaml
# DeathBan Pro Configuration
# https://github.com/yourname/deathban-pro

# General Settings
debug: false
update-check: true
metrics: true

# Ban Mode: "individual" or "shared"
mode: individual

# Rolling Window (deaths within this period count toward ban)
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3

# Ban Durations (escalating by offense level)
ban-durations:
  1: 1h      # First offense
  2: 6h     # Second offense
  3: 24h    # Third offense
  4: 72h    # Fourth offense
  5: 168h   # Fifth+ offense (1 week)

# Offense Reset
offense-reset:
  enabled: true
  clean-period: 168h  # 7 days without death resets offense level

# Worlds (empty = all worlds)
enabled-worlds: []
disabled-worlds:
  - world_lobby

# Bypass
bypass-permission: deathban.bypass

# Theme
theme: default
```

### messages.yml

```yaml
# DeathBan Pro Messages
# Supports MiniMessage format on Paper, legacy color codes on Spigot

prefix: "&8[&cDeathBan&8]&r "

ban:
  title: "&c&lYOU DIED"
  subtitle: "&7Banned for &e{duration}"
  kick-message: |
    &c&lDeath Ban
    
    &7You have been temporarily banned for dying.
    &7Duration: &e{duration}
    &7Offense Level: &c{offense_level}
    
    &7Return: &a{return_time}
  
return:
  title: "&a&lWELCOME BACK"
  subtitle: "&7Your ban has expired"
  message: "&aYou have returned from your death ban. Be careful!"

warnings:
  final-life: "&c&lWARNING: &7This is your final life! Next death = ban."
  near-limit: "&e&lCAUTION: &7{remaining} lives remaining in this period."

commands:
  check-self: "&7Your offense level: &e{offense_level}&7, Deaths in window: &e{deaths}/{max}"
  check-other: "&7{player}'s offense level: &e{offense_level}&7, Deaths: &e{deaths}/{max}"
  reset-success: "&aReset offense data for &e{player}"
  pardon-success: "&aPardoned &e{player}&a - they can rejoin now"
  pardon-not-banned: "&c{player} is not currently banned"
  reload-success: "&aConfiguration reloaded"
  
errors:
  no-permission: "&cYou don't have permission to do that."
  player-not-found: "&cPlayer not found: {player}"
  invalid-usage: "&cUsage: {usage}"
```

---

## Data Models

### PlayerData

```kotlin
data class PlayerData(
    val uuid: UUID,
    var offenseLevel: Int = 0,
    var offenseResetDate: Instant? = null,
    val deaths: MutableList<DeathRecord> = mutableListOf(),
    var currentBan: BanRecord? = null,
    var pendingPardon: Boolean = false
)

data class DeathRecord(
    val timestamp: Instant,
    val world: String,
    val cause: String,
    val killer: UUID? = null,
    val location: LocationData
)

data class LocationData(
    val x: Double,
    val y: Double,
    val z: Double
)

data class BanRecord(
    val startTime: Instant,
    val endTime: Instant,
    val offenseLevel: Int,
    val deathCause: String
)
```

### YAML Serialization Format

```yaml
# plugins/DeathBanPro/players/{uuid}.yml

offense-level: 2
offense-reset-date: "2024-10-15T10:30:00Z"
pending-pardon: false

current-ban:
  start-time: "2024-10-14T10:30:00Z"
  end-time: "2024-10-14T16:30:00Z"
  offense-level: 2
  death-cause: "FALL"

deaths:
  - timestamp: "2024-10-12T08:15:00Z"
    world: "world"
    cause: "ENTITY_ATTACK"
    killer: "550e8400-e29b-41d4-a716-446655440000"
    location:
      x: 100.5
      y: 64.0
      z: -200.3
  - timestamp: "2024-10-14T10:30:00Z"
    world: "world_nether"
    cause: "FALL"
    killer: null
    location:
      x: 50.0
      y: 45.0
      z: 100.0
```

---

## Commands & Permissions

### Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/deathban` | Show help | `deathban.use` |
| `/deathban check [player]` | Check status | `deathban.check` / `deathban.check.others` |
| `/deathban reset <player>` | Reset offense data | `deathban.admin` |
| `/deathban pardon <player>` | Remove active ban | `deathban.admin` |
| `/deathban reload` | Reload configuration | `deathban.admin` |

### Permissions

```yaml
# plugin.yml permissions
permissions:
  deathban.*:
    description: Full access to DeathBan Pro
    default: op
    children:
      deathban.use: true
      deathban.check: true
      deathban.check.others: true
      deathban.admin: true
      deathban.bypass: true
  
  deathban.use:
    description: Basic plugin access
    default: true
  
  deathban.check:
    description: Check own status
    default: true
  
  deathban.check.others:
    description: Check other players' status
    default: op
  
  deathban.admin:
    description: Admin commands (reset, pardon, reload)
    default: op
  
  deathban.bypass:
    description: Bypass death bans entirely
    default: op
```

---

## Tab Completion

### DeathBanTabCompleter.kt

```kotlin
class DeathBanTabCompleter(
    private val plugin: DeathBanPlugin
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
            else -> emptyList()
        }
    }
    
    private fun getSubcommands(sender: CommandSender): List<String> {
        return buildList {
            add("help")
            if (sender.hasPermission("deathban.check")) add("check")
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
                getOnlinePlayerNames() + getOfflinePlayerNames()
            } else emptyList()
            
            "pardon" -> if (sender.hasPermission("deathban.admin")) {
                getBannedPlayerNames()
            } else emptyList()
            
            else -> emptyList()
        }
    }
    
    private fun getOnlinePlayerNames(): List<String> =
        Bukkit.getOnlinePlayers().map { it.name }
    
    private fun getOfflinePlayerNames(): List<String> =
        plugin.dataManager.getAllStoredPlayers()
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }
            .filter { name -> Bukkit.getPlayer(name) == null }
    
    private fun getBannedPlayerNames(): List<String> =
        plugin.banManager.getActiveBans()
            .mapNotNull { Bukkit.getOfflinePlayer(it).name }
    
    private fun filterStartsWith(options: List<String>, prefix: String): List<String> =
        options.filter { it.lowercase().startsWith(prefix.lowercase()) }
}
```

---

## bStats Integration

### MetricsManager.kt

```kotlin
class MetricsManager(private val plugin: DeathBanPlugin) {
    
    private var metrics: Metrics? = null
    
    fun initialize() {
        if (!plugin.settings.metricsEnabled) {
            plugin.logger.info("Metrics disabled in config")
            return
        }
        
        metrics = Metrics(plugin, BSTATS_PLUGIN_ID).apply {
            // Custom chart: Ban mode
            addCustomChart(SimplePie("ban_mode") {
                plugin.settings.mode.name.lowercase()
            })
            
            // Custom chart: Rolling window enabled
            addCustomChart(SimplePie("rolling_window") {
                if (plugin.settings.rollingWindowEnabled) "enabled" else "disabled"
            })
            
            // Custom chart: Active theme
            addCustomChart(SimplePie("theme") {
                plugin.settings.theme
            })
            
            // Custom chart: Total bans issued
            addCustomChart(SingleLineChart("total_bans") {
                plugin.banManager.getTotalBansIssued()
            })
        }
        
        plugin.logger.info("bStats metrics initialized")
    }
    
    companion object {
        const val BSTATS_PLUGIN_ID = 00000 // Replace with actual ID after registration
    }
}
```

---

## Update Checker

### UpdateChecker.kt

```kotlin
class UpdateChecker(
    private val plugin: DeathBanPlugin,
    private val resourceId: Int
) {
    
    fun checkForUpdates() {
        if (!plugin.settings.updateCheckEnabled) return
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin) {
            try {
                val latestVersion = fetchLatestVersion()
                val currentVersion = plugin.description.version
                
                if (isNewerVersion(latestVersion, currentVersion)) {
                    notifyUpdate(currentVersion, latestVersion)
                } else {
                    plugin.logger.info("Running latest version ($currentVersion)")
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to check for updates: ${e.message}")
            }
        }
    }
    
    private fun fetchLatestVersion(): String {
        val url = URL("https://api.spigotmc.org/legacy/update.php?resource=$resourceId")
        return url.openConnection().apply {
            connectTimeout = 5000
            readTimeout = 5000
        }.getInputStream().bufferedReader().readText().trim()
    }
    
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
    
    private fun notifyUpdate(current: String, latest: String) {
        val message = buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("  DeathBan Pro Update Available!")
            appendLine("  Current: $current → Latest: $latest")
            appendLine("  Download: https://spigotmc.org/resources/$resourceId")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
        plugin.logger.warning(message)
        
        // Notify ops on join
        plugin.updateNotification = "DeathBan Pro $latest available (running $current)"
    }
}
```

---

## Edge Case Handling

### Crash Recovery

```kotlin
class PlayerDataManager(private val plugin: DeathBanPlugin) {
    
    private val dataFolder = File(plugin.dataFolder, "players")
    private val pendingBansFile = File(plugin.dataFolder, "pending-bans.yml")
    
    init {
        dataFolder.mkdirs()
        loadPendingBans()
    }
    
    // Persist pending bans immediately to survive crashes
    private val pendingBans = Collections.synchronizedSet(mutableSetOf<UUID>())
    
    fun addPendingBan(uuid: UUID) {
        pendingBans.add(uuid)
        savePendingBans()
    }
    
    fun removePendingBan(uuid: UUID) {
        pendingBans.remove(uuid)
        savePendingBans()
    }
    
    private fun savePendingBans() {
        val config = YamlConfiguration()
        config.set("pending", pendingBans.map { it.toString() })
        config.save(pendingBansFile)
    }
    
    private fun loadPendingBans() {
        if (!pendingBansFile.exists()) return
        
        val config = YamlConfiguration.loadConfiguration(pendingBansFile)
        config.getStringList("pending")
            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            .forEach { pendingBans.add(it) }
        
        if (pendingBans.isNotEmpty()) {
            plugin.logger.warning("Recovered ${pendingBans.size} pending bans from crash")
        }
    }
}
```

### Concurrent Death Events

```kotlin
class DeathListener(private val plugin: DeathBanPlugin) : Listener {
    
    // Prevent double-processing deaths
    private val processingDeaths = ConcurrentHashMap.newKeySet<UUID>()
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        
        // Check bypass
        if (player.hasPermission("deathban.bypass")) return
        
        // Check world
        if (!plugin.settings.isWorldEnabled(player.world.name)) return
        
        // Prevent concurrent processing
        if (!processingDeaths.add(player.uniqueId)) {
            plugin.logger.warning("Skipping duplicate death event for ${player.name}")
            return
        }
        
        try {
            processDeath(player, event)
        } finally {
            // Clear after respawn event, not immediately
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                processingDeaths.remove(player.uniqueId)
            }, 20L)
        }
    }
    
    private fun processDeath(player: Player, event: PlayerDeathEvent) {
        plugin.dataManager.addPendingBan(player.uniqueId)
        
        val data = plugin.dataManager.getOrCreate(player.uniqueId)
        val deathCause = event.entity.lastDamageCause?.cause?.name ?: "UNKNOWN"
        val killer = (event.entity.killer)?.uniqueId
        
        // Record death
        data.deaths.add(DeathRecord(
            timestamp = Instant.now(),
            world = player.world.name,
            cause = deathCause,
            killer = killer,
            location = LocationData(player.location)
        ))
        
        // Calculate ban
        val deathsInWindow = plugin.offenseManager.getDeathsInWindow(data)
        
        if (deathsInWindow >= plugin.settings.maxDeathsInWindow) {
            data.offenseLevel++
            plugin.banManager.applyBan(player, data)
        }
        
        plugin.dataManager.save(data)
    }
}
```

### Offline Pardon Notification

```kotlin
class JoinListener(private val plugin: DeathBanPlugin) : Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val data = plugin.dataManager.get(event.player.uniqueId) ?: return
        
        // Check if still banned
        val ban = data.currentBan
        if (ban != null && Instant.now().isBefore(ban.endTime)) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                plugin.messages.formatKickMessage(ban)
            )
            return
        }
        
        // Clear expired ban
        if (ban != null) {
            data.currentBan = null
            plugin.dataManager.save(data)
        }
    }
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val data = plugin.dataManager.get(player.uniqueId) ?: return
        
        // Notify of pardon (set by admin while offline)
        if (data.pendingPardon) {
            data.pendingPardon = false
            plugin.dataManager.save(data)
            
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.sendMessage(plugin.messages.format("pardon-notification"))
            }, 20L)
        }
        
        // Notify ops of updates
        if (player.hasPermission("deathban.admin") && plugin.updateNotification != null) {
            player.sendMessage("${ChatColor.YELLOW}${plugin.updateNotification}")
        }
    }
}
```

---

## Theme System

### Theme Interface

```kotlin
interface Theme {
    val id: String
    val name: String
    val author: String
    val version: String
    
    fun getBanTitle(): String
    fun getBanSubtitle(duration: String): String
    fun getKickMessage(context: BanContext): String
    fun getReturnTitle(): String
    fun getReturnSubtitle(): String
    fun getReturnMessage(): String
    
    fun getDeathSound(): Sound?
    fun getReturnSound(): Sound?
    fun getParticleEffect(): Particle?
}

data class BanContext(
    val playerName: String,
    val duration: String,
    val offenseLevel: Int,
    val deathCause: String,
    val returnTime: String
)
```

### Theme Loader

```kotlin
class ThemeLoader(private val plugin: DeathBanPlugin) {
    
    private val themes = mutableMapOf<String, Theme>()
    private val themesFolder = File(plugin.dataFolder, "themes")
    
    init {
        themes["default"] = DefaultTheme()
        loadExternalThemes()
    }
    
    private fun loadExternalThemes() {
        if (!themesFolder.exists()) {
            themesFolder.mkdirs()
            return
        }
        
        themesFolder.listFiles { f -> f.extension == "jar" }?.forEach { jar ->
            try {
                loadThemeJar(jar)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load theme ${jar.name}: ${e.message}")
            }
        }
    }
    
    private fun loadThemeJar(jar: File) {
        val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), plugin::class.java.classLoader)
        val config = YamlConfiguration.loadConfiguration(
            classLoader.getResourceAsStream("theme.yml")?.reader() ?: return
        )
        
        val mainClass = config.getString("main") ?: return
        val themeClass = classLoader.loadClass(mainClass)
        val theme = themeClass.getDeclaredConstructor().newInstance() as Theme
        
        themes[theme.id] = theme
        plugin.logger.info("Loaded theme: ${theme.name} v${theme.version} by ${theme.author}")
    }
    
    fun getTheme(id: String): Theme = themes[id] ?: themes["default"]!!
    
    fun getAvailableThemes(): List<Theme> = themes.values.toList()
}
```

---

## plugin.yml

```yaml
name: DeathBanPro
version: ${version}
main: com.example.deathban.DeathBanPlugin
api-version: '1.21'
description: Hardcore-style death penalties with escalating bans and optional shared lives
author: YourName
website: https://spigotmc.org/resources/XXXXX

softdepend:
  - PlaceholderAPI

commands:
  deathban:
    description: DeathBan Pro main command
    usage: /<command> [check|reset|pardon|reload] [player]
    aliases: [db, dban]

permissions:
  deathban.*:
    description: Full access to DeathBan Pro
    default: op
    children:
      deathban.use: true
      deathban.check: true
      deathban.check.others: true
      deathban.admin: true
      deathban.bypass: true
  deathban.use:
    description: Basic plugin access
    default: true
  deathban.check:
    description: Check own death ban status
    default: true
  deathban.check.others:
    description: Check other players' death ban status
    default: op
  deathban.admin:
    description: Administrative commands (reset, pardon, reload)
    default: op
  deathban.bypass:
    description: Bypass death bans entirely
    default: op
```

---

## Testing Strategy

### MockBukkit Unit Tests

```kotlin
// build.gradle.kts additions
dependencies {
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.80.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
}

// src/test/kotlin/com/example/deathban/BanManagerTest.kt
class BanManagerTest {
    
    private lateinit var server: ServerMock
    private lateinit var plugin: DeathBanPlugin
    
    @BeforeEach
    fun setup() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(DeathBanPlugin::class.java)
    }
    
    @AfterEach
    fun teardown() {
        MockBukkit.unmock()
    }
    
    @Test
    fun `player is banned after exceeding death limit`() {
        val player = server.addPlayer()
        
        repeat(3) {
            player.damage(100.0)
            server.pluginManager.assertEventFired(PlayerDeathEvent::class.java)
        }
        
        val data = plugin.dataManager.get(player.uniqueId)
        assertNotNull(data?.currentBan)
    }
    
    @Test
    fun `bypass permission prevents ban`() {
        val player = server.addPlayer()
        player.addAttachment(plugin, "deathban.bypass", true)
        
        repeat(5) {
            player.damage(100.0)
        }
        
        val data = plugin.dataManager.get(player.uniqueId)
        assertNull(data?.currentBan)
    }
}
```

---

## License

### Dual License Model

**Base Plugin (DeathBan Pro)**: MIT License

- Free to use, modify, redistribute
- Source available on GitHub
- Commercial use allowed

**Premium Themes**: Proprietary

- Single server license per purchase
- No redistribution
- Updates included
- Support via SpigotMC

---

## Changelog Format

```markdown
## [1.0.0] - 2024-XX-XX

### Added
- Initial release
- Individual ban mode with rolling window
- Escalating ban durations
- Per-player offense tracking
- Admin commands (check, reset, pardon, reload)
- bStats metrics integration
- Update checker
- Themeable messages

### Configuration
- Customizable ban durations per offense level
- Rolling window configuration
- World enable/disable list
- Full message customization
```
