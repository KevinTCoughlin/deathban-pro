package dev.coughlin.deathban;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * JAR-root copy of the plugin main class, compiled for Java 21.
 * Java 25 loads the real implementation from {@code META-INF/versions/25} instead.
 * This class must not reference any other DeathBan Pro class: those are Java 25 bytecode.
 */
public final class DeathBanPlugin extends JavaPlugin {
    static final String UNSUPPORTED_JVM_MESSAGE =
            "DeathBan Pro requires Java 25 and Paper 26.3. This server is Java %d. "
                    + "Java 21 and Minecraft 1.21 servers should use the previous release.";

    @Override
    public void onEnable() {
        int feature = Runtime.version().feature();
        getLogger().severe(String.format(UNSUPPORTED_JVM_MESSAGE, feature));
        getServer().getPluginManager().disablePlugin(this);
    }
}
