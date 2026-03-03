package dev.coughlin.deathban.theme

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.net.URLClassLoader
import java.util.logging.Logger

/**
 * Manages theme loading, switching, and retrieval.
 */
class ThemeManager(
    private val dataFolder: File,
    private val logger: Logger,
) {
    private val themesFolder = File(dataFolder, "themes")
    private val themes = mutableMapOf<String, Theme>()
    private var activeThemeId: String = "default"

    init {
        themesFolder.mkdirs()
        registerBuiltInThemes()
        loadExternalThemes()
    }

    private fun registerBuiltInThemes() {
        register(DefaultTheme())
        register(HalloweenTheme())
        logger.info("Registered ${themes.size} built-in themes")
    }

    private fun loadExternalThemes() {
        val jarFiles = themesFolder.listFiles { f -> f.extension == "jar" } ?: return

        for (jar in jarFiles) {
            try {
                loadThemeJar(jar)
            } catch (e: Exception) {
                logger.warning("Failed to load theme from ${jar.name}: ${e.message}")
            }
        }

        if (jarFiles.isNotEmpty()) {
            logger.info("Loaded ${jarFiles.size} external theme(s)")
        }
    }

    private fun loadThemeJar(jar: File) {
        val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), javaClass.classLoader)

        // Read theme.yml from JAR
        val themeYmlStream =
            classLoader.getResourceAsStream("theme.yml")
                ?: throw IllegalStateException("theme.yml not found in ${jar.name}")

        val config = YamlConfiguration.loadConfiguration(InputStreamReader(themeYmlStream))

        val mainClass =
            config.getString("main")
                ?: throw IllegalStateException("'main' not specified in theme.yml")

        val themeClass = classLoader.loadClass(mainClass)
        val theme = themeClass.getDeclaredConstructor().newInstance() as Theme

        // Validate theme matches theme.yml
        val declaredId = config.getString("id")
        if (declaredId != null && declaredId != theme.id) {
            logger.warning("Theme ID mismatch in ${jar.name}: yml says '$declaredId', class says '${theme.id}'")
        }

        register(theme)
        theme.onLoad()
        logger.info("Loaded external theme: ${theme.name} v${theme.version} by ${theme.author}")
    }

    fun register(theme: Theme) {
        val existing = themes[theme.id]
        if (existing != null) {
            logger.warning("Theme '${theme.id}' already registered, replacing")
            existing.onUnload()
        }
        themes[theme.id] = theme
    }

    fun unregister(id: String): Boolean {
        val theme = themes.remove(id) ?: return false
        theme.onUnload()
        return true
    }

    fun getTheme(id: String): Theme? = themes[id]

    fun getActiveTheme(): Theme = themes[activeThemeId] ?: themes["default"]!!

    fun setActiveTheme(id: String): Boolean {
        if (!themes.containsKey(id)) return false
        activeThemeId = id
        logger.info("Active theme set to: $id")
        return true
    }

    fun getActiveThemeId(): String = activeThemeId

    fun getAllThemes(): List<Theme> = themes.values.toList()

    fun getAvailableThemeIds(): List<String> = themes.keys.toList()

    fun reload() {
        // Unload external themes
        themes.values
            .filter { it.id !in listOf("default", "halloween") }
            .forEach { it.onUnload() }

        themes.clear()
        registerBuiltInThemes()
        loadExternalThemes()
    }
}
