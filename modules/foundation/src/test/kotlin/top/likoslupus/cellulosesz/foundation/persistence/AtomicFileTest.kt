package top.likoslupus.cellulosesz.foundation.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AtomicFileTest {

    @TempDir
    lateinit var directory: Path

    @Test
    fun `writes a new file with the exact content`() {
        val target = directory.resolve("data").resolve("value.txt")

        AtomicFile.writeUtf8AtomicallyBlocking(target, "hello")

        assertEquals("hello", Files.readString(target))
    }

    @Test
    fun `overwrites an existing file and leaves no temp file behind`() {
        val target = directory.resolve("value.txt")
        AtomicFile.writeUtf8AtomicallyBlocking(target, "first")
        AtomicFile.writeUtf8AtomicallyBlocking(target, "second")

        assertEquals("second", Files.readString(target))
        assertFalse(Files.exists(directory.resolve("value.txt.tmp")))
    }

}
