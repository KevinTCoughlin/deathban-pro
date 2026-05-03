# DeathBan Pro - API Reference

This document provides comprehensive API reference for DeathBan Pro, including event hooks, manager interfaces, and code examples for developers who want to integrate with or extend the plugin.

## Table of Contents

- [Overview](#overview)
- [DeathListener Event Hooks](#deathlistener-event-hooks)
- [BanManager API](#banmanager-api)
- [OffenseManager API](#offensemanager-api)
- [PlayerDataManager API](#playerdatamanager-api)
- [SharedLivesManager API](#sharedlivesmanager-api)
- [Configuration Access](#configuration-access)
- [Data Models](#data-models)
- [Code Examples](#code-examples)

## Overview

DeathBan Pro exposes its managers through the main plugin class `DeathBanPlugin`. Access managers via:

```kotlin
val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
val banManager = plugin.banManager
val offenseManager = plugin.offenseManager
val dataManager = plugin.dataManager
val sharedLivesManager = plugin.sharedLivesManager // null in INDIVIDUAL mode
```

## DeathListener Event Hooks

The `DeathListener` processes all player death events. Internally it uses these event flow stages:

### Event Processing Flow

1. **Permission Check** - Checks for `deathban.bypass` permission
2. **World Check** - Verifies world is enabled for the plugin
3. **Duplicate Prevention** - Prevents concurrent processing of same player
4. **Mode Routing** - Routes to individual or shared mode logic
5. **Ban Application** - Applies ban if thresholds met

### Hook Points

While DeathListener doesn't expose direct hooks, you can:
- Check player ban status via `BanManager.getActiveBans()`
- Monitor player data via `PlayerDataManager.get(uuid)`
- Listen to events through the managers' public state

### Listening to Death Events (Alternative)

Create your own listener to respond to plugin events:

```kotlin
@EventHandler(priority = EventPriority.HIGHEST)
fun onPlayerDeath(event: PlayerDeathEvent) {
    if (Bukkit.getPluginManager().isPluginEnabled("DeathBanPro")) {
        val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
        val player = event.entity
        
        if (!player.hasPermission("deathban.bypass")) {
            // Your custom logic here
        }
    }
}
```

## BanManager API

The `BanManager` handles ban application, pardon, and reset operations.

### Public Methods

#### `applyBan(player: Player, data: PlayerData, deathCause: String)`

Applies an individual ban to a player with escalating duration.

**Parameters:**
- `player` - The player to ban
- `data` - PlayerData with current offense level
- `deathCause` - The damage cause (e.g., "FALL", "LAVA")

**Side Effects:**
- Sets `data.currentBan` with calculated duration
- Saves player data asynchronously
- Shows title and plays sound/particles based on theme
- Kicks player after respawn animation
- Increments total bans counter

**Example:**
```kotlin
val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
val player = Bukkit.getPlayer("Steve")
val data = plugin.dataManager.getOrCreate(player.uniqueId)
data.offenseLevel = 2 // Applied 2 bans already
plugin.banManager.applyBan(player, data, "FALL")
```

#### `applySharedBan(player: Player, deathCause: String)`

Applies a ban in shared mode when the pool is empty.

**Parameters:**
- `player` - The player to ban
- `deathCause` - The damage cause

**Side Effects:**
- Creates BanRecord with `sharedLivesEmptyPoolBan` duration
- Shows theme effects and messages
- Kicks player after respawn

**Example:**
```kotlin
if (settings.mode == BanMode.SHARED && !plugin.sharedLivesManager!!.consumeLife(uuid)) {
    plugin.banManager.applySharedBan(player, "DROWNING")
}
```

#### `pardon(uuid: UUID): Boolean`

Removes an active ban for a player.

**Returns:** `true` if pardon successful, `false` if player not found or not banned

**Side Effects:**
- Clears `currentBan` from PlayerData
- Sets `pendingPardon` flag if player offline
- Saves player data asynchronously

**Example:**
```kotlin
if (plugin.banManager.pardon(uuid)) {
    Bukkit.getLogger().info("Player pardoned")
} else {
    Bukkit.getLogger().info("Player not banned or not found")
}
```

#### `reset(uuid: UUID): Boolean`

Completely resets a player's death record and offense level.

**Returns:** `true` if reset successful, `false` if player not found

**Side Effects:**
- Sets offense level to 0
- Clears all death records
- Removes active ban
- Clears pending pardon flag

**Example:**
```kotlin
plugin.banManager.reset(uuid) // Clean slate for player
```

#### `getActiveBans(): List<UUID>`

Gets list of UUIDs currently banned.

**Returns:** List of UUIDs with active bans

**Example:**
```kotlin
val bannedPlayers = plugin.banManager.getActiveBans()
for (uuid in bannedPlayers) {
    val data = plugin.dataManager.get(uuid)
    println("${data?.lastDeathTime} - ${data?.currentBan?.endTime}")
}
```

#### `getTotalBansIssued(): Int`

Gets total number of bans issued since plugin load.

**Returns:** Integer count

**Example:**
```kotlin
val stats = plugin.banManager.getTotalBansIssued()
println("Total bans this session: $stats")
```

## OffenseManager API

The `OffenseManager` tracks deaths within the rolling window and manages offense escalation.

### Public Methods

#### `getDeathsInWindow(data: PlayerData): Int`

Gets count of deaths within the rolling window period.

**Parameters:**
- `data` - PlayerData to check

**Returns:** Number of deaths in window (or all deaths if rolling window disabled)

**Example:**
```kotlin
val data = plugin.dataManager.get(uuid)
val recentDeaths = plugin.offenseManager.getDeathsInWindow(data)
println("Deaths in 24h window: $recentDeaths")
```

#### `shouldTriggerBan(data: PlayerData): Boolean`

Checks if a player should be banned based on deaths in window.

**Parameters:**
- `data` - PlayerData to check

**Returns:** `true` if deaths >= `max-deaths` threshold

**Logic:**
- If rolling window disabled: all deaths count
- If enabled: only deaths within `rolling-window.duration` count
- Compare against `rolling-window.max-deaths`

**Example:**
```kotlin
if (plugin.offenseManager.shouldTriggerBan(data)) {
    plugin.banManager.applyBan(player, data, "FALL")
}
```

#### `checkOffenseReset(data: PlayerData): Boolean`

Checks if player qualifies for offense reset after clean period.

**Parameters:**
- `data` - PlayerData to check

**Returns:** `true` if reset was applied, `false` otherwise

**Side Effects:** (if returns true)
- Sets offense level to 0
- Clears death records

**Example:**
```kotlin
if (plugin.offenseManager.checkOffenseReset(data)) {
    player.sendMessage(Component.text("Your record has been cleaned!").color(NamedTextColor.GREEN))
}
```

#### `pruneOldDeaths(data: PlayerData)`

Removes deaths outside rolling window from the record.

**Parameters:**
- `data` - PlayerData to prune

**Side Effects:** Modifies death list in place

**Example:**
```kotlin
plugin.offenseManager.pruneOldDeaths(data)
plugin.dataManager.saveAsync(data)
```

#### `getRemainingLives(data: PlayerData): Int`

Calculates remaining deaths before next ban.

**Parameters:**
- `data` - PlayerData to check

**Returns:** Remaining lives (0 or more)

**Formula:** `max-deaths - deaths-in-window`

**Example:**
```kotlin
val remaining = plugin.offenseManager.getRemainingLives(data)
player.sendMessage(Component.text("Lives remaining: $remaining"))
```

## PlayerDataManager API

The `PlayerDataManager` handles persistent storage of player death records and bans.

### Public Methods

#### `get(uuid: UUID): PlayerData?`

Retrieves cached PlayerData or null if not found.

**Parameters:**
- `uuid` - Player UUID

**Returns:** PlayerData or null

**Note:** This is cached - data is loaded from disk on demand

**Example:**
```kotlin
val data = plugin.dataManager.get(uuid) ?: return
println("Offense level: ${data.offenseLevel}")
```

#### `getOrCreate(uuid: UUID): PlayerData`

Gets PlayerData or creates empty record if doesn't exist.

**Parameters:**
- `uuid` - Player UUID

**Returns:** PlayerData (never null)

**Side Effects:** 
- Creates new PlayerData if needed
- Does NOT save to disk (save with `saveAsync` or `save`)

**Example:**
```kotlin
val data = plugin.dataManager.getOrCreate(uuid)
data.offenseLevel++
plugin.dataManager.saveAsync(data)
```

#### `save(data: PlayerData)`

Synchronously saves PlayerData to disk.

**Parameters:**
- `data` - PlayerData to save

**Side Effects:**
- Writes YAML file to `plugins/DeathBanPro/players/`
- Blocks until I/O complete

**Note:** Prefer `saveAsync` for non-blocking saves

**Example:**
```kotlin
data.offenseLevel = 0
plugin.dataManager.save(data) // Blocks
```

#### `saveAsync(data: PlayerData)`

Asynchronously saves PlayerData to disk (non-blocking).

**Parameters:**
- `data` - PlayerData to save

**Side Effects:**
- Queues async write to disk
- Returns immediately

**Example:**
```kotlin
data.offenseLevel++
plugin.dataManager.saveAsync(data) // Returns immediately
```

#### `saveAll()`

Saves all dirty PlayerData to disk (used on shutdown).

**Example:**
```kotlin
plugin.dataManager.saveAll() // Flush all changes
```

#### `clearCache()`

Clears all cached player data from memory.

**Side Effects:**
- Next access will reload from disk

**Example:**
```kotlin
plugin.dataManager.clearCache() // After manual file edits
```

#### `getActiveBans(): List<UUID>`

Gets UUIDs of all currently banned players.

**Returns:** List of UUIDs with active bans

**Example:**
```kotlin
val banned = plugin.dataManager.getActiveBans()
for (uuid in banned) {
    val data = plugin.dataManager.get(uuid)
    val expires = data?.currentBan?.endTime
    println("$uuid banned until $expires")
}
```

#### `addPendingBan(uuid: UUID)`

Marks a UUID for banning on next join (crash recovery).

**Parameters:**
- `uuid` - Player UUID

**Example:**
```kotlin
plugin.dataManager.addPendingBan(uuid)
```

#### `getPendingBans(): Set<UUID>`

Gets set of UUIDs with pending bans.

**Returns:** Set of UUIDs

**Example:**
```kotlin
val pending = plugin.dataManager.getPendingBans()
```

#### `removePendingBan(uuid: UUID)`

Clears pending ban flag for UUID.

**Parameters:**
- `uuid` - Player UUID

**Example:**
```kotlin
plugin.dataManager.removePendingBan(uuid)
```

## SharedLivesManager API

The `SharedLivesManager` manages shared life pools in SHARED mode.

**Availability:** Only available when `mode: shared` in config. Check with:
```kotlin
if (plugin.sharedLivesManager != null) {
    // Shared mode enabled
}
```

### Public Methods

#### `getGlobalPool(): SharedLivesPool`

Gets the global server life pool.

**Returns:** SharedLivesPool with current lives and max lives

**Example:**
```kotlin
val pool = plugin.sharedLivesManager?.getGlobalPool()
println("Server lives: ${pool?.lives}/${pool?.maxLives}")
```

#### `getPoolForPlayer(uuid: UUID): SharedLivesPool?`

Gets the player's team pool or null if in global pool.

**Parameters:**
- `uuid` - Player UUID

**Returns:** Team SharedLivesPool or null (null means using global pool)

**Example:**
```kotlin
val pool = plugin.sharedLivesManager?.getPoolForPlayer(uuid)
if (pool != null) {
    println("Team pool: ${pool.name}")
} else {
    println("Using global pool")
}
```

#### `consumeLife(uuid: UUID): Boolean`

Removes one life from player's pool (global or team).

**Parameters:**
- `uuid` - Player UUID

**Returns:** `true` if life consumed, `false` if pool empty

**Side Effects:**
- Decrements pool lives
- Saves pool state asynchronously

**Example:**
```kotlin
if (plugin.sharedLivesManager?.consumeLife(uuid) == true) {
    val pool = plugin.sharedLivesManager?.getPoolForPlayer(uuid) ?: 
               plugin.sharedLivesManager?.getGlobalPool()
    player.sendMessage("Life consumed! ${pool?.lives}/${pool?.maxLives} remaining")
}
```

#### `addLife(uuid: UUID): Boolean`

Adds one life to player's pool (up to max).

**Parameters:**
- `uuid` - Player UUID

**Returns:** `true` if life added, `false` if already at max

**Example:**
```kotlin
if (plugin.sharedLivesManager?.addLife(uuid) == true) {
    player.sendMessage("Life added!")
}
```

#### `setLives(lives: Int): Boolean`

Sets exact number of lives in global pool (admin).

**Parameters:**
- `lives` - Number of lives to set

**Returns:** `true` if set successfully

**Example:**
```kotlin
plugin.sharedLivesManager?.setLives(5)
```

#### `saveAll()`

Saves all pool states to disk.

**Example:**
```kotlin
plugin.sharedLivesManager?.saveAll()
```

## Configuration Access

### Settings Class

Access all configuration values through `plugin.settings`:

```kotlin
// Mode
val mode = plugin.settings.mode // BanMode.INDIVIDUAL or BanMode.SHARED

// Rolling window
val windowEnabled = plugin.settings.rollingWindowEnabled
val windowDuration = plugin.settings.rollingWindowDuration // Duration object
val maxDeaths = plugin.settings.maxDeathsInWindow

// Ban durations by offense level
val duration1h = plugin.settings.getBanDuration(1) // Duration for offense level 1
val duration2h = plugin.settings.getBanDuration(2)

// Offense reset
val resetEnabled = plugin.settings.offenseResetEnabled
val resetPeriod = plugin.settings.offenseResetPeriod

// World filtering
val isEnabled = plugin.settings.isWorldEnabled("world_nether")

// Theme
val themeId = plugin.settings.themeId
val soundsEnabled = plugin.settings.themeSoundsEnabled
val particlesEnabled = plugin.settings.themeParticlesEnabled

// Shared lives
val defaultLives = plugin.settings.sharedLivesDefault
val maxLives = plugin.settings.sharedLivesMax
val allowTeams = plugin.settings.sharedLivesAllowTeams
val emptyPoolBanDuration = plugin.settings.sharedLivesEmptyPoolBan
```

## Data Models

### PlayerData

Represents all state for a player:

```kotlin
data class PlayerData(
    val uuid: UUID,
    val deaths: MutableList<DeathRecord>,
    var offenseLevel: Int,
    var currentBan: BanRecord?,
    var lastDeathTime: Instant?,
    var pendingPardon: Boolean = false,
    var lastNotifiedOffenseLevel: Int = 0,
)

// Check if player is banned
val isBanned = data.currentBan?.let { it.endTime.isAfter(Instant.now()) } ?: false
```

### BanRecord

Represents an active ban:

```kotlin
data class BanRecord(
    val startTime: Instant,
    val endTime: Instant,
    val offenseLevel: Int,
    val deathCause: String,
)

// Check if ban is still active
val isActive = ban.endTime.isAfter(Instant.now())

// Get remaining duration
val remaining = Duration.between(Instant.now(), ban.endTime)
```

### DeathRecord

Represents a single death:

```kotlin
data class DeathRecord(
    val timestamp: Instant,
    val world: String,
    val cause: String,
    val killer: UUID?,
    val location: LocationData,
)
```

### SharedLivesPool

Represents a life pool:

```kotlin
data class SharedLivesPool(
    val id: String,
    val name: String,
    var lives: Int,
    val maxLives: Int,
    val createdAt: Instant,
    val members: MutableSet<UUID> = mutableSetOf(),
)

val isGlobal = pool.id == "global"
val isFull = pool.lives >= pool.maxLives
val isEmpty = pool.lives <= 0
```

## Code Examples

### Example 1: Check if Player is Currently Banned

```kotlin
val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
val uuid = Bukkit.getPlayer("Steve")?.uniqueId ?: return

val data = plugin.dataManager.get(uuid)
if (data?.isBanned() == true) {
    val ban = data.currentBan!!
    val remaining = Duration.between(Instant.now(), ban.endTime)
    println("${data.uuid} is banned for ${remaining.toMinutes()} more minutes")
} else {
    println("Player is not banned")
}
```

### Example 2: Create a Ban Status Command

```kotlin
@CommandHandler(path = "banstatus")
fun onBanStatus(sender: CommandSender, args: Array<String>) {
    val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
    
    val targetName = args.getOrNull(0) ?: "unknown"
    val target = Bukkit.getOfflinePlayer(targetName)
    val data = plugin.dataManager.get(target.uniqueId) ?: run {
        sender.sendMessage("Player has no death record")
        return
    }
    
    sender.sendMessage("━━━━ Ban Status ━━━━")
    sender.sendMessage("Player: ${target.name}")
    sender.sendMessage("Offense Level: ${data.offenseLevel}")
    sender.sendMessage("Deaths Recorded: ${data.deaths.size}")
    
    if (data.isBanned()) {
        val remaining = Duration.between(Instant.now(), data.currentBan!!.endTime)
        sender.sendMessage("STATUS: BANNED")
        sender.sendMessage("Remaining: ${remaining.toMinutes()} minutes")
    } else {
        val remaining = plugin.offenseManager.getRemainingLives(data)
        sender.sendMessage("STATUS: FREE")
        sender.sendMessage("Lives Remaining: $remaining")
    }
}
```

### Example 3: Track Deaths in a Custom Command

```kotlin
@CommandHandler(path = "mydeaths")
fun onMyDeaths(sender: CommandSender) {
    if (sender !is Player) {
        sender.sendMessage("Players only")
        return
    }
    
    val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
    val data = plugin.dataManager.get(sender.uniqueId) ?: run {
        sender.sendMessage("You have no death record")
        return
    }
    
    sender.sendMessage("Your Death History:")
    data.deaths.takeLast(5).forEach { death ->
        sender.sendMessage("  - ${death.cause} in ${death.world}")
    }
}
```

### Example 4: Shared Mode - Monitor Pool Status

```kotlin
fun checkPoolStatus() {
    val plugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as DeathBanPlugin
    if (plugin.settings.mode != BanMode.SHARED) return
    
    val pool = plugin.sharedLivesManager?.getGlobalPool() ?: return
    val remaining = pool.maxLives - pool.lives
    
    when {
        pool.lives == 0 -> Bukkit.broadcastMessage("§c§l✗ SHARED POOL EMPTY - Deaths will result in bans!")
        pool.lives <= 2 -> Bukkit.broadcastMessage("§e§l⚠ SHARED POOL CRITICAL - Only ${pool.lives} lives left!")
        remaining >= 3 -> Bukkit.broadcastMessage("§a✓ Shared pool recovering: ${pool.lives}/${pool.maxLives}")
    }
}
```

### Example 5: Integrate with Another Plugin

```kotlin
// In your plugin's onEnable()
fun setupDeathBanIntegration() {
    val deathbanPlugin = Bukkit.getPluginManager().getPlugin("DeathBanPro") as? DeathBanPlugin
    if (deathbanPlugin != null) {
        // Listen to deaths by checking ban status after death
        server.pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.LOWEST)
            fun onDeath(event: PlayerDeathEvent) {
                val player = event.entity
                val data = deathbanPlugin.dataManager.getOrCreate(player.uniqueId)
                
                // Custom logic: broadcast to Discord webhook, log to database, etc.
                logDeathToDashboard(player, data.offenseLevel)
            }
        }, this)
    }
}
```

---

**Last Updated:** Based on DeathBan Pro v1.0+  
**Kotlin Version:** 1.9.22+  
**Minecraft:** 1.21+
