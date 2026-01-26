# DeathBan Pro - Roadmap

## SpigotMC Ramp-Up Timeline

SpigotMC requires **3 free resources** before you can sell premium content.

| Week | Milestone | Actions |
|------|-----------|---------|
| 1-2 | Account Setup | Create account, enable 2FA, start forum activity |
| 3-4 | Build Presence | Contribute to discussions, help with plugin questions (target: 40 posts) |
| 5-6 | Free Resource #1 | Release SimpleAnnounce (see below) |
| 7-8 | Account Age Met | 8-week requirement fulfilled, continue posting (target: 80 posts) |
| 9-10 | Free Resource #2 | Release DeathBan Pro (base plugin) |
| 11-12 | Free Resource #3 | Release PlayerStats or SharedLives add-on |
| 13+ | Premium Eligible | Release first premium theme |

---

## Recommended Free Resources

### 1. SimpleAnnounce (Quick Win)

Simple scheduled broadcast plugin - easy to build, universally useful.

**Features:**
- Scheduled messages at configurable intervals
- Random or sequential message rotation
- Per-world messages
- Permission-based targeting

**Effort:** 1-2 days
**Purpose:** Quick resource to start the 3-resource countdown

### 2. DeathBan Pro (Main Plugin)

The core plugin as specified.

**Effort:** 1-2 weeks
**Purpose:** Your flagship product, establishes expertise

### 3. PlayerStats (Complementary)

Basic player statistics tracking.

**Features:**
- Track deaths, kills, playtime
- Leaderboards
- PlaceholderAPI support

**Effort:** 3-5 days
**Purpose:** Complements DeathBan, cross-promotion opportunity

**Alternative:** Release SharedLives as a separate free add-on.

---

## Development Phases

### Phase 1: Core Plugin (Weeks 1-2)

- [x] Project setup (Gradle, Kotlin, structure)
- [x] Configuration system (config.yml, messages.yml)
- [x] Player data persistence (YAML per-player)
- [x] Death event handling
- [x] Rolling window calculation
- [x] Ban application and enforcement
- [x] Login rejection for banned players
- [x] Basic commands (check, reset, pardon, reload)
- [x] Tab completion
- [x] bStats integration
- [x] Update checker

**Deliverable:** Fully functional individual mode

### Phase 2: Polish & Extras (Week 3)

- [x] Comprehensive testing (manual + MockBukkit)
- [x] Edge case handling (crashes, concurrent events)
- [ ] Performance optimization
- [ ] Documentation (config comments, wiki pages)
- [ ] SpigotMC listing preparation
- [ ] Screenshots and demo video

**Deliverable:** Release-ready v1.0.0

### Phase 3: Shared Lives Mode (Weeks 4-5)

- [ ] Shared lives pool system
- [ ] Team-based pools (optional)
- [ ] Life contribution/withdrawal commands
- [ ] Pool status display
- [ ] Integration with individual mode toggle

**Deliverable:** v1.1.0 with shared lives

### Phase 4: Theme System (Week 6)

- [ ] Theme interface and loader
- [ ] External JAR theme loading
- [ ] Default theme implementation
- [ ] Theme switching command
- [ ] Documentation for theme creators

**Deliverable:** v1.2.0 with theme support

### Phase 5: Premium Themes (Ongoing)

- [ ] Halloween theme ($3)
- [ ] Winter/Holiday theme ($3)
- [ ] Spring theme ($3)
- [ ] Summer theme ($3)
- [ ] Theme bundle ($10)

**Deliverable:** Premium revenue stream

---

## Feature Backlog

### High Priority
- [ ] PlaceholderAPI integration
- [ ] MySQL/SQLite storage option
- [ ] Ban history command
- [ ] Discord webhook notifications

### Medium Priority
- [ ] Web panel for ban management
- [ ] API for other plugins
- [ ] Custom death causes configuration
- [ ] Grace period for new players
- [ ] Appeal system

### Low Priority
- [ ] Bedrock support (Geyser/Floodgate)
- [ ] Bungeecord/Velocity sync
- [ ] Localization (multiple languages)
- [ ] Import from other death-ban plugins

---

## Version Milestones

### v1.0.0 - Initial Release
- Individual ban mode
- Rolling window system
- Escalating bans
- Core commands
- bStats + update checker

### v1.1.0 - Shared Lives
- Server-wide shared pool
- Team-based pools
- Life management commands

### v1.2.0 - Themes
- Theme system foundation
- External theme loading
- Default theme

### v1.3.0 - Integration
- PlaceholderAPI support
- Discord webhooks
- Database storage option

### v2.0.0 - Enterprise
- Web panel
- Plugin API
- Multi-server sync

---

## Pricing Strategy

| Product | Type | Price | Notes |
|---------|------|-------|-------|
| DeathBan Pro | Free | $0 | Core plugin, builds audience |
| Halloween Theme | Premium | $3 | Seasonal, limited appeal window |
| Winter Theme | Premium | $3 | Evergreen winter aesthetic |
| Spring Theme | Premium | $3 | Fresh, nature-inspired |
| Summer Theme | Premium | $3 | Beach/tropical vibes |
| All Seasons Bundle | Premium | $10 | 17% discount |

### Revenue Projections (Conservative)

| Month | Downloads (Free) | Theme Sales | Revenue |
|-------|------------------|-------------|---------|
| 1 | 100 | 5 | $15 |
| 3 | 500 | 25 | $75 |
| 6 | 1,500 | 75 | $225 |
| 12 | 5,000 | 250 | $750 |

---

## Success Metrics

### SpigotMC
- Download count
- Rating (target: 4.5+ stars)
- Review count
- Discussion engagement

### Technical
- Bug reports (lower is better)
- Feature requests (engagement signal)
- GitHub stars/forks

### Business
- Theme sales per month
- Bundle vs individual ratio
- Support ticket volume

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| SpigotMC rejects submission | Low | High | Follow guidelines exactly, test thoroughly |
| Competing plugin releases | Medium | Medium | Focus on unique features (rolling window + escalation) |
| Low adoption | Medium | Medium | Marketing: Reddit, Discord servers, MC forums |
| Support overwhelm | Medium | Low | Good docs, FAQ, Discord community |
| Piracy of premium themes | High | Low | Themes are low-cost impulse buys, focus on convenience |

---

## Pre-Launch Checklist

### Code
- [ ] All tests passing
- [ ] No compiler warnings
- [ ] Code > 10kb (SpigotMC requirement)
- [ ] JAR size reasonable (<1MB)
- [ ] No NMS/reflection unless necessary
- [ ] Works on Spigot AND Paper

### Documentation
- [ ] README.md
- [ ] config.yml with comments
- [ ] messages.yml with comments
- [ ] Wiki pages (optional but recommended)
- [ ] CHANGELOG.md

### SpigotMC Listing
- [ ] Title and tagline
- [ ] Full description (BBCode)
- [ ] Screenshots (min 3)
- [ ] Icon (256x256)
- [ ] Version description
- [ ] Source code link (optional)

### Marketing
- [ ] Demo video (YouTube, unlisted is fine)
- [ ] Reddit post (/r/admincraft, /r/mcservers)
- [ ] Discord server announcements
- [ ] Twitter/X post

### Account
- [ ] 8+ weeks old
- [ ] 80+ posts
- [ ] 20+ ratings given
- [ ] 2FA enabled
- [ ] 3 free resources published

---

## Contact & Support

- **SpigotMC Discussion:** Link to discussion thread
- **GitHub Issues:** Bug reports only
- **Discord:** Community questions
- **Email:** Business inquiries only
