package top.likoslupus.cellulosesz.foundation.database.driver

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

class DriverStoreTest {

    @TempDir
    lateinit var tempDir: Path

    private val payload = ByteArray(4096) { (it % 251).toByte() }

    @Test
    fun `a verified copy is returned as-is`() {
        val store = DriverStore(tempDir)
        val artifact = artifact(payload)

        store.install(artifact, payload.inputStream())

        val path = store.verified(artifact)
        assertEquals(store.expectedPath(artifact), path)
        assertArrayEquals(payload, Files.readAllBytes(path!!))
    }

    @Test
    fun `a tampered copy is quarantined instead of being used or deleted`() {
        val store = DriverStore(tempDir)
        val artifact = artifact(payload)
        store.install(artifact, payload.inputStream())
        Files.write(store.expectedPath(artifact), ByteArray(payload.size))

        assertNull(store.verified(artifact))
        val quarantined = Files.list(tempDir).use { stream ->
            stream.filter { it.fileName.toString().contains(".corrupt-") }.toList()
        }
        assertTrue(
            quarantined.isNotEmpty(),
            "the bad copy should be kept for inspection"
        )
    }

    @Test
    fun `a short download is rejected and leaves no usable jar`() {
        val store = DriverStore(tempDir)
        val artifact = artifact(payload)

        assertThrows(StorageException::class.java) {
            store.install(
                artifact,
                payload.copyOf(payload.size / 2).inputStream()
            )
        }
        assertNull(store.verified(artifact))
        assertTrue(Files.notExists(store.expectedPath(artifact)))
    }

    @Test
    fun `a wrong digest is rejected even at the right size`() {
        val store = DriverStore(tempDir)
        val artifact = artifact(payload)

        assertThrows(StorageException::class.java) {
            store.install(
                artifact,
                payload.copyOf().also { it[0] = (it[0] + 1).toByte() }.inputStream()
            )
        }
        assertTrue(Files.notExists(store.expectedPath(artifact)))
    }

    private fun artifact(bytes: ByteArray): DriverArtifact =
        DriverArtifact(
            coordinate = "org.example:driver:1.0",
            fileName = "driver-1.0.jar",
            size = bytes.size.toLong(),
            sha256 = HexFormat.of().formatHex(
                MessageDigest
                        .getInstance("SHA-256")
                        .digest(bytes)
            ),
            repositoryPath = "org/example/driver/1.0/driver-1.0.jar",
        )

}
