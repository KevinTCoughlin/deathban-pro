# DeathBan Pro - Troubleshooting Guide

This guide covers common issues, debugging techniques, and solutions for DeathBan Pro.

## Table of Contents

- [Common Issues & Solutions](#common-issues--solutions)
- [Debug Logging Setup](#debug-logging-setup)
- [Performance Tuning](#performance-tuning)
- [Data Migration & Recovery](#data-migration--recovery)
- [Verification & Testing](#verification--testing)
- [FAQ](#faq)

## Common Issues & Solutions

### Bans Not Applying to Players

**Symptom:** Players die but don't get banned.

#### Diagnosis

1. **Check if plugin is enabled**
   ```
   /plugins
   ```
   Should show: `[DeathBanPro]` (green = enabled, red = error)

2. **Enable debug logging** (see [Debug Logging Setup](#debug-logging-setup))

3. **Check player permissions**
   ```
   /lp user <player> permission info | grep deathban
   ```
   Should NOT have `deathban.bypass` permission

4. **Verify world is enabled**
   - Check `config.yml` for `enabled-worlds:` and `disabled-worlds:`
   - If world is in `disabled-worlds`, plugin won't process

#### Solutions

**Solution 1: Plugin not enabled**
```bash
# Check console for errors
# If "DeathBan Pro disabled", check error messages
# Common: plugin.yml syntax error
# Fix: Use online YAML validator to check plugin.yml
```

**Solution 2: World disabled**
```yaml
# In config.yml, either:

# Option A: Enable specific worlds
enabled-worlds:
  - world
  - world_nether

# Option B: Disable specific worlds
disabled-worlds:
  - world_creative

# Then: /deathban reload
```

**Solution 3: Bypass permission active**
```bash
# Remove bypass permission
/lp user <player> permission unset deathban.bypass

# Verify
/lp user <player> permission info deathban
```

**Solution 4: Rolling window threshold too high**
```yaml
# config.yml - check:
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3      # Must die 3+ times in 24h to ban

# If max-deaths: 10, need 10 deaths to trigger
# Reduce to 1-3 for testing
  max-deaths: 1

# Then: /deathban reload
```

### Players Not Banned on Join

**Symptom:** Banned player joins and isn't kicked, or gets kicked late.

#### Diagnosis

1. **Check if player is actually banned**
   ```
   /deathban check <player>
   ```
   Should show ban expiration time

2. **Check if ban is expired**
   ```
   Status: BANNED
   Ban expires in: [negative number] = EXPIRED
   Ban expires at: [past time] = EXPIRED
   ```

3. **Check player data file**
   ```bash
   # Browse: plugins/DeathBanPro/players/{uuid}.yml
   # Should contain:
   # currentBan:
   #   endTime: 2024-01-15T20:30:00Z
   ```

#### Solutions

**Solution 1: Ban is expired**
- Expected behavior: expired bans don't prevent join
- Ban duration may be set too short in config
```yaml
ban-durations:
  1: 1h        # Too short? Increase:
  1: 6h
```

**Solution 2: Data file corrupted**
```bash
# Check file format
cat plugins/DeathBanPro/players/{uuid}.yml

# Should be valid YAML
# If error, delete file (player record resets)
rm plugins/DeathBanPro/players/{uuid}.yml
```

**Solution 3: Late kick (10-30 seconds delay)**
- This is normal (by design)
- Ban applied at death, kick delayed 60 ticks (3 seconds)
- Plus server latency
- If excessively delayed (>30s), check logs for errors

### Plugin Not Saving Data

**Symptom:** Changes don't persist (bans cleared after restart, offense levels reset)

#### Diagnosis

1. **Check if data folder exists**
   ```bash
   ls -la plugins/DeathBanPro/
   ```
   Should show: `players/` and `shared-lives/` directories

2. **Check file permissions**
   ```bash
   ls -la plugins/DeathBanPro/players/
   # Should be writable by server user
   ```

3. **Check disk space**
   ```bash
   df -h
   # Should have >100MB free
   ```

4. **Check for write errors in logs**
   ```bash
   tail -50 logs/latest.log | grep -i "deathban\|error\|write"
   ```

#### Solutions

**Solution 1: Directory doesn't exist**
```bash
# Create directory
mkdir -p plugins/DeathBanPro/players
mkdir -p plugins/DeathBanPro/shared-lives

# Set permissions
chmod 755 plugins/DeathBanPro
chmod 755 plugins/DeathBanPro/players
chmod 755 plugins/DeathBanPro/shared-lives

# Restart server
```

**Solution 2: Permissions issue**
```bash
# Ensure server user can write
chown -R minecraft:minecraft plugins/DeathBanPro
chmod -R 755 plugins/DeathBanPro

# Or make world-writable (less secure)
chmod -R 777 plugins/DeathBanPro
```

**Solution 3: Out of disk space**
```bash
# Check available space
df -h

# If <100MB, clean up:
# - Delete old backups
# - Delete old logs (logs/ directory)
# - Verify issue resolved: /deathban reload
```

**Solution 4: Server crash during write**
- Some data loss possible if server hard-crashed
- Check for pending bans:
```bash
# On restart, server auto-processes pending bans
# Check logs: "Processing X pending bans"
```

### Configuration Not Loading

**Symptom:** Config changes don't take effect after `/deathban reload`

#### Diagnosis

1. **Check config file syntax**
   ```bash
   # Validate YAML
   yaml-lint plugins/DeathBanPro/config.yml
   
   # Or use online validator:
   # https://www.yamllint.com/
   ```

2. **Check for typos in keys**
   ```yaml
   # Common typos:
   mode: individual        # Wrong: should be "shared"
   rolling_window:         # Wrong: should be "rolling-window" (hyphen)
   
   # Correct:
   mode: individual
   rolling-window:
     enabled: true
   ```

3. **Check that reload command works**
   ```
   /deathban reload
   # Should output: "DeathBan Pro reloaded"
   # Check console for error messages
   ```

#### Solutions

**Solution 1: YAML syntax error**
- Copy config.yml from git repo (fresh copy)
- Or use YAML validator to find syntax errors
- Common errors:
  - Inconsistent indentation (mix tabs/spaces)
  - Mismatched colons
  - Improper list formatting

**Solution 2: Typo in config key**
```bash
# Verify all keys are correct:
grep -n "rolling-window:" plugins/DeathBanPro/config.yml
# vs
grep -n "rolling_window:" plugins/DeathBanPro/config.yml
```

**Solution 3: Value type mismatch**
```yaml
# Wrong types:
mode: shared              # OK
max-deaths: "3"           # Wrong: should be 3 (number, not string)
enabled: "true"           # Wrong: should be true (boolean)

# Correct types:
mode: shared              # String
max-deaths: 3             # Number
enabled: true             # Boolean
duration: 24h             # String (duration parser)
```

**Solution 4: Reload command permission**
```bash
# Must have permission:
/lp user <player> permission set deathban.admin

# Or use OP:
/op <player>
```

## Debug Logging Setup

### Enable Debug Mode

```yaml
# In config.yml:
debug: true

# Then reload:
# /deathban reload
```

### Log Output

With debug enabled, you'll see messages like:

```
[00:15:30 INFO] [DEBUG] Steve has bypass permission, skipping
[00:15:30 INFO] [DEBUG] World world_nether is disabled, skipping
[00:15:31 INFO] [DEBUG] Recorded death for Steve: FALL in world
[00:15:31 INFO] [DEBUG] Deaths in window: 1/3
[00:15:31 INFO] [DEBUG] Reset offense level for Steve due to clean period
[00:15:31 INFO] [DEBUG] Shared life consumed for Steve. Pool: 9/10
[00:15:32 INFO] [DEBUG] Shared pool empty - banning Steve
```

### Log File Location

```bash
# Main log
tail -f logs/latest.log

# Grep for DeathBan logs
tail -f logs/latest.log | grep -i deathban

# Search for specific player
grep "Steve" logs/latest.log | head -20
```

### Common Debug Output Explained

```
[00:15:30 INFO] [DEBUG] Recorded death for Steve: FALL in world
└─ Player Steve died from fall damage in world 'world'

[00:15:30 INFO] [DEBUG] Deaths in window: 2/3
└─ 2 deaths counted, need 3 to trigger ban (1 more death)

[00:15:30 INFO] [DEBUG] Recorded death for Steve: SUFFOCATION in world_nether
└─ Indicates world_nether is enabled (not skipped)

[00:15:30 INFO] [DEBUG] World world_creative is disabled, skipping
└─ Plugin ignored death in world_creative (as configured)

[00:15:30 INFO] Banned Steve for 1h (offense level 1)
└─ Ban applied! Check config.yml for durations by level
```

### Enable DEBUG Logs in Different Ways

**Temporary (until server restart):**
```
/deathban reload
# Edit config.yml to debug: true
# Reload
/deathban reload
```

**Permanent (persists after restart):**
```bash
# Edit config.yml
vim plugins/DeathBanPro/config.yml
# Set: debug: true
# Save and restart server
```

**Via Bukkit logging:**
```
# In server.properties:
# No direct way, but you can:
# 1. Enable debug in DeathBan config.yml
# 2. Or check logs for DeathBan messages
grep "deathban\|DeathBan\|DEBUG" logs/latest.log
```

## Performance Tuning

### Player Load Optimization

**Issue:** Server lags when many players online

**Investigation:**
```bash
# Check async tasks
/timings report

# Look for DeathBan tasks taking long
# If you see PlayerDataManager tasks taking >50ms, investigate
```

**Solutions:**

1. **Reduce data sync frequency**
   ```yaml
   # In config.yml - no direct setting, but defaults are optimized
   # Data syncs only on:
   # - Death event
   # - Command execution
   # - Server shutdown
   ```

2. **Disable metrics (minor improvement)**
   ```yaml
   metrics: false
   ```

3. **Disable update checking**
   ```yaml
   update-check: false
   ```

4. **Profile with debug logs**
   ```yaml
   debug: true  # Adds overhead, use temporarily only
   ```

### Memory Optimization

**Issue:** Server memory usage grows over time

**Investigation:**
```bash
# Check cache size
# DeathBan caches PlayerData in memory
# Default: lazy-load (only loaded players cached)

# Monitor with timings:
/timings report
# Look for "Memory" section
```

**Solutions:**

1. **Clear cache on reload**
   ```
   /deathban reload
   ```
   This clears all cached PlayerData (reloaded on next access)

2. **Restart server** (nuclear option)
   - Clears all JVM memory
   - Complete reset

3. **Disable file watching** (not applicable here)

### Database Query Optimization

Not applicable - DeathBan uses YAML files, not a database.

### Ban Check Performance

```yaml
# Fastest ban checking:
# 1. First check done on join (fast, single file read)
# 2. No repeated checks during gameplay (fast)
# 3. Only re-checked if player uses /deathban check command
```

## Data Migration & Recovery

### Backup Player Data

```bash
# Full backup
tar -czf deathban-backup-$(date +%s).tar.gz plugins/DeathBanPro/

# List backups
ls -lh deathban-backup-*.tar.gz

# Restore backup
tar -xzf deathban-backup-1234567890.tar.gz
# This restores to original location
```

### Manual Player Data Recovery

**Scenario:** A player's data file is corrupted

```bash
# 1. Identify the player UUID
# Can use: lp user <player> info | grep "UUID"
# Example: 550e8400-e29b-41d4-a716-446655440000

# 2. Locate file
cat plugins/DeathBanPro/players/550e8400-e29b-41d4-a716-446655440000.yml

# 3. If corrupted, restore from backup
tar -xzf deathban-backup-1234567890.tar.gz \
    plugins/DeathBanPro/players/550e8400-e29b-41d4-a716-446655440000.yml

# 4. Verify and reload
/deathban reload
```

### Reset Specific Player

```bash
# Option 1: Command (recommended)
/deathban reset <player>
# Removes all bans and death records

# Option 2: Manual file deletion
rm plugins/DeathBanPro/players/<uuid>.yml
# Player record recreated as new on next death

# Option 3: Pardon only (remove current ban)
/deathban pardon <player>
# Keeps death history, just removes current ban
```

### Migrate from Old Version

If upgrading from older version of DeathBan Pro:

```bash
# 1. Backup old data
cp -r plugins/DeathBanPro plugins/DeathBanPro.backup

# 2. Update plugin
rm plugins/DeathBanPro.jar
cp DeathBanPro-new.jar plugins/DeathBanPro.jar

# 3. Restart server (will load old data format)
# Server will automatically convert if needed

# 4. If conversion fails:
# - Check logs for error messages
# - Restore backup: cp -r plugins/DeathBanPro.backup plugins/DeathBanPro
# - Report issue with version info and logs
```

### Data Format Changes

View current data format:

```bash
cat plugins/DeathBanPro/players/550e8400-e29b-41d4-a716-446655440000.yml
```

Example output:
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
currentBan:
  startTime: 2024-01-15T14:30:00Z
  endTime: 2024-01-15T20:30:00Z
  offenseLevel: 2
  deathCause: FALL
lastDeathTime: 2024-01-15T14:30:00Z
```

## Verification & Testing

### Test Ban Application

1. **Setup test world**
   ```yaml
   enabled-worlds:
     - test-world
   ```

2. **Create test player** (or use yourself)

3. **Trigger deaths**
   ```
   # In Creative mode, switch to Survival
   # Or use: /kill @s (as player)
   ```

4. **Verify ban**
   ```
   /deathban check <player>
   # Should show: BANNED until [time]
   ```

5. **Test rejoin**
   - Leave server
   - Try to rejoin
   - Should be kicked with ban message

### Test Shared Mode

1. **Enable shared mode**
   ```yaml
   mode: shared
   
   shared-lives:
     default-lives: 3
     max-lives: 10
     allow-teams: true
   ```

2. **Reload**
   ```
   /deathban reload
   ```

3. **Check pool**
   ```
   /deathban lives
   # Should show: Server lives: 3/10
   ```

4. **Test consuming lives**
   ```
   /kill @s  # Die as player
   # Should show: Life consumed! 2/10 remaining
   
   /kill @s  # Die again
   # Should show: Life consumed! 1/10 remaining
   
   /kill @s  # Final death
   # Should show: BANNED (pool empty)
   ```

### Test Team Pools

1. **Create team**
   ```
   /deathban team create "Team Alpha"
   ```

2. **Join team**
   ```
   /deathban team join "Team Alpha"
   ```

3. **Test team pool**
   ```
   /deathban lives
   # Should show team pool status
   ```

### Test Offset Reset

1. **Configure reset period** (set low for testing)
   ```yaml
   offense-reset:
     enabled: true
     clean-period: 1m    # 1 minute (for testing)
   ```

2. **Trigger first ban**
   - Die 3 times
   - Get banned (offense level 1)

3. **Wait 1+ minute**

4. **Die again (verify reset)**
   ```
   # If reset worked:
   /deathban check <player>
   # Offense level: 0 (reset!)
   ```

## FAQ

### Q: How do I set different ban durations for different worlds?

**A:** Currently not supported per-world. All worlds use same durations. Workaround:
- Use enabled-worlds to disable in certain worlds
- Combine with `deathban.bypass` permission for specific worlds

### Q: Can I reduce ban duration after it's applied?

**A:** Yes, use `/deathban pardon` to remove the ban immediately.

### Q: How do I backup before major config changes?

**A:** 
```bash
# Backup data
cp -r plugins/DeathBanPro plugins/DeathBanPro.backup-$(date +%s)

# Make changes to config.yml
# If something breaks, restore:
# rm -r plugins/DeathBanPro
# mv plugins/DeathBanPro.backup-12345 plugins/DeathBanPro
```

### Q: What happens if server crashes during a ban?

**A:** Ban is still applied on next startup (via pending bans recovery).

### Q: Can I exclude admins from bans?

**A:** Yes, use `deathban.bypass` permission for admin group.

### Q: How do I see who was banned and when?

**A:** Check logs for ban messages, or use `/deathban check <player>`:
```
Status: BANNED
Offense level: 2
Ban expires at: 2024-01-15T20:30:00Z
```

### Q: Rolling window not working - all deaths count?

**A:** Check config:
```yaml
rolling-window:
  enabled: true      # Must be true
  duration: 24h
  max-deaths: 3
```

If still not working, check logs for "rolling window" messages with debug enabled.

### Q: How do I test without getting actually banned?

**A:** Use test world with `deathban.bypass`:
```yaml
enabled-worlds:
  - testing-world
```

Then join with bypass permission (or give to yourself with LuckPerms).

---

**Last Updated:** Based on DeathBan Pro v1.0+  
**For Issues:** Report to GitHub issues with debug logs attached
