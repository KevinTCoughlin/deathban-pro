# DeathBan Pro - Development Guide

This guide covers building from source, development setup, running tests, and extending the plugin with custom features.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building from Source](#building-from-source)
- [Running Tests](#running-tests)
- [IDE Setup](#ide-setup)
- [Project Structure](#project-structure)
- [Adding New Features](#adding-new-features)
- [Code Style & Conventions](#code-style--conventions)
- [Git Workflow & PR Process](#git-workflow--pr-process)

## Prerequisites

### Required Tools

- **Java**: 21+ (LTS recommended)
- **Git**: Latest version
- **Gradle**: 9.0+ (included as wrapper)
- **Kotlin**: 2.3+ (managed by Gradle)

### Verify Installation

```bash
# Check Java version
java -version
# Expected: java 21 LTS or later

# Check Git
git --version

# Gradle wrapper will auto-download
./gradlew --version
```

### System Requirements

- **RAM**: 4GB minimum, 8GB recommended
- **Disk Space**: 2GB for repo + dependencies
- **OS**: macOS, Linux, or Windows (Git Bash on Windows)

## Building from Source

### Basic Build

```bash
cd /path/to/deathban-pro

# Build production JAR (shadowJar includes dependencies)
./gradlew shadowJar

# Output: build/libs/DeathBanPro-*.jar
ls -lh build/libs/
```

### Build Variants

```bash
# Full build with tests and checks
./gradlew build

# Clean build (removes all generated files)
./gradlew clean shadowJar

# Build without tests (faster)
./gradlew shadowJar -x test

# Build with verbose output
./gradlew shadowJar --info
```

### Build Output

```
build/
├── libs/
│   ├── DeathBanPro-1.0.0.jar          # Production JAR (use this)
│   ├── DeathBanPro-1.0.0-all.jar      # All dependencies included
│   └── DeathBanPro-1.0.0-sources.jar  # Source attachment
├── classes/
│   └── kotlin/main/                   # Compiled Kotlin classes
└── reports/
    ├── tests/                         # Test reports (HTML)
    └── detekt/                        # Code analysis
```

### Installation After Build

```bash
# Copy to local Spigot/Paper server
cp build/libs/DeathBanPro-*.jar ~/server/plugins/

# Restart server to test
# Server output should show: "DeathBan Pro v1.0.0 enabled!"
```

### Troubleshooting Builds

**Problem: Java version error**
```
Caused by: java.lang.UnsupportedClassVersionError
```
**Solution:** Upgrade to Java 21+ or 25+
```bash
# Check which Java Gradle is using
./gradlew --version
# If wrong version, set JAVA_HOME:
export JAVA_HOME=/path/to/java21
./gradlew build
```

**Problem: Out of memory**
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution:** Increase Gradle memory
```bash
# Create gradle.properties or add to existing:
echo "org.gradle.jvmargs=-Xmx2g" >> gradle.properties
./gradlew clean build
```

**Problem: "Cannot find symbol" errors**
```
error: unresolved reference: BanRecord
```
**Solution:** Regenerate project
```bash
./gradlew clean
./gradlew build
```

## Running Tests

### Run All Tests

```bash
# Full test suite
./gradlew test

# Output includes:
# - Unit tests
# - Code coverage
# - Test report in build/reports/tests/test/
```

### Run Specific Tests

```bash
# Run a single test class
./gradlew test --tests BanManagerTest

# Run a single test method
./gradlew test --tests BanManagerTest.testBanWithEscalation

# Run tests matching pattern
./gradlew test --tests "*Manager*"
```

### Test Coverage

```bash
# Run with coverage report
./gradlew test jacocoTestReport

# View coverage: build/reports/jacoco/test/html/index.html
open build/reports/jacoco/test/html/index.html
```

### Test Reports

```bash
# View test results
open build/reports/tests/test/index.html

# Test structure:
src/test/kotlin/
├── dev/coughlin/deathban/
│   ├── manager/
│   │   ├── BanManagerTest.kt
│   │   └── OffenseManagerTest.kt
│   ├── config/
│   │   └── SettingsTest.kt
│   └── data/
│       └── PlayerDataManagerTest.kt
```

### Test Failures

**Check test output:**
```bash
./gradlew test --info 2>&1 | tail -50
```

**Common Issues:**

1. **Database/File Issues**: Tests expect clean state
   ```bash
   # Clean and retry
   ./gradlew cleanTest test
   ```

2. **Port Conflicts**: Another server running
   ```bash
   # Kill process using port (if applicable)
   lsof -i :25565
   ```

3. **Timeout**: Test takes too long
   ```bash
   # Increase timeout in build.gradle.kts:
   test {
       timeout = Duration.ofMinutes(10)
   }
   ```

## IDE Setup

### IntelliJ IDEA

#### Import Project

1. **Open IntelliJ → File → Open**
2. **Select** `/path/to/deathban-pro`
3. **Trust project** when prompted
4. **Wait** for indexing to complete (~2-3 min)

#### Configure Java

1. **File → Project Structure → Project**
2. **SDK**: Select Java 21+ (or download)
3. **Language Level**: 21 or 25+
4. **Apply → OK**

#### Configure Gradle

1. **File → Settings → Build, Execution, Deployment → Gradle**
2. **Use Gradle from**: `wrapper`
3. **Apply → OK**
4. **Gradle → Reload All Gradle Projects** (refresh icon)

#### Run Configurations

Create run config for testing:
1. **Run → Edit Configurations**
2. **+ → Gradle**
3. **Name**: `Run Tests`
4. **Tasks**: `test`
5. **OK**

Run tests: **Shift+F10** or **Run → Run 'Run Tests'**

#### Debugging

1. **Set breakpoint** (click line number)
2. **Run → Debug 'Run Tests'**
3. **Step through** (F7 to step into, F8 to step over)

### VS Code

#### Extensions

```
Install in VS Code:
- Extension Pack for Java (Microsoft)
  └─ Includes Debugger, Test Runner
- Gradle for Java (Microsoft)
- Kotlin Language (JetBrains)
```

#### Setup

1. **File → Open Folder** → select repo
2. **Command Palette** (Cmd+Shift+P) → "Java: Create Java Project"
   - Or auto-detects Gradle project
3. **Select Source Folder** if prompted

#### Build & Test

```bash
# Terminal → New Terminal (Ctrl+`)
./gradlew build
./gradlew test
```

#### Debugging

1. **Set breakpoint** (F9 on line number)
2. **Run → Start Debugging** (F5)
3. **Select Java**
4. **Select build task** when prompted

### Command Line Only

If using neither IDE:

```bash
# Full workflow
./gradlew clean build test

# Run specific task
./gradlew test --tests OffenseManagerTest

# Generate IDE configs (for compatibility)
./gradlew idea  # IntelliJ
./gradlew eclipse  # Eclipse

# Watch for changes (auto-rebuild on save)
./gradlew build -x test -t
```

## Project Structure

### Source Layout

```
src/
├── main/
│   ├── kotlin/
│   │   └── dev/coughlin/deathban/
│   │       ├── DeathBanPlugin.kt          # Main plugin class
│   │       ├── command/
│   │       │   ├── DeathBanCommand.kt     # Command handler
│   │       │   └── DeathBanTabCompleter.kt # Tab completion
│   │       ├── config/
│   │       │   ├── Settings.kt            # Config parsing
│   │       │   ├── OffenseConfig.kt       # Config interface
│   │       │   ├── BanMode.kt             # Enum: INDIVIDUAL/SHARED
│   │       │   └── Messages.kt            # Message system
│   │       ├── data/
│   │       │   ├── PlayerData.kt          # Data class
│   │       │   ├── PlayerDataManager.kt   # Persistence layer
│   │       │   ├── BanRecord.kt           # Ban data
│   │       │   ├── DeathRecord.kt         # Death data
│   │       │   ├── LocationData.kt        # Location snapshot
│   │       │   └── SharedLivesPool.kt     # Shared pool
│   │       ├── listener/
│   │       │   ├── DeathListener.kt       # Death event handler
│   │       │   └── JoinListener.kt        # Join event handler
│   │       ├── manager/
│   │       │   ├── BanManager.kt          # Ban logic
│   │       │   ├── OffenseManager.kt      # Escalation logic
│   │       │   ├── SharedLivesManager.kt  # Pool management
│   │       │   └── BanContext.kt          # Ban context
│   │       ├── theme/
│   │       │   ├── Theme.kt               # Interface
│   │       │   ├── ThemeManager.kt        # Theme engine
│   │       │   ├── DefaultTheme.kt        # Default impl
│   │       │   └── HalloweenTheme.kt      # Example theme
│   │       └── util/
│   │           ├── TimeUtil.kt            # Duration parsing
│   │           ├── ColorUtil.kt           # Color formatting
│   │           └── UpdateChecker.kt       # Update checking
│   │
│   └── resources/
│       ├── plugin.yml                     # Plugin manifest
│       ├── config.yml                     # Default config
│       └── messages.yml                   # Default messages
│
├── test/
│   └── kotlin/
│       └── dev/coughlin/deathban/
│           ├── manager/
│           │   ├── BanManagerTest.kt
│           │   └── OffenseManagerTest.kt
│           ├── config/
│           │   └── SettingsTest.kt
│           └── data/
│               └── PlayerDataManagerTest.kt
│
└── build.gradle.kts                       # Build config
```

### Configuration Files

```
plugins/DeathBanPro/
├── config.yml              # Server config
├── messages.yml            # Message templates
├── players/                # Player data (auto-created)
│   └── {uuid}.yml
├── shared-lives/           # Pool data (auto-created)
│   ├── global.yml
│   └── {team-name}.yml
└── themes/                 # Custom themes (optional)
    ├── my-theme.jar
    └── custom-effects.jar
```

## Adding New Features

### Adding a New Command

**Example: `/deathban stats` to show statistics**

#### 1. Add to DeathBanCommand

```kotlin
// File: src/main/kotlin/.../command/DeathBanCommand.kt

override fun onCommand(
    sender: CommandSender,
    cmd: Command,
    label: String,
    args: Array<String>,
): Boolean {
    if (args.isEmpty() || args[0].equals("stats", ignoreCase = true)) {
        return showStats(sender)
    }
    // ... existing commands
}

private fun showStats(sender: CommandSender): Boolean {
    if (!sender.hasPermission("deathban.admin")) {
        sender.sendMessage(Component.text("No permission").color(NamedTextColor.RED))
        return true
    }
    
    val totalBans = banManager.getTotalBansIssued()
    val activeBans = banManager.getActiveBans().size
    
    sender.sendMessage(
        Component.text("DeathBan Pro Statistics")
            .append(Component.newline())
            .append(Component.text("Total bans issued: $totalBans"))
            .append(Component.newline())
            .append(Component.text("Active bans: $activeBans"))
    )
    return true
}
```

#### 2. Add to Tab Completer

```kotlin
// File: src/main/kotlin/.../command/DeathBanTabCompleter.kt

override fun onTabComplete(
    sender: CommandSender,
    cmd: Command,
    label: String,
    args: Array<String>,
): List<String> {
    return when {
        args.size == 1 -> listOf(
            "check", "lives", "team", "theme", 
            "reset", "pardon", "reload", "stats"  // Add stats
        ).filter { it.startsWith(args[0], ignoreCase = true) }
        // ...
    }
}
```

#### 3. Test It

```bash
./gradlew shadowJar
cp build/libs/DeathBanPro-*.jar ~/server/plugins/
# Restart server
# In game: /deathban stats
```

### Creating a New Listener

**Example: Log all deaths to console**

#### 1. Create DeathLogger

```kotlin
// File: src/main/kotlin/.../listener/DeathLogger.kt

package dev.coughlin.deathban.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.Plugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DeathLogger(
    private val plugin: Plugin,
) : Listener {
    private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val time = LocalDateTime.now().format(formatter)
        val cause = player.lastDamageCause?.cause?.name ?: "UNKNOWN"
        
        plugin.logger.info("[$time] ${player.name} died to $cause")
    }
}
```

#### 2. Register in DeathBanPlugin

```kotlin
// In onEnable() method

private fun registerListeners() {
    val deathListener = DeathListener(...)
    val deathLogger = DeathLogger(this)  // Add
    
    server.pluginManager.registerEvents(deathListener, this)
    server.pluginManager.registerEvents(deathLogger, this)  // Add
}
```

#### 3. Test

```bash
./gradlew shadowJar
# Run server, watch logs as players die
```

### Adding a New Manager

**Example: StatisticsManager to track ban statistics**

#### 1. Create Manager Class

```kotlin
// File: src/main/kotlin/.../manager/StatisticsManager.kt

package dev.coughlin.deathban.manager

import dev.coughlin.deathban.data.PlayerDataManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class StatisticsManager(
    private val dataManager: PlayerDataManager,
) {
    private val totalBans = AtomicLong(0)
    private val startTime = Instant.now()
    
    fun recordBan(uuid: java.util.UUID) {
        totalBans.incrementAndGet()
    }
    
    fun getStatistics(): StatisticsData {
        val duration = Duration.between(startTime, Instant.now())
        return StatisticsData(
            totalBans = totalBans.get(),
            uptime = duration,
            averageBansPerHour = totalBans.get() / duration.toHours().coerceAtLeast(1),
        )
    }
    
    data class StatisticsData(
        val totalBans: Long,
        val uptime: Duration,
        val averageBansPerHour: Long,
    )
}
```

#### 2. Integrate in DeathBanPlugin

```kotlin
// In DeathBanPlugin.kt

lateinit var statisticsManager: StatisticsManager
    private set

override fun onEnable() {
    // ... existing code
    
    statisticsManager = StatisticsManager(dataManager)  // Add
    
    // ... rest of onEnable
}

// Update BanManager to call:
// statisticsManager.recordBan(uuid)  // After applying ban
```

### Extending Themes

**Example: Create a "Cherry Blossom" theme**

#### 1. Create Theme Class

```kotlin
// File: src/main/kotlin/.../theme/CherryBlossomTheme.kt

package dev.coughlin.deathban.theme

import org.bukkit.Particle
import org.bukkit.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class CherryBlossomTheme : Theme {
    override fun getId() = "cherry-blossom"
    override fun getName() = "Cherry Blossom"
    
    override fun getBanTitle(): Component {
        return Component.text("✿ ").color(NamedTextColor.LIGHT_PURPLE)
            .append(Component.text("You were banned").color(NamedTextColor.WHITE))
            .append(Component.text(" ✿").color(NamedTextColor.LIGHT_PURPLE))
    }
    
    override fun getBanSubtitle(duration: String): Component {
        return Component.text("Return in $duration").color(NamedTextColor.GRAY)
    }
    
    override fun getDeathSound() = Sound.ENTITY_PLAYER_LEVELUP
    
    override fun getDeathParticle() = Particle.CHERRY_LEAVES
    
    override fun getKickMessage(context: BanContext): String {
        return "§d✿ §fYou have been banned for §e${context.duration}§f §d✿"
    }
}
```

#### 2. Register Theme

```kotlin
// In ThemeManager.kt

fun loadThemes() {
    themes["cherry-blossom"] = CherryBlossomTheme()
    // ... other themes
}
```

#### 3. Use Theme

```bash
# In config.yml:
theme: cherry-blossom

# Reload:
/deathban reload
```

## Code Style & Conventions

### Kotlin Style Guide

Follow [Google's Kotlin Style Guide](https://android.github.io/kotlin-guidelines/):

#### Naming

```kotlin
// Classes: PascalCase
class BanManager { }

// Functions: camelCase
fun applyBan() { }

// Constants: UPPER_SNAKE_CASE
companion object {
    const val DEFAULT_DURATION = 3600L
}

// Private/internal: start with _
private var _cache: Map<UUID, PlayerData>? = null
```

#### Formatting

```kotlin
// 4-space indentation (never tabs)
if (condition) {
    doSomething()
}

// Line length: 100-120 characters preferred
// Break long lines:
val result = veryLongFunctionName(
    parameter1,
    parameter2,
    parameter3,
)

// Data classes: 2-3 fields per line max
data class BanRecord(
    val startTime: Instant,
    val endTime: Instant,
    val offenseLevel: Int,
    val deathCause: String,
)
```

#### Nullability

```kotlin
// Prefer non-nullable, use ? only when needed
fun loadData(uuid: UUID): PlayerData? {
    // Can return null
}

// Use ?: for defaults
val name = player?.name ?: "Unknown"

// Use !! only when 100% sure (avoid when possible)
val required = data.currentBan!! // BAD - use non-null assertion only when necessary
```

#### Comments

Only comment non-obvious logic:

```kotlin
// Good: explains WHY
// Use ConcurrentHashMap to prevent duplicate death processing
private val processingDeaths = ConcurrentHashMap.newKeySet<UUID>()

// Bad: explains WHAT (code already shows that)
// Increment the offset
offset++
```

### Error Handling

```kotlin
// Use safe calls and elvis operators
val duration = settings.getBanDuration(level) ?: Duration.ofHours(1)

// Log errors, don't silently fail
try {
    dataManager.save(data)
} catch (e: Exception) {
    plugin.logger.warning("Failed to save player data: ${e.message}")
}

// Return null/false for expected failures
fun loadData(file: File): PlayerData? {
    return if (file.exists()) {
        parseYaml(file)
    } else {
        null  // Expected, not exceptional
    }
}
```

### Testing

```kotlin
// Test structure
class BanManagerTest {
    private lateinit var banManager: BanManager
    
    @Before
    fun setup() {
        // Setup test fixtures
    }
    
    @Test
    fun `test ban escalation`() {
        // Arrange
        val player = mockPlayer()
        
        // Act
        banManager.applyBan(player, data, "FALL")
        
        // Assert
        assertTrue(data.isBanned())
    }
}
```

## Git Workflow & PR Process

### Fork & Clone

```bash
# Fork on GitHub (click Fork button)

# Clone your fork
git clone https://github.com/YOUR_USERNAME/deathban-pro.git
cd deathban-pro

# Add upstream remote
git remote add upstream https://github.com/KevinTCoughlin/deathban-pro.git

# Verify
git remote -v
# origin  https://github.com/YOUR_USERNAME/deathban-pro.git
# upstream  https://github.com/KevinTCoughlin/deathban-pro.git
```

### Create Feature Branch

```bash
# Update main
git fetch upstream
git checkout main
git merge upstream/main

# Create branch
git checkout -b feature/add-new-command
# or: git switch -c feature/add-new-command

# Naming convention:
# - feature/...   (new feature)
# - fix/...       (bug fix)
# - docs/...      (documentation)
# - refactor/...  (code improvement)
```

### Development Workflow

```bash
# Make changes
vim src/main/kotlin/.../MyClass.kt

# Test locally
./gradlew test

# Commit (atomic commits, good messages)
git add .
git commit -m "Add new ban command with validation"
# Good commit message:
# - Imperative mood: "add" not "added"
# - First line: 50 chars max
# - Body: explain WHY, not WHAT

# Push to your fork
git push origin feature/add-new-command
```

### Create Pull Request

1. **Go to GitHub**: https://github.com/YOUR_USERNAME/deathban-pro
2. **Click "Compare & pull request"**
3. **Fill PR template:**
   - Title: Concise description
   - Description: What changed and why
   - Tests: How to test
   - Related Issues: Closes #123
4. **Submit**

### PR Review Process

- Maintainer reviews changes
- May request modifications
- Address feedback with new commits (don't force push)
- CI/CD runs tests automatically

### Merge & Cleanup

```bash
# After merge on GitHub

# Update local main
git fetch upstream
git checkout main
git merge upstream/main

# Delete branch
git branch -d feature/add-new-command
git push origin --delete feature/add-new-command
```

### Common Git Commands

```bash
# View status
git status

# View changes
git diff                    # Unstaged changes
git diff --staged           # Staged changes
git diff upstream/main      # Compare to main

# Undo changes
git checkout -- file.kt     # Discard changes
git restore file.kt         # Alternative to checkout
git reset HEAD file.kt      # Unstage file

# Interactive rebase (clean up commits)
git rebase -i upstream/main

# Squash commits
git rebase -i HEAD~3        # Last 3 commits

# Fetch latest without merge
git fetch --all
git log --oneline main..upstream/main
```

---

**Last Updated:** Based on DeathBan Pro v1.0+  
**Kotlin Version:** 1.9.22+  
**Build Tool:** Gradle 8.0+  
**Min Java:** 21 LTS or 25+
