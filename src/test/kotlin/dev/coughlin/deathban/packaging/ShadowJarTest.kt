package dev.coughlin.deathban.packaging

import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.time.Duration
import kotlin.test.assertEquals

class ShadowJarTest {
    @Test
    fun `shaded jar provides runtime helpers without test classpath Kotlin`() {
        val jar = File(System.getProperty("deathban.shadowJar"))
        URLClassLoader(arrayOf(jar.toURI().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
            val files = loader.loadClass("kotlin.io.FilesKt")
            val extension = files.getMethod("getExtension", File::class.java).apply { isAccessible = true }
            assertEquals("yml", extension.invoke(null, File("player.yml")))
            loader.loadClass("kotlin.io.CloseableKt")
            loader.loadClass("kotlin.sequences.Sequence")
            val timeUtil = loader.loadClass("dev.coughlin.deathban.util.TimeUtil")
            assertEquals(
                Duration.ofHours(24),
                timeUtil.getMethod("parseDuration", String::class.java).invoke(timeUtil.getField("INSTANCE").get(null), "24h"),
            )
        }
    }
}
