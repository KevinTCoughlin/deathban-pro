# Changelog

All notable changes to DeathBan Pro will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0-beta.1] - 2025-01-25

### Added
- **Theme System**
  - Extensible theme interface for customizing visuals and audio
  - Built-in `default` theme with standard red/gray styling
  - Built-in `halloween` theme with spooky effects (skulls, soul particles, wither sounds)
  - External theme loading from JAR files in `plugins/DeathBanPro/themes/`
  - Theme commands: `/deathban theme list|set|preview`
  - Configurable sound and particle toggles

- **Shared Lives Mode**
  - Server-wide shared lives pool
  - Team-based pools with create/join/leave commands
  - Configurable default and max lives
  - Pool status display and management commands

- **Core Features**
  - Individual mode with rolling death window
  - Escalating ban durations (5 levels by default)
  - Offense reset after clean play period
  - World filtering (enable/disable specific worlds)
  - Full command suite with tab completion
  - bStats metrics integration
  - SpigotMC update checker
  - Crash recovery with pending bans persistence

- **Repository**
  - GitHub Actions CI/CD workflows
  - Issue and PR templates
  - Dependabot configuration
  - Comprehensive documentation

### Technical
- Kotlin 1.9.22 with Java 21 target
- Spigot API 1.21.1 compatibility
- Shadow JAR with relocated bStats
- 47 unit tests covering core logic

## [1.2.0-alpha.1] - 2025-01-25

### Added
- Theme system with DefaultTheme and HalloweenTheme
- Theme commands and tab completion
- Sound and particle effects integration

## [1.1.0-alpha.1] - 2025-01-25

### Added
- Shared lives mode with global and team pools
- Life management commands
- SharedLivesPool persistence

## [1.0.0-alpha.2] - 2025-01-25

### Added
- Unit test suite (TimeUtil, OffenseManager, PlayerData, SharedLivesPool)
- OffenseConfig interface for testability

### Fixed
- Shadow plugin compatibility with Java 21

## [1.0.0-alpha.1] - 2025-01-25

### Added
- Initial implementation
- Individual ban mode with rolling window
- Basic commands (check, reset, pardon, reload)
- Player data persistence
- bStats and update checker

[Unreleased]: https://github.com/KevinTCoughlin/deathban-pro/compare/v1.0.0-beta.1...HEAD
[1.0.0-beta.1]: https://github.com/KevinTCoughlin/deathban-pro/compare/v1.2.0-alpha.1...v1.0.0-beta.1
[1.2.0-alpha.1]: https://github.com/KevinTCoughlin/deathban-pro/compare/v1.1.0-alpha.1...v1.2.0-alpha.1
[1.1.0-alpha.1]: https://github.com/KevinTCoughlin/deathban-pro/compare/v1.0.0-alpha.2...v1.1.0-alpha.1
[1.0.0-alpha.2]: https://github.com/KevinTCoughlin/deathban-pro/compare/v1.0.0-alpha.1...v1.0.0-alpha.2
[1.0.0-alpha.1]: https://github.com/KevinTCoughlin/deathban-pro/releases/tag/v1.0.0-alpha.1
