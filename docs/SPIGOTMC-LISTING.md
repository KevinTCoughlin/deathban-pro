# SpigotMC Resource Listing

**Copy-paste ready content for SpigotMC resource submission.**

---

## Resource Title

```
DeathBan Pro - Hardcore Death Penalties with Escalating Bans
```

---

## Tag Line (140 chars max)

```
Hardcore-style death bans with rolling windows, escalating penalties, and seasonal themes. Perfect for hardcore SMP servers!
```

---

## Overview (Main Description)

```bbcode
[CENTER]
[SIZE=6][B][COLOR=#c0392b]DeathBan Pro[/COLOR][/B][/SIZE]
[SIZE=4]Hardcore Death Penalties Done Right[/SIZE]

[IMG]https://i.imgur.com/PLACEHOLDER_BANNER.png[/IMG]
[/CENTER]

[HR][/HR]

[SIZE=5][B]What is DeathBan Pro?[/B][/SIZE]

DeathBan Pro brings hardcore-style consequences to your Minecraft server without the permadeath. When players die, they receive a temporary ban that escalates with repeated deaths - teaching them to play more carefully while keeping them engaged.

[B]Perfect for:[/B]
[LIST]
[*]Hardcore SMP servers
[*]Content creator challenges
[*]Seasonal events
[*]Any server wanting meaningful death consequences
[/LIST]

[HR][/HR]

[SIZE=5][B]Key Features[/B][/SIZE]

[IMG]https://i.imgur.com/PLACEHOLDER_FEATURE1.png[/IMG]

[B][COLOR=#27ae60]Rolling Death Window[/COLOR][/B]
Deaths only count within a configurable time window (default: 24 hours). Die 3 times in 24 hours? You're banned. Survive 24 hours? Your death counter resets.

[B][COLOR=#2980b9]Escalating Bans[/COLOR][/B]
First offense: 1 hour. Second: 6 hours. Third: 24 hours. Players learn quickly that repeated carelessness has consequences.

[CODE]
ban-durations:
  1: 1h      # First offense
  2: 6h      # Second offense
  3: 24h     # Third offense
  4: 72h     # Fourth offense
  5: 168h    # Fifth+ (1 week)
[/CODE]

[B][COLOR=#8e44ad]Offense Reset System[/COLOR][/B]
Play safely for 7 days (configurable) and your offense level resets. Rewards careful gameplay!

[B][COLOR=#e67e22]Full Customization[/COLOR][/B]
Every message, duration, and threshold is configurable. Support for MiniMessage (Paper) and legacy color codes (Spigot).

[HR][/HR]

[SIZE=5][B]Commands & Permissions[/B][/SIZE]

[CODE]
COMMANDS:
/deathban                    - Show help
/deathban check [player]     - Check ban status
/deathban reset <player>     - Reset player's offense data
/deathban pardon <player>    - Remove active ban
/deathban reload             - Reload configuration

PERMISSIONS:
deathban.use           - Basic access (default: true)
deathban.check         - Check own status (default: true)
deathban.check.others  - Check others (default: op)
deathban.admin         - Admin commands (default: op)
deathban.bypass        - Never get banned (default: op)
[/CODE]

[HR][/HR]

[SIZE=5][B]Installation[/B][/SIZE]

[LIST=1]
[*]Download DeathBan Pro
[*]Place in your [ICODE]plugins[/ICODE] folder
[*]Restart your server
[*]Edit [ICODE]plugins/DeathBanPro/config.yml[/ICODE]
[*]Use [ICODE]/deathban reload[/ICODE] to apply changes
[/LIST]

[B]Requirements:[/B]
[LIST]
[*]Spigot or Paper 1.21+
[*]Java 21+
[/LIST]

[HR][/HR]

[SIZE=5][B]Default Configuration[/B][/SIZE]

[SPOILER="config.yml"]
[CODE=YAML]
# DeathBan Pro Configuration

debug: false
update-check: true
metrics: true

# Ban Mode
mode: individual

# Rolling Window
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3

# Ban Durations
ban-durations:
  1: 1h
  2: 6h
  3: 24h
  4: 72h
  5: 168h

# Offense Reset
offense-reset:
  enabled: true
  clean-period: 168h

# Worlds
enabled-worlds: []
disabled-worlds:
  - world_lobby

# Bypass
bypass-permission: deathban.bypass

# Theme
theme: default
[/CODE]
[/SPOILER]

[HR][/HR]

[SIZE=5][B]Frequently Asked Questions[/B][/SIZE]

[SPOILER="FAQ"]
[B]Q: Does this work on Spigot?[/B]
A: Yes! DeathBan Pro supports both Spigot and Paper 1.21+.

[B]Q: What happens if the server crashes while banning someone?[/B]
A: Pending bans are persisted to disk immediately. On restart, any interrupted bans will be properly applied.

[B]Q: Can I disable bans in certain worlds?[/B]
A: Yes! Use the [ICODE]disabled-worlds[/ICODE] config option.

[B]Q: How do I pardon someone who was banned unfairly?[/B]
A: Use [ICODE]/deathban pardon <player>[/ICODE] - works even if they're offline.

[B]Q: Is there PlaceholderAPI support?[/B]
A: Coming in a future update!
[/SPOILER]

[HR][/HR]

[SIZE=5][B]Planned Features[/B][/SIZE]

[LIST]
[*][B]Shared Lives Mode[/B] - Pool lives across the server or teams
[*][B]Seasonal Themes[/B] - Halloween, Winter, and more visual themes
[*][B]PlaceholderAPI[/B] - Integrate with scoreboards and TAB
[*][B]Web Panel[/B] - Browser-based ban management
[/LIST]

[HR][/HR]

[SIZE=5][B]Support[/B][/SIZE]

[LIST]
[*][URL='GITHUB_URL']GitHub Issues[/URL] - Bug reports
[*][URL='DISCUSSION_URL']Discussion Thread[/URL] - Questions & suggestions
[*][URL='DISCORD_URL']Discord Server[/URL] - Community support
[/LIST]

[HR][/HR]

[CENTER]
[SIZE=3][COLOR=#7f8c8d]Made with care for the Minecraft community[/COLOR][/SIZE]
[SIZE=2]Using [URL='https://bstats.org/plugin/bukkit/DeathBanPro']bStats[/URL] for anonymous metrics (can be disabled)[/SIZE]
[/CENTER]
```

