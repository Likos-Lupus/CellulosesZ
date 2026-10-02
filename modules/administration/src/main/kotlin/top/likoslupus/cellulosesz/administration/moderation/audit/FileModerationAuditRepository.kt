package top.likoslupus.cellulosesz.administration.moderation.audit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Writes one immutable audit record per file under `moderation/audit/<utc-day>/`. A crash cannot
 * corrupt other records and backups are trivial; there is no append lock and no large array rewrite.
 */
internal class FileModerationAuditRepository(
    private val dataRoot: () -> Path,
) : ModerationAuditRepository {

    override suspend fun append(entry: ModerationAuditFile) =
        withContext(Dispatchers.IO) {
            val day = LocalDate.ofInstant(
                Instant.ofEpochMilli(entry.occurredAtEpochMillis),
                ZoneOffset.UTC,
            )
            val directory = dataRoot()
                    .resolve("moderation")
                    .resolve("audit")
                    .resolve(day.toString())
            val file = directory.resolve("${entry.occurredAtEpochMillis}-${entry.id}.json")
            AtomicFile.writeUtf8AtomicallyBlocking(
                file,
                StorageJson.format.encodeToString(
                    ModerationAuditFile.serializer(),
                    entry
                ),
            )
        }

}
