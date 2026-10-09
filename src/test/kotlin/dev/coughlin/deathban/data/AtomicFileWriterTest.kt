package dev.coughlin.deathban.data

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AtomicFileWriterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `replacement preserves complete UTF-8 content without leftover temporary files`() {
        val target = File(tempDir, "nested/player.yml")
        AtomicFileWriter.write(target, "previous")
        val content = "player: café\n".repeat(10000)

        AtomicFileWriter.write(target, content)

        assertEquals(content, target.readText())
        assertEquals(listOf("player.yml"), target.parentFile.list()!!.toList())
    }

    @Test
    fun `failed replacement preserves target and cleans temporary file`() {
        val target = File(tempDir, "player.yml")
        target.mkdir()
        File(target, "sentinel").writeText("preserve")

        assertFailsWith<IOException> { AtomicFileWriter.write(target, "replacement") }

        assertEquals("preserve", File(target, "sentinel").readText())
        assertTrue(tempDir.list()!!.contentEquals(arrayOf("player.yml")))
    }
}
