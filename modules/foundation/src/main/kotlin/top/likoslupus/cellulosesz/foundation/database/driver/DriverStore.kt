package top.likoslupus.cellulosesz.foundation.database.driver

import top.likoslupus.cellulosesz.foundation.database.StorageException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.*

/**
 * The on-disk driver cache. A jar is only ever handed out after its size and SHA-256 match the
 * manifest; a file that fails verification is quarantined (never silently deleted) and the caller
 * may install a verified copy in its place.
 */
public class DriverStore(
    public val directory: Path,
) {

    public fun expectedPath(artifact: DriverArtifact): Path =
        directory.resolve(artifact.fileName)

    /** The verified local copy, or `null` when it is absent or had to be quarantined. */
    public fun verified(artifact: DriverArtifact): Path? {
        val path = expectedPath(artifact)
        if (!Files.isRegularFile(path)) {
            return null
        }
        if (matches(artifact, path)) {
            return path
        }
        quarantine(path)
        return null
    }

    /**
     * Streams [content] into place. The bytes are written to a side file, verified, and only then
     * moved over [expectedPath], so an interrupted transfer can never look like a usable driver.
     */
    public fun install(artifact: DriverArtifact, content: InputStream) {
        Files.createDirectories(directory)
        val target = expectedPath(artifact)
        val temporary = directory.resolve("${artifact.fileName}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            Files.deleteIfExists(temporary)
            var size = 0L
            Files.newOutputStream(
                temporary,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            ).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = content.read(buffer)
                    if (read < 0) {
                        break
                    }
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    size += read
                }
            }
            if (size != artifact.size) {
                throw StorageException(
                    "downloaded ${artifact.coordinate} is $size bytes but the manifest pins ${artifact.size}"
                )
            }
            val actual = HexFormat.of().formatHex(digest.digest())
            if (!actual.equals(artifact.sha256, ignoreCase = true)) {
                throw StorageException(
                    "downloaded ${artifact.coordinate} has SHA-256 $actual but the manifest pins ${artifact.sha256}"
                )
            }
            move(temporary, target)
        } catch (exception: Throwable) {
            runCatching { Files.deleteIfExists(temporary) }
            throw exception
        }
    }

    private fun matches(artifact: DriverArtifact, path: Path): Boolean {
        if (runCatching { Files.size(path) }.getOrDefault(-1L) != artifact.size) {
            return false
        }
        val actual = runCatching { sha256Of(path) }.getOrNull() ?: return false
        return actual.equals(artifact.sha256, ignoreCase = true)
    }

    /** Keeps a bad copy around for inspection instead of deleting a file the operator may own. */
    private fun quarantine(path: Path) {
        val target = directory.resolve("${path.fileName}.corrupt-${System.currentTimeMillis()}")
        runCatching { move(path, target) }
    }

    private fun move(source: Path, target: Path) {
        runCatching { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE) }
                .recoverCatching {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
                .getOrThrow()
    }

    private fun sha256Of(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return HexFormat.of().formatHex(digest.digest())
    }

}
