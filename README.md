# DeathBan Pro

[![CI](https://github.com/KevinTCoughlin/deathban-pro/actions/workflows/ci.yml/badge.svg)](https://github.com/KevinTCoughlin/deathban-pro/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/KevinTCoughlin/deathban-pro/branch/main/graph/badge.svg)](https://codecov.io/gh/KevinTCoughlin/deathban-pro)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-brightgreen)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://adoptium.net/)

Hardcore-style death penalties for Minecraft servers. When players die, they receive temporary bans that escalate with repeated deaths.

🌐 **[View Landing Page](https://kevintcoughlin.com/deathban-pro/)**

## Features

- **Two Modes**: Individual (per-player tracking) or Shared (pooled server/team lives)
- **Rolling Death Window** - Deaths only count within a configurable time period (default: 24 hours)
- **Escalating Bans** - Each offense results in longer ban durations
- **Shared Lives** - Server or team shares a pool of lives; when empty, deaths trigger bans
- **Team Pools** - Create team-based life pools for group play
- **Offense Reset** - Play safely for a period to reset your offense level
- **World Configuration** - Enable/disable in specific worlds
- **Full Customization** - All messages, durations, and thresholds are configurable

## Requirements

- Spigot or Paper 1.21+
- Java 21+

## Installation

1. Download a JAR from [GitHub Releases](https://github.com/KevinTCoughlin/deathban-pro/releases) (currently prerelease builds; test on a staging server first)
2. Place `DeathBanPro.jar` in your `plugins` folder
3. Restart your server
4. Edit `plugins/DeathBanPro/config.yml` to customize
5. Use `/deathban reload` to apply changes

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/deathban` | Show help | `deathban.use` |
| `/deathban check [player]` | Check ban status | `deathban.check` / `deathban.check.others` |
| `/deathban lives` | Check shared lives pool | `deathban.use` |
| `/deathban lives add` | Add a life to the pool | `deathban.lives.add` |
| `/deathban lives set <n>` | Set pool lives (admin) | `deathban.admin` |
| `/deathban team create <name>` | Create a team pool | `deathban.team.manage` |
| `/deathban team join <name>` | Join a team pool | `deathban.team.manage` |
| `/deathban team leave` | Leave your team pool | `deathban.team.manage` |
| `/deathban reset <player>` | Reset offense data | `deathban.admin` |
| `/deathban pardon <player>` | Remove active ban | `deathban.admin` |
| `/deathban reload` | Reload configuration | `deathban.admin` |

Team names must be 1–32 characters and may contain lowercase letters, numbers, underscores, and hyphens.

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `deathban.use` | Basic plugin access | true |
| `deathban.check` | Check own status | true |
| `deathban.check.others` | Check other players | op |
| `deathban.admin` | Admin commands | op |
| `deathban.lives.add` | Refill shared life pools | op |
| `deathban.team.manage` | Create teams or change membership | op |
| `deathban.bypass` | Never get banned | op |

### Shared-life permission migration

Refilling lives and creating, joining, or leaving teams now require dedicated permissions in addition to `deathban.use`. They default to operators to prevent ordinary players from bypassing scarcity by refilling lives or moving to fresh pools. Grant `deathban.lives.add` and `deathban.team.manage` only to trusted players. Granting them broadly restores the previous unrestricted behavior; there is no earned-life cost or team-switch cooldown yet.

## Configuration

```yaml
# Mode: "individual" or "shared"
mode: individual

# Shared Lives (when mode: shared)
shared-lives:
  default-lives: 10
  max-lives: 20
  allow-teams: true
  empty-pool-ban: 1h

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

## Themes

DeathBan Pro includes a theme system for customizing the visual and audio experience:

| Theme | Description |
|-------|-------------|
| `default` | Standard red/gray color scheme |
| `halloween` | Spooky theme with skulls, soul particles, and wither sounds |

### Theme Commands

| Command | Description |
|---------|-------------|
| `/deathban theme list` | List available themes |
| `/deathban theme set <id>` | Change the active theme (admin) |
| `/deathban theme preview <id>` | Preview a theme |

### Custom Themes

Create custom themes by implementing the `Theme` interface and packaging as a JAR. Place theme JARs in `plugins/DeathBanPro/themes/`.

> ⚠️ **Security Warning:** Theme JARs execute arbitrary code with the same permissions as the server process. Only install theme JARs from sources you trust.

## Documentation

- **[API Reference](docs/API_REFERENCE.md)** - Complete API documentation with code examples for DeathListener, BanManager, OffenseManager, PlayerDataManager, and SharedLivesManager
- **[Architecture Guide](docs/ARCHITECTURE.md)** - System design, component interactions, data flows, and concurrency model
- **[Development Guide](docs/DEVELOPMENT_GUIDE.md)** - Building from source, running tests, IDE setup, and extending the plugin
- **[Troubleshooting Guide](docs/TROUBLESHOOTING.md)** - Common issues, debug logging, performance tuning, and data recovery
- **[Configuration Examples](docs/CONFIGURATION_EXAMPLES.md)** - Real-world examples for hardcore mode, survival, PvP, teams, events, and multi-world setups

## Building

```bash
./gradlew shadowJar
```

The built JAR will be in `build/libs/`.

See the [Development Guide](docs/DEVELOPMENT_GUIDE.md) for detailed build instructions and troubleshooting.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup and guidelines, or check the [Development Guide](docs/DEVELOPMENT_GUIDE.md) for comprehensive information on:

- Building and testing
- IDE setup (IntelliJ, VS Code)
- Adding new features
- Code conventions

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

## License

MIT License - see [LICENSE](LICENSE) for details.
