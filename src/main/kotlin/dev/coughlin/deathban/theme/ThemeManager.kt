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
    private val externalClassLoaders = mutableListOf<URLClassLoader>()
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
        try {
            val config =
                classLoader
                    .getResourceAsStream("theme.yml")
                    ?.use { stream ->
                        InputStreamReader(stream).use(YamlConfiguration::loadConfiguration)
                    }
                    ?: throw IllegalStateException("theme.yml not found in ${jar.name}")

            val mainClass =
                config.getString("main")
                    ?: throw IllegalStateException("'main' not specified in theme.yml")

            val themeClass = classLoader.loadClass(mainClass)
            val theme = themeClass.getDeclaredConstructor().newInstance() as Theme
            require(THEME_ID_PATTERN.matches(theme.id)) {
                "Theme ID '${theme.id}' must use 1-32 lowercase letters, numbers, underscores, or hyphens"
            }
            require(!themes.containsKey(theme.id)) {
                "Theme ID '${theme.id}' is already registered"
            }

            val declaredId = config.getString("id")
            if (declaredId != null && declaredId != theme.id) {
                logger.warning("Theme ID mismatch in ${jar.name}: yml says '$declaredId', class says '${theme.id}'")
            }

            theme.onLoad()
            register(theme)
            externalClassLoaders.add(classLoader)
            logger.info("Loaded external theme: ${theme.name} v${theme.version} by ${theme.author}")
        } catch (e: Exception) {
            runCatching { classLoader.close() }
            throw e
        }
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
        closeExternalThemes()
        themes.clear()
        registerBuiltInThemes()
        loadExternalThemes()
    }

    fun close() {
        closeExternalThemes()
    }

    private fun closeExternalThemes() {
        // Unload external themes
        themes.values
            .filter { it.id !in listOf("default", "halloween") }
            .forEach { it.onUnload() }

        // Close external classloaders to release file handles
        externalClassLoaders.forEach { cl ->
            runCatching { cl.close() }.onFailure { e ->
                logger.warning("Failed to close theme classloader: ${e.message}")
            }
        }
        externalClassLoaders.clear()
    }

    companion object {
        private val THEME_ID_PATTERN = Regex("[a-z0-9_-]{1,32}")
    }
}
