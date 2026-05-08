# DeathBan Pro - Architecture Guide

This document describes the system architecture, component interactions, and data flows in DeathBan Pro.

## Table of Contents

- [System Overview](#system-overview)
- [Component Architecture](#component-architecture)
- [Death Event Flow](#death-event-flow)
- [Individual Mode Logic](#individual-mode-logic)
- [Shared Mode Logic](#shared-mode-logic)
- [Configuration Precedence](#configuration-precedence)
- [Data Persistence Strategy](#data-persistence-strategy)
- [Concurrency & Thread Safety](#concurrency--thread-safety)

## System Overview

DeathBan Pro implements a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│         Minecraft Spigot/Paper Server                   │
│  ┌───────────────────────────────────────────────────┐  │
│  │  DeathBan Pro Plugin                              │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Command Layer (DeathBanCommand)            │  │  │
│  │  │  - /deathban check, reset, pardon, reload   │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                       ▲                             │  │
│  │                       │                             │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Event Layer (DeathListener, JoinListener)  │  │  │
│  │  │  - PlayerDeathEvent → Death processing      │  │  │
│  │  │  - PlayerJoinEvent → Join handling          │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                       ▲                             │  │
│  │                       │                             │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Manager Layer (Business Logic)             │  │  │
│  │  │  ┌────────────────┬──────────────┐           │  │  │
│  │  │  │ BanManager     │ OffenseManager│          │  │  │
│  │  │  │ - Apply bans   │ - Rolling win │          │  │  │
│  │  │  │ - Pardon/reset │ - Escalation │          │  │  │
│  │  │  └────────────────┴──────────────┘          │  │  │
│  │  │  ┌────────────────┬──────────────┐           │  │  │
│  │  │  │SharedLivesManager ThemeManager│           │  │  │
│  │  │  │ - Pool mgmt    │ - Theming    │          │  │  │
│  │  │  └────────────────┴──────────────┘          │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  │                       ▲                             │  │
│  │                       │                             │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Data Layer (Persistence)                   │  │  │
│  │  │  ┌──────────────────────────────────────┐   │  │  │
│  │  │  │ PlayerDataManager                    │   │  │  │
│  │  │  │ - Cache management                   │   │  │  │
│  │  │  │ - YAML serialization                 │   │  │  │
│  │  │  │ - Async I/O                          │   │  │  │
│  │  │  └──────────────────────────────────────┘   │  │  │
│  │  │                                               │  │  │
│  │  │  plugins/DeathBanPro/                        │  │  │
│  │  │  ├── config.yml                             │  │  │
│  │  │  ├── messages.yml                           │  │  │
│  │  │  ├── players/                               │  │  │
│  │  │  │   └── {uuid}.yml (PlayerData)            │  │  │
│  │  │  └── shared-lives/                          │  │  │
│  │  │      ├── global.yml                         │  │  │
│  │  │      └── {team-name}.yml                    │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Component Architecture

### Core Components

#### 1. **DeathBanPlugin** (Main Entry Point)

- Lifecycle management (onEnable, onDisable, reload)
- Component initialization and injection
- Listener and command registration
- Metrics and update checking

**Key Responsibilities:**

- Create and manage all manager instances
- Load configuration
- Initialize theme engine
- Handle plugin startup/shutdown

#### 2. **Event Layer**

##### DeathListener

- Handles `PlayerDeathEvent` (MONITOR priority)
- Routes to individual or shared mode logic
- Manages concurrent death processing
- Prevents duplicate death processing

##### JoinListener

- Handles `PlayerJoinEvent`
- Checks for pending bans from crash recovery
- Notifies player if currently banned
- Shows update notifications

#### 3. **Manager Layer**

##### BanManager

**Responsibilities:**

- Apply bans based on offense escalation
- Pardon players (remove active bans)
- Reset player records (clean slate)
- Track total bans issued

**State:** Atomic counter for metrics

**Dependencies:**

- Settings (for ban durations)
- PlayerDataManager (for persistence)
- ThemeManager (for effects)

##### OffenseManager

**Responsibilities:**

- Check if player meets ban threshold
- Track deaths within rolling window
- Check if player qualifies for offense reset
- Calculate remaining lives

**State:** Stateless (reads from PlayerData)

**Key Logic:**

```
shouldTriggerBan = deaths_in_window >= max_deaths

deaths_in_window = if rolling_window_enabled:
                        count deaths in last 24h
                    else:
                        total death count
```

##### SharedLivesManager

**Responsibilities:**

- Manage global life pool
- Support team-based pools (when enabled)
- Add/consume lives
- Persist pool state

**State:** In-memory pools with periodic YAML sync

**Dependencies:**

- Settings (for pool limits)
- PlayerDataManager (for team membership)

##### ThemeManager

**Responsibilities:**

- Load and manage theme implementations
- Apply theme effects (particles, sounds, messages)
- Provide theme lifecycle

**State:** Active theme instance

#### 4. **Data Layer**

##### PlayerDataManager

**Responsibilities:**

- Load/cache PlayerData from YAML
- Persist changes asynchronously
- Handle concurrent access with locks
- Manage pending bans (crash recovery)

**Caching Strategy:**

- In-memory cache with lazy loading
- Load on first access (getOrCreate)
- Async writes with WorkManager
- Crash recovery via pending bans set

**File Structure:**

```
plugins/DeathBanPro/players/
├── 550e8400-e29b-41d4-a716-446655440000.yml
├── 6ba7b810-9dad-11d1-80b4-00c04fd430c8.yml
└── ...
```

**Persistence Format:**

```yaml
uuid: 550e8400-e29b-41d4-a716-446655440000
offenseLevel: 2
deaths:
  - timestamp: 2024-01-15T14:30:00Z
    world: world
    cause: FALL
    killer: null
    location:
      x: 100.5
      y: 64.0
      z: -200.3
      yaw: 45.0
      pitch: -25.0
currentBan:
  startTime: 2024-01-15T14:30:00Z
  endTime: 2024-01-15T20:30:00Z
  offenseLevel: 2
  deathCause: FALL
lastDeathTime: 2024-01-15T14:30:00Z
```

## Death Event Flow

### Complete Flow Diagram

```
PlayerDeathEvent
       │
       ├─ Check bypass permission
       │   (deathban.bypass)
       │   └─ YES ──→ [SKIP]
       │
       ├─ Check world enabled
       │   └─ NO ──→ [SKIP]
       │
       ├─ Check duplicate
       │   (prevent concurrent)
       │   └─ YES ──→ [SKIP]
       │
       ├─ Route by mode:
       │
       ├─ INDIVIDUAL MODE:
       │   │
       │   ├─ Add pending ban (recovery)
       │   ├─ Load/create PlayerData
       │   ├─ Check offense reset
       │   ├─ Prune old deaths
       │   ├─ Record new death
       │   │
       │   ├─ Check shouldTriggerBan:
       │   │   (deaths_in_window >= max_deaths)
       │   │   │
       │   │   ├─ YES:
       │   │   │   ├─ Increment offense level
       │   │   │   ├─ Apply ban (BanManager)
       │   │   │   ├─ Show theme effects
       │   │   │   ├─ Queue kick (60 ticks later)
       │   │   │   └─ Save PlayerData
       │   │   │
       │   │   └─ NO:
       │   │       ├─ Save PlayerData
       │   │       ├─ Clear pending ban
       │   │       └─ Warn player (lives remaining)
       │   │
       │
       ├─ SHARED MODE:
       │   │
       │   ├─ Try consumeLife()
       │   │   (remove from global/team pool)
       │   │   │
       │   │   ├─ SUCCESS (life available):
       │   │   │   ├─ Decrement pool lives
       │   │   │   ├─ Notify player (lives left)
       │   │   │   ├─ Warn if pool critical
       │   │   │   └─ Save pool state
       │   │   │
       │   │   └─ FAILURE (pool empty):
       │   │       ├─ Apply shared ban
       │   │       ├─ Show theme effects
       │   │       ├─ Queue kick (60 ticks later)
       │   │       └─ Save PlayerData
       │
       └─ Event processing complete
```

### Key Decision Points

1. **Permission Check** - Bypass prevents all bans
2. **World Check** - Enable/disable plugin per-world
3. **Duplicate Check** - Prevent event re-entrance (5 second window)
4. **Mode Routing** - Completely different logic paths
5. **Threshold Check** - Ban only if deaths meet threshold

## Individual Mode Logic

### State Machine

```
Player Joins
    │
    ├─ Load PlayerData
    │   (or create if new)
    │
    ├─ Check if banned:
    │   (currentBan.endTime > now)
    │   │
    │   ├─ YES:
    │   │   ├─ Kick immediately
    │   │   └─ Log ban event
    │   │
    │   └─ NO:
    │       └─ Allow join
    │
    │
Player Dies (Multiple Times)
    │
    ├─ First Death (offense level: 0)
    │   └─ Deaths in window: 1/3
    │      └─ Status: FREE
    │
    ├─ Second Death (offense level: 0)
    │   └─ Deaths in window: 2/3
    │      └─ Status: FREE (warn: 1 life left)
    │
    ├─ Third Death (offense level: 0)
    │   └─ Deaths in window: 3/3
    │      ├─ TRIGGER BAN
    │      ├─ Increment offense level → 1
    │      ├─ Apply ban: 1 hour
    │      ├─ Kick player
    │      └─ Clear death window
    │
    │
Player Returns After Ban Expires
    │
    ├─ Load PlayerData
    ├─ currentBan.endTime < now
    │   └─ Ban is expired, allow join
    │
    ├─ Later: Dies again (offense level: 1)
    │   └─ Deaths in window: 1/3
    │      └─ Status: FREE (plays from clean record)
    │
    │ ... but if dies too soon (within rolling window):
    │
    ├─ Dies within rolling window (e.g., same day)
    │   └─ Deaths in window: 1 (from previous)
    │      │
    │      └─ After 3 more deaths:
    │          ├─ TRIGGER BAN
    │          ├─ Increment offense level → 2
    │          ├─ Apply ban: 6 hours (escalated)
    │          └─ Cycle repeats
    │
    │
Clean Period Triggers Reset
    │
    ├─ Player dies once
    ├─ Waits 7 days (clean-period) without dying
    ├─ Next death loads data
    │   └─ checkOffenseReset returns true
    │       ├─ Reset offense level → 0
    │       ├─ Clear death records
    │       └─ Player starts fresh
```

### Configuration Effects

**Rolling Window Enabled (default: true)**

- Only deaths in last 24h count toward threshold
- Older deaths are pruned
- Allows players to "wait out" old deaths

**Rolling Window Disabled**

- ALL deaths ever recorded count
- Escalation never stops (without reset)
- More punitive

**Offense Reset Enabled (default: true)**

- After clean period (7 days), offense resets
- Provides path to redemption
- Requires no deaths during period

**Offense Reset Disabled**

- Players never escape high offense levels
- Previous bans always count
- Very punitive

## Shared Mode Logic

### Pool State Machine

```
Server Start (or reload)
    │
    ├─ Load/create global pool
    │   └─ Initial lives: config.shared-lives.default-lives (10)
    │   └─ Max lives: config.shared-lives.max-lives (20)
    │
    │
Player Dies
    │
    ├─ Check team assignment
    │   ├─ If in team pool → use team pool
    │   └─ If no team → use global pool
    │
    ├─ Try consumeLife():
    │   │
    │   ├─ IF pool.lives > 0:
    │   │   ├─ Decrement pool.lives
    │   │   ├─ Save pool state
    │   │   ├─ Notify player: "Life consumed! X/20 left"
    │   │   │
    │   │   └─ Pool status:
    │   │       ├─ if lives == 1: warn "FINAL LIFE!"
    │   │       ├─ if lives <= 3: warn "CRITICAL!"
    │   │       └─ Status: ALIVE
    │   │
    │   └─ ELSE (pool.lives == 0):
    │       ├─ Return false (no life available)
    │       ├─ Trigger ban:
    │       │   ├─ Apply shared ban (duration: 1h)
    │       │   ├─ Create BanRecord
    │       │   ├─ Show effects
    │       │   └─ Kick player
    │       └─ Status: BANNED
    │
    │
Team Pool Dynamics (if enabled)
    │
    ├─ /deathban team create "Awesome Team"
    │   ├─ Create new pool for team
    │   └─ Lives initialized to default
    │
    ├─ /deathban team join "Awesome Team"
    │   ├─ Add player to team pool
    │   └─ Future deaths consume from team pool
    │
    ├─ Team members die
    │   ├─ Shared lives consumed (affects all team members)
    │   └─ No individual offense levels (shared fate)
    │
    ├─ /deathban team leave
    │   ├─ Remove player from team pool
    │   └─ Return to global pool
    │
    │
Admin Commands
    │
    ├─ /deathban lives add
    │   ├─ Add 1 life to global pool (if < max)
    │   └─ Useful for revival events
    │
    ├─ /deathban lives set 5
    │   ├─ Set exact number (1-20)
    │   └─ Useful for emergency resets
```

### Key Differences from Individual Mode

| Aspect | Individual | Shared |
|--------|-----------|--------|
| Offense Level | Per player | N/A (global threshold) |
| Deaths Tracked | Individual | Pool lives |
| Escalation | Per-player offense | Fixed ban duration |
| Reset | Per-player reset | Global reset only |
| Team Support | No | Yes (optional) |
| Shared Fate | No | Yes |

## Configuration Precedence

### Priority Order

1. **Command Line Arguments** (none - not applicable)
2. **Config File** (`plugins/DeathBanPro/config.yml`)
3. **Default Values** (hardcoded in Settings class)

### Example Precedence Resolution

```kotlin
// For rolling-window.duration:

// 1. Check config.yml
val duration = config.getString("rolling-window.duration")
// Result: "24h" from file, or null if missing

// 2. Use default if missing
val resolved = duration ?: "24h"

// 3. Parse with TimeUtil
val finalDuration = TimeUtil.parseDuration(resolved)
// Result: Duration.ofHours(24)
```

### Loading Order on Startup

```
1. saveDefaultConfig()
   └─ Copies plugin.yml defaults if config.yml missing

2. settings = Settings(config)
   ├─ Parse all config values
   ├─ Apply defaults for missing keys
   └─ Validate (implicit - no validation errors possible)

3. messages = Messages(file)
   └─ Load message templates from messages.yml

4. themeManager.setActiveTheme(settings.themeId)
   └─ Load and apply theme

5. offenseManager = OffenseManager(settings)
   └─ Capture settings for death calculations

6. dataManager = PlayerDataManager(folder)
   └─ Initialize cache, ready for lazy loading

7. Plugin ready to process events
```

### Reload Flow

```
/deathban reload
    │
    ├─ reloadConfig()
    │   └─ Reload from disk
    │
    ├─ settings = Settings(config)
    │   └─ Re-parse all values
    │
    ├─ messages.reload()
    │   └─ Re-load message templates
    │
    ├─ themeManager.reload()
    │   └─ Reload theme files
    │
    ├─ dataManager.clearCache()
    │   └─ Force reload from disk on next access
    │
    ├─ sharedLivesManager?.reload()
    │   └─ Reload pool state from disk
    │
    └─ Ready for new events with new config
```

## Data Persistence Strategy

### Persistence Layer Design

```
In-Memory Layer         File System Layer
┌──────────────────┐    ┌──────────────────┐
│ PlayerData Cache │◄──►│ players/         │
│ {uuid -> data}   │    │ {uuid}.yml       │
└──────────────────┘    └──────────────────┘
      ▲                        ▲
      │ saveAsync()            │ (async write)
      │                        │
      └────────────────────────┘

┌──────────────────┐    ┌──────────────────┐
│ SharedLivesPool  │◄──►│ shared-lives/    │
│ (in-memory)      │    │ *.yml            │
└──────────────────┘    └──────────────────┘
      ▲
      │ Save on shutdown
      │
```

### Write Strategy

#### Synchronous Write

**Used for:** Shutdown, explicit commands

```kotlin
dataManager.save(data)
// Blocks until I/O complete
// Ensures durability before next operation
```

#### Asynchronous Write

**Used for:** Death events, normal operation

```kotlin
dataManager.saveAsync(data)
// Returns immediately
// Queued for background write
// Better performance, eventual consistency
```

### Crash Recovery

**Problem:** Server crash during async write → data loss

**Solution:** Pending bans set

```kotlin
// When death processed:
dataManager.addPendingBan(player.uuid)

// Ban applied:
banManager.applyBan(player, data, cause)
// (saves async)

// Server crashes before write...

// Server restarts:
processPendingBans()
// Checks all pending bans
// If player online & banned: kick them
// Clear pending flag
```

### Data Loading

#### Lazy Loading on First Access

```
getOrCreate(uuid):
    if uuid in cache:
        return cache[uuid]
    else:
        data = loadFromFile(uuid)
        cache[uuid] = data
        return data
```

#### Cache Invalidation

```
clearCache():
    cache.clear()
    // Next access reloads from disk
    // Used after manual file edits
    // Used on /deathban reload
```

### Persistence Guarantees

**Strong Guarantees:**

- Death records never lost (written before response)
- Bans never lost (written before kick)
- Shutdown flushes all pending writes
- All operations serialized per player

**Eventual Consistency:**

- Multiple deaths may queue async writes
- Later writes override earlier ones
- Within few seconds of finality

**Atomicity:**

- Per-file (full PlayerData written atomically)
- If write fails, previous version unchanged
- No partial record corruption

## Concurrency & Thread Safety

### Thread Model

```
Main Thread                     Background Threads
(Bukkit/Paper)                  (Work Queue)
       │                              │
       ├─ DeathEvent               ┌──┴──┐
       │   └─ addPendingBan()    │        │
       │   └─ getOrCreate()      │ I/O    │
       │   └─ applyBan()         │ Writer │
       │   └─ saveAsync()        │        │
       │       └──queue task ─────►        │
       │                         │        │
       ├─ Command execution       └──┬────┘
       │   └─ check              ┌──────┐
       │   └─ pardon          │  │
       │   └─ reset           │  │
       │   └─ reload          │  │
       │                      │  │
       └─ Listener events     └──┘
           (all on main)
```

### Synchronization Points

**Protected Access:**

- PlayerData Cache: ConcurrentHashMap (lock-free for most operations)
- PlayerData mutation: Serialized per-player file writes
- SharedLivesPool: Copy-on-write for team list

**Lock-free Operations:**

- Reads from cache (ConcurrentHashMap)
- Atomic increments (totalBansIssued)

**Serialized Operations:**

- File I/O (one write per player per operation)
- Theme loading (synchronized on first use)
- Pool updates (atomic increment/decrement)

### Data Race Prevention

**Scenario 1: Concurrent Deaths**

```
Player dies twice simultaneously
    │
    ├─ Thread 1: DeathEvent for fall
    │   ├─ getOrCreate(uuid) → data1
    │   ├─ Increment offense
    │   └─ saveAsync(data1)
    │
    ├─ Thread 2: DeathEvent for drown
    │   ├─ getOrCreate(uuid) → data1 (from cache)
    │   ├─ Increment offense
    │   └─ saveAsync(data1)
    │
    ├─ Potential Issue: Both read same data
    │   └─ Result: Both increments applied
    │      (may miss one death)
    │
    └─ Mitigation: processingDeaths set
        ├─ Thread 1: adds uuid → success
        ├─ Thread 2: add uuid → fails
        ├─ Thread 2: returns early (SKIPPED)
        └─ Result: Only one processed
```

**Scenario 2: Reload During Write**

```
/deathban reload
    ├─ clearCache()
    └─ Running async write completes
        └─ Result: Stale data discarded
           (fresh load from disk on next access)
```

---

**Last Updated:** Based on DeathBan Pro v1.0+  
**Threading Model:** Bukkit scheduler based  
**Data Format:** YAML with async I/O
