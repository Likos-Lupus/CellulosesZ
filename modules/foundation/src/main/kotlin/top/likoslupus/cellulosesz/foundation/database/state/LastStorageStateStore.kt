package top.likoslupus.cellulosesz.foundation.database.state

import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * Sidecar persistence for [LastStorageState]. Content is machine state (not user config), so it is
 * written atomically and, where supported, with owner-only permissions.
 */
public class LastStorageStateStore(
    private val directory: Path,
) {

    private val file: Path = directory.resolve("last-successful.json")

    public fun read(): LastStorageState? =
        when {
            !Files.exists(file) -> null
            else -> try {
                StorageJson.format.decodeFromString(
                    LastStorageState.serializer(),
                    Files.readString(file)
                )
            } catch (_: IOException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    public fun write(state: LastStorageState) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            file,
            StorageJson.format.encodeToString(
                LastStorageState.serializer(),
                state
            ),
        )
        restrictPermissions()
    }

    private fun restrictPermissions() {
        val permissions = runCatching {
            Files.getPosixFilePermissions(file)
        }.getOrNull()
            ?: return
        runCatching {
            Files.setPosixFilePermissions(
                file,
                permissions + setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                ),
            )
        }
    }

}
