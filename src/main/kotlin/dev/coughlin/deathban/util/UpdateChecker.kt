package dev.coughlin.deathban.util

import org.bukkit.plugin.Plugin
import java.net.URI

class UpdateChecker(
    private val plugin: Plugin,
    private val resourceId: Int,
    private val onUpdateFound: (currentVersion: String, latestVersion: String) -> Unit,
) {
    fun checkForUpdates() {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    val latestVersion = fetchLatestVersion()
                    val currentVersion = plugin.description.version.removeSuffix("-SNAPSHOT")

                    if (isNewerVersion(latestVersion, currentVersion)) {
                        plugin.server.scheduler.runTask(
                            plugin,
                            Runnable {
                                onUpdateFound(currentVersion, latestVersion)
                            },
                        )
                    } else {
                        plugin.logger.info("Running latest version ($currentVersion)")
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to check for updates: ${e.message}")
                }
            },
        )
    }

    private fun fetchLatestVersion(): String {
        val url = URI("https://api.spigotmc.org/legacy/update.php?resource=$resourceId").toURL()
        return url
            .openConnection()
            .apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "DeathBanPro/${plugin.description.version}")
            }.getInputStream()
            .bufferedReader()
            .readText()
            .trim()
    }

    private fun isNewerVersion(
        latest: String,
        current: String,
    ): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
