package dev.coughlin.deathban.theme

import org.bukkit.Particle
import org.bukkit.Sound

/**
 * Theme interface for customizing DeathBan Pro's visual and audio feedback.
 *
 * To create a custom theme:
 * 1. Implement this interface
 * 2. Package as a JAR with theme.yml in root:
 *    ```yaml
 *    id: my-theme
 *    name: My Theme
 *    version: 1.0.0
 *    author: YourName
 *    main: com.example.MyTheme
 *    ```
 * 3. Place JAR in plugins/DeathBanPro/themes/
 *
 * **Security Warning:** Theme JARs execute arbitrary code with the same permissions
 * as the server process. Only install theme JARs from sources you trust. A malicious
 * theme could access the filesystem, network, or server internals.
 */
interface Theme {
    /** Unique identifier for this theme (lowercase, no spaces) */
    val id: String

    /** Display name shown in commands and logs */
    val name: String

    /** Theme author */
    val author: String

    /** Theme version */
    val version: String

    // Ban screen customization
    fun getBanTitle(): String

    fun getBanSubtitle(duration: String): String

    fun getKickMessage(context: BanContext): String

    // Return screen customization
    fun getReturnTitle(): String

    fun getReturnSubtitle(): String

    fun getReturnMessage(): String

    // Warning messages
    fun getWarningFinalLife(): String

    fun getWarningNearLimit(remaining: Int): String

    // Optional: Sound effects (null = no sound)
    fun getDeathSound(): Sound? = null

    fun getReturnSound(): Sound? = null

    // Optional: Particle effects (null = no particles)
    fun getDeathParticle(): Particle? = null

    fun getReturnParticle(): Particle? = null

    // Optional: Called when theme is loaded
    fun onLoad() {}

    // Optional: Called when theme is unloaded
    fun onUnload() {}
}

/**
 * Context passed to theme for kick message generation.
 */
data class BanContext(
    val playerName: String,
    val duration: String,
    val offenseLevel: Int,
    val deathCause: String,
    val returnTime: String,
    val isSharedMode: Boolean = false,
    val poolLives: Int = 0,
    val poolMax: Int = 0,
)
