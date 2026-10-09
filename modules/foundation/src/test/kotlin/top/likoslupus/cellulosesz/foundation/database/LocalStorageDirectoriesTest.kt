package top.likoslupus.cellulosesz.foundation.database

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalStorageDirectoriesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `sqlite creates the parent directory of the database file`() {
        val storageRoot = tempDir.resolve("cellulosesz")
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                type = DatabaseType.SQLITE,
                sqlite = SqliteSettings(path = "nested/cellulosesz.db"),
            ),
            storageRoot,
        )

        descriptor.ensureLocalStorageDirectories()

        assertTrue(Files.isDirectory(storageRoot.resolve("nested")))
    }

    @Test
    fun `h2 creates the parent directory of the database file`() {
        val storageRoot = tempDir.resolve("cellulosesz")
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                type = DatabaseType.H2,
                h2 = H2Settings(path = "db/cellulosesz"),
            ),
            storageRoot,
        )

        descriptor.ensureLocalStorageDirectories()

        assertTrue(Files.isDirectory(storageRoot.resolve("db")))
    }

    @Test
    fun `remote backends create no directory`() {
        val storageRoot = tempDir.resolve("cellulosesz")
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(type = DatabaseType.MYSQL),
            storageRoot,
        )

        descriptor.ensureLocalStorageDirectories()

        assertFalse(Files.exists(storageRoot))
    }

}
