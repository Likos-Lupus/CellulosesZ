package top.likoslupus.cellulosesz.communication.mail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Per-owner mailbox files under `communication/mail/<uuid>.json`. A corrupt file is never
 * overwritten; reads and sends fail safely instead of resetting the mailbox to empty.
 */
internal class FileMailboxRepository(
    private val dataRoot: () -> Path,
) : MailboxRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun load(ownerId: UUID): Mailbox? =
        locks.withLock(ownerId) {
            withContext(Dispatchers.IO) {
                val file = path(ownerId)
                if (!Files.exists(file)) {
                    return@withContext null
                }

                val text = try {
                    Files.readString(file)
                } catch (exception: IOException) {
                    throw MailboxDataException(
                        "unable to read mailbox for $ownerId",
                        exception,
                    )
                }

                val decoded = try {
                    StorageJson.format.decodeFromString(
                        MailboxFile.serializer(),
                        text,
                    )
                } catch (exception: Exception) {
                    throw MailboxDataException(
                        "corrupt mailbox for $ownerId",
                        exception,
                    )
                }

                validate(file, ownerId, decoded)
                decoded.toDomain()
            }
        }

    override suspend fun save(mailbox: Mailbox) =
        locks.withLock(mailbox.ownerId) {
            withContext(Dispatchers.IO) {
                AtomicFile.writeUtf8AtomicallyBlocking(
                    path(mailbox.ownerId),
                    StorageJson.format.encodeToString(
                        MailboxFile.serializer(),
                        mailbox.toFile(),
                    ),
                )
            }
        }

    override suspend fun delete(ownerId: UUID): Boolean =
        locks.withLock(ownerId) {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(path(ownerId))
            }
        }

    private fun validate(
        file: Path,
        expectedId: UUID,
        decoded: MailboxFile,
    ) {
        if (decoded.schemaVersion != MAILBOX_SCHEMA_VERSION) {
            throw MailboxDataException(
                "unsupported mailbox schema ${decoded.schemaVersion} in $file"
            )
        }
        if (decoded.ownerId != expectedId.toString()) {
            throw MailboxDataException(
                "mailbox file name does not match its ownerId: $file"
            )
        }
        if (decoded.ownerName.isBlank()) {
            throw MailboxDataException(
                "mailbox file has a blank owner name: $file"
            )
        }
        if (decoded.messages.size > MAX_STORED_MAIL_MESSAGES) {
            throw MailboxDataException(
                "mailbox exceeds the stored message cap: $file"
            )
        }

        val ids = HashSet<String>()
        decoded.messages.forEach {
            if (!ids.add(it.id)) {
                throw MailboxDataException(
                    "mailbox contains a duplicate message id '${it.id}': $file"
                )
            }
            if (it.sentAtEpochMillis <= 0) {
                throw MailboxDataException(
                    "mailbox message has an invalid sent timestamp: $file"
                )
            }
            val expires = it.expiresAtEpochMillis
            if (expires != null
                && expires <= it.sentAtEpochMillis
            ) {
                throw MailboxDataException(
                    "mailbox message expiry is not after its send time: $file"
                )
            }
            val read = it.readAtEpochMillis
            if (read != null
                && read < it.sentAtEpochMillis
            ) {
                throw MailboxDataException(
                    "mailbox message read time precedes its send time: $file"
                )
            }
        }
    }

    private fun directory(): Path =
        dataRoot()
                .resolve("mail")

    private fun path(ownerId: UUID): Path =
        directory()
                .resolve("$ownerId.json")

}
