# Changelog

All notable changes to DeathBan Pro will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- **reload() not propagating Settings** — Managers and listeners are now rebuilt on reload so config changes take effect immediately
- **Thread-unsafe async saves in PlayerDataManager** — Player data is now snapshotted before async disk writes to prevent ConcurrentModificationException
- **Thread-unsafe async saves in SharedLivesManager** — Pool data is now snapshotted into immutable records before async serialization
- **Instant.parse(null) crash on corrupted player files** — Null-safe parsing with graceful fallback for corrupted ban timestamps and death records
- **URLClassLoader leak in ThemeManager** — External theme classloaders are now stored and properly closed on reload
- **Tab completion performing disk I/O on main thread** — Stored/banned player names are now cached with async refresh to prevent lag spikes
- **handleLives showing stale pool state** — Pool is now queried after mutation for accurate feedback
- **lives set command bypassing SharedLivesManager** — Now uses proper `setLives()` API with logging, lastModified update, and async save
- **Expired ban cleanup never persisted** — Fixed dead code path in JoinListener so expired ban removal is saved to disk
- **getActiveBans() polluting cache** — Active bans scan no longer permanently caches every player file
- **Corrupted death records aborting entire player load** — Individual death records are now parsed defensively; one bad record no longer loses the whole file
- **Contribution UUID parsing crash** — SharedLivesManager now uses runCatching for contribution UUID parsing, consistent with member parsing
- **Out-of-order async persistence** — Player, pending-ban, and shared-lives saves are serialized and stale snapshots can no longer overwrite newer state
- **Repeated return notifications** — Welcome-back messaging is now shown only when a ban actually expires during login
- **Unsafe async Bukkit API access** — Tab-completion file scans remain asynchronous while player-name lookups run on the server thread
- **Invalid configuration values** — Unsafe modes, thresholds, pool sizes, duration values, and ban-duration levels now fail fast with clear errors
- **Unsafe team and theme IDs** — IDs are validated before use in YAML paths or theme registration
- **Theme loader cleanup** — Failed loads and plugin shutdown now close external theme classloaders reliably

### Changed

- Updated plugin.yml usage to include all subcommands (lives, team, theme)
- Added security warning for external theme JARs in Theme interface docs and README
- Release builds now embed and verify the requested version and publish only the shaded plugin JAR
- Release versions are validated as SemVer, with every accepted prerelease form published as a prerelease

### Technical

- Kotlin 2.3.21 with Java 21 target
- Spigot API 1.21+ compatibility
- 131+ unit tests covering core logic

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

- Kotlin 2.3.21 with Java 21 target
- Spigot API 1.21+ compatibility
- Shadow JAR with relocated bStats
- 100+ unit tests covering core logic

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
