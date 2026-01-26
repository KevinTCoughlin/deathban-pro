# DeathBan Pro

Hardcore-style death penalties for Minecraft servers. When players die, they receive temporary bans that escalate with repeated deaths.

## Features

- **Rolling Death Window** - Deaths only count within a configurable time period (default: 24 hours)
- **Escalating Bans** - Each offense results in longer ban durations
- **Offense Reset** - Play safely for a period to reset your offense level
- **World Configuration** - Enable/disable in specific worlds
- **Full Customization** - All messages, durations, and thresholds are configurable

## Requirements

- Spigot or Paper 1.21+
- Java 21+

## Installation

1. Download the latest release
2. Place `DeathBanPro.jar` in your `plugins` folder
3. Restart your server
4. Edit `plugins/DeathBanPro/config.yml` to customize
5. Use `/deathban reload` to apply changes

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/deathban` | Show help | `deathban.use` |
| `/deathban check [player]` | Check ban status | `deathban.check` / `deathban.check.others` |
| `/deathban reset <player>` | Reset offense data | `deathban.admin` |
| `/deathban pardon <player>` | Remove active ban | `deathban.admin` |
| `/deathban reload` | Reload configuration | `deathban.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `deathban.use` | Basic plugin access | true |
| `deathban.check` | Check own status | true |
| `deathban.check.others` | Check other players | op |
| `deathban.admin` | Admin commands | op |
| `deathban.bypass` | Never get banned | op |

## Configuration

```yaml
# Rolling window - deaths within this period count toward ban
rolling-window:
  enabled: true
  duration: 24h
  max-deaths: 3

# Ban durations by offense level
ban-durations:
  1: 1h      # First offense
  2: 6h      # Second offense
  3: 24h    # Third offense
  4: 72h    # Fourth offense
  5: 168h   # Fifth+ offense

# Offense resets after playing safely for this period
offense-reset:
  enabled: true
  clean-period: 168h  # 7 days
```

## Building

```bash
./gradlew shadowJar
```

The built JAR will be in `build/libs/`.

## License

MIT License - see [LICENSE](LICENSE) for details.