---

## Donation Description (Optional)

```
Support DeathBan Pro development! Donations help fund new features, seasonal themes, and ongoing maintenance. Thank you!
```

---

## Version Title Format

```
v1.0.0 - Initial Release
```

---

## Version Description Template

```bbcode
[SIZE=5][B]DeathBan Pro v1.0.0[/B][/SIZE]

[B]Initial Release[/B]

[SIZE=4][COLOR=#27ae60]Added[/COLOR][/SIZE]
[LIST]
[*]Individual death ban mode
[*]Rolling window system (configurable, default 24h)
[*]Escalating ban durations (5 levels)
[*]Offense reset after clean period
[*]Full command suite with tab completion
[*]bStats metrics integration
[*]Automatic update checker
[*]Theme system foundation
[/LIST]

[SIZE=4][COLOR=#e67e22]Configuration[/COLOR][/SIZE]
[LIST]
[*]Customizable ban durations per offense level
[*]Rolling window duration and death threshold
[*]World enable/disable lists
[*]Complete message customization
[/LIST]

[B]Supported Versions:[/B] Spigot/Paper 1.21+
[B]Java Version:[/B] 21+

[URL='CHANGELOG_URL']Full Changelog[/URL]
```

---

## Screenshots (Recommended)

Prepare these screenshots:
1. **Ban Screen** - The kick message when banned
2. **Return Message** - Welcome back message after ban expires
3. **Check Command** - Output of `/deathban check`
4. **Config File** - Clean config.yml preview
5. **Console Output** - Plugin startup messages

Recommended dimensions: 1920x1080 or 1280x720

---

## Icon

- Size: 256x256 PNG
- Concept: Skull with clock/timer, red/black color scheme
- No text in icon (appears too small)

---

## Native Minecraft Version

```
1.21
```

---

## Tested Minecraft Versions

```
1.21
```

---

## Source Code Link (Optional)

```
https://github.com/YOURUSERNAME/deathban-pro
```

---

## Required Dependencies

```
None
```

---

## Optional Dependencies

```
PlaceholderAPI (future support)
```

---

## Categories

Primary: `Mechanics`
Secondary: `Fun`

---

## Keywords/Tags

```
death, ban, hardcore, smp, penalty, lives, tempban, deathban, consequences
```
