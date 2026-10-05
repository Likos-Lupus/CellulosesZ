package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

/**
 * Per-player kit claim files under `utility/kit-claims/<uuid>.json`. A corrupt file is never
 * overwritten; only that player's kit claims become unavailable.
 */
internal class FileKitClaimRepository(
    private val dataRoot: () -> Path,
) : KitClaimRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun load(playerId: UUID): Map<KitName, KitClaim> =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                readFile(playerId)?.toDomain()
                    ?: emptyMap()
            }
        }

    override suspend fun reserve(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    ) =
        locks.withLock(player.id) {
            withContext(Dispatchers.IO) {
                upsert(
                    player,
                    kit,
                    KitClaimStatus.RESERVED,
                    at
                )
            }
        }

    override suspend fun markDelivered(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    ) =
        locks.withLock(player.id) {
            withContext(Dispatchers.IO) {
                upsert(
                    player,
                    kit,
                    KitClaimStatus.DELIVERED,
                    at
                )
            }
        }

    override suspend fun reset(
        playerId: UUID,
        name: KitName,
    ): Boolean =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                val current = readFile(playerId)
                    ?: return@withContext false
                if (!current.claims.containsKey(name.value))
                    return@withContext false

                writeFile(
                    KitClaimsFile(
                        KIT_CLAIMS_SCHEMA_VERSION,
                        current.playerId,
                        current.playerName,
                        current.claims - name.value,
                    )
                )

                true
            }
        }

    private fun upsert(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        status: KitClaimStatus,
        at: Instant,
    ) {
        val current = readFile(player.id)
        val claims = (current?.claims ?: emptyMap()) +
                (kit.name.value to KitClaimRecord(
                    kitId = kit.id.value.toString(),
                    claimedAtEpochMillis = at.toEpochMilli(),
                    status = status,
                ))
        if (claims.size > MAX_STORED_CLAIMS_PER_PLAYER) {
            throw KitClaimDataException("claim history exceeds the stored cap for ${player.id}")
        }

        writeFile(
            KitClaimsFile(
                KIT_CLAIMS_SCHEMA_VERSION,
                player.id.toString(),
                player.name,
                claims,
            )
        )
    }

    private fun readFile(playerId: UUID): KitClaimsFile? {
        val file = path(playerId)
        if (!Files.exists(file)) {
            return null
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw KitClaimDataException("unable to read kit claims for $playerId", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(
                KitClaimsFile.serializer(),
                text
            )
        } catch (exception: Exception) {
            throw KitClaimDataException("corrupt kit claims for $playerId", exception)
        }

        validate(file, playerId, decoded)
        return decoded
    }

    private fun validate(
        file: Path,
        expectedId: UUID,
        decoded: KitClaimsFile
    ) {
        if (decoded.schemaVersion != KIT_CLAIMS_SCHEMA_VERSION) {
            throw KitClaimDataException("unsupported kit claims schema ${decoded.schemaVersion} in $file")
        }
        if (decoded.playerId != expectedId.toString()) {
            throw KitClaimDataException("kit claims file name does not match its playerId: $file")
        }
        if (decoded.playerName.isBlank()) {
            throw KitClaimDataException("kit claims file has a blank player name: $file")
        }
        if (decoded.claims.size > MAX_STORED_CLAIMS_PER_PLAYER) {
            throw KitClaimDataException("kit claims file exceeds the stored claim cap: $file")
        }

        decoded.claims.forEach { (rawName, record) ->
            KitName.parse(rawName)
                ?: throw KitClaimDataException("kit claims file has an invalid kit name '$rawName': $file")
            try {
                UUID.fromString(record.kitId)
            } catch (_: IllegalArgumentException) {
                throw KitClaimDataException("kit claims file has an invalid kit id: $file")
            }

            if (record.claimedAtEpochMillis <= 0L) {
                throw KitClaimDataException("kit claims file has an invalid claim timestamp: $file")
            }
        }
    }

    private fun writeFile(document: KitClaimsFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            path(UUID.fromString(document.playerId)),
            StorageJson.format.encodeToString(
                KitClaimsFile.serializer(),
                document
            ),
        )
    }

    private fun KitClaimsFile.toDomain(): Map<KitName, KitClaim> =
        claims.mapNotNull { (rawName, record) ->
            val name = KitName.parse(rawName)
                ?: return@mapNotNull null

            name to KitClaim(
                kitId = UUID.fromString(record.kitId),
                claimedAt = Instant.ofEpochMilli(record.claimedAtEpochMillis),
                status = record.status,
            )
        }.toMap()

    private fun directory(): Path =
        dataRoot()
                .resolve("kit-claims")

    private fun path(playerId: UUID): Path =
        directory()
                .resolve("$playerId.json")

}
