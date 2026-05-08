# DeathBan Pro - Configuration Examples

This document provides real-world configuration examples for different game modes and use cases.

## Table of Contents

- [Hardcore Mode (1 Death = Ban)](#hardcore-mode-1-death--ban)
- [Survival Multiplayer (Escalating)](#survival-multiplayer-escalating)
- [PvP Hardcore](#pvp-hardcore)
- [Creative Safe Zone](#creative-safe-zone)
- [Team-Based Multiplayer](#team-based-multiplayer)
- [Seasonal Events](#seasonal-events)
- [Difficulty Progression](#difficulty-progression)
- [Discord/External Integration](#discordexternal-integration)
- [Multi-World Setup](#multi-world-setup)

## Hardcore Mode (1 Death = Ban)

**Use Case:** Pure hardcore experience - one death = permanent ban (until appeal)

### Configuration

```yaml
# plugins/DeathBanPro/config.yml

# ===== Core Settings =====
debug: false
update-check: true
metrics: true
mode: individual

# ===== Rolling Window =====
# Disable rolling window for permanent escalation
rolling-window:
  enabled: false        # All deaths count toward offense
  duration: 24h
  max-deaths: 1         # Only 1 death before ban (ignored when disabled)

# ===== Ban Durations =====
# Very long bans for hardcore
ban-durations:
  1: 999999h            # Offense 1: ~114 years (effectively permanent)
  2: 999999h
  3: 999999h
  4: 999999h
  5: 999999h

# ===== Offense Reset =====
# Disable reset - no redemption
offense-reset:
  enabled: false
  clean-period: 168h

# ===== World Configuration =====
enabled-worlds:
  - world
  - world_nether
  - world_the_end

# ===== Theme =====
theme: halloween        # Spooky theme for hardcore
theme-sounds: true
theme-particles: true
```

### Result

- **First death** → Instant ban (effectively permanent)
- **No reset** → Cannot return without admin intervention
- **All worlds** → Hardcore everywhere
- **Scariest experience** → Perfect for hardcore purists

### Command Examples

```bash
# Admin: Pardon a player (appeals)
/deathban pardon <player>

# Check ban status
/deathban check <player>

# Admin: Completely reset (for appeals)
/deathban reset <player>
```

## Survival Multiplayer (Escalating)

**Use Case:** Balanced survival server with escalating consequences

### Configuration

```yaml
# plugins/DeathBanPro/config.yml

# ===== Core Settings =====
debug: false
update-check: true
metrics: true
mode: individual

# ===== Rolling Window =====
# 24-hour rolling window - dies reset daily
rolling-window:
  enabled: true
  duration: 24h         # Deaths within 24 hours count
  max-deaths: 3         # 3 deaths = ban

# ===== Ban Durations =====
# Escalating: short → medium → long
ban-durations:
  1: 1h                 # First offense: 1 hour
  2: 6h                 # Second offense: 6 hours
  3: 24h                # Third offense: 1 day
  4: 72h                # Fourth offense: 3 days
  5: 168h               # Fifth+ offense: 1 week

# ===== Offense Reset =====
# Reset after 1 week of clean play
offense-reset:
  enabled: true
  clean-period: 168h    # 7 days

# ===== World Configuration =====
enabled-worlds:
  - world
  - world_nether

disabled-worlds:
  - world_creative      # Creative mode = no bans

# ===== Theme =====
theme: default
theme-sounds: true
theme-particles: true
```

### Result

- **Forgiving start:** 3 free deaths per day
- **Escalating punishment:** 1h → 6h → 1 day → 3 days → 1 week
- **Redemption path:** 7 days clean play resets offense
- **Safe zones:** Creative mode has no bans
- **Balanced difficulty:** Not too punitive, but consequences for repeated deaths

### Command Examples

```bash
# Players: Check own status
/deathban check

# Players: Check status on join (auto-displayed if banned)
# Shows: "Ban expires in 4 hours 23 minutes"

# Admins: Warn problem player
/deathban pardon <player>    # Remove current ban
# Message: "Consider playing more carefully - next ban is 6 hours"

# Admins: Remove escalation for reformed player
/deathban reset <player>      # Back to offense level 0
```

### Gameplay Example

```
Day 1:
  - 3 deaths in world → offense level 0, warning
  - No ban yet

Day 2 (dies again):
  - 1 death within 24h → 3 deaths total in window
  - Ban applied: 1 hour
  - "You died from fall. Banned for 1 hour. Return at 19:30"

Hour 3: Returns from ban

Week 1 (never dies again):
  - 7 days pass without death → clean period reached
  - Offense level resets to 0

Week 2 (dies 3x again):
  - 3 deaths in 24h → ban applied
  - But offense level still 0 (reset)
  - Ban: 1 hour (starts over at level 1)
```

## PvP Hardcore

**Use Case:** PvP-focused server where deaths are mostly combat

### Configuration

```yaml
# plugins/DeathBanPro/config.yml

# ===== Core Settings =====
debug: false
update-check: true
metrics: true
mode: individual

# ===== Rolling Window =====
# Shorter window - PvP deaths more forgiving
rolling-window:
  enabled: true
  duration: 6h          # Shorter window (3x combat in 6h = ban)
  max-deaths: 3

# ===== Ban Durations =====
# More forgiving - combat has high death rate
ban-durations:
  1: 15m                # First: 15 minutes
  2: 1h                 # Second: 1 hour
  3: 3h                 # Third: 3 hours
  4: 6h                 # Fourth: 6 hours
  5: 12h                # Fifth+: 12 hours

# ===== Offense Reset =====
# Reset quickly in combat
offense-reset:
  enabled: true
  clean-period: 72h     # 3 days (shorter than survival)

# ===== World Configuration =====
# PvP zones only
enabled-worlds:
  - pvp_arena
  - pvp_warzone

disabled-worlds:
  - world                # Regular survival = no bans
  - world_spawn         # Spawn area = no bans

# ===== Bypass =====
bypass-permission: deathban.bypass

# ===== Theme =====
theme: default
theme-sounds: true
theme-particles: true
```

### Result

- **Combat-focused:** Only affects PvP arenas
- **Forgiving:** 15 min first ban, resets in 3 days
- **High frequency:** 6-hour window for PvP combat
- **Safe spawn:** Main world and spawn unaffected

### Command Examples

```bash
# Give PvP bypass for new players
/lp user <newbie> permission set deathban.bypass true

# Remove bypass after tutorial
/lp user <newbie> permission unset deathban.bypass

# Check arena ban status
/deathban check <player>
```

## Creative Safe Zone

**Use Case:** Large server with creative and survival zones

### Configuration

```yaml
# plugins/DeathBanPro/config.yml

# ===== Core Settings =====
debug: false
update-check: true
metrics: true
mode: individual

# ===== Rolling Window =====
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 2         # More strict - only 2 deaths

# ===== Ban Durations =====
ban-durations:
  1: 2h
  2: 8h
  3: 24h
  4: 72h
  5: 168h

# ===== Offense Reset =====
offense-reset:
  enabled: true
  clean-period: 72h

# ===== World Configuration =====
# ONLY survival world has bans
enabled-worlds:
  - survival_world

# Explicitly disable creative
disabled-worlds:
  - creative_world
  - creative_building
  - creative_plots
  - world_spawn

# ===== Theme =====
theme: default
```

### Result

- **Clearly separated:** Survival = hardcore, Creative = sandbox
- **No leakage:** Deaths in creative don't affect survival record
- **Player choice:** Choose mode without penalty
- **Safe experimentation:** Test ideas in creative risk-free

### Typical Setup

```
Server Layout:
├── Survival World (bans apply)
│   └── Plugin: ENABLED
├── Creative World (no bans)
│   └── Plugin: DISABLED
└── Spawn Area (neutral)
    └── Plugin: DISABLED
```

## Team-Based Multiplayer

**Use Case:** Factions, teams, or clan-based PvP

### Configuration

```yaml
# plugins/DeathBanPro/config.yml

# ===== Core Settings =====
debug: false
update-check: true
metrics: true

# ===== SHARED MODE FOR TEAM LIVES =====
mode: shared

# ===== Shared Lives Configuration =====
shared-lives:
  default-lives: 15     # Each team starts with 15 lives
  max-lives: 30         # Can earn/gift up to 30
  allow-teams: true     # Teams can be created
  empty-pool-ban: 1h    # Ban when team pool empty

# ===== Rolling Window =====
# Not used in shared mode, but required in config
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3

# ===== Offense Reset =====
# Not used in shared mode
offense-reset:
  enabled: true
  clean-period: 168h

# ===== World Configuration =====
enabled-worlds:
  - world_faction_wars

disabled-worlds:
  - world_safe
  - world_spawn

# ===== Theme =====
theme: default
theme-sounds: true
theme-particles: true
```

### Team Workflow

```bash
# Team leader creates team pool
/deathban team create "Team Alpha"

# Members join team
/deathban team join "Team Alpha"

# Shared pool management
/deathban lives               # View pool status
/deathban lives add           # Add 1 life (earned)
/deathban lives set 20        # Admin: set exact number

# View team
/deathban team list           # See all teams (admin)
/deathban team info "Team Alpha"  # Team details

# Leave team
/deathban team leave

# Earn lives through events
# /deathban lives add (can be limited by permission)
```

### Gameplay Example

```
Setup:
  - Team Alpha created: 15/30 lives
  - Members: Alex, Bob, Carol

Day 1:
  - Alex dies (fall) → Team lives: 14/30
  - Bob dies (PvP) → Team lives: 13/30
  - Carol dies (lava) → Team lives: 12/30

Week 1:
  - 50+ more deaths → Team lives: 0/30
  - Next death → TEAM BANNED for 1 hour
  - ALL members banned (shared fate)

Earn lives back:
  - Complete quest → /deathban lives add
  - Team lives: 1/30
  - Members unbanned immediately
```

### Configuration Variations

**Hardcore Teams (3 lives total)**

```yaml
shared-lives:
  default-lives: 3      # Only 3 lives for entire team
  max-lives: 5
  allow-teams: true
  empty-pool-ban: 24h   # Full day ban if depleted
```

**Forgiving Teams (50 lives)**

```yaml
shared-lives:
  default-lives: 50     # Very forgiving
  max-lives: 100        # Team members can farm lives
  allow-teams: true
  empty-pool-ban: 15m   # Quick ban as warning
```

## Seasonal Events

**Use Case:** Different rules for events or seasons

### Base Configuration (Regular Season)

```yaml
# plugins/DeathBanPro/config.yml
mode: individual
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3
ban-durations:
  1: 1h
  2: 6h
  3: 24h
  4: 72h
  5: 168h
```

### Hardcore Event (Temporary)

**When:** Special event weekend

**Change:**

```bash
# SSH into server
vi plugins/DeathBanPro/config.yml

# Change:
mode: shared              # Shared lives for team experience
shared-lives:
  default-lives: 1        # Hardcore!
  max-lives: 1
  empty-pool-ban: 168h    # 1 week ban

# Reload:
# /deathban reload
```

### Example Event Script

```bash
#!/bin/bash
# hardcore-event.sh - Toggle hardcore mode

CONFIG="plugins/DeathBanPro/config.yml"

if grep -q "default-lives: 1" "$CONFIG"; then
    # Currently hardcore, revert to normal
    sed -i 's/default-lives: 1/default-lives: 10/' "$CONFIG"
    sed -i 's/mode: shared/mode: individual/' "$CONFIG"
    echo "Event ended - reverted to normal"
else
    # Currently normal, enable hardcore
    sed -i 's/default-lives: 10/default-lives: 1/' "$CONFIG"
    sed -i 's/mode: individual/mode: shared/' "$CONFIG"
    echo "Event started - hardcore mode enabled"
fi

# Reload in-game
# Send command via RCON or script
```

## Difficulty Progression

**Use Case:** Players unlock harder challenges over time

### Beginner World (Very Forgiving)

```yaml
# config-beginner.yml
rolling-window:
  enabled: true
  duration: 48h         # Deaths count for 2 days
  max-deaths: 5         # 5 free deaths

ban-durations:
  1: 30m                # Short bans
  2: 1h
  3: 2h
  4: 4h
  5: 8h

offense-reset:
  enabled: true
  clean-period: 24h     # Reset after 1 day clean
```

### Intermediate World (Balanced)

```yaml
# config-intermediate.yml
rolling-window:
  enabled: true
  duration: 24h         # Deaths count for 1 day
  max-deaths: 3         # 3 free deaths

ban-durations:
  1: 1h
  2: 6h
  3: 24h
  4: 72h
  5: 168h

offense-reset:
  enabled: true
  clean-period: 72h     # Reset after 3 days clean
```

### Hardcore World (Difficult)

```yaml
# config-hardcore.yml
rolling-window:
  enabled: false        # All deaths count forever

ban-durations:
  1: 168h               # 1 week minimum
  2: 336h               # 2 weeks
  3: 999999h            # Effectively permanent
  4: 999999h
  5: 999999h

offense-reset:
  enabled: false        # No redemption
```

### Implementation

```bash
# Use one config per world
# Symlink to create multiple configs:
ln -s config-beginner.yml plugins/DeathBanPro/config-beginner.yml
ln -s config-intermediate.yml plugins/DeathBanPro/config-intermediate.yml
ln -s config-hardcore.yml plugins/DeathBanPro/config-hardcore.yml

# Each world uses its own config by modifying the plugin
# Or use a config manager that supports per-world settings
```

## Discord/External Integration

**Use Case:** Notify Discord when bans occur

### Setup

1. **Discord Bot Token**
   - Create bot in Discord Developer Portal
   - Get webhook URL (easiest) or bot token

2. **Custom Plugin Integration**

   ```kotlin
   // Create a DeathBan listener that sends to Discord
   
   @EventHandler
   fun onPlayerDeath(event: PlayerDeathEvent) {
       val deathban = Bukkit.getPluginManager().getPlugin("DeathBanPro") as? DeathBanPlugin
       if (deathban != null) {
           val data = deathban.dataManager.getOrCreate(event.entity.uniqueId)
           if (deathban.offenseManager.shouldTriggerBan(data)) {
               // Send to Discord webhook
               sendDiscordNotification(event.entity, data)
           }
       }
   }
   
   fun sendDiscordNotification(player: Player, data: PlayerData) {
       val webhook = "https://discord.com/api/webhooks/..."
       val duration = settings.getBanDuration(data.offenseLevel + 1)
       
       val json = """
       {
           "content": "",
           "embeds": [{
               "title": "🚫 Player Banned",
               "description": "${player.name} was banned",
               "fields": [
                   {"name": "Duration", "value": "$duration"},
                   {"name": "Offense Level", "value": "${data.offenseLevel}"},
                   {"name": "Deaths", "value": "${data.deaths.size}"}
               ],
               "color": 16711680
           }]
       }
       """
       
       // POST to webhook
       // (use HTTP library like OkHttp or HttpClient)
   }
   ```

3. **Example Discord Embed**

   ```
   🚫 Player Banned
   
   Duration: 1 hour
   Offense Level: 1
   Deaths in window: 3
   Cause: FALL
   ```

### Alternative: Custom Logger

```bash
# Parse DeathBan logs and send to Discord via separate script
# Run every minute via cron

#!/bin/bash
# discord-notifier.sh

LAST_LINE=$(tail -1 ~/.last_deathban_line)
NEW_LINES=$(tail -n +$LAST_LINE logs/latest.log | grep "Banned.*for")

for line in $NEW_LINES; do
    # Extract player name and duration
    PLAYER=$(echo $line | grep -oP '(?<=Banned )\w+')
    DURATION=$(echo $line | grep -oP '\d+\s\w+')
    
    # Send to Discord
    curl -X POST -H 'Content-Type: application/json' \
        -d '{"content":"'$PLAYER' banned for '$DURATION'"}' \
        $WEBHOOK_URL
done
```

## Multi-World Setup

**Use Case:** Multiple servers or instances

### Shared Central Config

```yaml
# This config is replicated across all servers
# via Git, rsync, or shared storage

# ===== Core Settings =====
mode: individual

# ===== Rolling Window =====
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3

# ===== Ban Durations =====
# Same across all servers
ban-durations:
  1: 1h
  2: 6h
  3: 24h
  4: 72h
  5: 168h

# ===== Offense Reset =====
offense-reset:
  enabled: true
  clean-period: 168h

# ===== Per-World Settings =====
enabled-worlds:
  - world
  - world_nether
  - nether_realm
  - sky_world
```

### Player Data Synchronization

**Problem:** Ban data stored locally per server

**Solution 1: Shared Storage (Recommended)**

```bash
# Use network mount or shared database
# All servers read/write to same directory

# Setup:
# - NFS mount: /mnt/shared/deathban/
# - Symlink in each server:
#   plugins/DeathBanPro/players → /mnt/shared/deathban/players/

# Pros: Single source of truth, instant sync
# Cons: Network dependency, NFS latency
```

**Solution 2: Periodic Sync**

```bash
#!/bin/bash
# sync-deathban.sh - Run every 15 minutes

# Pull latest from primary server
rsync -avz \
    server1:/var/games/minecraft/plugins/DeathBanPro/players/ \
    /var/games/minecraft/plugins/DeathBanPro/players/

# Pros: Decoupled, fault-tolerant
# Cons: Eventual consistency (slight delay possible)
```

**Solution 3: Database Backend** (Future)

```kotlin
// If implemented: Write PlayerData to shared database
// Instead of YAML files per-server
// All servers query same source

val playerData = database.query(
    "SELECT * FROM player_data WHERE uuid = ?",
    uuid
)
```

### Example Multi-World Network

```
Network Setup:
├── Server 1 (Survival)
│   ├── world (bans apply)
│   └── plugins/DeathBanPro/players/
│       └── synced to shared storage
│
├── Server 2 (PvP Arena)
│   ├── pvp_world (bans apply)
│   └── plugins/DeathBanPro/players/
│       └── synced to shared storage
│
├── Server 3 (Creative)
│   ├── creative_world (bans disabled)
│   └── plugins/DeathBanPro/players/
│       └── synced to shared storage
│
└── Shared Storage (NFS/Database)
    ├── Bans sync instantly
    ├── Players banned on all servers
    └── Single offense level across network
```

### BungeeCord Integration

```yaml
# Main config (copied to all servers)
# With symlinked players/ directory

# Result:
# Player banned on any server → instantly banned on all
# Player checks status on any server → sees global ban
# Single unified hardcore experience
```

---

**Last Updated:** Based on DeathBan Pro v1.0+  
**Customizable:** All examples modifiable for your needs  
**Default Config:** See `config.yml` in plugin JAR
