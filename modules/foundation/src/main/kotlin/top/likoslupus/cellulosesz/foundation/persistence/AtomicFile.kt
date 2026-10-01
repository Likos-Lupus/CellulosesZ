package top.likoslupus.cellulosesz.foundation.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.*

/**
 * Crash-safe UTF-8 file writes: write a sibling temp file, force it to disk, then atomically
 * replace the target (falling back to a plain replace when the platform cannot do atomic moves).
 */
public object AtomicFile {

    public suspend fun writeUtf8Atomically(path: Path, content: String) {
        withContext(Dispatchers.IO) {
            writeUtf8AtomicallyBlocking(path, content)
        }
    }

    public fun writeUtf8AtomicallyBlocking(path: Path, content: String) {
        val parent = path.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }

        val temp = if (parent != null) {
            parent.resolve("${path.fileName}.tmp")
        } else {
            Path.of("${path.fileName}.tmp")
        }

        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        Files.newByteChannel(
            temp,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
        ).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }

            if (channel is FileChannel) {
                channel.force(true)
            }
        }

        try {
            Files.move(
                temp,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temp,
                path,
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

}
