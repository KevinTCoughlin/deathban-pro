package dev.coughlin.deathban.packaging

import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.time.Duration
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `java 25 loads the real plugin and java 21 would load the guard`() {
        val jar = File(System.getProperty("deathban.shadowJar"))
        JarFile(jar).use { manifest ->
            assertEquals("true", manifest.manifest.mainAttributes.getValue("Multi-Release"))
        }
        ZipFile(jar).use { zip ->
            val guard = classBytes(zip, "dev/coughlin/deathban/DeathBanPlugin.class")
            assertEquals(65, classMajor(guard))
            assertTrue(String(guard, Charsets.ISO_8859_1).contains("requires Java 25 and Paper 26.3"))
            val real = classBytes(zip, "META-INF/versions/25/dev/coughlin/deathban/DeathBanPlugin.class")
            assertEquals(69, classMajor(real))
        }
        assertEquals(65, classMajor(versionedPlugin(jar, "21")))
        assertEquals(69, classMajor(versionedPlugin(jar, "25")))
    }

    private fun versionedPlugin(
        jar: File,
        javaFeature: String,
    ): ByteArray =
        JarFile(jar, true, ZipFile.OPEN_READ, Runtime.Version.parse(javaFeature)).use { versioned ->
            val entry = versioned.getJarEntry("dev/coughlin/deathban/DeathBanPlugin.class")
            versioned.getInputStream(entry).readBytes()
        }

    private fun classBytes(
        zip: ZipFile,
        name: String,
    ): ByteArray = zip.getInputStream(zip.getEntry(name)).readBytes()

    private fun classMajor(bytes: ByteArray): Int = (bytes[6].toInt() and 0xff shl 8) or (bytes[7].toInt() and 0xff)
}
